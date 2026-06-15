[Skip to main content](https://docs.mambu.com/docs/authorization-holds/#__docusaurus_skipToContent_fallback)

[![Mambu](https://docs.mambu.com/img/logo.svg)](https://docs.mambu.com/)

[Mambu APIs](https://docs.mambu.com/docs/authorization-holds/#)

- [API v2](https://docs.mambu.com/api/pages/api-v2/welcome/)
- [API v1](https://docs.mambu.com/api/pages/api-v1/welcome/)
- [Audit Trail](https://docs.mambu.com/api/pages/audit-trail/welcome/)
- [Mambu Payments](https://docs.numeral.io/reference/introduction)
- [Mambu MPG](https://docs.mambu.com/api/pages/payments-mpg/welcome/)
- [Streaming API](https://docs.mambu.com/api/pages/streaming/streaming-index/)

[User Guide](https://docs.mambu.com/docs/) [Support](https://docs.mambu.com/docs/mambu-support/) [Release Notes](https://docs.mambu.com/release-notes/)

[Status](https://status.mambu.com/)

`ctrl`  `K`

- [Welcome to Mambu](https://docs.mambu.com/docs/)
- [Documentation Changelog](https://docs.mambu.com/docs/documentation-changelog/)
- [Getting Started](https://docs.mambu.com/docs/authorization-holds/#)

- [Managing the Mambu UI](https://docs.mambu.com/docs/authorization-holds/#)

- [Managing your Organization](https://docs.mambu.com/docs/authorization-holds/#)

- [Data and Reporting](https://docs.mambu.com/docs/authorization-holds/#)

- [Analytics and Insights](https://docs.mambu.com/docs/authorization-holds/#)

- [Users and Access Control](https://docs.mambu.com/docs/authorization-holds/#)

- [Clients and Groups](https://docs.mambu.com/docs/authorization-holds/#)

- [Loans](https://docs.mambu.com/docs/authorization-holds/#)

- [Deposits](https://docs.mambu.com/docs/authorization-holds/#)

- [Credit Arrangements](https://docs.mambu.com/docs/authorization-holds/#)

- [Cards](https://docs.mambu.com/docs/authorization-holds/#)

  - [Cards Introduction](https://docs.mambu.com/docs/cards-introduction/)
  - [Token Management](https://docs.mambu.com/docs/token-management/)
  - [Authorization Holds](https://docs.mambu.com/docs/authorization-holds/)
  - [Transaction Processing](https://docs.mambu.com/docs/card-transaction-processing/)
  - [Cards Integrations](https://docs.mambu.com/docs/authorization-holds/#)
- [Payments](https://docs.mambu.com/docs/authorization-holds/#)

- [Transactions and Interest](https://docs.mambu.com/docs/authorization-holds/#)

- [Notifications](https://docs.mambu.com/docs/authorization-holds/#)

- [Auditing](https://docs.mambu.com/docs/authorization-holds/#)

- [Accounting](https://docs.mambu.com/docs/authorization-holds/#)

- [Islamic Banking](https://docs.mambu.com/docs/authorization-holds/#)

- [Tellers](https://docs.mambu.com/docs/authorization-holds/#)

- [Developer](https://docs.mambu.com/docs/authorization-holds/#)

- [Release Cycle and Compatibility](https://docs.mambu.com/docs/authorization-holds/#)

- [Mambu Support](https://docs.mambu.com/docs/authorization-holds/#)


- [Home page](https://docs.mambu.com/)
- Cards
- Authorization Holds

On this page

# Authorization Holds

_Authorization holds_ are the result of successful payment card authorizations.

There are two types of authorization holds:

- Debit (DBIT) holds: decrease the available balance of a card account.
- Credit (CRDT) holds: have no impact on the available balance.

You can process authorization requests, authorization advices, incremental authorizations, authorization reversals, and balance inquiries via the Cards API. For more information, see [Cards](https://docs.mambu.com/api/api-v2/cards/cards/) in our API Reference.

For more information on the permissions needed to manage authorization holds setup, see the [Permissions](https://docs.mambu.com/docs/permissions/) article. Authorization holds have no impact on the ledger or the total balance of a deposit account.

## Authorization hold states [​](https://docs.mambu.com/docs/authorization-holds/\#authorization-hold-states "Direct link to Authorization hold states")

Authorization holds can be in one of the following statuses:
`PENDING`: Active hold, decreasing the available balance.
`SETTLED`: Not active, released during the processing of a related payment card transaction.
`EXPIRED`: Not active, marked as expired during automated cron job process.
`REVERSED`: Not active, marked as reversed as a result of the processing of a reversal instruction or a decline notification.

You can retrieve either an array of [all authorization holds](https://docs.mambu.com/api/api-v2/deposits/get-all-authorization-holds/) for a given deposit account or [details on a specific hold](https://docs.mambu.com/api/api-v2/cards/get-authorization-hold-by-id/) on a given card by its ID.

Card authorization holds are distinct from transaction holds, which may be applied to any deposit transaction. For more information, see [Transaction Holds](https://docs.mambu.com/docs/transaction-holds/).

The following table describes the different types of messages that pass between the card issuer and Mambu, depending on the type of authorization initiated.

| Card message Type | Debit Cards Availability | Credit Cards Availability |
| --- | --- | --- |
| Debit Authorization Request | YES | YES |
| Credit Authorization Request | YES | NO |
| Debit Authorization Advice | YES | NO |
| Credit Authorization Advice | YES | NO |
| Balance Inquiry | YES | YES |
| Full Authorization Reversal | YES | YES |
| Partial Authorization Reversal | YES | YES |

### Authorization requests [​](https://docs.mambu.com/docs/authorization-holds/\#authorization-requests "Direct link to Authorization requests")

Authorization requests are a part of the Dual Message Schema, the purpose of which is to perform an authorization before sending related clearing transaction. Debit authorization is used to check the card and the account, that there is enough account balance for a cash withdrawal, purchase of goods and services, or other debit type of operation. The request body parameter `advice` must be set to `FALSE` to enable balance check and separate the authorization request from the authorization advice. Credit authorization is used to check the card and the account in order to execute a refund, cashback or other type of credit operation.

[Authorization requests](https://docs.mambu.com/api/api-v2/cards/create-authorization-hold/) can only be performed via [Mambu API v2](https://docs.mambu.com/api/api-v2/cards/cards/). Each authorization hold request in Mambu will have a reference number. Once an authorization hold is created, it has the status `PENDING` until it is settled, reversed, or expired.

### Authorization advices [​](https://docs.mambu.com/docs/authorization-holds/\#authorization-advices "Direct link to Authorization advices")

If authorization happens on behalf of the issuer, without checking whether the card holder has sufficient funds to cover a given transaction, third-party systems use authorization advice to inform about the result of such authorizations. There can be negative or positive advice. Mambu supports positive authorization advice. [Authorization advice](https://docs.mambu.com/api/api-v2/cards/create-authorization-hold/) creates a hold on a card account without checking the available balance. The request body parameter `advice` must be set to `TRUE` to disable the available balance check.

The final authorization advice contains the final hold amount. Usually it is not clear whether the final hold amount is higher or lower than the initial amount. To resolve this porblem, you can change the hold amount while calling the [dedicated cards PATCH authorization hold API](https://docs.mambu.com/api/api-v2/cards/patch-authorization-hold/).

### Incremental authorization [​](https://docs.mambu.com/docs/authorization-holds/\#incremental-authorization "Direct link to Incremental authorization")

Authorization holds with a `PENDING` status can be [increased](https://docs.mambu.com/api/api-v2/cards/increase-authorization-hold/) by making an API request and supplying the card token (`cardReferenceToken`) and the authorization hold reference (`authorizationHoldExternalReferenceId`) along with the amount used to raise the held amount.

### Authorization reversals [​](https://docs.mambu.com/docs/authorization-holds/\#authorization-reversals "Direct link to Authorization reversals")

Mambu supports both full and partial authorization reversals. After receiving a partial reversal, the referenced authorization hold with a `PENDING` status can be [decreased](https://docs.mambu.com/api/api-v2/cards/decrease-authorization-hold/) by making an API request and supplying the card token (`cardReferenceToken`) and the authorization hold reference (`authorizationHoldExternalReferenceId`), along with the amount to decrease the held amount. If the amount used is equal to or larger than the held amount, the hold is fully reversed and the hold is given a `REVERSED` status.

You can also fully reverse a `PENDING` authorization hold by making a [delete](https://docs.mambu.com/api/api-v2/cards/reverse-authorization-hold/) request and supplying the card token (`cardReferenceToken`) and the authorization hold reference (`authorizationHoldExternalReferenceId`).

### Balance inquiry [​](https://docs.mambu.com/docs/authorization-holds/\#balance-inquiry "Direct link to Balance inquiry")

Using a card token, you can also get account balances. The response of the [Get account balances](https://docs.mambu.com/api/api-v2/cards/get-account-balances/) request contains the following information: the available balance, the total balance, the credit limit, the account currency, the account ID, and the card type.

The available balance and the ledger balance are also returned in the response of each successfully created authorization hold or card transaction.

### Authorization Holds Configuration [​](https://docs.mambu.com/docs/authorization-holds/\#authorization-holds-configuration "Direct link to Authorization Holds Configuration")

warning

We strongly recommend that you use an end-to-end test to test any configuration changes in your sandbox environment before implementing them in your production environment. The test should check that the hourly `AUTHORIZATION_HOLDS_EXPIRATION` cron job correctly sets authorization holds to expired. To perform a test on holds in the past contact us through [Mambu Support](https://docs.mambu.com/docs/mambu-support/) so that we can assist you with manipulating the data in the database.

The authorization holds configuration determines the expiration period for authorization holds in days. The default value is seven days but it can be changed. You may also specify different expiry periods for specific merchant category codes (MCC). You may configure authorization holds settings either through the Mambu UI or via the API. For more information, see [Authorization Holds Configuration](https://docs.mambu.com/docs/configuration-as-code-for-authorization-holds/).

Please Note

This element can also be configured using Configuration as Code (CasC). For more information, see [Configuration as Code](https://docs.mambu.com/docs/configuration-as-code-1/).

To change the default expiration period for authorization holds:

1. On the main menu, go to **Administration** \> **Financial Setup** \> **Authorization Holds**.
2. Next to **Default period before expiration**, enter the number of days after which you want the authorization holds to expire. The value must be between 1 and 36,525 days.
3. Select **Save**.

![Setting-an-expiration-date-for-authorization-holds](https://docs.mambu.com/assets/images/authorization-holds-add-catogory-code-5f273cb40d5553f1caccf49c142524ee.png)

To add an expiration period for an MCC:

1. On the main menu, go to **Administration** \> **Financial Setup** \> **Authorization Holds**.
2. Select **Add**.
3. Enter a value for the **Merchant Category Code**.
4. Enter a value for the **Days to Expiration**. The value must be between 1 and 36,525 days.
5. Select **Save**.

Mambu authorization holds support the custom expiration period. You can set expiration period in days while creating a hold in the request body parameter `customExpirationPeriod`. The `PENDING` debit authorization hold can be updated using the [Update authorization](https://docs.mambu.com/api/api-v2/cards/patch-authorization-hold/).

### Hourly authorization holds cron job [​](https://docs.mambu.com/docs/authorization-holds/\#hourly-authorization-holds-cron-job "Direct link to Hourly authorization holds cron job")

The hourly cron job `AUTHORIZATION_HOLDS_EXPIRATION` runs automatically and marks any authorization hold requests on which a settlement action has not been taken before the expiration period has elapsed. For example, if the expiration period set for authorization holds is set to 14 days, then any authorization holds created or updated more than 14 days ago are candidates to be expired.

When an authorization hold expires, its state changes from `PENDING` to `EXPIRED` and the hold amount no longer affects a client's available balance. For more information about cron jobs at Mambu, see [Automatic End of Day Actions](https://docs.mambu.com/docs/automatic-end-of-day-actions/) and [Hourly jobs](https://docs.mambu.com/docs/automatic-end-of-day-actions/#hourly-jobs).

If you change your authorization holds configuration, the new configuration will take effect with the next `AUTHORIZATION_HOLDS_EXPIRATION` cron job to run. Holds made directly on deposit accounts via account ID do not expire. For more information, see [Transaction Holds](https://docs.mambu.com/docs/transaction-holds/).

**Tags:**

- [authorization-holds](https://docs.mambu.com/tags/authorization-holds/)
- [cards](https://docs.mambu.com/tags/cards/)
- [api](https://docs.mambu.com/tags/api/)
- [holds](https://docs.mambu.com/tags/holds/)
- [configuration](https://docs.mambu.com/tags/configuration/)
- [cron-job](https://docs.mambu.com/tags/cron-job/)
- [mcc](https://docs.mambu.com/tags/mcc/)
- [reversals](https://docs.mambu.com/tags/reversals/)

Last updated on **Apr 13, 2026**

[Previous\\
\\
Token Management](https://docs.mambu.com/docs/token-management/) [Next\\
\\
Transaction Processing](https://docs.mambu.com/docs/card-transaction-processing/)

- [Authorization hold states](https://docs.mambu.com/docs/authorization-holds/#authorization-hold-states)
  - [Authorization requests](https://docs.mambu.com/docs/authorization-holds/#authorization-requests)
  - [Authorization advices](https://docs.mambu.com/docs/authorization-holds/#authorization-advices)
  - [Incremental authorization](https://docs.mambu.com/docs/authorization-holds/#incremental-authorization)
  - [Authorization reversals](https://docs.mambu.com/docs/authorization-holds/#authorization-reversals)
  - [Balance inquiry](https://docs.mambu.com/docs/authorization-holds/#balance-inquiry)
  - [Authorization Holds Configuration](https://docs.mambu.com/docs/authorization-holds/#authorization-holds-configuration)
  - [Hourly authorization holds cron job](https://docs.mambu.com/docs/authorization-holds/#hourly-authorization-holds-cron-job)

Technology

- [Our platform](https://www.mambu.com/cloud-banking-platform)
- [Lending](https://www.mambu.com/cloud-lending-software)
- [Deposits](https://www.mambu.com/deposits)
- [Services](https://www.mambu.com/professional-services)
- [Customers](https://www.mambu.com/customers)
- [Partners](https://www.mambu.com/partners)

Company

- [About](https://www.mambu.com/customers)
- [Careers](https://www.mambu.com/careers)
- [Our Offices](https://www.mambu.com/offices)
- [SpeakUp](https://www.mambu.com/speaking-up)
- [Contact Us](https://www.mambu.com/contact-us)
- [Diversity and Inclusion](https://www.mambu.com/diversity-and-inclusion)

Insights

- [Events](https://www.mambu.com/insights/events)
- [Articles](https://www.mambu.com/insights/articles)
- [News](https://www.mambu.com/insights/press)
- [Webinars](https://www.mambu.com/insights/webinars)
- [Reports](https://www.mambu.com/insights/reports)

Developers

- [Support](https://docs.mambu.com/docs/mambu-support/)
- [User Guide](https://docs.mambu.com/docs/)
- [API Reference](https://docs.mambu.com/api/)
- [Ecosystem](https://ecosystem.mambu.com/)
- [Status](https://status.mambu.com/)

- [LinkedIn](https://www.linkedin.com/company/mambu)
- [Twitter](https://twitter.com/Mambu_com)
- [Facebook](https://facebook.com/mambucloud)
- [YouTube](https://www.youtube.com/channel/UC0vXbn7DBeCVXTD1GmYZd2A)
- [Instagram](https://www.instagram.com/life_atmambu/)

Copyright Mambu \| [Privacy](https://www.mambu.com/en/legal/privacy-policy) \| [Cookie Policy](https://www.mambu.com/en/legal/cookie-policy)

×