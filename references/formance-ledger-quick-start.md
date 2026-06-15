Ledger

# Quick Start

Get from zero to your first transactions in 5 minutes. By the end, you'll have funded an account, split a payment, and checked balances — all using Numscript.

This guide assumes you have `fctl` installed and a sandbox running. If not, start with the [Platform Quick Start](https://docs.formance.com/getting-started/quickstart).

## [Your First Transaction\#](https://docs.formance.com/modules/ledger/quick-start?deployment=cloud&license=ee\#your-first-transaction)

### Create a ledger

fctlcurlHTTPie

```
fctl ledger create main
```

POST/api/ledger/v2/main

This creates a ledger named `main`. All accounts and transactions will live here.

### Fund a user account

Send $100 from `@world` (an infinite funding source) to a user account:

Numscript

[Run in Numscript playground](https://playground.numscript.org/?data=eyJzY3JpcHQiOiJzZW5kIFtVU0QvMiAxMDAwMF0gKFxuICBzb3VyY2UgPSBAd29ybGRcbiAgZGVzdGluYXRpb24gPSBAdXNlcjphbGljZVxuKSIsImlucHV0cyI6IntcbiAgXCJ2YXJpYWJsZXNcIjoge30sXG4gIFwiYmFsYW5jZXNcIjoge1xuICAgIFwid29ybGRcIjoge1xuICAgICAgXCJVU0QvMlwiOiAxMDAwMDAwXG4gICAgfSxcbiAgICBcInVzZXI6YWxpY2VcIjoge1xuICAgICAgXCJVU0QvMlwiOiAxMDAwMDAwXG4gICAgfVxuICB9LFxuICBcIm1ldGFkYXRhXCI6IHt9XG59In0%3D&run=1 "Run in Numscript playground")

```
send [USD/2 10000] (
  source = @world
  destination = @user:alice
)
```

fctlcurlHTTPie

```
fctl ledger send main --numscript '{script}'
```

POST/api/ledger/v2/main/transactions

`USD/2` means US dollars with 2 decimal places. `10000` = $100.00. Accounts are created automatically on first use.

### Split a payment

Alice buys something for $30. The merchant gets 90%, the platform takes 10% as a fee:

Numscript

[Run in Numscript playground](https://playground.numscript.org/?data=eyJzY3JpcHQiOiJzZW5kIFtVU0QvMiAzMDAwXSAoXG4gIHNvdXJjZSA9IEB1c2VyOmFsaWNlXG4gIGRlc3RpbmF0aW9uID0ge1xuICAgIDkwJSB0byBAbWVyY2hhbnQ6Ym9iXG4gICAgcmVtYWluaW5nIHRvIEBwbGF0Zm9ybTpmZWVzXG4gIH1cbikiLCJpbnB1dHMiOiJ7XG4gIFwidmFyaWFibGVzXCI6IHt9LFxuICBcImJhbGFuY2VzXCI6IHtcbiAgICBcIndvcmxkXCI6IHtcbiAgICAgIFwiVVNELzJcIjogMTAwMDAwMFxuICAgIH0sXG4gICAgXCJ1c2VyOmFsaWNlXCI6IHtcbiAgICAgIFwiVVNELzJcIjogMTAwMDAwMFxuICAgIH0sXG4gICAgXCJtZXJjaGFudDpib2JcIjoge1xuICAgICAgXCJVU0QvMlwiOiAxMDAwMDAwXG4gICAgfSxcbiAgICBcInBsYXRmb3JtOmZlZXNcIjoge1xuICAgICAgXCJVU0QvMlwiOiAxMDAwMDAwXG4gICAgfVxuICB9LFxuICBcIm1ldGFkYXRhXCI6IHt9XG59In0%3D&run=1 "Run in Numscript playground")

```
send [USD/2 3000] (
  source = @user:alice
  destination = {
    90% to @merchant:bob
    remaining to @platform:fees
  }
)
```

fctlcurlHTTPie

```
fctl ledger send main --numscript '{script}'
```

POST/api/ledger/v2/main/transactions

The ledger enforces that Alice has sufficient funds. If she doesn't, the transaction is rejected — no partial execution, no overdraft.

### Check balances

fctlcurlHTTPie

```
fctl ledger accounts list --ledger=main
```

GET/api/ledger/v2/main/accounts

You should see:

| Account | Balance (USD/2) | Meaning |
| --- | --- | --- |
| `user:alice` | 7000 | $70.00 remaining |
| `merchant:bob` | 2700 | $27.00 (90% of $30) |
| `platform:fees` | 300 | $3.00 (10% of $30) |

Every cent is accounted for. The sum of all non-world balances equals the total funded.

## [What Just Happened\#](https://docs.formance.com/modules/ledger/quick-start?deployment=cloud&license=ee\#what-just-happened)

- **Double-entry**: every `send` creates balanced movements — what leaves one account enters another
- **`@world`**: a special infinite source/sink for money entering and exiting the ledger
- **`USD/2`**: [Universal Monetary Notation](https://docs.formance.com/modules/numscript/monetary-notation) — the asset type and precision, no floating-point
- **Splits**: Numscript natively handles percentage splits with `remaining` catching rounding
- **Scarcity**: the ledger rejects transactions when the source account has insufficient funds