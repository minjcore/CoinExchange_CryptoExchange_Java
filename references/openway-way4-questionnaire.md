# OpenWay WAY4 — NIVC Questionnaire Summary

Source: `NIVC OpenWay - Questionare.pdf` (project root, 20 pages)  
Format: Black = NIVC questions, Red/orange = OpenWay responses.

---

## I. Kỹ thuật / Technical

### 1. Thông tin chung (General Technical Info)

#### Modules đề xuất

| Module | Giải pháp WAY4 | Ghi chú |
|---|---|---|
| Payment Engine | WAY4 Transaction Switch + Acquiring + Gateway/eCommerce | Core |
| Bank & Payment Network Connector | WAY4 TS với rule-based routing | Core |
| Wallet Core | WAY4 (account/contract/product/balance/ledger) | Core |
| Accounting Core | WAY4 core | Core |
| Merchant Management (MMS) | Libra Merchant Portal | Core |
| Quản lý thẻ (Card Module) | WAY4 Issuing + Acquiring | Nice to have |
| Promotion / Loyalty | Libra + WAY4 Loyalty | Nice to have |

#### Kiến trúc

- **Mô hình:** Microservice + SOA
- **Hạ tầng:** On-premise (Physical), VMware/Hyper-V, Cloud-native (AWS/Azure/GCP) — hỗ trợ cả 3
- **Containerization:** Docker (Kubernetes không được đề cập)
- **Database:** Oracle DB (centralized)
- **Cross-platform:** Hỗ trợ Linux/Unix; vận hành trên nhiều OS, hạ tầng, môi trường khác nhau
- **Tài liệu:** Cung cấp đầy đủ

#### Product Management

- Back-office portal, parameter-driven (không cần thay đổi code)
- RBAC (Role-Based Access Control)
- Maker-checker / 4-eyes approval

#### Connection Pool

Cấu hình linh hoạt tùy theo: deployment model, HA/Cluster config, hạ tầng, TPS dự kiến, kiến trúc tích hợp của Ngân hàng.

#### Scalability

- Scale-up và scale-out đều được hỗ trợ
- Thời gian scale khi tải đột biến: phụ thuộc vào quy trình khách hàng

---

### 2. High Availability (HA)

#### SLA

Thống nhất với Ngân hàng/Khách hàng trong quá trình thiết kế kiến trúc và ký hợp đồng, theo tiêu chuẩn NHNN.

#### HA Models

Hỗ trợ linh hoạt: **Active-Active**, **Active-Passive**, **Cluster** — tùy yêu cầu vận hành và hạ tầng.

#### Zero-Downtime Deployment

- HA/Cluster mode: hỗ trợ **rolling deployment** cho một số thành phần → giảm thiểu downtime
- Nâng cấp lớn (version, schema changes, core processing logic): **có thể cần maintenance window**
- ZDD phụ thuộc vào: phạm vi bản vá, kiến trúc HA/DR, thiết kế DB, yêu cầu rollback, quy trình vận hành Ngân hàng

#### Config Management

- **Tập trung** qua DB + system config tables
- Quản lý: business params, routing, authorization, product setup, risk rules, integration params
- **Hot-reload:** nhiều tham số hỗ trợ dynamic config, không cần restart toàn bộ hệ thống
- Hot-reload phụ thuộc vào từng module và loại cấu hình cụ thể

---

### 2b. Disaster Recovery (DR)

#### RTO / RPO

Đáp ứng theo tiêu chuẩn Ngân hàng Nhà nước.

#### DR Models

Hỗ trợ tất cả: **Active-Passive** (DR standby), **Active-Standby** (DR có data nhưng không nhận tải), **Active-Active** (cả hai site nhận tải).

#### DB Sync

Hỗ trợ Real-time và Asynchronous. Cơ chế thực tế phụ thuộc vào:
- DB solution (Oracle, PostgreSQL, MS SQL)
- Kiến trúc HA/DR
- RPO/RTO yêu cầu
- Băng thông giữa các site
- Chiến lược vận hành Ngân hàng

#### DC → DR Failover

- Hỗ trợ: **Automated**, **Semi-automated**, **Manual** — tùy kiến trúc và mức độ tự động hóa
- Failover thực hiện đồng bộ tại: Network/DNS-GSLB → Application → Database

#### In-flight Transactions khi Failover

WAY4 đảm bảo tính toàn vẹn dữ liệu thông qua:
- Transaction status, authorization documents, message logs, reconciliation process
- Giao dịch chưa hoàn tất: xác định qua transaction status + authorization documents + message logs
- WAY4 có thể retry, reversal, advice tùy trạng thái thực tế và quy tắc payment scheme

#### Chống trùng lặp sau DR switch

