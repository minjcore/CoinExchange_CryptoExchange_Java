Why was this page helpful to you?

Solved my problem

Easy to understand

Good example

Another reason

Submit

Why was this page not helpful to you?

Couldn't find what I was looking for

Too complicated / Too many steps

Unclear language or terminology

Problem with the code / errors

Another reason

Submit

Thank you!

Your feedback helps us improve our product.

Why was this page helpful to you?

Solved my problem

Easy to understand

Good example

Another reason

Submit

Why was this page not helpful to you?

Couldn't find what I was looking for

Too complicated / Too many steps

Unclear language or terminology

Problem with the code / errors

Another reason

Submit

Thank you!

Your feedback helps us improve our product.

![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/pay-by-link-plane.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/checkout.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/terminal-2.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/unified-commerce.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/plugins.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/pay-by-link-plane.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/platforms.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/marketplaces.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/accounts.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/capital.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/issuing.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/classic-platforms.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/accounts.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/issuing.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/payout.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/person-circle.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/reporting.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/risk-team.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/pay-by-link-plane.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/credentials.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/event-code.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/warning.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/news.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/tools.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/settings.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/store.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/users-9.svg)

Was this page helpful?

![Tools-2 icon](https://docs.adyen.com/user/themes/adyen-docs/assets/icons/tools-2.svg)

# API idempotency

Learn how to safely resend API requests without performing the same operation multiple times.


[View source](https://docs.adyen.com/development-resources/api-idempotency.md)

Copy as Markdown

The Adyen API supports [idempotency](https://tools.ietf.org/html/rfc7231#section-4.2.2), allowing you to retry a request multiple times while only performing the action once. This helps avoid unwanted duplication in case of failures and retries. For example, in the case of a timeout error, it is possible to safely retry sending the same API payment call multiple times with the guarantee that the payment detail will only be charged once.

The accounting rules in the Adyen payments platform take care of most potential double-processing issues that can impact payment [modifications](https://docs.adyen.com/get-started-with-adyen/adyen-glossary#payment-modifications-definition). For example, the following default rules apply:

- [Captures](https://docs.adyen.com/online-payments/capture): when partial captures are allowed, it is not possible to capture a higher amount than the authorized one.
- [Refunds](https://docs.adyen.com/online-payments/refund): when multiple refunds are allowed, by default the total refunded value cannot exceed the captured amount.

To minimize unwanted side effects when requests are duplicated, you can also take the following actions on your end:

- Implement asynchronous server-to-server [webhooks](https://docs.adyen.com/development-resources/webhooks). For example, this approach helps keep track of missing responses, a common consequence of a data transmission timeout.
- Enable idempotency in your API requests. The Adyen API supports idempotency on POST requests (other request types such as GET, DELETE and PUT are idempotent by definition).

## [Anchor](https://docs.adyen.com/development-resources/api-idempotency\#enable-idempotency) Enable idempotency

To submit a request for idempotent processing, send a request with the `idempotency-key:<key>` in the header.

The `<key>` is a unique identifier for the message with a maximum of 64 characters. We recommend using a UUID. If you do not receive a response (for example, in case of a timeout), you can safely retry the request with the same HTTP header. If the Adyen payments platform already processed the request, the response to the first attempt will be returned without duplication.

To verify that a request was processed idempotently, check the `idempotency-key` HTTP header returned in the response.

Make sure that you code for case-insensitive HTTP headers.

Here is an example of how to include the `idempotency-key` in a [/payments](https://docs.adyen.com/api-explorer/Checkout/latest/post/payments) request:

curlJavaPHPC#NodeJS (JavaScript)RubycurlJavaPHPC#NodeJS (JavaScript)Ruby

Expand view

Copy link to code block

Copy code

Copy code

```bash

curl https://checkout-test.adyen.com/v72/payments \

-H 'x-api-key: ADYEN_API_KEYYour API key from your Customer Area.' \

-H 'idempotency-key: YOUR_IDEMPOTENCY_KEY' \

-H 'content-type: application/json' \

-d '{

  "merchantAccount": "YOUR_MERCHANT_ACCOUNTName of your merchant account.",

  "reference": "My first Adyen test payment",

  "amount": {

    "value": 100010 euros in minor units,

    "currency": "EUR"

  },

	"paymentMethod": {

    "type": "scheme",

    "encryptedCardNumber": "test_4111111111111111",

    "encryptedExpiryMonth": "test_03",

    "encryptedExpiryYear": "test_2030",

    "encryptedSecurityCode": "test_737"

  }

}'
```

## [Anchor](https://docs.adyen.com/development-resources/api-idempotency\#key-scope-and-validity-time) Key scope and validity time

Keys are stored at a company account level. The system checks that the idempotency keys are unique to the company account. Idempotency keys are valid for a minimum period of 7 days after first submission.

If you are targeting multiple regional endpoints simultaneously, idempotency keys will not be checked for duplication in other regions.

## [Anchor](https://docs.adyen.com/development-resources/api-idempotency\#security-considerations) Security considerations

Generate unique idempotency keys per request using the **version 4 (random)** UUID type to prevent two API credentials under the same account from accessing each others responses.

Lowering the access level for an API credential does not prevent them from retrieving past responses if the user still has access to previously used keys.

## [Anchor](https://docs.adyen.com/development-resources/api-idempotency\#error-handling) Error handling

### [Anchor](https://docs.adyen.com/development-resources/api-idempotency\#transient-errors) Transient errors

In rare instances, you could receive a transient error. This is a temporary error that has no side effects and can be retried. For example, it could come from a race condition of sending two payment requests with the same idempotency key at the same time. One will end up being processed while the other will return a transient error.

The response will contain an HTTP header **transient-error** with the value **true**, which indicates that the request can be retried at a later time using the same idempotency key. If the API does not return a transient error header, or returns a header with a value of false, do not retry the request. If you submit a duplicate request before the first request has completed, the API returns an **HTTP 422 – Unprocessable Entity** or **HTTP 409 - Conflict** status with the error code **704: "request already processed or in progress"**.

### [Anchor](https://docs.adyen.com/development-resources/api-idempotency\#retries-and-exponential-backoff) Retries and exponential backoff

Use an exponential backoff strategy when retrying transactions to avoid flooding the API and running into rate-limiting. See [the Wikipedia article on exponential backoff](https://en.wikipedia.org/wiki/Exponential_backoff) for more information.

### [Anchor](https://docs.adyen.com/development-resources/api-idempotency\#designing-for-resilience) Designing for resilience

To enable idempotent behavior, Adyen must retain a consistent, stateful data store to compare each request against. Idempotent processing relies on this data store being available. In the unlikely event that this data store should become unavailable, the API returns an **HTTP 503 – Service Unavailable** status with the error code **703: "required resource temporarily unavailable"**. The system marks the request as a transient error (see above).

If there are many of these responses, you can either:

- Pause processing and retry the requests later (if this is feasible).
- Fall back to non-idempotent processing by not submitting the `idempotency-key` HTTP header.

Was this page helpful?