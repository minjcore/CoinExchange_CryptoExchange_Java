/**
 * Load test — Withdraw 3-phase flow (UC-2)
 * Each VU runs: accept → settle (simulates payout success path).
 * Tests freeze + ledger write, then settle + ledger write in sequence.
 *
 * Pre-condition: members 2001..2100 must exist with balance >= 10_000_000_000 VND each.
 *
 * Run:
 *   k6 run scenarios/withdraw.js
 *   k6 run -e BASE_URL=http://staging:8080 -e CONCURRENT_USERS=50 scenarios/withdraw.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const CONCURRENT = parseInt(__ENV.CONCURRENT_USERS || '50');
const HDR = { 'Content-Type': 'application/json' };

const acceptErrors = new Rate('withdraw_accept_errors');
const settleErrors = new Rate('withdraw_settle_errors');
const acceptLatency = new Trend('withdraw_accept_latency', true);
const settleLatency = new Trend('withdraw_settle_latency', true);

export const options = {
  scenarios: {
    withdraw_flow: {
      executor: 'constant-vus',
      vus: CONCURRENT,
      duration: '60s',
    },
  },
  thresholds: {
    withdraw_accept_errors: ['rate<0.01'],
    withdraw_settle_errors: ['rate<0.01'],
    withdraw_accept_latency: ['p(95)<400'],
    withdraw_settle_latency: ['p(95)<400'],
  },
};

export default function () {
  const memberId = 2000 + ((__VU - 1) % 100) + 1;
  const ref = `wd-${__VU}-${__ITER}`;
  const amount = '10000';

  // Phase 1: Accept (freeze)
  const accept = http.post(`${BASE_URL}/v1/withdrawals`, JSON.stringify({
    businessRef: ref,
    memberId: memberId,
    amount: amount,
    currency: 'VND',
    useFreeze: true,
  }), { headers: HDR });

  const acceptOk = check(accept, {
    'accept 200': r => r.status === 200,
  });
  acceptErrors.add(!acceptOk);
  acceptLatency.add(accept.timings.duration);

  if (!acceptOk) return;

  // Phase 2: Settle (simulates bank payout confirmed)
  const settle = http.post(`${BASE_URL}/v1/withdrawals/settle`, JSON.stringify({
    businessRef: ref,
    memberId: memberId,
    amount: amount,
    currency: 'VND',
  }), { headers: HDR });

  const settleOk = check(settle, {
    'settle 200': r => r.status === 200,
  });
  settleErrors.add(!settleOk);
  settleLatency.add(settle.timings.duration);
}
