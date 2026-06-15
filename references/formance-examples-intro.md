Cookbook

# Introduction

This guide helps you apply Formance Ledger patterns to real-world financial operations. The recipes that follow are illustrative — they show how Numscript can serve as an intent layer where business, finance, and engineering teams agree on financial logic in a clear, executable form. Your own regulatory, legal, and accounting obligations will shape the final implementation; treat these as a starting vocabulary, not a turnkey solution.

## [Intent-Based Finance\#](https://docs.formance.com/examples/introduction?deployment=cloud&license=ee\#intent-based-finance)

At the heart of the Formance approach is the concept of **Numscript as an intent layer**. This means:

### [Separation of Concerns\#](https://docs.formance.com/examples/introduction?deployment=cloud&license=ee\#separation-of-concerns)

### Define Actors and Accounts

Your business and finance teams identify all parties involved in your operations: customers, banks, payment processors, internal accounts, liability accounts, etc.

Each actor is represented by accounts in the ledger with clear naming conventions and metadata.

### Map Events and Bookings

Together, teams enumerate all the financial events that can occur in your business: deposits, withdrawals, authorizations, settlements, refunds, chargebacks, minting, burning, etc.

Each event translates to specific accounting movements between accounts.

### Write Numscripts

Once actors, accounts, and events are defined, Numscripts encode the business logic. These scripts become the single source of truth that both business and technical teams can read and validate.

Numscripts are intentionally readable. Your finance team should be able to review a script and confirm it matches their understanding of the business logic.

### Execute with Variables

When events occur in your system, you simply "play" the appropriate Numscript with the correct variables. The ledger handles the rest: validating constraints, recording transactions, maintaining balances.

### [Benefits of This Approach\#](https://docs.formance.com/examples/introduction?deployment=cloud&license=ee\#benefits-of-this-approach)

- **Business Clarity**: Finance and operations teams can understand and validate the logic without deep technical knowledge.
- **Rapid Iteration**: Changes to business logic require updating Numscripts, not redeploying entire systems.
- **Consistency**: The same Numscript produces the same results, eliminating implementation drift across services.

## [Leveraging Bi-Temporality\#](https://docs.formance.com/examples/introduction?deployment=cloud&license=ee\#leveraging-bi-temporality)

