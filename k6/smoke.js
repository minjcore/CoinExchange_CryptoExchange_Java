/**
 * Smoke test — verify all endpoints are reachable and return expected status.
 * Run: k6 run smoke.js
 * Run against staging: k6 run -e BASE_URL=http://staging:8080 smoke.js
 */
import http from 'k6/http';
import { check, group } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const HDR = { 'Content-Type': 'application/json' };

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    checks: ['rate==1.0'],
  },
};

export default function () {
  group('health', () => {
    const r = http.get(`${BASE_URL}/health`);
    check(r, { 'health 200': res => res.status === 200 });
  });

  group('deposit notify', () => {
    const r = http.post(`${BASE_URL}/v1/deposits/notify`, JSON.stringify({
      virtualAccount: 'VA00000001',
      grossAmount: '100000',
      bankRef: `BANK-SMOKE-${Date.now()}`,
      businessRef: `smoke-dep-${Date.now()}`,
      currency: 'VND',
      notifiedAt: new Date().toISOString(),
    }), { headers: HDR });
    check(r, { 'deposit 202': res => res.status === 202 });
  });

  group('payment', () => {
    const r = http.post(`${BASE_URL}/v1/payments`, JSON.stringify({
      businessRef: `smoke-pay-${Date.now()}`,
      memberId: 1001,
      merchantId: 2001,
      amount: '50000',
      currency: 'VND',
      netToMerchant: '49000',
    }), { headers: HDR });
    check(r, { 'payment 2xx': res => res.status >= 200 && res.status < 300 });
  });

  group('transfer', () => {
    const r = http.post(`${BASE_URL}/v1/transfers`, JSON.stringify({
      businessRef: `smoke-tx-${Date.now()}`,
      fromMemberId: 1001,
      toMemberId: 1002,
      amount: '10000',
      currency: 'VND',
      feeAmount: '0',
    }), { headers: HDR });
    check(r, { 'transfer 2xx': res => res.status >= 200 && res.status < 300 });
  });

  group('withdrawal 3-phase', () => {
    const ref = `smoke-wd-${Date.now()}`;

    const accept = http.post(`${BASE_URL}/v1/withdrawals`, JSON.stringify({
      businessRef: ref,
      memberId: 1001,
      amount: '20000',
      currency: 'VND',
      useFreeze: true,
    }), { headers: HDR });
    check(accept, { 'withdraw accept 2xx': res => res.status >= 200 && res.status < 300 });

    const settle = http.post(`${BASE_URL}/v1/withdrawals/settle`, JSON.stringify({
      businessRef: ref,
      memberId: 1001,
      amount: '20000',
      currency: 'VND',
    }), { headers: HDR });
    check(settle, { 'withdraw settle 2xx': res => res.status >= 200 && res.status < 300 });
  });

  group('ibft 3-phase', () => {
    const ref = `smoke-ibft-${Date.now()}`;

    const accept = http.post(`${BASE_URL}/v1/ibft`, JSON.stringify({
      businessRef: ref,
      memberId: 1001,
      principalAmount: '500000',
      platformFee: '5000',
      napasCost: '1100',
      currency: 'VND',
      destinationBankAccountNumber: '1234567890',
      destinationBankCode: 'VCB',
    }), { headers: HDR });
    check(accept, { 'ibft accept 2xx': res => res.status >= 200 && res.status < 300 });

    const settle = http.post(`${BASE_URL}/v1/ibft/settle`, JSON.stringify({
      businessRef: ref,
      memberId: 1001,
      principalAmount: '500000',
      platformFee: '5000',
      napasCost: '1100',
      currency: 'VND',
    }), { headers: HDR });
    check(settle, { 'ibft settle 2xx': res => res.status >= 200 && res.status < 300 });
  });
}