WAY4 dùng: unique transaction identity, transaction reference, authorization document, processing status trong DB. Giao dịch đã ghi nhận sẽ được kế thừa theo dữ liệu được sync sang DR.

#### DR Drill

Hệ thống cho phép diễn tập DR định kỳ **mà không ảnh hưởng đến DC production**.

#### Failover & Backup

- Auto failover: HA + tự động hoặc bán tự động tùy kiến trúc
- Backup strategy: Full, Differential, Transaction Logs — theo chuẩn tài chính/ngân hàng; tùy DB solution và chính sách vận hành

---

### 3. Data Consistency & Reconciliation

#### Clearing & Settlement / Dispute Resolution

Hỗ trợ độ tự động hóa cao qua: centralized processing, workflow, rule-based processing.

#### Hot-spot Account

Hỗ trợ tất cả các kỹ thuật: **batch update**, **shadow/sub-accounts**, **caching** — tùy mô hình kinh doanh.

#### Distributed Consistency

WAY4 dùng **Centralized Database** architecture → không cần 2PC / Saga / Outbox pattern.

Compensating transactions:
- Lỗi **nội bộ WAY4**: có cơ chế xử lý tự động để hoàn tiền
- Lỗi **ngoài WAY4** (IPS, trung gian thanh toán): phụ thuộc quy trình thanh quyết toán với đối tác

#### Replication Lag

N/A — mô hình Centralized Database, không dùng Read-Replica.

#### Isolation Levels & Race Conditions

Oracle DB handles natively — không bị ảnh hưởng bởi Read Committed / Repeatable Read / Serializable config. Oracle cũng xử lý Optimistic/Pessimistic locking natively.

#### Idempotency

- Hệ thống có đủ trường thông tin để phân tách giao dịch, tránh trùng lặp
- **Request ID / Correlation ID** được lưu trữ và kiểm tra tại **tất cả các tầng** (API Gateway, Middleware, Core DB)

#### Data Bloat & Indexing

- Hỗ trợ **Oracle partitioning**
- **WAY4 Housekeeping module** cho data archiving
- Data archiving theo cơ chế Oracle + WAY4 housekeeping (không ảnh hưởng DB hiện hành)

---

### 4. Integration Capabilities

#### Protocols hỗ trợ

| Protocol | Use case |
|---|---|
| **ISO 8583** | Card networks, switch, NAPAS, Visa, Mastercard, ATM/POS |
| **RESTful API / JSON** | E-wallet, mobile banking, digital channels, eKYC, CRM, LOS/LMS, merchant portal |
| **SOAP / XML Web Service** | Enterprise / legacy systems |
| **File-based / Batch** | CSV, XML, fixed-length — clearing, settlement, reconciliation, reporting |
| **Message Queue / JMS / TCP / HTTPS** | Real-time / near-real-time giữa các hệ thống |
| **WebSocket / gRPC** | Qua integration layer / middleware / API gateway của Ngân hàng |

#### Async Integration

Hỗ trợ async integration; hoạt động với các Message Broker/Middleware phổ biến (Kafka, RabbitMQ, v.v.) tùy kiến trúc Ngân hàng.

#### Integration Security

Hỗ trợ: **OAuth2.0**, **mTLS**, **JWT**, **IP Whitelisting**, **Digital Signature** — tùy mô hình tích hợp và yêu cầu bảo mật.

#### API Gateway

Tích hợp và vận hành phía sau **API Gateway / ESB** của Ngân hàng để quản lý tập trung.

#### External Service Coupling

Hỗ trợ: timeout, retry, fallback, async processing — giảm thiểu cascading failure khi external service (SMS OTP, eKYC) gặp sự cố.

---

### 5. Retry, In-doubt Transactions, Failover

#### Idempotency / Transaction ID

Hệ thống quản lý Transaction ID / Reference Number chặt chẽ — tránh double-spending khi Retry.

#### Retry Policy

Hỗ trợ cơ chế **Store & Forward (SAF)**.

#### Timeout Handling

Tùy trường hợp giao dịch: reversal tự động hoặc đánh dấu in-doubt để kiểm soát (đối soát cuối ngày).

#### In-doubt Transaction Polling

**WAY4 Scheduler module** — background job chủ động polling trạng thái giao dịch bị rớt.

#### Circuit Breaker

Hệ thống có cơ chế **chủ động ngắt kết nối tạm thời** để giải phóng resource (thread/connection pool).

#### Fallback Routing

Hỗ trợ — ví dụ SMS Gateway A fail → tự động chuyển sang SMS Gateway B.

---

### 6. Observability

#### Centralized Logging

- Log lưu trữ per-application
- Nếu dùng **WAY4 Health Monitoring**: có thể tích hợp với centralized log management system
- Retention: real-time + tùy yêu cầu khách hàng
- **Log masking**: tuân thủ **PCI SSF** — che dấu tự động dữ liệu nhạy cảm (PAN, PIN, CVV) trước khi ghi log

