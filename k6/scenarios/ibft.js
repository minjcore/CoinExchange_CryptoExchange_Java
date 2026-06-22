/**
 * Load test — IBFT 3-phase flow (UC-5)
 * Each VU runs: accept → settle (simulates NAPAS confirmation).
 * Highest DB write load: freeze wallet + 2 ledger entries on accept,
 * then settle wallet + 2 ledger entries.
 *
 * Pre-condition: members 3001..3100 must exist with balance >= 100_000_000_000 VND.
 *
 * Run:
 *   k6 run scenarios/ibft.js
 *   k6 run -e BASE_URL=http://staging:8080 -e CONCURRENT_USERS=30 scenarios/ibft.js
 */
import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const CONCURRENT = parseInt(__ENV.CONCURRENT_USERS || '30');

function hdr(ref) {
  return { 'Content-Type': 'application/json', 'X-Idempotency-Key': ref };
}

const acceptErrors = new Rate('ibft_accept_errors');
const settleErrors = new Rate('ibft_settle_errors');
const releaseErrors = new Rate('ibft_release_errors');
const acceptLatency = new Trend('ibft_accept_latency', true);
const settleLatency = new Trend('ibft_settle_latency', true);

export const options = {
  scenarios: {
    ibft_settle: {
      executor: 'constant-vus',
      vus: CONCURRENT,
      duration: '60s',
      exec: 'settleFlow',
    },
    ibft_release: {
      executor: 'constant-vus',
      vus: Math.floor(CONCURRENT / 5),  // 20% of load exercises release path
      duration: '60s',
      exec: 'releaseFlow',
      startTime: '5s',
    },
  },
  thresholds: {
    ibft_accept_errors: ['rate<0.01'],
    ibft_settle_errors: ['rate<0.01'],
    ibft_release_errors: ['rate<0.01'],
    ibft_accept_latency: ['p(95)<500'],
    ibft_settle_latency: ['p(95)<500'],
  },
};

function acceptIbft(memberId, ref) {
  const res = http.post(`${BASE_URL}/v1/ibft`, JSON.stringify({
    businessRef: ref,
    memberId: memberId,
    principalAmount: '500000',
    platformFee: '5000',
    napasCost: '1100',
    currency: 'VND',
    destinationBankAccountNumber: '9999888877776666',
    destinationBankCode: 'VCB',
  }), { headers: hdr(ref) });

  acceptErrors.add(res.status !== 200);
  acceptLatency.add(res.timings.duration);
  if (res.status !== 200) return null;
  return JSON.parse(res.body).data.coaTransId;
}

export function settleFlow() {
  const memberId = 3000 + ((__VU - 1) % 100) + 1;
  const ref = `ibft-s-${__VU}-${__ITER}`;

  const coaTransId = acceptIbft(memberId, ref);
  if (!coaTransId) return;

  const settle = http.post(`${BASE_URL}/v1/ibft/settle`, JSON.stringify({
    businessRef: ref,
    memberId: memberId,
    coaTransId: coaTransId,
    principal: '500000',
    platformFee: '5000',
    napasCost: '1100',
  }), { headers: hdr(`${ref}:settle`) });

  settleErrors.add(settle.status !== 200);
  settleLatency.add(settle.timings.duration);
  check(settle, { 'ibft settle 200': r => r.status === 200 });
}

export function releaseFlow() {
  const memberId = 3000 + ((__VU - 1) % 100) + 1;
  const ref = `ibft-r-${__VU}-${__ITER}`;

  const coaTransId = acceptIbft(memberId, ref);
  if (!coaTransId) return;

  const release = http.post(`${BASE_URL}/v1/ibft/release`, JSON.stringify({
    businessRef: ref,
    memberId: memberId,
    coaTransId: coaTransId,
    gross: '506100',  // principal + platformFee + napasCost
  }), { headers: hdr(`${ref}:release`) });

  releaseErrors.add(release.status !== 200);
  check(release, { 'ibft release 200': r => r.status === 200 });
}
