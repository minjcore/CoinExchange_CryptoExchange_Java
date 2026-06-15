For AI agents: visit https://docs.moderntreasury.com/llms.txt for an index of all pages formatted in Markdown and endpoints in OpenAPI.

As payment orders progress through a money transfer lifecycle, Modern Treasury sends webhooks to convey latest status. The message body includes both an event name and data representation of the payment order. When there is an error, it will also be included.

> 🚧
>
> Not all `PaymentOrder` events are represented by Payment Order [status](https://docs.moderntreasury.com/platform/reference/payment-order-object#payment-order-statuses).

# Payment Order Events   [Skip link to Payment Order Events](https://docs.moderntreasury.com/platform/reference/payment-orders\#payment-order-events)

| Event | Description |
| --- | --- |
| **`created`** | A payment order has been created. This happens when the [Async Create Payment Order](https://docs.moderntreasury.com/platform/reference/create-payment-order-async) endpoint is used. |
| **`updated`** | An attribute other than the payment order `status` has been modified. This happens when a user or API client edits a payment order. |
| **`failed`** | A payment order has failed. This can happen for a few reasons. If you use the [Async Create Payment Order](https://docs.moderntreasury.com/platform/reference/create-payment-order-async) endpoint and the creation fails, you will receive this event. If the payment fails at the bank or network level, then you can receive this event.<br>You can simulate failed payment orders by following the guide [here](https://docs.moderntreasury.com/platform/docs/simulate-a-payment-order-failure).<br>You may redraft a failed payment order by using the [Update Payment Order](https://docs.moderntreasury.com/platform/reference/update-payment-order) endpoint. |
| **`approved`** | A payment order has been approved. |
| **`denied`** | A payment order has been denied. |
| **`cancelled`** | A payment order has been cancelled. |
| **`begin_processing`** | A payment order has begun processing. Payment orders will no longer be editable after this point. |
| **`finish_processing`** | A payment order has finished processing and been sent to the bank. |
| **`acknowledged`** | A Payment Order has been _acknowledged_ by the bank. A Payment Order has passed initial validation & processing and may have been assigned unique identifiers by the originating bank. This **does not** mean the payment has been transmitted by the bank to the underlying network (e.g. NACHA or Fedwire). This **does not** guarantee that the payment will settle and may still fail due to compliance or other reasons. A Payment Order **can only have** _acknowledged_ event when it is in _sent_ state. Acknowledgement by bank is ignored in all other states.<br>_Does not change the payment order's state._ |
| **`confirmed`** | A Payment Order has been transmitted by the bank to the underlying network and has been _confirmed_ by the network. This is typically accompanied by the creation of `PaymentReference` objects representing unique reference numbers assigned to the payment by the network (e.g. ACH trace numbers or Fedwire IMAD/OMAD). This **does not** guarantee that the payment will settle and may still be failed or returned by the receiving bank (RDFI).<br>_Does not change the payment order's state._ |
| **`tentatively_reconciled`** | A payment order has been tentatively reconciled to a pending transaction. This can be helpful to process if you are confident the transaction will post. [See the guide for more information.](https://docs.moderntreasury.com/reconciliation/docs/tentative-reconciliation)<br>_Does not change the payment order's state._ |
| **`completed`** | A payment order has been completed. |
| **`returned`** | A payment order has been returned. You may redraft the payment order by using the [Update Payment Order](https://docs.moderntreasury.com/platform/reference/update-payment-order) endpoint. |
| **`reconciled`** | A payment order has been reconciled with a posted transaction.<br>_Does not change the payment order's state._ |
| **`receiving_account_reconciled`** | For a payment between internal accounts at different banks, funds have posted in the receiving account.<br>_Does not change the payment order's state._ |
| **`redrafted`** | A payment order has been redrafted. |
| **`reversed`** | A payment order reversal has been sent. |
| **`nsf_deferment`** | This only applies for payment orders that are using NSF Protection. If the counterparty's balance is insufficient to cover the payment order, or if there is an error from Plaid while retrieving the balance, this event is sent. This means that the payment order was not sent to the bank and is still in the `approved` state. |
| **`nsf_plaid_error_but_processing`** | This only applies for payment orders that are using NSF Protection. This event can only be fired if your NSF Protection settings have opted you into initiating the payment orders even when the Plaid check has failed.<br>When this event fires, it means that we were unable to pull the counterparty's balance from Plaid but still sent the payment order the bank. |
| **`unreconciled`** | A payment order has been unreconciled from a transaction. |
| **`linked_entity`** | A payment order has been linked to another entity (note the different data [schema](https://docs.moderntreasury.com/platform/reference/entity-links) ) |
| **`unlinked_entity`** | A payment order has been unlinked from another entity (note the different data [schema](https://docs.moderntreasury.com/platform/reference/entity-links) ) |
| **`held`** | A payment order has been placed on hold for compliance reasons. Learn more about holds [here](https://docs.moderntreasury.com/payments/docs/holds) . |
| **`resolved`** | A payment order hold has been removed. Learn more about holds [here](https://docs.moderntreasury.com/payments/docs/holds) . |

> 🚧
>
> Note that not all `PaymentOrder` events are associated with a change in `status`.

# Special Cases   [Skip link to Special Cases](https://docs.moderntreasury.com/platform/reference/payment-orders\#special-cases)

## Check Payment Order Completed Status   [Skip link to Check Payment Order Completed Status](https://docs.moderntreasury.com/platform/reference/payment-orders\#check-payment-order-completed-status)

In most cases, Check Payment Orders will be marked as `completed` when the bank reports the check as deposited by the recipient. This behavior allows you to retrieve outstanding checks by retrieving payment orders in the `sent` status.

## Acknowledged and Confirmed Events   [Skip link to Acknowledged and Confirmed Events](https://docs.moderntreasury.com/platform/reference/payment-orders\#acknowledged-and-confirmed-events)

The `acknowledged` and `confirmed` events are not available at all banking partners and for all payment types (aka “rails”). The availability of these events is dependent on the data sources available at each banking partner that Modern Treasury is integrated with and your organization’s specific configuration. Please reach out to your customer success manager (CSM) during onboarding if you need these events.

## Duplicate Events   [Skip link to Duplicate Events](https://docs.moderntreasury.com/platform/reference/payment-orders\#duplicate-events)

A single `PaymentOrder` may receive multiple events of the same type in the event of a _redraft_. When a terminal payment (i.e. `failed`, `returned`, `cancelled`) is redrafted, it will be re-transmitted and may receive certain events again. In these cases, the `id` of the `PaymentOrder` will remain constant and your system should handle this appropriately.

## Completed and Reconciled Events   [Skip link to Completed and Reconciled Events](https://docs.moderntreasury.com/platform/reference/payment-orders\#completed-and-reconciled-events)

`completed` : Triggered when the bank has accepted and executed the payment instruction, and the transaction has been posted to the customer’s ledger, indicating that funds have been accepted or released.

`reconciled` : Triggered when Modern Treasury has matched the payment order to a corresponding posted transaction from the bank. A payment should be considered fully settled once it reaches the reconciled state.

This change provides clearer visibility into the lifecycle of each payment order and improves event timing across payment types.

# Sample Events   [Skip link to Sample Events](https://docs.moderntreasury.com/platform/reference/payment-orders\#sample-events)

## Successful event   [Skip link to Successful event](https://docs.moderntreasury.com/platform/reference/payment-orders\#successful-event)

Successful Event

```json
{
  "event": "created",
  "data": {
    "id": "fd3c1f59-6d5b-466c-9126-7f8c82ad6e1c",
    "type": "ach",
    ...
  }
}
```

## Event with errors   [Skip link to Event with errors](https://docs.moderntreasury.com/platform/reference/payment-orders\#event-with-errors)

Event with errors

```json
{
  "event": "failed",
  "data": {
    "id": "fd3c1f59-6d5b-466c-9126-7f8c82ad6e1c"
  },
  "errors": {
    "code": "parameter_invalid",
    "message": "amount is invalid",
    "parameter": "amount"
  }
}
```

## Error from vendor   [Skip link to Error from vendor](https://docs.moderntreasury.com/platform/reference/payment-orders\#error-from-vendor)

Error from vendor

```json
{
  "event": "failed",
  "data": {
    "id": "4897da42-7abd-464e-ab57-d4bf13f49aa0",
    "vendor_failure_reason":"Following payer details are missing: payer_postcode"
  }
}
```

## Completed Payment Order   [Skip link to Completed Payment Order](https://docs.moderntreasury.com/platform/reference/payment-orders\#completed-payment-order)

Sample Completed & Not Reconciled Payment Order WebhookSample Completed & Reconciled Payment Order WebhookSample Payment Order Linked Webhook

```json
{
  "event": "completed",
  "data": {
    "accounting_category_id": null,
    "amount": 123100,
    "charge_bearer": null,
    "counterparty_id": "928db55e-6552-4aaf-96d7-10c693922b1f",
    "created_at": "2019-12-12T22:27:22Z",
    "currency": "USD",
    "description": null,
    "direction": "credit",
    "effective_date": "2019-12-13",
    "foreign_exchange_contract": null,
    "foreign_exchange_indicator": null,
    "id": "9f7e0efa-aeb7-4378-ae18-b4bb038e5495",
    "metadata": {},
    "object": "payment_order",
    "originating_account_id": "b9fc1ae0-d493-4f01-a7b3-b39104e802b5",
    "priority": "normal",
    "receiving_account": {
      "account_details": [\
        { ... },\
      ],
      "account_type": "checking",
      "created_at": "2019-11-21T22:51:04Z",
      "id": "71fae619-afa6-45e6-8630-ce4d0b3d6387",
      "intermediate_account_id": null,
      "object": "external_account",
      "party_address": null,
      "party_name": "John Smith",
      "party_type": null,
      "routing_details": [\
        { ... },\
      ],
      "updated_at": "2019-11-21T22:51:04Z",
      "verification_status": "unverified"
    },
    "receiving_account_id": "71fae619-afa6-45e6-8630-ce4d0b3d6387",
    "receiving_account_type": "external_account",
    "remittance_information": "December Rent",
    "statement_descriptor": "Rent",
    "status": "completed",
    "reconciliation_status": "unreconciled",
    "transaction_ids": [],
    "type": "ach",
    "transaction_monitoring_enabled": false,
    "decision_id": null,
    "updated_at": "2019-12-12T22:27:25Z"
  }
}
```

```json
{
  "event": "completed",
  "data": {
    "accounting_category_id": null,
    "amount": 123100,
    "charge_bearer": null,
    "counterparty_id": "928db55e-6552-4aaf-96d7-10c693922b1f",
    "created_at": "2019-12-12T22:27:22Z",
    "currency": "USD",
    "description": null,
    "direction": "credit",
    "effective_date": "2019-12-13",
    "foreign_exchange_contract": null,
    "foreign_exchange_indicator": null,
    "id": "9f7e0efa-aeb7-4378-ae18-b4bb038e5495",
    "metadata": {},
    "object": "payment_order",
    "originating_account_id": "b9fc1ae0-d493-4f01-a7b3-b39104e802b5",
    "priority": "normal",
    "receiving_account": {
      "account_details": [\
        { ... },\
      ],
      "account_type": "checking",
      "created_at": "2019-11-21T22:51:04Z",
      "id": "71fae619-afa6-45e6-8630-ce4d0b3d6387",
      "intermediate_account_id": null,
      "object": "external_account",
      "party_address": null,
      "party_name": "John Smith",
      "party_type": null,
      "routing_details": [\
        { ... },\
      ],
      "updated_at": "2019-11-21T22:51:04Z",
      "verification_status": "unverified"
    },
    "receiving_account_id": "71fae619-afa6-45e6-8630-ce4d0b3d6387",
    "receiving_account_type": "external_account",
    "remittance_information": "December Rent",
    "statement_descriptor": "Rent",
    "status": "completed",
    "reconciliation_status": "reconciled",
    "transaction_ids": [],
    "type": "ach",
    "transaction_monitoring_enabled": false,
    "decision_id": null,
    "updated_at": "2019-12-12T22:27:25Z"
  }
}
```

```json
{
  "event": "linked_entity",
  "data": {
    "id": "21bfd337-5d13-4c9a-8f9e-2409023d9627",
    "entity_a_id": "1bbc5c0e-fdee-4a4b-b16f-d8164444861f",
    "entity_a_type": "payment_order",
    "entity_b_id": "22ccfb02-fdee-4a4b-b16f-d8164444861f",
    "entity_b_type": "payment_order",
    "created_by_actor_id": "b45c5b0e-fdee-4a4b-b16f-d8164444861f",
    "created_by_actor_type": "user",
    "created_by_actor_name": "Some Name",
    "live_mode": true,
    "created_at": "2019-11-09T00:11:07Z",
    "updated_at": "2019-11-09T00:11:07Z"
  }
}
```

Updated about 1 month ago

* * *

Updatedabout 1 month ago

* * *