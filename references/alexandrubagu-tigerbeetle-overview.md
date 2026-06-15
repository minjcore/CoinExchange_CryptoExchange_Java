## What is TigerBeetle?

TigerBeetle is a purpose-built financial accounting database designed specifically for **Online Transaction Processing (OLTP)** workloads. Unlike general-purpose databases like PostgreSQL or MySQL, TigerBeetle is architected from the ground up to handle the unique challenges of financial transactions at scale.

Built in Zig for maximum performance, TigerBeetle uses a debit-credit accounting model and provides strong consistency guarantees through distributed consensus. It's designed to process up to **1 million transactions per second** while maintaining strict safety and durability requirements.

## The Debit-Credit Model: Centuries of Proven Accounting

TigerBeetle uses **double-entry bookkeeping** model that captures the "who, what, when, where, why, and how much" of every transaction.

### Core Principles

- **Every transfer records movement of value** from one account to another (single debit, single credit)
- **Money never appears or disappears** \- it always has a source and destination
- **Immutability** \- once recorded, transfers cannot be erased. Corrections use separate reversal transfers
- **Simple and complete** \- two entities (accounts, transfers) and one invariant (every debit has an equal credit) model any exchange of value

## Understanding Financial Account Types

TigerBeetle implements the five fundamental account types from classical accounting. Understanding these types is crucial for proper data modeling.

### The Five Account Types

#### 1\. Asset Accounts

**Definition:** What you own that could produce income or be sold.

**Examples:**

- Cash in bank
- Accounts receivable (money owed to you)
- Inventory
- Equipment and property

**Balance calculation:**`balance = debits - credits`

**TigerBeetle flag:**`credits_must_not_exceed_debits` (prevents negative balances)

#### 2\. Liability Accounts

**Definition:** What you owe to other people or entities.

**Examples:**

