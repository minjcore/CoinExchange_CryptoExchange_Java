/**
 * Load test — Internal Transfer (UC-4)
 * Simplest sync write path: single DB write, no freeze/settle phases.
 * Use this to establish baseline TPS for the Data Plane.
 *
 * Pre-condition: members 1001..1100 must exist with balance >= 1_000_000_000 VND each.
 * Seed: INSERT INTO wallet ... (see k6/seed/README.md)
 *
 * Run:
 *   k6 run scenarios/transfer.js
 *   k6 run -e BASE_URL=http://staging:8080 -e TARGET_RPS=200 scenarios/transfer.js
 */
import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TARGET_RPS = parseInt(__ENV.TARGET_RPS || '100');

function hdr(ref) {
  return { 'Content-Type': 'application/json', 'X-Idempotency-Key': ref };
}

const errorRate = new Rate('transfer_errors');
const latency = new Trend('transfer_latency', true);

export const options = {
  scenarios: {
    constant_load: {
      executor: 'constant-arrival-rate',
      rate: TARGET_RPS,
      timeUnit: '1s',
      duration: '60s',
      preAllocatedVUs: TARGET_RPS * 2,
      maxVUs: TARGET_RPS * 4,
    },
  },
  thresholds: {
    transfer_errors: ['rate<0.01'],         // <1% error
    transfer_latency: ['p(95)<300'],        // p95 < 300ms
    'http_req_duration{scenario:constant_load}': ['p(99)<500'],
  },
};

export default function () {
  // VU 1-50 → fromMemberId 1001-1050, toMemberId 1051-1100
  // keeps transfers between separate wallet pairs → no lock contention
  const from = 1000 + ((__VU - 1) % 50) + 1;
  const to = 1050 + ((__VU - 1) % 50) + 1;
  const ref = `tx-${__VU}-${__ITER}`;

  const res = http.post(`${BASE_URL}/v1/transfers`, JSON.stringify({
    businessRef: ref,
    fromMemberId: from,
    toMemberId: to,
    amount: '1000',
    currency: 'VND',
    // feeAmount omitted — parseAmount rejects '0'
  }), { headers: hdr(ref) });

  const ok = check(res, {
    'status 200': r => r.status === 200,
    'has businessRef': r => {
      try { return JSON.parse(r.body).data.businessRef === ref; } catch { return false; }
    },
  });

  errorRate.add(!ok);
  latency.add(res.timings.duration);
}
