Ledger

# Accounts

An account is a container for assets. Its primary function is to group assets together based on criteria that best suit the business's needs. This grouping helps organize and manage assets efficiently, making it easier to track financial information and understand the business's financial position. Businesses can maintain a clear and structured financial record by categorizing assets into different accounts, facilitating better decision-making and financial planning.

For example, in a bank, accounts are used to group money by customer, ensuring each customer has their own account. This system allows the bank to keep track of each customer's funds separately, providing clarity and precision in managing financial resources. By assigning individual accounts, banks can offer personalized services and maintain accurate records of each customer's deposits, withdrawals, and other transactions.

In a food delivery app, accounts are used to organize financial activities efficiently. Each restaurant has an account to manage their earnings from orders. Riders have accounts to track their delivery payments. Customers have accounts for their wallets, where they can add and spend money for orders. The platform itself maintains separate accounts for their service fees and taxes. This system ensures that all financial transactions are clearly categorized, making managing and overseeing the app's overall financial operations easier.

In the previous examples, accounts were modeled by just a balance, but the ledger tracks more granular information. For each asset in an account, the ledger records **volumes**: the cumulative `input` (total received as the destination of postings) and `output` (total sent as the source of postings). This gives a precise record of inflows and outflows, not just the net balance.

## [Balance equation\#](https://docs.formance.com/modules/ledger/core-concepts/accounts?deployment=cloud&license=ee\#balance-equation)

The balance of an account for a given asset is derived from its volumes:

Balance=Input−OutputBalance = Input - OutputBalance=Input−Output

Both `input` and `output` are append-only and always increase; the balance is never stored directly, only computed.

## [Creating accounts\#](https://docs.formance.com/modules/ledger/core-concepts/accounts?deployment=cloud&license=ee\#creating-accounts)

The number of accounts in a ledger is unlimited. Accounts also do not need to be created prior to being used, as submitting a transaction involving it will automatically make it exist in the ledger.

## [Naming accounts\#](https://docs.formance.com/modules/ledger/core-concepts/accounts?deployment=cloud&license=ee\#naming-accounts)

Account addresses follow a hierarchical structure using colon-separated segments, validated against the pattern: `^[a-zA-Z0-9_-]+(:[a-zA-Z0-9_-]+)*$`

Each segment matches `[a-zA-Z0-9_-]+`, meaning it can contain letters (a-z, A-Z), digits (0-9), underscores (\_), hyphens (-).

**Examples:**

- `users` — a single segment
- `users:001:main` — a multi-level address with `:` as separator

This hierarchical structure lets you organize accounts by type, user ID, or provider — for example `payments:stripe:customer-123` or `wallets:user:42:main`.

If an invalid account address is used in a transaction or API call, the system returns a validation error and the operation is rejected.

Bash

```
payments:001:authorizations:001
sales:001:contract
```

To enforce specific naming conventions beyond the basic pattern, use a [Ledger Schema](https://docs.formance.com/modules/ledger/working-with/ledger-schema). Schemas let you define which account structures are valid and reject transactions that use undefined accounts.

## [Using Metadata\#](https://docs.formance.com/modules/ledger/core-concepts/accounts?deployment=cloud&license=ee\#using-metadata)

Accounts can bear metadata, which is in a key-value format. This metadata can be used to store any information that is relevant to the account, like a reference to an external system, or a description of the account.

Accounts metadata can also be used in Numscript transactions, see [metadata in Numscript](https://docs.formance.com/modules/numscript/reference/metadata) for more information.

### [Updating Metadata\#](https://docs.formance.com/modules/ledger/core-concepts/accounts?deployment=cloud&license=ee\#updating-metadata)

Metadata can be updated on accounts and transactions using dedicated endpoints:

fctlcurlHTTPie

```
fctl ledger accounts set-metadata users:001 --ledger=my-ledger
```

POST/api/ledger/v2/my-ledger/accounts/users:001/metadata

fctlcurlHTTPie

```
fctl ledger transactions set-metadata 123 --ledger=my-ledger
```

POST/api/ledger/v2/my-ledger/transactions/123/metadata

### [Update Behavior\#](https://docs.formance.com/modules/ledger/core-concepts/accounts?deployment=cloud&license=ee\#update-behavior)

Metadata updates are **additive** and **idempotent**:

| Scenario | Behavior |
| --- | --- |
| Key doesn't exist | Key is added with the provided value |
| Key exists with different value | Key is updated to the new value |
| Key exists with same value | No operation (idempotent) |

Metadata updates never remove existing keys. To "remove" a metadata key, you can set its value to an empty string or a sentinel value that your application interprets as "deleted".

### [Metadata in Transactions\#](https://docs.formance.com/modules/ledger/core-concepts/accounts?deployment=cloud&license=ee\#metadata-in-transactions)

When creating transactions, you can set metadata directly in the Numscript using `set_tx_meta()`:

Numscript

[Run in Numscript playground](https://playground.numscript.org/?data=eyJzY3JpcHQiOiJzZXRfdHhfbWV0YShcInJlZmVyZW5jZVwiLCBcIm9yZGVyLTEyMzQ1XCIpXG5zZXRfdHhfbWV0YShcInR5cGVcIiwgXCJwYXltZW50XCIpXG5cbnNlbmQgW1VTRC8yIDEwMDBdIChcbiAgc291cmNlID0gQGN1c3RvbWVyczoxMjNcbiAgZGVzdGluYXRpb24gPSBAbWVyY2hhbnRzOjQ1NlxuKSIsImlucHV0cyI6IntcbiAgXCJ2YXJpYWJsZXNcIjoge30sXG4gIFwiYmFsYW5jZXNcIjoge1xuICAgIFwid29ybGRcIjoge1xuICAgICAgXCJVU0QvMlwiOiAxMDAwMDAwXG4gICAgfSxcbiAgICBcImN1c3RvbWVyczoxMjNcIjoge1xuICAgICAgXCJVU0QvMlwiOiAxMDAwMDAwXG4gICAgfSxcbiAgICBcIm1lcmNoYW50czo0NTZcIjoge1xuICAgICAgXCJVU0QvMlwiOiAxMDAwMDAwXG4gICAgfVxuICB9LFxuICBcIm1ldGFkYXRhXCI6IHt9XG59In0%3D&run=1 "Run in Numscript playground")

```
set_tx_meta("reference", "order-12345")
set_tx_meta("type", "payment")

send [USD/2 1000] (
  source = @customers:123
  destination = @merchants:456
)
```

Account metadata can also be set during transaction creation using `set_account_meta()`.