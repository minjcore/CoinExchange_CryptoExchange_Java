[Skip to main content](https://docs.mambu.com/docs/mambu-apis/#__docusaurus_skipToContent_fallback)

On this page

Mambu offers a full suite of RESTful APIs to provide programmatic access to nearly every aspect of our banking software, typically with JSON or YAML requests. Some relevant information may be found here in our User Guide, but our primary API reference content for the Mambu v1 and v2, Payments, and Streaming APIs may be found on our [API Reference](https://docs.mambu.com/api/). There, you will find complete reference material including authentication and request format information, endpoints, field descriptions, example requests and responses, and general help on requirements and conventions. For information on using the MPO API, see our [MPO documentation](https://ecosystem.mambu.com/mpo/overview/).

Mambu APIs allow users to perform many tasks, including:

- create clients, loan accounts, or savings accounts
- make transactions
- manage system configuration
- set up branches
- configure interest rates
- manage API keys, user roles, and access permissions
- and much, much more

Providing so much functionality via API is a cornerstone of our "composable banking" ethos, allowing you to easily build up your business around Mambu's Core Banking feature set or just dip in to our complete feature set to take advantage of the features you need, such as our trusted general ledger solution.

## Mambu APIs [​](https://docs.mambu.com/docs/mambu-apis/\#mambu-apis "Direct link to Mambu APIs")

- [API v2](https://docs.mambu.com/api/pages/api-v2/welcome/): APIs for our core banking platform. These provide access to our most widely used features such as client & account management, transactions & general ledger journal entries, tasks and system configuration. This is likely the first point of reference when building integrations and custom tools.
- [API v1](https://docs.mambu.com/api/pages/api-v1/welcome/): Our legacy core banking API; while we recommend that all new integrations are built using our v2 APIs, these endpoints and their documentation are retained for existing customers who may still be using them in legacy integrations. For more information, see [Using API v1 and API v2](https://docs.mambu.com/docs/mambu-apis/#using-api-v1-and-api-v2).
- [Mambu Payments Gateway API](https://api.mambu.com/payments-api/#welcome): Our payments product helps customers support national and international, standards-based schemes, including SEPA. APIs are provided for making and receiving payments and inquiries as well as mapping Mambu accounts to international account numbering systems such as IBAN and communicating with partners to deliver anti-money laundering measures and other requirements.
- [Streaming API](https://docs.mambu.com/api/pages/streaming/streaming-index/): Our streaming API is an enterprise feature which allows customers to set up configurable event feeds that can be used to power your own banking ecosystem, be it communication tool CRMs or your own auditing and logging system.

## Using API v1 and API v2 [​](https://docs.mambu.com/docs/mambu-apis/\#using-api-v1-and-api-v2 "Direct link to Using API v1 and API v2")

API v1 is no longer being actively developed. We strongly recommend that all customers use our API v2 endpoints for all new integrations and transition to API v2 endpoints for existing features wherever possible.

API v2 can, however, be used in parallel with API v1, as they do not conflict.

Request versioning is supported via the `Accept` header. An `Accept` header is required for all API v2 requests.

Example:

```http
Accept: application/vnd.mambu.v2+json
```

or

```http
Accept: application/vnd.mambu.v2+yaml
```

- [Mambu APIs](https://docs.mambu.com/docs/mambu-apis/#mambu-apis)
- [Using API v1 and API v2](https://docs.mambu.com/docs/mambu-apis/#using-api-v1-and-api-v2)

×