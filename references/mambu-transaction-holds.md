[Skip to main content](https://docs.mambu.com/docs/transaction-holds/#__docusaurus_skipToContent_fallback)

[![Mambu](https://docs.mambu.com/img/logo.svg)](https://docs.mambu.com/)

[Mambu APIs](https://docs.mambu.com/docs/transaction-holds/#)

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
- [Getting Started](https://docs.mambu.com/docs/transaction-holds/#)

- [Managing the Mambu UI](https://docs.mambu.com/docs/transaction-holds/#)

- [Managing your Organization](https://docs.mambu.com/docs/transaction-holds/#)

- [Data and Reporting](https://docs.mambu.com/docs/transaction-holds/#)

- [Analytics and Insights](https://docs.mambu.com/docs/transaction-holds/#)

- [Users and Access Control](https://docs.mambu.com/docs/transaction-holds/#)

- [Clients and Groups](https://docs.mambu.com/docs/transaction-holds/#)

- [Loans](https://docs.mambu.com/docs/transaction-holds/#)

- [Deposits](https://docs.mambu.com/docs/transaction-holds/#)

  - [Deposit Products](https://docs.mambu.com/docs/transaction-holds/#)

  - [Managing Deposit Accounts](https://docs.mambu.com/docs/transaction-holds/#)

  - [Working with Deposit Accounts](https://docs.mambu.com/docs/transaction-holds/#)

    - [Interest Rate Management Enhancements](https://docs.mambu.com/docs/interest-rate-management-enhancements/)
    - [Creating a Deposit Account](https://docs.mambu.com/docs/creating-a-deposit-account/)
    - [Deposit Account Life Cycle and States](https://docs.mambu.com/docs/deposit-accounts-life-cycle-and-states/)
    - [Deposit Account Overview Details](https://docs.mambu.com/docs/deposit-account-overview-details/)
    - [Deposits, Withdrawals and Transfers](https://docs.mambu.com/docs/deposits-withdrawals-and-transfers/)
    - [Changing Interest Rates on Active Deposit Accounts](https://docs.mambu.com/docs/changing-the-interest-rate/)
    - [Maximum Deposit Account Balance](https://docs.mambu.com/docs/maximum-deposit-account-balance/)
    - [Blocking Funds in Deposit Accounts](https://docs.mambu.com/docs/blocking-funds-in-deposit-accounts/)
    - [Transaction Holds](https://docs.mambu.com/docs/transaction-holds/)
    - [Technical Overdraft](https://docs.mambu.com/docs/technical-overdraft/)
    - [Managing Fees in Deposit Accounts](https://docs.mambu.com/docs/managing-fees-in-deposit-accounts/)
    - [Truncating and rounding interest](https://docs.mambu.com/docs/truncating-and-rounding-interest-deposits/)
- [Credit Arrangements](https://docs.mambu.com/docs/transaction-holds/#)

- [Cards](https://docs.mambu.com/docs/transaction-holds/#)

- [Payments](https://docs.mambu.com/docs/transaction-holds/#)

- [Transactions and Interest](https://docs.mambu.com/docs/transaction-holds/#)

- [Notifications](https://docs.mambu.com/docs/transaction-holds/#)

- [Auditing](https://docs.mambu.com/docs/transaction-holds/#)

- [Accounting](https://docs.mambu.com/docs/transaction-holds/#)

- [Islamic Banking](https://docs.mambu.com/docs/transaction-holds/#)

- [Tellers](https://docs.mambu.com/docs/transaction-holds/#)

- [Developer](https://docs.mambu.com/docs/transaction-holds/#)

- [Release Cycle and Compatibility](https://docs.mambu.com/docs/transaction-holds/#)

- [Mambu Support](https://docs.mambu.com/docs/transaction-holds/#)


- [Home page](https://docs.mambu.com/)
- Deposits
- Working with Deposit Accounts
- Transaction Holds

On this page

# Transaction Holds

A _transaction hold_ refers to a kind of transaction authorization in which the balance is unavailable to the account holder until it is either settled or cancelled.

Transaction holds are a flexible tool that can be useful or necessary for several common processes and flows, such as:

- Single Euro Payments Area (SEPA) instant payment flows
- check settlements in the US or other countries
- multicurrency settlements
- other scenarios where a hold must be posted prior to an actual transaction

For any type of deposit account, you can create, settle, reverse, and retrieve authorization holds directly on the account. Once you create a hold, the hold balance on the deposit account increases by the specified amount, and the available balance decreases by the specified amount.

Transaction holds are available via requests to the Mambu v2 API. For more information, see [`GET /deposits/{depositAccountId}/authorizationholds`](https://docs.mambu.com/api/api-v2/deposits/get-all-authorization-holds/).

warning

Transaction holds are separate from **card authorization holds**, which are used to verify electronic transactions initiated with a debit or credit card. For more information, see [Authorization Holds](https://docs.mambu.com/docs/authorization-holds/).

## Required permissions [​](https://docs.mambu.com/docs/transaction-holds/\#required-permissions "Direct link to Required permissions")

To perform any action relating to holds on deposit accounts, you must have the following permissions in the **Holds (via account ID)** category assigned to your user account:

- **Create Holds** (`CREATE_HOLDS`)
- **View Holds** (`VIEW_HOLDS`)
- **Update Holds** (`UPDATE_HOLDS`)
- **Delete Holds** (`DELETE_HOLDS`)

## Posting transaction holds [​](https://docs.mambu.com/docs/transaction-holds/\#posting-transaction-holds "Direct link to Posting transaction holds")

You can post a debit or credit transaction hold to any type of deposit account by making a `POST` request to the [`/deposits/{depositAccountId}/authorizationholds`](https://docs.mambu.com/api/api-v2/deposits/create-authorization-hold/) endpoint.

Here are some important things to consider when posting transaction holds:

- Transaction holds can be done against all types of deposit accounts, regardless of the overdraft settings.
- The `externalReferenceId` field is mandatory and must be a unique 32 characters.
- The specified amount cannot be greater than the available balance of the deposit account, or the transaction hold will fail.
- The `creditDebitIndicator` field is mandatory. Enter either `DBIT` or `CRDT`.
- The source field specifies the source of the hold: `CARD` or `ACCOUNT`.
- For credit-type holds, the amount is validated against maximum deposit value.
- The hold will be registered in **Pending** state.
- Holds never expire.

## Settling transaction holds [​](https://docs.mambu.com/docs/transaction-holds/\#settling-transaction-holds "Direct link to Settling transaction holds")

Transaction holds are settled with withdrawal or deposit transactions.

A debit-type transaction hold in the **Pending** state can be settled by making a `POST` request to the [`/deposits/{depositAccountId}/withdrawal-transactions`](https://docs.mambu.com/api/api-v2/deposits_transactions/make-withdrawal/) endpoint.

A credit-type transaction hold in the **Pending** state can be settled by making a `POST` request to the [`/deposits/{depositAccountId}/deposit-transactions`](https://docs.mambu.com/api/api-v2/deposits_transactions/make-deposit/) endpoint.

Here are some important things to consider when making a withdrawal to settle a transaction hold:

- The amount cannot differ from the amount of the authorization hold, or the action will fail.
- The `valueDate` field is not supported in cases where the `holdExternalReferenceId` is specified in the action. If the `valueDate` is specified, the request will fail.
- Once the withdrawal to settle the hold is made:
  - For debit-type holds, the hold balance on the deposit account will decrease by the specified amount, the available balance will remain the same, and the total balance will decrease.
  - For credit-type holds, the forwardBalance on the deposit account will increase by the specified amount. Available balance will not be increased.
- Once the settlement is made, the hold identified by `holdExternalReferenceId` is updated to **Settled**.

## Reversing transaction holds [​](https://docs.mambu.com/docs/transaction-holds/\#reversing-transaction-holds "Direct link to Reversing transaction holds")

You can reverse any transaction hold that is in the **Pending** state by making a `DELETE` request to the [`/deposits/{depositAccountId}/authorizationholds/{authorizationHoldExternalReferenceId}`](https://docs.mambu.com/api/api-v2/deposits/reverse-authorization-hold/) endpoint.

Once the request is successfully made, the hold balance on the deposit account decreases by the amount of the reversed hold, the available balance increases by the same amount, and the hold transaction goes into the **Reversed** state.

**Tags:**

- [transaction-holds](https://docs.mambu.com/tags/transaction-holds/)
- [payments](https://docs.mambu.com/tags/payments/)
- [risk-management](https://docs.mambu.com/tags/risk-management/)
- [compliance](https://docs.mambu.com/tags/compliance/)
- [authorization](https://docs.mambu.com/tags/authorization/)

Last updated on **Apr 13, 2026**

[Previous\\
\\
Blocking Funds in Deposit Accounts](https://docs.mambu.com/docs/blocking-funds-in-deposit-accounts/) [Next\\
\\
Technical Overdraft](https://docs.mambu.com/docs/technical-overdraft/)

- [Required permissions](https://docs.mambu.com/docs/transaction-holds/#required-permissions)
- [Posting transaction holds](https://docs.mambu.com/docs/transaction-holds/#posting-transaction-holds)
- [Settling transaction holds](https://docs.mambu.com/docs/transaction-holds/#settling-transaction-holds)
- [Reversing transaction holds](https://docs.mambu.com/docs/transaction-holds/#reversing-transaction-holds)

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