#### Distributed Tracing

Tất cả giao dịch đi qua WAY4 Applications đều được trace với **condition ID xuyên suốt**. Hỗ trợ Jaeger/Zipkin/OpenTelemetry qua lớp tích hợp.

#### Metrics & Dashboards

**WAY4 Health Monitoring module:**
- RED metrics: Rate, Errors, Duration
- Hardware: CPU, RAM, Disk I/O, Network, JVM Heap/GC
- Dashboards real-time (có thể tích hợp Prometheus, AppDynamics, Dynatrace)

#### Alerting

WAY4 Health Monitoring module — cảnh báo tự động theo ngưỡng error rate, success rate, latency.

---

## II. AML, Fraud Prevention, Compliance

### 1. AML & Fraud

#### Real-time Risk Module

- **Scoring, risk rule checking, usage limiter** trong thời gian thực
- Tích hợp external Fraud/RMS real-time
- Giao dịch bị **reject/challenge ngay** khi phát hiện rủi ro
- Latency overhead khi check fraud ngoài WAY4: **~300ms**

#### Rule / Scenario Management

- **Parameter-driven, rule-based** — nghiệp vụ/risk ops cấu hình và cập nhật **không cần code**
- Velocity checks, geo-risk, abnormal behavior scenarios đều config-driven

#### Auto-blocking

Khi phát hiện giao dịch/khách hàng rủi ro cao: tự động phong tỏa thẻ, khóa tài khoản, hold tiền — qua fraud/risk rule + event workflow.

#### Blacklist / Sanction Screening

Kiểm tra real-time: thông tin khách hàng, thẻ, tài khoản, giao dịch.

---

### 2. Security & Compliance

#### PCI DSS

WAY4 tuân thủ **PCI Secure Software Framework (PCI SSF)** — dành cho software vendor, áp dụng cho các thành phần/phạm vi sản phẩm.

#### Tokenization & Encryption

- **Tokenization** và mã hóa cho dữ liệu nhạy cảm: PAN, PIN, CVV, PII
- Cả tầng **lưu trữ** và **truyền tải**
- Cơ chế: encryption, TLS/mTLS, masking, **HSM integration**
- Thiết kế hỗ trợ tuân thủ PCI DSS/PCI SSF và quy định bảo vệ dữ liệu cá nhân hiện hành

---

## III. Reporting & Regulatory Compliance

#### NHNN Reports

- Báo cáo tuân thủ theo yêu cầu khách hàng
- Cập nhật linh hoạt theo thay đổi quy định: **config-driven** (parameter config, data mapping, report template, rule) — không cần code

#### Report Builder

- Reporting qua reporting tools, API, tích hợp Data Warehouse
- Export formats: **Excel, CSV, XML, PDF, JSON**
- Truyền dữ liệu: **SFTP / API** → DW, Data Lake, external systems

#### Heavy Reports

Báo cáo lớn chạy trên **hệ thống thứ 3** (isolated) — không ảnh hưởng core DB online.

---

## IV. Business Terms

| Item | OpenWay answer |
|---|---|
| Pricing model | **Fixed fee** (base); có thể đàm phán flexible model |
| Module bundling | **Có thể chọn lọc** (à la carte) |

---

## Performance Numbers (from pages 1-5)

| Scenario | TPS | Infrastructure |
|---|---|---|
| Online Authorization (peak) | ~800 TPS | 1 Oracle RAC node, Exadata |
| Online Processing | 1,152–1,165 TPS | 3-4 Oracle RAC nodes, Exadata |
| API Service | 1,793 TPS | 4-node Oracle RAC, Exadata |

Latency: "Tuân thủ yêu cầu của tổ chức thẻ" (card scheme compliant) — không nêu số cụ thể.

---

## Key Observations for NIVC

1. **Centralized DB (Oracle)** — WAY4 không dùng distributed consistency patterns (Saga, Outbox). Nếu NIVC tích hợp WAY4 như một external service, phải tự xử lý distributed consistency phía NIVC side.

2. **~300ms fraud check latency** — nếu NIVC routing mọi giao dịch qua external WAY4 fraud module, cần tính vào SLA tổng.

3. **Async integration có hỗ trợ** — WAY4 có thể làm việc với Kafka/RabbitMQ nhưng theo kiến trúc Ngân hàng, không phải native event-driven.

4. **Module pricing là à la carte** — có thể mua từng phần, không cần bundle toàn bộ.

5. **Config-driven operations** — fraud rules, report templates, product setup đều không cần code → tốt cho ops team NIVC.

6. **ZDD có điều kiện** — major upgrades vẫn cần maintenance window. Cần lên kế hoạch upgrade window với NIVC ops.