- Loans and mortgages
- Accounts payable (money you owe)
- Customer deposits (e.g., a bank account from the bank's perspective)
- Deferred revenue

**Balance calculation:**`balance = credits - debits`

**TigerBeetle flag:**`debits_must_not_exceed_credits` (prevents negative balances)

#### 3\. Equity Accounts

**Definition:** The residual value owned by shareholders after deducting liabilities.

**Examples:**

- Shareholder capital
- Retained earnings
- Owner's equity

**Balance calculation:**`balance = credits - debits`

#### 4\. Income Accounts

**Definition:** Increases in assets or decreases in liabilities that increase equity.

**Examples:**

- Sales revenue
- Service fees
- Interest income
- Commission income

**Balance calculation:**`balance = credits - debits`

#### 5\. Expense Accounts

**Definition:** Decreases in assets or increases in liabilities that decrease equity.

**Examples:**

- Salaries and wages
- Rent
- Utilities
- Marketing costs

**Balance calculation:**`balance = debits - credits`

### The Accounting Equation

All five account types maintain this fundamental equation:

```
Assets - Liabilities = Equity + Income - Expenses
```

Every transaction involves at least one debit and one credit, ensuring both sides of the equation remain balanced.

### Debits and Credits Explained

Unlike intuitive "positive/negative" systems, accounting uses debits and credits based on account type:

| Account Type | Debit Effect | Credit Effect |
| --- | --- | --- |
| **Asset** | Increases balance | Decreases balance |
| **Liability** | Decreases balance | Increases balance |
| **Equity** | Decreases balance | Increases balance |
| **Income** | Decreases balance | Increases balance |
| **Expense** | Increases balance | Decreases balance |

### Real-World Example: User Makes a Purchase

When a user pays $100 to a merchant:

```
Transfer 1:
  debit_account_id: user_cash_account (Asset)      # Decreases user's cash
  credit_account_id: merchant_cash_account (Asset) # Increases merchant's cash
  amount: 10000 # $100.00 in cents

Transfer 2 (merchant records revenue):
  debit_account_id: merchant_cash_account (Asset)  # Cash received
  credit_account_id: merchant_revenue (Income)     # Records income
  amount: 10000
```

### Perspective Matters

The same financial instrument appears as different account types depending on perspective:

- **Bank deposit:** Asset for the depositor, Liability for the bank
- **Loan:** Asset for the lender, Liability for the borrower
- **Invoice:** Asset (receivable) for seller, Liability (payable) for buyer

### TigerBeetle's Data Model

TigerBeetle enforces the debit-credit schema natively with each transfer capturing:

- **debit\_account\_id** and **credit\_account\_id** \- Who is transacting
- **ledger** \- What type of asset (each ledger tracks a separate asset type, e.g., USD, EUR, loyalty points)
- **timestamp** \- When the transfer was processed
- **user\_data\_32/64/128** \- Custom metadata (e.g., transaction locale, reference IDs)
- **code** \- Why the transfer happened (maps to business events like "purchase", "refund", "fee")
- **amount** \- How much was transferred (unsigned 128-bit integer)

### Handling Fractional Amounts

TigerBeetle stores amounts as unsigned 128-bit integers. To represent fractional currencies, map the smallest useful unit to 1.

**Asset scale** expresses this as a power of 10:

- USD cents: scale = 2 (100 cents = $1.00), so $123.45 = 12345
- BTC satoshis: scale = 8, so 0.00000001 BTC = 1
- JPY: scale = 0 (no fractional units), so ¥1000 = 1000

**Important:** Asset scales cannot be changed after account creation due to immutability constraints.

## Compound Transfers: Multi-Party Transactions

While TigerBeetle's core transfer model supports single debit/single credit for maximum performance, real-world scenarios often require multi-party transactions. TigerBeetle provides patterns for implementing these using **linked events**.

### Linked Events for Atomicity

The `flags.linked` property creates chains of transfers that succeed or fail together as an atomic unit. When a transfer has the linked flag set, it connects to the next transfer in the request. If any transfer in the chain fails, all transfers in that chain fail.

```
// Example: Three linked transfers (A, B, C)
// All three succeed together or all three fail together
Transfer A: { ..., flags: [:linked] }
Transfer B: { ..., flags: [:linked] }
Transfer C: { ..., flags: [] } // Last one terminates the chain
```

### Pattern 1: One-to-Many Transfers

Scenario: Split a single payment across multiple recipients (e.g., splitting tips, revenue sharing).

```
defmodule PaymentSystem.CompoundTransfers do
  # Split $100 from user to three recipients ($50, $30, $20)
  def split_payment(user_account, recipients_and_amounts) do
    transfers = recipients_and_amounts
    |> Enum.with_index()
    |> Enum.map(fn {{recipient_account, amount}, index} ->
      is_last = index == length(recipients_and_amounts) - 1

      %{
        id: generate_transfer_id(),
        debit_account_id: user_account,
        credit_account_id: recipient_account,
        amount: amount,
        ledger: @ledger_usd,
        code: @code_split_payment,
        flags: if(is_last, do: [], else: [:linked])
      }
    end)

    TigerBeetle.create_transfers(transfers)
  end
end

# Usage
split_payment(user_wallet, [\
  {recipient_1, 5000}, # $50.00\
  {recipient_2, 3000}, # $30.00\
  {recipient_3, 2000}  # $20.00\
])
```

### Pattern 2: Many-to-One Transfers

Scenario: Collect funds from multiple sources into one account (e.g., collecting fees, pooling funds).

```
# Collect $10 fee from 5 different users into platform account
def collect_fees(user_accounts, platform_account, fee_amount) do
  transfers = user_accounts
  |> Enum.with_index()
  |> Enum.map(fn {user_account, index} ->
    is_last = index == length(user_accounts) - 1

    %{
      id: generate_transfer_id(),
      debit_account_id: user_account,
      credit_account_id: platform_account,
      amount: fee_amount,
      ledger: @ledger_usd,
      code: @code_platform_fee,
      flags: if(is_last, do: [], else: [:linked])
    }
  end)

  TigerBeetle.create_transfers(transfers)
end
```

### Pattern 3: Many-to-Many with Control Account

For complex scenarios with multiple debits **and** multiple credits, use an intermediary **control account**:

```
# Transfer funds from 3 users to 2 merchants (e.g., group purchase)
def group_purchase(buyer_accounts, seller_accounts, control_account) do
  # Phase 1: Debit all buyers to control account (linked)
  buyer_transfers = buyer_accounts
  |> Enum.with_index()
  |> Enum.map(fn {{account, amount}, index} ->
    %{
      id: generate_transfer_id(),
      debit_account_id: account,
      credit_account_id: control_account,
      amount: amount,
      ledger: @ledger_usd,
      code: @code_group_purchase,
      flags: [:linked] # All buyer transfers are linked
    }
  end)

  # Phase 2: Credit all sellers from control account (linked)
  seller_transfers = seller_accounts
  |> Enum.with_index()
  |> Enum.map(fn {{account, amount}, index} ->
    is_last = index == length(seller_accounts) - 1

    %{
      id: generate_transfer_id(),
      debit_account_id: control_account,
      credit_account_id: account,
      amount: amount,
      ledger: @ledger_usd,
      code: @code_group_purchase,
      flags: if(is_last, do: [], else: [:linked])
    }
  end)

  all_transfers = buyer_transfers ++ seller_transfers
  TigerBeetle.create_transfers(all_transfers)
end
```

### Balancing Transfers

TigerBeetle provides special flags for conditional transfers based on available balance:

- `flags.balancing_debit` \- Transfer up to the available debit balance (useful for "drain account" operations)
- `flags.balancing_credit` \- Transfer up to the available credit balance

```
# Drain all available funds from an account
%{
  id: generate_transfer_id(),
  debit_account_id: source_account,
  credit_account_id: destination_account,
  amount: 0, # Will be calculated based on available balance
  ledger: @ledger_usd,
  code: @code_account_closure,
  flags: [:balancing_debit]
}
```

## Performance: Built for Speed

TigerBeetle achieves exceptional performance through multiple design decisions:

### 1\. Interface Designed for OLTP

Unlike traditional databases where business logic lives in the application, **TigerBeetle embeds the accounting logic inside the database**. Applications speak debit-credit directly without translating to SQL. This eliminates expensive network locks and round trips.

### 2\. Pervasive Batching

TigerBeetle processes batches of up to **8,190 transfers per request**. The cost of replication through consensus is paid once per batch, making it nearly as fast as an in-memory hash map while providing extreme durability.

Under light load, batches automatically shrink to trade unnecessary throughput for better latency.

### 3\. Extreme Engineering

- **Built from scratch in Zig** \- no dependencies, all layers co-designed for OLTP
- **Cache-aligned data structures** \- transfers are 128 bytes, cache-line aligned. Processing a batch is one tight CPU loop
- **Static memory allocation** \- never runs out of memory, no GC pauses, no mutex contention, no fragmentation
- **io\_uring support** \- zero-syscall networking and storage I/O
- **Single-threaded by design** \- avoids contention issues and lock overhead

### Why Single-Threaded?

Financial databases are notoriously difficult to shard because:

- Business transactions involve multiple accounts across shards
- Hot accounts (business income/expense accounts) are involved in many transactions
- Cross-shard transactions become complex and slow
- Row locks on hot accounts create bottlenecks

TigerBeetle provides strong consistency without row locks, sidestepping contention issues entirely. The single-core design with extreme optimizations delivers higher throughput than multi-threaded approaches.

## Safety and Reliability

TigerBeetle provides enterprise-grade safety guarantees essential for financial systems:

### Distributed Consensus

TigerBeetle uses a **consensus algorithm** to replicate data across multiple nodes. This provides:

- **Fault tolerance** \- continues operating if nodes fail
- **Strong consistency** \- all nodes see the same state
- **Durability** \- data is safely replicated before acknowledging writes

### Immutability

Unlike SQL databases with UPDATE and DELETE, TigerBeetle enforces **append-only immutability**. Transfers cannot be modified or deleted, ensuring:

- Complete audit trail
- Protection against accidental data loss
- Regulatory compliance
- Easy reconciliation

## Real-World Example: Payment Processing System with Elixir

Let's build a practical payment processing system that handles user wallets, merchant payments, and platform fees using TigerBeetle and Elixir.

### Designing the Account Structure

Before coding, let's design our account structure using the five account types. This is a critical step in any TigerBeetle implementation.

#### For a Payment Platform (e.g., Stripe-like service)

| Account | Type | Flag | Purpose |
| --- | --- | --- | --- |
| User Wallet | Asset | credits\_must\_not\_exceed\_debits | User's cash balance (what they own) |
| Merchant Account | Asset | credits\_must\_not\_exceed\_debits | Merchant's receivable funds |
| Platform Revenue | Income | debits\_must\_not\_exceed\_credits | Platform's fee income |
| User Liability (Bank View) | Liability | debits\_must\_not\_exceed\_credits | What platform owes to users (bank perspective) |
| Operating Expenses | Expense | credits\_must\_not\_exceed\_debits | Platform operational costs |

#### Example Transaction Flow

When a user pays $100 to a merchant with a $2.50 platform fee:

```
# Step 1: Debit user wallet (Asset decreases)
Transfer 1:
  debit_account_id: user_wallet (Asset)
  credit_account_id: merchant_account (Asset)
  amount: 9750 # $97.50 to merchant

# Step 2: Collect platform fee (Income increases)
Transfer 2:
  debit_account_id: user_wallet (Asset)
  credit_account_id: platform_revenue (Income)
  amount: 250 # $2.50 fee

# These can be linked for atomicity
transfers = [\
  %{..., flags: [:linked]},  # Merchant payment\
  %{..., flags: []}          # Platform fee\
]
```

### Installation

First, install TigerBeetle:

```
# Download TigerBeetle binary
curl -L https://tigerbeetle.com/download/tigerbeetle-latest-x86_64-linux.zip -o tigerbeetle.zip
unzip tigerbeetle.zip
chmod +x tigerbeetle

# Create and format a data file (for a 3-replica cluster)
./tigerbeetle format --cluster=0 --replica=0 --replica-count=3 0_0.tigerbeetle
./tigerbeetle format --cluster=0 --replica=1 --replica-count=3 0_1.tigerbeetle
./tigerbeetle format --cluster=0 --replica=2 --replica-count=3 0_2.tigerbeetle

# Start the cluster
./tigerbeetle start --addresses=3000,3001,3002 0_0.tigerbeetle &
./tigerbeetle start --addresses=3000,3001,3002 0_1.tigerbeetle &
./tigerbeetle start --addresses=3000,3001,3002 0_2.tigerbeetle &
```

### Elixir Integration

Add the TigerBeetle Elixir client to your `mix.exs`:

```
defp deps do
  [\
    {:tigerbeetle, "~> 0.1"}\
  ]
end
```

### Payment System Architecture

Our system will handle:

- **User wallets** \- each user has a wallet account
- **Merchant accounts** \- merchants receive payments
- **Platform revenue account** \- collects fees
- **Pending transfers** \- two-phase transfers for authorization and capture

### Account Setup

```
defmodule PaymentSystem.Accounts do
  @moduledoc """
  Manages TigerBeetle accounts for the payment system.
  """

  @ledger_usd 1
  @code_wallet 1
  @code_merchant 2
  @code_platform 3

  def create_user_wallet(user_id) do
    account = %{
      id: generate_account_id(user_id, :wallet),
      ledger: @ledger_usd,
      code: @code_wallet,
      flags: [:debits_must_not_exceed_credits], # Prevent overdrafts
      user_data_128: encode_user_id(user_id)
    }

    TigerBeetle.create_accounts([account])
  end

  def create_merchant_account(merchant_id) do
    account = %{
      id: generate_account_id(merchant_id, :merchant),
      ledger: @ledger_usd,
      code: @code_merchant,
      flags: [],
      user_data_128: encode_merchant_id(merchant_id)
    }

    TigerBeetle.create_accounts([account])
  end

  def create_platform_revenue_account() do
    account = %{
      id: platform_account_id(),
      ledger: @ledger_usd,
      code: @code_platform,
      flags: [],
      user_data_128: 0
    }

    TigerBeetle.create_accounts([account])
  end

  defp generate_account_id(entity_id, type) do
    # Use consistent hashing to generate unique 128-bit account IDs
    :crypto.hash(:sha256, "#{type}:#{entity_id}")
    |> :binary.decode_unsigned()
    |> rem(340_282_366_920_938_463_463_374_607_431_768_211_456)
  end

  defp platform_account_id(), do: 1

  defp encode_user_id(user_id), do: user_id
  defp encode_merchant_id(merchant_id), do: merchant_id
end
```

## Two-Phase Transfers: Authorization and Settlement

TigerBeetle implements two-phase transfers, inspired by the [two-phase commit protocol](https://en.wikipedia.org/wiki/Two-phase_commit_protocol) used in distributed transactions. This pattern separates fund reservation (authorization) from fund movement (settlement).

### How Two-Phase Transfers Work

#### Phase 1: Reserve Funds (Pending Transfer)

Create a transfer with `flags.pending`. This reserves the amount in the accounts' `debits_pending` and `credits_pending` fields without affecting the posted balance.

```
pending_transfer = %{
  id: transfer_id,
  debit_account_id: user_wallet,
  credit_account_id: merchant_account,
  amount: 10000, # $100.00
  ledger: @ledger_usd,
  code: @code_payment,
  flags: [:pending],
  timeout: 3600 # Optional: auto-void after 1 hour
}
```

#### Phase 2: Resolve Funds (Post, Void, or Expire)

You have three options to resolve a pending transfer:

**1\. Post (Capture):** Move funds to destination

```
post_transfer = %{
  id: new_transfer_id,
  pending_id: transfer_id, # Reference to pending transfer
  amount: 10000, # Can post partial amount or full amount
  flags: [:post_pending_transfer]
  # Other fields can be 0 or match the pending transfer
}
```

**2\. Void (Cancel):** Return funds to original account

```
void_transfer = %{
  id: new_transfer_id,
  pending_id: transfer_id,
  flags: [:void_pending_transfer]
  # Amount and accounts are ignored
}
```

**3\. Expire (Automatic):** If timeout is set and elapses, funds automatically return

### Key Constraints

- **Single resolution:** Each pending transfer can only be resolved once
- **Partial posting:** You can post less than the pending amount; the remainder returns to the debit account
- **Immutability:** Posting/voiding creates a new transfer; the pending transfer remains unchanged
- **Account invariants respected:** Reserved amounts respect balance constraints during all phases

### When to Use Two-Phase Transfers

- **Hotel/rental reservations:** Hold funds during booking, capture on check-in, void on cancellation
- **E-commerce:** Authorize at checkout, capture when shipping, void if out of stock
- **Ride-sharing:** Pre-authorize estimated fare, capture actual amount after ride
- **Marketplace escrow:** Hold buyer funds, release to seller after delivery confirmation
- **Recurring subscriptions:** Authorize before billing period, capture if service is active

### Payment Processing with Two-Phase Transfers

Let's implement a complete payment flow with authorization, capture, and void capabilities:

```
defmodule PaymentSystem.Transfers do
  @moduledoc """
  Handles payment transfers with authorization and capture flow.
  """

  @ledger_usd 1
  @code_payment 100
  @code_fee 101
  @code_refund 102

  @platform_fee_percent 2.5

  def authorize_payment(user_wallet_id, merchant_id, amount_cents, payment_id) do
    merchant_account_id = PaymentSystem.Accounts.generate_account_id(merchant_id, :merchant)
    platform_account_id = PaymentSystem.Accounts.platform_account_id()

    # Calculate platform fee
    fee_cents = calculate_fee(amount_cents)
    merchant_amount = amount_cents - fee_cents

    # Create pending transfer (authorization)
    transfer_to_merchant = %{
      id: generate_transfer_id(payment_id, :merchant),
      debit_account_id: user_wallet_id,
      credit_account_id: merchant_account_id,
      amount: merchant_amount,
      ledger: @ledger_usd,
      code: @code_payment,
      flags: [:pending], # Two-phase: pending authorization
      user_data_128: payment_id
    }

    transfer_to_platform = %{
      id: generate_transfer_id(payment_id, :platform),
      debit_account_id: user_wallet_id,
      credit_account_id: platform_account_id,
      amount: fee_cents,
      ledger: @ledger_usd,
      code: @code_fee,
      flags: [:pending],
      user_data_128: payment_id
    }

    case TigerBeetle.create_transfers([transfer_to_merchant, transfer_to_platform]) do
      {:ok, _} -> {:ok, :authorized}
      {:error, reason} -> {:error, reason}
    end
  end

  def capture_payment(payment_id) do
    # Post the pending transfers (capture authorization)
    merchant_transfer_id = generate_transfer_id(payment_id, :merchant)
    platform_transfer_id = generate_transfer_id(payment_id, :platform)

    capture_merchant = %{
      id: generate_transfer_id(payment_id, :merchant_capture),
      debit_account_id: 0, # Not used for posting
      credit_account_id: 0, # Not used for posting
      amount: 0,
      pending_id: merchant_transfer_id, # References pending transfer
      ledger: @ledger_usd,
      code: @code_payment,
      flags: [:post_pending_transfer],
      user_data_128: payment_id
    }

    capture_platform = %{
      id: generate_transfer_id(payment_id, :platform_capture),
      debit_account_id: 0,
      credit_account_id: 0,
      amount: 0,
      pending_id: platform_transfer_id,
      ledger: @ledger_usd,
      code: @code_fee,
      flags: [:post_pending_transfer],
      user_data_128: payment_id
    }

    case TigerBeetle.create_transfers([capture_merchant, capture_platform]) do
      {:ok, _} -> {:ok, :captured}
      {:error, reason} -> {:error, reason}
    end
  end

  def void_payment(payment_id) do
    # Void pending transfers (cancel authorization)
    merchant_transfer_id = generate_transfer_id(payment_id, :merchant)
    platform_transfer_id = generate_transfer_id(payment_id, :platform)

    void_merchant = %{
      id: generate_transfer_id(payment_id, :merchant_void),
      debit_account_id: 0,
      credit_account_id: 0,
      amount: 0,
      pending_id: merchant_transfer_id,
      ledger: @ledger_usd,
      code: @code_payment,
      flags: [:void_pending_transfer],
      user_data_128: payment_id
    }

    void_platform = %{
      id: generate_transfer_id(payment_id, :platform_void),
      debit_account_id: 0,
      credit_account_id: 0,
      amount: 0,
      pending_id: platform_transfer_id,
      ledger: @ledger_usd,
      code: @code_fee,
      flags: [:void_pending_transfer],
      user_data_128: payment_id
    }

    case TigerBeetle.create_transfers([void_merchant, void_platform]) do
      {:ok, _} -> {:ok, :voided}
      {:error, reason} -> {:error, reason}
    end
  end

  def refund_payment(user_wallet_id, merchant_id, amount_cents, payment_id, refund_id) do
    merchant_account_id = PaymentSystem.Accounts.generate_account_id(merchant_id, :merchant)
    platform_account_id = PaymentSystem.Accounts.platform_account_id()

    fee_cents = calculate_fee(amount_cents)
    merchant_amount = amount_cents - fee_cents

    # Reverse the original transfers
    refund_from_merchant = %{
      id: generate_transfer_id(refund_id, :merchant),
      debit_account_id: merchant_account_id,
      credit_account_id: user_wallet_id,
      amount: merchant_amount,
      ledger: @ledger_usd,
      code: @code_refund,
      flags: [],
      user_data_128: payment_id # Reference original payment
    }

    refund_from_platform = %{
      id: generate_transfer_id(refund_id, :platform),
      debit_account_id: platform_account_id,
      credit_account_id: user_wallet_id,
      amount: fee_cents,
      ledger: @ledger_usd,
      code: @code_refund,
      flags: [],
      user_data_128: payment_id
    }

    case TigerBeetle.create_transfers([refund_from_merchant, refund_from_platform]) do
      {:ok, _} -> {:ok, :refunded}
      {:error, reason} -> {:error, reason}
    end
  end

  defp calculate_fee(amount_cents) do
    trunc(amount_cents * @platform_fee_percent / 100)
  end

  defp generate_transfer_id(payment_id, suffix) do
    :crypto.hash(:sha256, "#{payment_id}:#{suffix}")
    |> :binary.decode_unsigned()
    |> rem(340_282_366_920_938_463_463_374_607_431_768_211_456)
  end
end
```

### Querying Account Balances

```
defmodule PaymentSystem.Queries do
  @moduledoc """
  Query account balances and transfer history.
  """

  def get_wallet_balance(user_id) do
    account_id = PaymentSystem.Accounts.generate_account_id(user_id, :wallet)

    case TigerBeetle.lookup_accounts([account_id]) do
      {:ok, [account]} ->
        balance = account.credits_posted - account.debits_posted
        pending = account.credits_pending - account.debits_pending

        {:ok, %{
          available: balance,
          pending: pending,
          total: balance + pending
        }}

      {:error, reason} -> {:error, reason}
    end
  end

  def get_transfer_history(account_id, opts \\ []) do
    limit = Keyword.get(opts, :limit, 100)

    filter = %{
      account_id: account_id,
      timestamp_min: 0,
      timestamp_max: :max,
      limit: limit,
      flags: [:debits, :credits]
    }

    case TigerBeetle.get_account_transfers(filter) do
      {:ok, transfers} -> {:ok, transfers}
      {:error, reason} -> {:error, reason}
    end
  end
end
```

### Usage Example

```
# Create accounts
{:ok, _} = PaymentSystem.Accounts.create_user_wallet(user_id: 12345)
{:ok, _} = PaymentSystem.Accounts.create_merchant_account(merchant_id: 67890)
{:ok, _} = PaymentSystem.Accounts.create_platform_revenue_account()

# Process payment with authorization and capture
user_wallet_id = PaymentSystem.Accounts.generate_account_id(12345, :wallet)
payment_id = generate_unique_payment_id()

# Step 1: Authorize payment ($100.00)
{:ok, :authorized} = PaymentSystem.Transfers.authorize_payment(
  user_wallet_id,
  67890,
  10_000, # $100.00 in cents
  payment_id
)

# Check pending balance
{:ok, balance} = PaymentSystem.Queries.get_wallet_balance(12345)
# balance.pending == 10_000

# Step 2: Capture payment (or void if needed)
{:ok, :captured} = PaymentSystem.Transfers.capture_payment(payment_id)
# Or: {:ok, :voided} = PaymentSystem.Transfers.void_payment(payment_id)

# Check final balance
{:ok, balance} = PaymentSystem.Queries.get_wallet_balance(12345)
# balance.available reduced by 10_000

# Process refund if needed
refund_id = generate_unique_refund_id()
{:ok, :refunded} = PaymentSystem.Transfers.refund_payment(
  user_wallet_id,
  67890,
  10_000,
  payment_id,
  refund_id
)
```

## Key Benefits of This Architecture

- **Atomic operations** \- merchant payment and platform fee execute atomically
- **Two-phase transfers** \- authorize first, capture or void later
- **Immutable audit trail** \- all transfers are permanent records
- **Balance guarantees** \- the `debits_must_not_exceed_credits` flag prevents overdrafts
- **High performance** \- batch operations, no SQL overhead, optimized for financial workloads
- **Strong consistency** \- distributed consensus ensures data integrity

## System Architecture Considerations

TigerBeetle works alongside your existing stack:

### Complementary to OLGP Databases

- **TigerBeetle** \- handles financial transactions (accounts, transfers, balances)
- **PostgreSQL/MySQL** \- stores user profiles, product catalog, order details, etc.
- **Application layer** \- coordinates between databases

### Elixir Integration Patterns

```
defmodule PaymentSystem.Coordinator do
  @moduledoc """
  Coordinates between PostgreSQL (order data) and TigerBeetle (financial transactions).
  """

  alias PaymentSystem.{Repo, Order}

  def process_order(user_id, merchant_id, order_params) do
    Repo.transaction(fn ->
      # 1. Create order in PostgreSQL
      {:ok, order} = create_order(user_id, merchant_id, order_params)

      # 2. Authorize payment in TigerBeetle
      user_wallet_id = PaymentSystem.Accounts.generate_account_id(user_id, :wallet)

      case PaymentSystem.Transfers.authorize_payment(
        user_wallet_id,
        merchant_id,
        order.total_cents,
        order.id
      ) do
        {:ok, :authorized} ->
          # 3. Update order status
          order
          |> Order.changeset(%{status: :authorized})
          |> Repo.update()

        {:error, reason} ->
          Repo.rollback(reason)
      end
    end)
  end

  def fulfill_order(order_id) do
    order = Repo.get!(Order, order_id)

    # Capture payment in TigerBeetle
    case PaymentSystem.Transfers.capture_payment(order.id) do
      {:ok, :captured} ->
        order
        |> Order.changeset(%{status: :completed})
        |> Repo.update()

      {:error, reason} ->
        {:error, reason}
    end
  end
end
```

## Production Deployment

TigerBeetle supports several deployment options:

### Docker Deployment

```
docker run -p 3000:3000 \
  -v $(pwd)/data:/data \
  ghcr.io/tigerbeetle/tigerbeetle:latest \
  start --addresses=3000 /data/0_0.tigerbeetle
```

### Systemd Service

```
[Unit]
Description=TigerBeetle Replica 0
After=network.target

[Service]
Type=simple
User=tigerbeetle
ExecStart=/usr/local/bin/tigerbeetle start --addresses=3000,3001,3002 /var/lib/tigerbeetle/0_0.tigerbeetle
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### Cluster Configuration

For production, run a 3 or 5 replica cluster for fault tolerance:

- **3 replicas** \- tolerates 1 failure
- **5 replicas** \- tolerates 2 failures

## When to Use TigerBeetle

TigerBeetle excels in scenarios requiring:

- **High-volume financial transactions** \- payment processing, money transfers, wallet systems
- **Real-time balance tracking** \- gaming currency, loyalty points, credits systems
- **Strict audit requirements** \- regulatory compliance, financial reporting
- **Write-heavy workloads** \- continuous transaction recording
- **Hot account scenarios** \- merchant accounts, platform revenue accounts with high contention

## Comparison with Traditional Approaches

### PostgreSQL Ledger Tables

- **Pros**: Familiar, flexible schema, rich query capabilities
- **Cons**: Slower (10x+), requires careful index management, complex concurrency handling, no built-in two-phase transfers

### Third-Party Ledger APIs (Stripe, Modern Treasury)

- **Pros**: Managed service, no infrastructure
- **Cons**: Network latency, vendor lock-in, usage-based pricing, limited customization

### TigerBeetle

- **Pros**: Extreme performance, purpose-built for accounting, two-phase transfers, immutability, self-hosted
- **Cons**: Specialized use case, smaller ecosystem, requires cluster management

## Conclusion

TigerBeetle represents a paradigm shift in financial data storage. By embracing the centuries-old debit-credit model and building a database specifically for OLTP workloads, it delivers performance and safety guarantees that general-purpose databases simply cannot match.

For Elixir developers building financial applications, TigerBeetle integrates seamlessly and provides a rock-solid foundation for transaction processing. Whether you're building a payment platform, digital wallet, gaming economy, or any system requiring precise financial tracking, TigerBeetle is worth serious consideration.

### Resources

**Official TigerBeetle Resources:**

- [TigerBeetle Official Website](https://tigerbeetle.com/)
- [TigerBeetle Documentation](https://docs.tigerbeetle.com/)
- [TigerBeetle GitHub Repository](https://github.com/tigerbeetle/tigerbeetle)
- [TigerBeetle Elixir Client](https://hex.pm/packages/tigerbeetle)

**Specific Documentation Guides:**

- [Data Modeling Guide](https://docs.tigerbeetle.com/coding/data-modeling/) \- Core concepts, debits vs credits, fractional amounts
- [Financial Accounting Deep Dive](https://docs.tigerbeetle.com/coding/financial-accounting/) \- The five account types and double-entry bookkeeping
- [Two-Phase Transfers](https://docs.tigerbeetle.com/coding/two-phase-transfers/) \- Authorization, capture, and void patterns
- [Linked Events](https://docs.tigerbeetle.com/coding/linked-events/) \- Atomic multi-transfer operations
- [Compound Transfers Recipe](https://docs.tigerbeetle.com/coding/recipes/multi-debit-credit-transfers/) \- Multi-debit and multi-credit patterns

**External References:**

- [Two-Phase Commit Protocol (Wikipedia)](https://en.wikipedia.org/wiki/Two-phase_commit_protocol)
- [Double-Entry Bookkeeping (Wikipedia)](https://en.wikipedia.org/wiki/Double-entry_bookkeeping)
- [Debits and Credits (Wikipedia)](https://en.wikipedia.org/wiki/Debits_and_credits)

Ready to build with TigerBeetle?

Start with the installation guide and experiment with the examples above. The performance and safety guarantees will transform how you think about financial data storage.

[Back to Blog](https://alexandrubagu.github.io/blog.html)