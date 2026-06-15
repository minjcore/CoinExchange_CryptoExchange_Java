![Correspondent banking: the hidden cost of cross-border payments ](https://cdn.prod.website-files.com/651d5e7d177adbd2c4ef5dad/695fa4284feb6ad2a8a6d0bf_Correspondent_Banking_How_It_Works_and_Why_It%27s_Expensive.jpg)

[What is correspondent banking?](https://www.opendue.com/blog/correspondent-banking-how-it-works-and-why-its-expensive#what-is-correspondent-banking)

[How does correspondent banking work?](https://www.opendue.com/blog/correspondent-banking-how-it-works-and-why-its-expensive#how-does-correspondent-banking-work)

[Why is correspondent banking expensive?](https://www.opendue.com/blog/correspondent-banking-how-it-works-and-why-its-expensive#why-is-correspondent-banking-expensive)

[Modern alternatives to correspondent banking](https://www.opendue.com/blog/correspondent-banking-how-it-works-and-why-its-expensive#modern-alternatives-to-correspondent-banking)

[Cost and speed comparison](https://www.opendue.com/blog/correspondent-banking-how-it-works-and-why-its-expensive#cost-and-speed-comparison)

[How does Due reduce cross‑border payment costs?](https://www.opendue.com/blog/correspondent-banking-how-it-works-and-why-its-expensive#how-does-due-reduce-crossborder-payment-costs)

[Book a demo](https://meetings.hubspot.com/opendue/demo?uuid=a3b65d0c-2e38-4b6d-8f6e-b230fd77f73e)

![](https://cdn.prod.website-files.com/65035c417fe69396bd8c0d5c/6876d3b3760eb28d650af21f_globe-due-AI.svg)

![](https://cdn.prod.website-files.com/651d5e7d177adbd2c4ef5dad/68822638c5da91f0e16c1dda_due_touch-icon.png)

Due Team

[Payments](https://www.opendue.com/blog-category/payments)

![](https://cdn.prod.website-files.com/65035c417fe69396bd8c0d5c/687438b8014eb05ea02950e8_icon_min-read.svg)

10 min read

![](https://cdn.prod.website-files.com/65035c417fe69396bd8c0d5c/687438cd09c5e71a9605fbbd_icon_published-on.svg)

Published: Jan 06, 2026

![](https://cdn.prod.website-files.com/65035c417fe69396bd8c0d5c/6989d75b3912865430571522_ic_round-update.svg)

Updated: Jan 06, 2026

# Correspondent banking: the hidden cost of cross-border payments

Key takeaways

1. Correspondent banking explains why cross-border payments are slow and expensive: money moves through multiple banks, requires pre-funded nostro/vostro accounts, repeated compliance checks, and often loses 1–4% in fees and FX spreads along the way.
2. These costs are structural, not arbitrary: capital tied up in idle accounts, intermediary fees, FX mark-ups, and duplicated AML checks all add friction, even with tools like SWIFT gpi.
3. Modern alternatives reduce these frictions: domestic instant payment systems, stablecoin rails, and payment infrastructure providers can cut settlement times from days to seconds and reduce fees significantly, with Due combining these rails into a single, transparent platform.

International payments are essential to global business, but they are still slower and more expensive than most people expect. In many cases, this is because payments move through correspondent banking, a system where banks rely on other banks to move money across borders. A finance team sending £800 (GBP) to a supplier overseas may wait several days for the payment to arrive, only to find that a noticeable amount has been lost to fees and exchange rate margins along the way.

Recent findings from the [Financial Stability Board (FSB)](https://www.fsb.org/2025/10/fsb-calls-for-enhanced-policy-implementation-to-achieve-tangible-improvements-in-cross-border-payments/) show that, despite years of reform efforts, these frictions have not been fully resolved. Costs remain high, settlement can still take days, and outcomes vary widely depending on the countries and payment corridors involved. The FSB emphasizes that while the policy framework has been developed, enhanced implementation at the jurisdictional level is now critical to achieving meaningful progress in cross-border banking payments.

This blog demystifies correspondent banking, breaks down the costs that make cross‑border transfers so expensive and explores modern alternatives.

## What is correspondent banking?

Correspondent banking is a global financial framework that allows banks to provide international payment and clearing services in countries and currencies where they do not have a physical presence. Through the correspondent banking system, financial institutions can send and receive cross-border payments, settle foreign currencies, and offer international banking services without establishing overseas branches.

When a bank in Ghana needs to send U.S. dollars, it cannot access the U.S. Federal Reserve’s clearing system directly. Instead, it relies on a correspondent bank in the United States. The Ghanaian bank holds a nostro account with that correspondent, an account denominated in a foreign currency and maintained abroad. The U.S. correspondent records the same funds as a vostro account, treating them as a liability on its own balance sheet. These mirrored nostro and vostro accounts form the settlement backbone of the correspondent banking system.

The correspondent banking system is essential to global trade, particularly for smaller banks and financial institutions in emerging markets. Large international banks with direct access to central-bank clearing networks, such as the Federal Reserve or the Bank of England, act as correspondents for other institutions. Through correspondent banking relationships, banks can provide wire transfers, foreign-exchange settlement, trade finance, cheque clearing, and other cross-border financial services.

Without correspondent banking, international payments would require direct bilateral agreements between every bank worldwide, significantly increasing settlement times, operational risk, and transaction costs.

## How does correspondent banking work?

Correspondent banking system sounds simple, one bank sends money to another via a trusted intermediary, but the actual process involves multiple hops, regulatory checks and ledger entries. The diagram below illustrates a typical transaction.

![Correspondent bank's way of work](https://cdn.prod.website-files.com/651d5e7d177adbd2c4ef5dad/695fa4c165a0eadacee798ec_Frame%201437253234%20(1).png)

### Nostro and vostro accounts

**Nostro and vostro accounts explained:** a nostro account is a bank’s record of its own money on deposit in a foreign bank, while a vostro account is the same balance recorded on the foreign bank’s books. When a Ghanaian bank opens a U.S. dollar account with a New York correspondent, it treats the funds as an asset (nostro), and the U.S. bank records the same balance as a liability (vostro).

Banks must pre‑fund these accounts, so they have sufficient liquidity to settle outgoing payments. [Industry estimates](https://documents.keyrock.com/hubfs/Stablecoin-Payments-The-Trillion-Dollar-Opportunity.pdf) suggest that $27 trillion (USD) is locked up in pre‑funded nostro/vostro accounts globally; at a 5% interest rate, parking $1 billion (USD) in a nostro account costs about $50 million (USD) per year in lost yield. Capital tied up in dormant accounts is one reason cross‑border payments are expensive.

### Example: sending U.S. dollars from New York to Manila

1. **Originating bank** – A corporate client in New York instructs its bank to pay a supplier in Manila. The originating bank sends a SWIFT MT 103 (or GPI) message to its correspondent with the payment details.
2. **Correspondent bank 1 (U.S. clearing)** – The originator’s correspondent debits its customer’s nostro account and, if necessary, routes the payment through the domestic clearing system (Fedwire). It forwards the payment to a second correspondent who has access to the Philippines.
3. **Correspondent bank 2 (intermediary)** – This bank maintains relationships with both the U.S. and Philippine banks. It may convert the currency or pass the payment unchanged. Each bank performs anti‑money‑laundering (AML) and know‑your‑customer (KYC) checks.
4. **Correspondent bank 3 (Philippine clearing)** – The final correspondent credits the receiving bank’s nostro account, converts dollars to Philippine pesos if required and sends a message through the local clearing system.
5. **Receiving bank** – It credits the beneficiary’s account once it has received confirmation and completed its own compliance checks.

Although SWIFT GPI enables tracking and reduces messaging latency, settlement still depends on how quickly each bank credits the beneficiary.

## Why is correspondent banking expensive?

Despite improvements in messaging, cross‑border payments remain costly. Four structural drivers explain why.

### 1\. Nostro account costs

Maintaining pre‑funded balances ties up working capital. Banks must hold foreign currency in multiple jurisdictions to guarantee settlement, which incurs an opportunity cost. The [Keyrock stablecoin report](https://documents.keyrock.com/hubfs/Stablecoin-Payments-The-Trillion-Dollar-Opportunity.pdf) estimates that $27 trillion (USD) sits idle in nostro/vostro accounts and that the cost of pre‑funding runs 3–5 % annually. For example, a bank keeping $50 million (USD) in a London nostro account could forgo $2.5 million (USD) in annual interest if rates are 5%. Customers ultimately pay for this inefficiency through higher fees and spreads.

### 2\. Transaction fees at each bank

Each correspondent bank charges a processing fee. Industry sources note that international wire transfer fees typically range from $15–$50 (USD) per intermediary, and most payments involve one to three intermediaries. On top of these explicit charges, receiving banks often deduct $15–$25 (USD). When added together, total fees can amount to 1–4 % of the transfer amount. For a $1,000 (USD) wire through three banks, that equates to roughly $45 (USD) in fees, even before foreign‑exchange costs.

### 3\. Foreign‑exchange spreads

Most correspondent payments involve currency conversion. Banks quote an exchange rate worse than the mid‑market rate (the interbank rate), pocketing the difference. Traditional wire transfers find that banks typically add [2–4 % to the mid‑market rate](https://www.opendue.com/blog/how-to-send-money-from-the-u-s-to-china---best-way-2025-comparison). In some corridors, spreads can reach 4–6 %. Because FX spreads are usually embedded in the quoted rate, senders often underestimate this cost. For example, sending €10,000 (EUR) to Hong Kong at a 3 % spread means the beneficiary receives roughly €300 (EUR) less than the mid‑market value.

### 4\. Compliance and AML costs

Every correspondent bank must perform KYC/AML checks on both the sender and recipient to combat financial crime. Each correspondent bank in the chain must run its own KYC/AML and sanctions checks, and official [BIS analysis](https://www.bis.org/publ/arpdf/ar2024e.pdf) shows these checks are often duplicated across institutions, increasing compliance cost and contributing to higher fees and delays in cross-border payments. These costs are recovered through higher transaction fees and spreads.

### 5\. De‑risking and shrinking networks

In the wake of stricter AML regulations, many correspondent banks have exited high‑risk corridors, a phenomenon known as de‑risking. Fewer correspondents mean less competition, higher fees and longer routings. The World Bank’s Remittance Prices Worldwide database shows that banks remain the most expensive providers of remittances, charging an average fee of [14.55%](https://remittanceprices.worldbank.org/sites/default/files/rpw_main_report_and_annex_q125_1_0.pdf) in early 2025, while the global average cost across all providers is 6.49%. These structural costs are not arbitrary mark‑ups; they reflect capital requirements, compliance obligations and network shrinkage.

The infographic below visualises the cost breakdown for a typical cross‑border payment.

![Cost breakdown_cross-border payments](https://cdn.prod.website-files.com/651d5e7d177adbd2c4ef5dad/695fa57801718163f8bbe0d1_Frame%201437253231%20(1).png)

## Modern alternatives to correspondent banking

Companies frustrated by high fees and slow settlement are exploring alternative payment rails. Each option offers different benefits and limitations.

### Real‑time domestic systems

Domestic instant‑payment schemes, such as the U.S. FedNow Service, Europe’s TARGET Instant Payment Settlement (TIPS) and the UK’s Faster Payments Service (FPS), enable 24/7 near‑instant settlement within a single currency. [FedNow](https://www.frbservices.org/resources/fees/fednow-2025) charges $0.045 (USD) per credit transfer and $0.01 (USD) per request‑for‑payment, plus a $25 (USD) monthly participation fee. The [European Central Bank’s TIPS](https://www.ecb.europa.eu/paym/target/tips/html/index.pl.html) charges €0.002 per transaction, split equally between the payer and payee (about €0.001 each).

[Pay.UK’s 2025 fee schedule](https://www.wearepay.uk/wp-content/uploads/2025/05/Pay.UK-Faster-Payments-System-Principles-v-10.1-May-2025.pdf) shows that direct participants in FPS pay an indicative £0.00881 per transaction and a £0.010194 per transaction service management fee, with a monthly standing charge of £38,000. While these costs are negligible compared with SWIFT correspondent banking costs, domestic systems cannot settle cross-border transactions on their own; they cover only one currency.

### SWIFT GPI and SWIFT Go

SWIFT GPI enhances the transparency of correspondent payments by attaching a unique transaction identifier, enabling banks to track payments in real time.  GPI does not eliminate correspondent bank fees or pre‑funding; it merely accelerates messaging. SWIFT Go, launched in 2021, aims to improve low‑value transfers by requiring participating banks to provide upfront fee disclosures and same‑day crediting, but it still runs on the existing correspondent network.

### Stablecoin rails

Stablecoins offer a different way to settle cross-border payments because value moves on blockchain rails rather than through traditional correspondent banking chains. [Due notes](https://www.opendue.com/blog/best-stablecoin-companies-in-2025-transforming-global-money-transfers) that banks can charge $15–$50 (USD) for an international wire, while stablecoin transactions can cost less than $0.1 (USD) and settle 24/7 in minutes rather than days.

Due also points to the scale behind this shift is also significant: in 2024, stablecoin networks processed an estimated $27.6 trillion (USD) in annual transaction volume, and the World Bank’s data still shows the average cost of sending money across borders is 6.26% in fees. Due positions its Stablecoin Transfer API as the practical bridge for businesses, handling custody, compliance, liquidity, and real-time FX conversion, while also supporting local payout and collection rails across 80+ countries.

### Payment infrastructure providers

Specialised fintechs provide multi‑currency accounts and direct connections to local clearing systems. By maintaining licences and bank accounts in multiple countries, providers can net transactions internally and instruct domestic transfers to beneficiaries, thereby avoiding most correspondent hops. These providers typically charge 1–3% of the transaction amount, including FX; still higher than stablecoin fees but often lower than banks. They also handle compliance centrally, giving businesses a single interface for global payments. The trade‑off is that funds flow through the provider’s balance sheet, which may not be appropriate for very large or regulated payments.

### When does correspondent banking still make sense?

Despite alternatives, correspondent banking remains useful in certain scenarios:

- **High‑value transactions.** Large corporate or interbank transfers exceeding $100,000 (USD) often need the credit quality and legal certainty of bank‑to‑bank settlement. Stablecoin liquidity may not support multi‑million‑dollar payments without slippage.
- **Exotic currencies.** Many emerging‑market currencies lack instant‑payment schemes or stablecoin liquidity. Correspondent banks may be the only route to clear funds.
- **Regulatory or contractual requirements.** Some jurisdictions require payments through licensed banks for tax, AML or reporting purposes. Certain trade settlements, escrow arrangements or capital‑market transactions mandate bank settlement.
- **Integration with treasury systems.** Corporate enterprise resource planning (ERP) systems are built around bank APIs and SWIFT messaging. Reengineering processes for new rails may be costly.

## Cost and speed comparison

The table below summarises the key differences between traditional correspondent banking, SWIFT gpi, stablecoin rails and fintech payment providers. It reflects typical costs and speeds; actual fees vary by corridor and provider.

| Rail or provider | Base fee per transaction | FX mark-up | Settlement speed | Availability |
| --- | --- | --- | --- | --- |
| Traditional correspondent banking (SWIFT) | $15–$50 per intermediary.<br> <br>Receiving banks may add $15–$25. | 2–4% of the amount. | 3–5 days; some corridors are shorter. | Business hours only. |
| SWIFT gpi / Go | Same base fees as traditional SWIFT.<br> <br>Adds tracking and upfront fee disclosure. | Same 2–4% FX spread. | Within 1 day for ~99% of payments. | Business hours only. |
| Stablecoin rails | <$0.01 per transaction. | 0–0.5%. | Seconds. | 24/7/365. |
| Payment infrastructure providers | 1–3%, often all-inclusive. | Included in the fee. | Same day, typically within hours. | 24/7 for some providers. |
| FedNow (US domestic) | $0.045 + $0.01 per request. | Not applicable (USD only). | Seconds. | 24/7. |
| TIPS (EU domestic) | €0.002 split between parties. | Not applicable (EUR only). | Seconds. | 24/7. |
| UK FPS (domestic) | £0.00881 per transaction.<br> <br>Plus £0.010194 service fee. | Not applicable (GBP only). | Seconds. | 24/7. |

Comparison of payment rails and providers by base transaction fees, FX mark-ups, settlement speed, and availability.


Fees and settlement times represent typical benchmarks and may vary by provider, corridor, and transaction type.

## How does Due reduce cross‑border payment costs?

[Due’s cross‑border infrastructure](https://www.opendue.com/) combines traditional and modern rails to minimise fees and delays. The platform connects directly to local payment systems in more than 80 countries, allowing clients to fund a single multi‑currency account and send domestic‑equivalent payments to suppliers and employees.

Where local real‑time payment systems exist, Due uses them to settle funds in seconds for a small per‑transaction fee. For corridors without instant rails, Due can still route payments through trusted correspondent partners, but reduces the number of hops. The platform also offers stablecoin settlement as an option, enabling near‑instant delivery with minimal fees when counterparties accept digital currency.

Key advantages include:

- **Direct connections to local payment rails.** By maintaining relationships with clearing houses in over 80 countries, Due avoids multiple correspondent banks and credits the beneficiary directly.
- **Stablecoin option.** Companies can settle in tokenised U.S. dollars (e.g., USDC), bypassing bank‑hours restrictions and reducing costs to fractions of a cent.
- **Multi‑currency virtual accounts.** Clients hold balances in major currencies and convert when needed, avoiding unnecessary FX conversions.
- **Transparent pricing.** Due’s pricing is designed to remove the uncertainty that comes with correspondent banking. Due’s processing fees are typically 0.2% to 0.3% for cross-border B2B payments and under 1% for merchant processing.

Due also discloses all fees upfront, so you can see the total cost before you send the payment, and avoid hidden intermediary bank fees that often appear when a transfer moves through multiple correspondent banks.

To see how much you could save by re‑routing payments through modern infrastructure, [book a demo with Due](https://meetings.hubspot.com/opendue/demo?) and model your potential savings.

Download Due & Move Money Without Borders

[Explore Api](https://www.opendue.com/blog/correspondent-banking-how-it-works-and-why-its-expensive#)

Download Due & Move Money Without Borders

[Explore Api](https://www.opendue.com/blog/correspondent-banking-how-it-works-and-why-its-expensive#)

![](https://cdn.prod.website-files.com/65035c417fe69396bd8c0d5c/6876d3b3760eb28d650af21f_globe-due-AI.svg)

Your browser does not support the video tag.

![](https://cdn.prod.website-files.com/65035c417fe69396bd8c0d5c/688e1d06ad982e5a557c8770_due-send-money.png)

![](https://cdn.prod.website-files.com/65035c417fe69396bd8c0d5c/650871df1b7622863e8b87f7_duo-logo-white.svg)

Designed for global scale.

Get paid anywhere - bank transfers, mobile money, digital wallets and more in 80+ markets. Receive stablecoins instantly or settle into your local currency. Near-zero fees.

[Explore Api](https://www.opendue.com/blog/correspondent-banking-how-it-works-and-why-its-expensive#)

![](https://cdn.prod.website-files.com/65035c417fe69396bd8c0d5c/650871df1b7622863e8b87f7_duo-logo-white.svg)

Designed for global scale.

Get paid anywhere - bank transfers, mobile money, digital wallets and more in 80+ markets. Receive stablecoins instantly or settle into your local currency. Near-zero fees.

[Explore Api](https://www.opendue.com/blog/correspondent-banking-how-it-works-and-why-its-expensive#)

![](https://cdn.prod.website-files.com/65035c417fe69396bd8c0d5c/688e1d06ad982e5a557c8770_due-send-money.png)

![](https://cdn.prod.website-files.com/65035c417fe69396bd8c0d5c/688e1d06ad982e5a557c8770_due-send-money.png)

![](https://cdn.prod.website-files.com/65035c417fe69396bd8c0d5c/650871df1b7622863e8b87f7_duo-logo-white.svg)

Designed for global scale.

Get paid anywhere - bank transfers, mobile money, digital wallets and more in 80+ markets. Receive stablecoins instantly or settle into your local currency. Near-zero fees.

[Explore Api](https://www.opendue.com/blog/correspondent-banking-how-it-works-and-why-its-expensive#)

![](https://cdn.prod.website-files.com/65035c417fe69396bd8c0d5c/650871df1b7622863e8b87f7_duo-logo-white.svg)

Designed for global scale.

Get paid anywhere - bank transfers, mobile money, digital wallets and more in 80+ markets. Receive stablecoins instantly or settle into your local currency. Near-zero fees.

[Explore Api](https://www.opendue.com/blog/correspondent-banking-how-it-works-and-why-its-expensive#)

![](https://cdn.prod.website-files.com/65035c417fe69396bd8c0d5c/688e1d06ad982e5a557c8770_due-send-money.png)

![](https://cdn.prod.website-files.com/65035c417fe69396bd8c0d5c/688e1d06ad982e5a557c8770_due-send-money.png)

![](https://cdn.prod.website-files.com/65035c417fe69396bd8c0d5c/650871df1b7622863e8b87f7_duo-logo-white.svg)

Designed for global scale.

Get paid anywhere - bank transfers, mobile money, digital wallets and more in 80+ markets. Receive stablecoins instantly or settle into your local currency. Near-zero fees.

[Explore Api](https://www.opendue.com/blog/correspondent-banking-how-it-works-and-why-its-expensive#)

![](https://cdn.prod.website-files.com/65035c417fe69396bd8c0d5c/650871df1b7622863e8b87f7_duo-logo-white.svg)

Designed for global scale.

Get paid anywhere - bank transfers, mobile money, digital wallets and more in 80+ markets. Receive stablecoins instantly or settle into your local currency. Near-zero fees.

[Explore Api](https://www.opendue.com/blog/correspondent-banking-how-it-works-and-why-its-expensive#)

![](https://cdn.prod.website-files.com/65035c417fe69396bd8c0d5c/688e1d06ad982e5a557c8770_due-send-money.png)

## FAQ

### What is correspondent banking, and how does it work?

People ask this when they make international payments, but don’t understand why banks need intermediaries to move money across borders.

### Why are international payments so expensive?

This is one of the most common queries, driven by unexpected fees, poor FX rates, and delayed settlements.

### What are nostro and vostro accounts?

Searchers usually see these terms on bank statements or payment documentation and want a simple explanation.

### How long do international bank transfers take?

Users want to know why some transfers arrive in hours while others take several days.

### Are there alternatives to correspondent banking for cross-border payments?

This reflects growing interest in stablecoins, fintech payment providers, and faster settlement options.

[https://www.fsb.org/2025/10/fsb-calls-for-enhanced-policy-implementation-to-achieve-tangible-improvements-in-cross-border-payments/](https://www.fsb.org/2025/10/fsb-calls-for-enhanced-policy-implementation-to-achieve-tangible-improvements-in-cross-border-payments/)

[https://documents.keyrock.com/hubfs/Stablecoin-Payments-The-Trillion-Dollar-Opportunity.pdf](https://documents.keyrock.com/hubfs/Stablecoin-Payments-The-Trillion-Dollar-Opportunity.pdf)

[https://www.bis.org/publ/arpdf/ar2024e.pdf](https://www.bis.org/publ/arpdf/ar2024e.pdf)

[https://www.frbservices.org/resources/fees/fednow-2025](https://www.frbservices.org/resources/fees/fednow-2025)

[https://www.ecb.europa.eu/paym/target/tips/html/index.pl.html](https://www.ecb.europa.eu/paym/target/tips/html/index.pl.html)

‍

Share

Download Due & Move Money Without Borders

[![](https://cdn.prod.website-files.com/65035c417fe69396bd8c0d5c/6876d2ea6f65d49c6d9ac943_Google.svg)](https://play.google.com/store/apps/details?id=com.due.superlite&pcampaignid=web_share)[![](https://cdn.prod.website-files.com/65035c417fe69396bd8c0d5c/6876d2e7c4c183b8342636e6_AppStore.svg)](https://apps.apple.com/app/id6479643613)

![](https://cdn.prod.website-files.com/65035c417fe69396bd8c0d5c/6876d3b3760eb28d650af21f_globe-due-AI.svg)

Your browser does not support the video tag.

## Blog & News

[Read more](https://www.opendue.com/blog)

[![](https://cdn.prod.website-files.com/651d5e7d177adbd2c4ef5dad/690f3c7238387e308b7cf97a_Due%20Post%20Stablecoin%20vs%20Traditional%20FX%20Which%20Is%20Better%20for%20Cross%20Border%20Payments.png)\\
\\
Payments\\
\\
May 29, 2026\\
\\
Stablecoin vs Traditional FX for Cross-Border Payments (2026 Guide)](https://www.opendue.com/blog/stablecoin-vs-traditional-fx)

[![](https://cdn.prod.website-files.com/651d5e7d177adbd2c4ef5dad/6a0d8facccea1ba2f3498696_how%20to%20send%20money%20t%20dubai.jpg)\\
\\
International Money Transfers\\
\\
May 19, 2026\\
\\
How to send money to Dubai: 2026 guide for individuals and businesses](https://www.opendue.com/blog/how-to-send-money-to-dubai)

[![](https://cdn.prod.website-files.com/651d5e7d177adbd2c4ef5dad/6a0c554df74dafd48e8629b9_How%20to%20Send%20Money%20to%20Mexico%20from%20the%20US.jpg)\\
\\
International Money Transfers\\
\\
May 19, 2026\\
\\
How to send money to Mexico: 2026 guide for individuals and businesses](https://www.opendue.com/blog/how-to-send-money-to-mexico)

[Read more](https://www.opendue.com/blog)

## Leave Old Finance Behind

[Explore api](https://due.readme.io/docs/overview#/) [Book a demo](https://crm.due.dev/book/demo)

By clicking **"Accept"**, you agree to the storing of cookies on your device to enhance site navigation, analyze site usage, and assist in our marketing efforts. View our [Privacy Policy](https://www.opendue.com/legal/privacy-policy-individuals) for more information.

[Preferences](https://www.opendue.com/blog/correspondent-banking-how-it-works-and-why-its-expensive#) [Reject](https://www.opendue.com/blog/correspondent-banking-how-it-works-and-why-its-expensive#) [Accept](https://www.opendue.com/blog/correspondent-banking-how-it-works-and-why-its-expensive#)

Preferences

**Manage Consent Preferences by Category**

Essentials

**Always active**

Necessary for the site to function. Always On.

Marketing

Marketing

Used for targeted advertising.

Personalization

Personalization

Remembers your preferences and provides enhanced features.

Analytics

Analytics

Measures usage and improves your experience.

Marketing

Marketing

Used for targeted advertising.

Personalization

Personalization

Remembers your preferences and provides enhanced features.

Analytics

Analytics

Measures usage and improves your experience.

Marketing

Marketing

Used for targeted advertising.

Personalization

Personalization

Remembers your preferences and provides enhanced features.

Analytics

Analytics

Measures usage and improves your experience.

Marketing

Marketing

Used for targeted advertising.

Personalization

Personalization

Remembers your preferences and provides enhanced features.

Analytics

Analytics

Measures usage and improves your experience.

[Reject All](https://www.opendue.com/blog/correspondent-banking-how-it-works-and-why-its-expensive#) [Accept All](https://www.opendue.com/blog/correspondent-banking-how-it-works-and-why-its-expensive#)

Thank you! Your submission has been received!

Oops! Something went wrong while submitting the form.