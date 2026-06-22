/**
 * Smoke test — verify all endpoints are reachable and return expected status.
 * Run: k6 run smoke.js
 * Run against staging: k6 run -e BASE_URL=http://staging:8080 smoke.js
 *
 * Pre-conditions:
 *   member 1001 has wallet + VA00001001 + balance >= 1_000_000
 *   member 2001 has wallet + balance >= 100_000
 *   member 3001 has wallet + balance >= 1_000_000
 */
import http from 'k6/http';
import { check, group } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

function hdr(ref) {
  return { 'Content-Type': 'application/json', 'X-Idempotency-Key': ref };
}

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    checks: ['rate==1.0'],
  },
};

// unique suffix per run so smoke is safe to re-run without idempotency conflicts
const RUN = Math.random().toString(36).slice(2, 8);

export default function () {
  group('health', () => {
    const r = http.get(`${BASE_URL}/health`);
    check(r, { 'health 200': res => res.status === 200 });
  });

  group('deposit notify', () => {
    const ref = `smoke-dep-${RUN}`;
    const r = http.post(`${BASE_URL}/v1/deposits/notify`, JSON.stringify({
      virtualAccount: 'VA00001001',
      grossAmount: '100000',
      bankRef: 'BANK-SMOKE-1',
      businessRef: ref,
      currency: 'VND',
      notifiedAt: new Date().toISOString(),
    }), { headers: hdr(ref) });
    check(r, { 'deposit 202': res => res.status === 202 });
  });

  group('payment', () => {
    const ref = `smoke-pay-${RUN}`;
    const r = http.post(`${BASE_URL}/v1/payments`, JSON.stringify({
      businessRef: ref,
      memberId: 1001,
      merchantId: 2001,
      amount: '50000',
      currency: 'VND',
      netToMerchant: '50000',  // must equal amount (v1: no fee split)
    }), { headers: hdr(ref) });
    check(r, { 'payment 2xx': res => res.status >= 200 && res.status < 300 });
  });

  group('transfer', () => {
    const ref = `smoke-tx-${RUN}`;
    const r = http.post(`${BASE_URL}/v1/transfers`, JSON.stringify({
      businessRef: ref,
      fromMemberId: 1001,
      toMemberId: 1051,
      amount: '10000',
      currency: 'VND',
      // feeAmount omitted — parseAmount rejects '0'; omit for zero-fee transfer
    }), { headers: hdr(ref) });
    check(r, { 'transfer 2xx': res => res.status >= 200 && res.status < 300 });
  });

  group('withdrawal 3-phase', () => {
    const ref = `smoke-wd-${RUN}`;

    const accept = http.post(`${BASE_URL}/v1/withdrawals`, JSON.stringify({
      businessRef: ref,
      memberId: 2001,
      amount: '20000',
      currency: 'VND',
      useFreeze: true,
    }), { headers: hdr(ref) });

    const acceptOk = check(accept, { 'withdraw accept 2xx': res => res.status >= 200 && res.status < 300 });
    if (!acceptOk) return;

    const coaTransId = JSON.parse(accept.body).data.coaTransId;

    const settle = http.post(`${BASE_URL}/v1/withdrawals/settle`, JSON.stringify({
      businessRef: ref,
      memberId: 2001,
      coaTransId: coaTransId,
      principal: '20000',
    }), { headers: hdr(`${ref}:settle`) });
    check(settle, { 'withdraw settle 2xx': res => res.status >= 200 && res.status < 300 });
  });

  group('ibft 3-phase', () => {
    const ref = `smoke-ibft-${RUN}`;

    const accept = http.post(`${BASE_URL}/v1/ibft`, JSON.stringify({
      businessRef: ref,
      memberId: 3001,
      principalAmount: '500000',
      platformFee: '5000',
      napasCost: '1100',
      currency: 'VND',
      destinationBankAccountNumber: '1234567890',
      destinationBankCode: 'VCB',
    }), { headers: hdr(ref) });

    const acceptOk = check(accept, { 'ibft accept 2xx': res => res.status >= 200 && res.status < 300 });
    if (!acceptOk) return;

    const coaTransId = JSON.parse(accept.body).data.coaTransId;

    const settle = http.post(`${BASE_URL}/v1/ibft/settle`, JSON.stringify({
      businessRef: ref,
      memberId: 3001,
      coaTransId: coaTransId,
      principal: '500000',
      platformFee: '5000',
      napasCost: '1100',
    }), { headers: hdr(`${ref}:settle`) });
    check(settle, { 'ibft settle 2xx': res => res.status >= 200 && res.status < 300 });
  });
}
