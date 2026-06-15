// GtelPay orchestrator (BFF) — thin Go service.
// Exposes S1 public HTTP; sequences the two cores over HTTP:
//   - core.wallet   via wallet-internal  (default :8082)
//   - core.accounting via accounting S2  (default :8081)
// No domain logic here — only IO, validation, fee/idempotency propagation, saga ordering.
package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"
)

// ---------- config ----------

func env(k, def string) string {
	if v := os.Getenv(k); v != "" {
		return v
	}
	return def
}

var (
	walletURL     = env("WALLET_URL", "http://localhost:8082/v1")
	accountingURL = env("ACCOUNTING_URL", "http://localhost:8081/v1")
	listenAddr    = ":" + env("PORT", "8080")
	httpClient    = &http.Client{Timeout: 5 * time.Second}
)

// ---------- wire types ----------

type apiResponse struct {
	Code      int    `json:"code"`
	Message   string `json:"message"`
	Data      any    `json:"data,omitempty"`
	Timestamp string `json:"timestamp"`
}

type paymentRequest struct {
	BusinessRef   string `json:"businessRef"`
	MemberID      int64  `json:"memberId"`
	MerchantID    int64  `json:"merchantId"`
	Amount        string `json:"amount"`
	Currency      string `json:"currency"`
	NetToMerchant string `json:"netToMerchant"`
}

type walletMutation struct {
	MemberID   int64  `json:"memberId"`
	WalletType string `json:"walletType"`
	Currency   string `json:"currency"`
	Amount     string `json:"amount"`
	BusinessRef string `json:"businessRef"`
	TxType     string `json:"txType"`
	CoaTransID *int64 `json:"coaTransId,omitempty"`
	UseCase    string `json:"useCase,omitempty"`
}

type walletTxResult struct {
	WalletTxID int64  `json:"walletTxId"`
	WalletID   int64  `json:"walletId"`
	Available  string `json:"available"`
	Frozen     string `json:"frozen"`
}

type journalLine struct {
	AccountCode string `json:"account_code"`
	Amount      string `json:"amount"`
	Side        string `json:"side"`
	Currency    string `json:"currency"`
}

type journalHeader struct {
	ID     int64  `json:"id"`
	Status string `json:"status"`
}

type postResult struct {
	ID     int64  `json:"id"`
	Status string `json:"status"`
}

type paymentResult struct {
	BusinessRef string `json:"businessRef"`
	WalletTxID  int64  `json:"walletTxId"`
	CoaTransID  int64  `json:"coaTransId"`
	Status      string `json:"status"`
}

// ---------- errors ----------

type apiErr struct {
	status int
	code   int
	msg    string
}

func (e *apiErr) Error() string { return e.msg }
func badRequest(msg string) *apiErr { return &apiErr{http.StatusBadRequest, 1001, msg} }

// ---------- main ----------

func main() {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /health", func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, http.StatusOK, map[string]string{"status": "UP"})
	})
	mux.HandleFunc("GET /v1/wallets/balance", handleBalance)
	mux.HandleFunc("POST /v1/payments", handlePayment)

	log.Printf("orchestrator listening on %s (wallet=%s accounting=%s)", listenAddr, walletURL, accountingURL)
	log.Fatal(http.ListenAndServe(listenAddr, mux))
}

// ---------- handlers ----------

func handleBalance(w http.ResponseWriter, r *http.Request) {
	memberID, err := requireMemberID(r)
	if err != nil {
		writeErr(w, err)
		return
	}
	q := r.URL.Query()
	var view map[string]any
	if e := getJSON(walletURL+"/wallets/balance?memberId="+strconv.FormatInt(memberID, 10)+
		"&walletType="+q.Get("walletType")+"&currency="+q.Get("currency"), &view); e != nil {
		writeErr(w, e)
		return
	}
	writeOK(w, view)
}

func handlePayment(w http.ResponseWriter, r *http.Request) {
	req, err := readPayment(r)
	if err != nil {
		writeErr(w, err)
		return
	}
	if e := validatePayment(req, r.Header.Get("X-Idempotency-Key")); e != nil {
		writeErr(w, e)
		return
	}
	res, e := runPaymentSaga(req)
	if e != nil {
		writeErr(w, e)
		return
	}
	writeOK(w, res)
}

// ---------- saga: wallet debit -> ledger POSTED -> wallet credit (ADR-027) ----------