A critical feature of the Formance Ledger is **[bi-temporality](https://docs.formance.com/modules/ledger/working-with/bi-temporality)**, which means every transaction tracks two timestamps:

- **Request Time**: When the transaction was submitted to the ledger (machine clock time)
- **Transaction Time**: When the transaction is considered to have occurred (business effective time)

### [Why This Matters for Financial Operations\#](https://docs.formance.com/examples/introduction?deployment=cloud&license=ee\#why-this-matters-for-financial-operations)

In real-world financial operations, you frequently encounter timing mismatches:

- **Settlement Delays**: Payment authorizations happen in real-time, but bank settlements occur T+1 to T+3 days later. Your ledger should reflect the true effective dates of these movements, not just when you learned about them.
- **Batch Processing**: Bank statements arrive the next day. You need to record yesterday's transactions with their correct effective timestamps.
- **Corrections and Adjustments**: When errors are discovered or chargebacks occur, you may need to backdate transactions to reflect when they truly should have occurred.
- **Multi-Timezone Operations**: Events happening in different timezones need consistent temporal representation.

### [Implementing Bi-Temporality in Your Numscripts\#](https://docs.formance.com/examples/introduction?deployment=cloud&license=ee\#implementing-bi-temporality-in-your-numscripts)

When executing Numscripts for real-world events, **always provide the appropriate transaction timestamp**, if reporting for point in time balances of accounts is important:

JSON

```
{
  "script": {
    "plain": "send [USD/2 100.00] (\n  source = @bank:account\n  destination = @user:123:main\n)",
    "vars": {
      "amount": "100.00",
      "userId": "123"
    }
  },
  "timestamp": "2024-10-20T14:30:00Z"  // Business effective time, not current time
}
```

If you don't specify a timestamp, the ledger uses the current machine time.

**Backdating Considerations**

When inserting backdated transactions (transaction time in the past), the ledger validates that the entire timeline remains consistent. This means a backdated transaction could be rejected if it would cause account balances to become invalid at any point in the future timeline.

Plan your account structures and constraints carefully to accommodate late-arriving information, or consider allowing overdraft.

## [Key Capabilities Demonstrated\#](https://docs.formance.com/examples/introduction?deployment=cloud&license=ee\#key-capabilities-demonstrated)

Throughout these recipes, you'll see how the Formance Ledger handles:

### [Multi-Asset Operations\#](https://docs.formance.com/examples/introduction?deployment=cloud&license=ee\#multi-asset-operations)

Accounts are multi-asset by default. The same account structure and Numscripts work across USD, EUR, GBP, stablecoins, or any other asset you define.

Numscript

[Run in Numscript playground](https://playground.numscript.org/?data=eyJzY3JpcHQiOiJ2YXJzIHtcbiAgbW9uZXRhcnkgJHBheW1lbnRcbn1cblxuc2VuZCAkcGF5bWVudCAoXG4gIHNvdXJjZSA9IEBiYW5rOm5vc3Ryb1xuICBkZXN0aW5hdGlvbiA9IEB1c2VyOm1haW5cbikiLCJpbnB1dHMiOiJ7XG4gIFwidmFyaWFibGVzXCI6IHtcbiAgICBcInBheW1lbnRcIjogXCJVU0QvMiAxMDBcIlxuICB9LFxuICBcImJhbGFuY2VzXCI6IHtcbiAgICBcIndvcmxkXCI6IHtcbiAgICAgIFwiVVNELzJcIjogMTAwMDAwMFxuICAgIH0sXG4gICAgXCJiYW5rOm5vc3Ryb1wiOiB7XG4gICAgICBcIlVTRC8yXCI6IDEwMDAwMDBcbiAgICB9LFxuICAgIFwidXNlcjptYWluXCI6IHtcbiAgICAgIFwiVVNELzJcIjogMTAwMDAwMFxuICAgIH1cbiAgfSxcbiAgXCJtZXRhZGF0YVwiOiB7fVxufSJ9&run=1 "Run in Numscript playground")

```
vars {
  monetary $payment
}

send $payment (
  source = @bank:nostro
  destination = @user:main
)
```

The same script runs against any currency — call it with `$payment = "USD/2 100"`, `"EUR/2 50"`, `"BTC/8 1000000"`, or any asset you've defined. A `monetary` is the asset code and amount together (e.g. `USD/2 100`), sent bare with `send $payment (…)`. To pass them separately use `asset $a` and `number $n` declarations, then write `send [$a $n] (…)`.

### [Rich Metadata\#](https://docs.formance.com/examples/introduction?deployment=cloud&license=ee\#rich-metadata)

Attach business context to accounts and transactions directly within your Numscripts for powerful querying, reporting, and integration.

Numscript

[Run in Numscript playground](https://playground.numscript.org/?data=eyJzY3JpcHQiOiJ2YXJzIHtcbiAgYWNjb3VudCAkdXNlclxuICBtb25ldGFyeSAkYW1vdW50XG4gIHN0cmluZyAkYXV0aF9pZFxuICBzdHJpbmcgJGNhcmRfc2NoZW1lXG59XG5cbnNlbmQgJGFtb3VudCAoXG4gIHNvdXJjZSA9IHtcbiAgICBAdXNlcjptYWluXG4gICAgQHVzZXI6Y3JlZGl0X2xpbmVcbiAgfVxuICBkZXN0aW5hdGlvbiA9IEBob2xkOmF1dGhvcml6YXRpb25cbilcblxuc2V0X3R4X21ldGEoXCJwYXltZW50X21ldGhvZFwiLCBcImNhcmRcIilcbnNldF90eF9tZXRhKFwiY2FyZF9zY2hlbWVcIiwgJGNhcmRfc2NoZW1lKVxuc2V0X3R4X21ldGEoXCJhdXRob3JpemF0aW9uX2lkXCIsICRhdXRoX2lkKSIsImlucHV0cyI6IntcbiAgXCJ2YXJpYWJsZXNcIjoge1xuICAgIFwidXNlclwiOiBcImRlZmF1bHQ6dXNlclwiLFxuICAgIFwiYW1vdW50XCI6IFwiVVNELzIgMTAwXCIsXG4gICAgXCJhdXRoX2lkXCI6IFwiYXV0aF9pZFwiLFxuICAgIFwiY2FyZF9zY2hlbWVcIjogXCJjYXJkX3NjaGVtZVwiXG4gIH0sXG4gIFwiYmFsYW5jZXNcIjoge1xuICAgIFwid29ybGRcIjoge1xuICAgICAgXCJVU0QvMlwiOiAxMDAwMDAwXG4gICAgfSxcbiAgICBcInVzZXI6bWFpblwiOiB7XG4gICAgICBcIlVTRC8yXCI6IDEwMDAwMDBcbiAgICB9LFxuICAgIFwidXNlcjpjcmVkaXRfbGluZVwiOiB7XG4gICAgICBcIlVTRC8yXCI6IDEwMDAwMDBcbiAgICB9LFxuICAgIFwiaG9sZDphdXRob3JpemF0aW9uXCI6IHtcbiAgICAgIFwiVVNELzJcIjogMTAwMDAwMFxuICAgIH0sXG4gICAgXCJkZWZhdWx0OnVzZXJcIjoge1xuICAgICAgXCJVU0QvMlwiOiAxMDAwMDAwXG4gICAgfVxuICB9LFxuICBcIm1ldGFkYXRhXCI6IHt9XG59In0%3D&run=1 "Run in Numscript playground")

```
vars {
  account $user
  monetary $amount
  string $auth_id
  string $card_scheme
}

send $amount (
  source = {
    @user:main
    @user:credit_line
  }
  destination = @hold:authorization
)

set_tx_meta("payment_method", "card")
set_tx_meta("card_scheme", $card_scheme)
set_tx_meta("authorization_id", $auth_id)
```

Metadata can be any type: strings, numbers, monetary values, portions, or even account references. See the [Numscript metadata reference](https://docs.formance.com/modules/numscript/reference/metadata) for complete details.

### [Constraint Enforcement\#](https://docs.formance.com/examples/introduction?deployment=cloud&license=ee\#constraint-enforcement)

The ledger automatically enforces business rules:

- Prevent overdrafts on customer accounts
- Ensure sufficient reserves for liabilities
- Validate transaction amounts and parties

* * *

These are **starting points**, not finished solutions. Your business is unique, and your ledger implementation should reflect that uniqueness while maintaining the clarity and auditability that Formance enables.