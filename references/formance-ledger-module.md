Ledger

# Ledger

Formance Ledger is a programmable, double-entry accounting database designed for financial applications. It provides an immutable, tamper-evident transaction log with built-in concurrency control, multi-asset support, and a powerful scripting language ( [Numscript](https://docs.formance.com/modules/numscript)) for modeling complex money movements.

## [Getting Started\#](https://docs.formance.com/modules/ledger?deployment=cloud&license=ee\#getting-started)

[Quick Start\\
\\
Your first transactions in 5 minutes — fund accounts, split payments, check balances.](https://docs.formance.com/modules/ledger/quick-start) [Numscript\\
\\
The scripting language for modeling financial operations — splits, holds, overdrafts, and more.](https://docs.formance.com/modules/numscript) [Accounts & Transactions\\
\\
How the data model works — accounts, transactions, and the immutable log.](https://docs.formance.com/modules/ledger/core-concepts/accounts) [Example Implementations\\
\\
Omnibus accounts, card issuing, payment acceptance, stablecoin operations.](https://docs.formance.com/examples/introduction)

## [Product Ledger vs General Ledger\#](https://docs.formance.com/modules/ledger?deployment=cloud&license=ee\#product-ledger-vs-general-ledger)

| Ledger Type | Purpose |
| --- | --- |
| **General Ledger (GL)** | Organizes financial events to produce clarity on the financial position of a business. Typically lives in an ERP (Sage) or GL tool (QuickBooks). |
| **Product Ledger** | Technological foundation for automated flow of funds. Focuses on scarcity, concurrency, auditability, immutability, and performance. |

Formance Ledger enforces the double-entry transaction model by design — every transaction involves two or more accounts in compensating directions, and balances are derived from an immutable log. It is flexible regarding accounting business rules (account classification, debit/credit conventions), letting you choose how much accounting logic to handle in real-time versus delegating to a GL.

Use Formance as a product ledger. Add basic account classification in your [Chart of Accounts](https://docs.formance.com/modules/ledger/core-concepts/chart-of-accounts) to lay a foundation for subsequent data mapping to your GL.

## [Data Immutability\#](https://docs.formance.com/modules/ledger?deployment=cloud&license=ee\#data-immutability)

Each transaction produces a hash from its data combined with the previous transaction's hash, creating a tamper-evident chain. This ensures the ledger remains a permanent, indelible history of transactions — similar to a blockchain mechanism.

Log hashing can be disabled for performance. See [Data isolation with buckets](https://docs.formance.com/v3.2/modules/ledger/working-with/data-isolation-buckets#features) for configuration.

## [Data Model\#](https://docs.formance.com/modules/ledger?deployment=cloud&license=ee\#data-model)

| Resource | Description |
| --- | --- |
| **Accounts** | Containers for assets. See [Accounts](https://docs.formance.com/modules/ledger/core-concepts/accounts). |
| **Transactions** | Movements of assets between accounts. See [Transactions](https://docs.formance.com/modules/ledger/core-concepts/transactions). |
| **Logs** | Immutable log entries (`NEW_TRANSACTION`, `SET_METADATA`) — the primary source of truth. |

All resources are fully isolated per ledger. Two ledgers can have accounts with the same name without interference.

## [Single vs Multi-Ledger\#](https://docs.formance.com/modules/ledger?deployment=cloud&license=ee\#single-vs-multi-ledger)

Formance Ledger is multi-ledger — you can operate multiple independent ledgers in a single instance. The choice depends on your application:

- **Multi-ledger**: better horizontal scaling (write locking is per-ledger), good for multi-tenant apps or high write volume with data segregation
- **Single ledger**: simpler — no need to manage cross-ledger consistency or check which ledger an account belongs to