func runPaymentSaga(req paymentRequest) (*paymentResult, error) {
	cur := strings.ToUpper(req.Currency)
	net := req.NetToMerchant
	if strings.TrimSpace(net) == "" {
		net = req.Amount
	}

	// 1. provision both wallets (idempotent)
	if e := call("POST", walletURL+"/wallets/provision",
		map[string]any{"memberId": req.MemberID, "walletType": "USER", "currency": cur}, nil); e != nil {
		return nil, e
	}
	if e := call("POST", walletURL+"/wallets/provision",
		map[string]any{"memberId": req.MerchantID, "walletType": "MERCHANT", "currency": cur}, nil); e != nil {
		return nil, e
	}

	// 2. wallet debit (USER, gross)
	var debit walletTxResult
	if e := call("POST", walletURL+"/wallets/debit", walletMutation{
		MemberID: req.MemberID, WalletType: "USER", Currency: cur, Amount: req.Amount,
		BusinessRef: req.BusinessRef, TxType: "PAYMENT_DEBIT", UseCase: "PAYMENT",
	}, &debit); e != nil {
		return nil, e
	}

	// 3. accounting journal -> lines -> POSTED.
	// Compensation (ADR-008): if posting fails after the debit committed, the user is
	// already debited — credit them back with {businessRef}:comp so no money is stranded.
	var posted postResult
	if e := postPaymentLedger(req, cur, net, &posted); e != nil {
		compensateDebit(req, cur)
		return nil, e
	}

	// 4. wallet credit (MERCHANT, net) correlated to the posted journal.
	// Forward-retry (ADR-008): the ledger is POSTED and must NOT be reversed; retry the
	// downstream credit (idempotent on businessRef). If still failing, leave it for the
	// aging job — ledger stands, money is owed to the merchant, not lost.
	creditErr := retry(3, func() error {
		return call("POST", walletURL+"/wallets/credit", walletMutation{
			MemberID: req.MerchantID, WalletType: "MERCHANT", Currency: cur, Amount: net,
			BusinessRef: req.BusinessRef, TxType: "PAYMENT_CREDIT", CoaTransID: &posted.ID, UseCase: "PAYMENT",
		}, nil)
	})
	if creditErr != nil {
		return nil, &apiErr{http.StatusBadGateway, 1004,
			"merchant credit failed after ledger POSTED; ledger stands, credit retry pending: " + creditErr.Error()}
	}

	return &paymentResult{BusinessRef: req.BusinessRef, WalletTxID: debit.WalletTxID, CoaTransID: posted.ID, Status: "SUCCESS"}, nil
}

func postPaymentLedger(req paymentRequest, cur, net string, posted *postResult) error {
	var jh journalHeader
	if e := call("POST", accountingURL+"/journal-entries", map[string]any{
		"reference_id": req.BusinessRef, "use_case": "PAYMENT", "description": "wallet payment",
	}, &jh); e != nil {
		return e
	}
	lines := map[string]any{"lines": []journalLine{
		{"2110", req.Amount, "DEBIT", cur},
		{"3500", req.Amount, "CREDIT", cur},
		{"3500", req.Amount, "DEBIT", cur},
		{"2120", net, "CREDIT", cur},
	}}
	if e := call("POST", fmt.Sprintf("%s/journal-entries/%d/lines", accountingURL, jh.ID), lines, nil); e != nil {
		return e
	}
	return call("POST", fmt.Sprintf("%s/journal-entries/%d/post", accountingURL, jh.ID), nil, posted)
}

// compensateDebit credits the user back (idempotent on {businessRef}:comp) when posting
// fails after the debit. Best-effort: if it also fails, log for ops — do not mask the
// original error.
func compensateDebit(req paymentRequest, cur string) {
	e := call("POST", walletURL+"/wallets/credit", walletMutation{
		MemberID: req.MemberID, WalletType: "USER", Currency: cur, Amount: req.Amount,
		BusinessRef: req.BusinessRef + ":comp", TxType: "ADJUSTMENT_CREDIT", UseCase: "PAYMENT_COMP",
	}, nil)
	if e != nil {
		log.Printf("COMPENSATION FAILED businessRef=%s: %v (manual ops required)", req.BusinessRef, e)
	}
}

func retry(attempts int, fn func() error) error {
	var err error
	for i := 0; i < attempts; i++ {
		if err = fn(); err == nil {
			return nil
		}
		time.Sleep(time.Duration(i+1) * 100 * time.Millisecond)
	}
	return err
}

// ---------- validation ----------

func validatePayment(req paymentRequest, idemKey string) error {
	if strings.TrimSpace(req.BusinessRef) == "" {
		return badRequest("businessRef required")
	}
	if idemKey != "" && idemKey != req.BusinessRef {
		return badRequest("X-Idempotency-Key must match businessRef")
	}
	if req.MemberID <= 0 || req.MerchantID <= 0 {
		return badRequest("memberId and merchantId must be positive")
	}
	if req.MemberID == req.MerchantID {
		return badRequest("memberId and merchantId must differ")
	}
	if strings.TrimSpace(req.Amount) == "" {
		return badRequest("amount required")
	}
	if len(strings.TrimSpace(req.Currency)) != 3 {
		return badRequest("currency must be a 3-letter code")
	}
	net := req.NetToMerchant
	if strings.TrimSpace(net) != "" && net != req.Amount {
		return badRequest("v1 requires netToMerchant equals amount")
	}
	return nil
}

// ---------- IO helpers ----------

func readPayment(r *http.Request) (paymentRequest, error) {
	var req paymentRequest
	ct := r.Header.Get("Content-Type")
	if strings.Contains(ct, "application/x-www-form-urlencoded") {
		if err := r.ParseForm(); err != nil {
			return req, badRequest("invalid form body")
		}
		mid, err := strconv.ParseInt(strings.TrimSpace(r.FormValue("memberId")), 10, 64)
		if err != nil {
			return req, badRequest("memberId must be a number")
		}
		merch, err := strconv.ParseInt(strings.TrimSpace(r.FormValue("merchantId")), 10, 64)
		if err != nil {
			return req, badRequest("merchantId must be a number")
		}
		return paymentRequest{
			BusinessRef: r.FormValue("businessRef"), MemberID: mid, MerchantID: merch,
			Amount: r.FormValue("amount"), Currency: r.FormValue("currency"), NetToMerchant: r.FormValue("netToMerchant"),
		}, nil
	}
	body, _ := io.ReadAll(r.Body)
	if err := json.Unmarshal(body, &req); err != nil {
		return req, badRequest("invalid json body")
	}
	return req, nil
}

func requireMemberID(r *http.Request) (int64, error) {
	raw := strings.TrimSpace(r.Header.Get("X-Member-Id"))
	if raw == "" {
		return 0, badRequest("missing X-Member-Id (dev auth stub)")
	}
	id, err := strconv.ParseInt(raw, 10, 64)
	if err != nil {
		return 0, badRequest("invalid X-Member-Id")
	}
	return id, nil
}

// call does an HTTP request to a core; out may be nil. Non-2xx propagates the core's status.
func call(method, url string, body any, out any) error {
	var rdr io.Reader
	if body != nil {
		b, _ := json.Marshal(body)
		rdr = bytes.NewReader(b)
	}
	httpReq, _ := http.NewRequest(method, url, rdr)
	if body != nil {
		httpReq.Header.Set("Content-Type", "application/json")
	}
	resp, err := httpClient.Do(httpReq)
	if err != nil {
		return &apiErr{http.StatusBadGateway, 1004, "core unreachable: " + err.Error()}
	}
	defer resp.Body.Close()
	data, _ := io.ReadAll(resp.Body)
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		var e apiResponse
		_ = json.Unmarshal(data, &e)
		msg := e.Message
		if msg == "" {
			msg = string(data)
		}
		return &apiErr{resp.StatusCode, e.Code, msg}
	}
	if out != nil && len(data) > 0 {
		return json.Unmarshal(data, out)
	}
	return nil
}

func getJSON(url string, out any) error { return call("GET", url, nil, out) }

func writeOK(w http.ResponseWriter, data any) {
	writeJSON(w, http.StatusOK, apiResponse{Code: 0, Message: "OK", Data: data, Timestamp: time.Now().UTC().Format(time.RFC3339)})
}

func writeErr(w http.ResponseWriter, err error) {
	if ae, ok := err.(*apiErr); ok {
		writeJSON(w, ae.status, apiResponse{Code: ae.code, Message: ae.msg, Timestamp: time.Now().UTC().Format(time.RFC3339)})
		return
	}
	writeJSON(w, http.StatusInternalServerError, apiResponse{Code: 5000, Message: err.Error(), Timestamp: time.Now().UTC().Format(time.RFC3339)})
}

func writeJSON(w http.ResponseWriter, status int, body any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(body)
}
