Chat with Stripe sales


![](https://stripe.com/resources/more/payment-settlement-explained-how-it-works-and-how-long-it-takes)![](https://stripe.com/resources/more/payment-settlement-explained-how-it-works-and-how-long-it-takes)

8 sales reps available


We're here to discuss your business needs.

Chat now with sales

How Payment Settlement Works and How Long It Takes \| Stripe

[Start now](https://dashboard.stripe.com/register) [Contact sales](https://stripe.com/contact/sales)

Payments


Payments



Accept payments online, in person, and around the world with a payments solution built for any business—from scaling startups to global enterprises.

[Learn more](https://stripe.com/payments)

1. [Introduction](https://stripe.com/resources/more/payment-settlement-explained-how-it-works-and-how-long-it-takes#introduction)
2. [The payment settlement process](https://stripe.com/resources/more/payment-settlement-explained-how-it-works-and-how-long-it-takes#the-payment-settlement-process)
3. [Who is involved in payment settlement systems?](https://stripe.com/resources/more/payment-settlement-explained-how-it-works-and-how-long-it-takes#who-is-involved-in-payment-settlement-systems)
4. [Timing and cycles of settlement periods](https://stripe.com/resources/more/payment-settlement-explained-how-it-works-and-how-long-it-takes#timing-and-cycles-of-settlement-periods)   1. [Automated Clearing House (ACH) transactions](https://stripe.com/resources/more/payment-settlement-explained-how-it-works-and-how-long-it-takes#automated-clearing-house-ach-transactions)
2. [Wire transfers](https://stripe.com/resources/more/payment-settlement-explained-how-it-works-and-how-long-it-takes#wire-transfers)
3. [Digital wallets](https://stripe.com/resources/more/payment-settlement-explained-how-it-works-and-how-long-it-takes#digital-wallets)
4. [Global payment networks](https://stripe.com/resources/more/payment-settlement-explained-how-it-works-and-how-long-it-takes#global-payment-networks)
5. [Security and fraud prevention during settlement](https://stripe.com/resources/more/payment-settlement-explained-how-it-works-and-how-long-it-takes#security-and-fraud-prevention-during-settlement)   1. [Security measures](https://stripe.com/resources/more/payment-settlement-explained-how-it-works-and-how-long-it-takes#security-measures)
2. [Fraud prevention measures](https://stripe.com/resources/more/payment-settlement-explained-how-it-works-and-how-long-it-takes#fraud-prevention-measures)
6. [Legal and compliance considerations around payment settlements](https://stripe.com/resources/more/payment-settlement-explained-how-it-works-and-how-long-it-takes#legal-and-compliance-considerations-around-payment-settlements)
7. [Payment settlement best practices for businesses](https://stripe.com/resources/more/payment-settlement-explained-how-it-works-and-how-long-it-takes#payment-settlement-best-practices-for-businesses)
8. [How Stripe Payments can help](https://stripe.com/resources/more/payment-settlement-explained-how-it-works-and-how-long-it-takes#how-stripe-payments-can-help)
9. [Get started with Stripe](https://dashboard.stripe.com/register)

Payment settlement is the stage in [credit card processing](https://stripe.com/resources/more/credit-card-processing-101) where a transaction is finalized and the funds are transferred from the buyer’s account to the seller’s account. For businesses, this is when the money from sales or services rendered becomes available in their bank accounts. Payment settlement is an important part of worldwide digital payments, which are projected to reach [$24.07 trillion in 2025](https://www.statista.com/outlook/dmo/fintech/digital-payments/worldwide).

When a customer makes a purchase, the transaction goes through different stages, including [authorization](https://stripe.com/resources/more/card-authorization-explained) and batching, before it reaches settlement. Settlement signifies the completion of a financial transaction and enables a business to access the funds. For credit card payments, settlement usually takes one to three business days after the transaction to complete. This process’s speed and efficiency depend on the transaction type and can directly impact a business’s cash availability and financial management.

Below, we’ll discuss what businesses should know about how payment settlement works, how long it takes, and best practices.

**What’s in this article?**

- The payment settlement process

- Who is involved in payment settlement systems?

- Timing and cycles of settlement periods

- Security and fraud prevention during settlement

- Legal and compliance considerations around payment settlements

- Payment settlement best practices for businesses

- How Stripe Payments can help


## The payment settlement process

Here’s a step-by-step breakdown of the payment settlement process:

**1\. Authorization:** After a customer initiates a payment, a [point-of-sale (POS) system](https://stripe.com/resources/more/pos-systems-for-businesses) sends an authorization request with transaction details to the acquiring bank. The acquiring bank forwards this request to the card association (e.g., Visa, Mastercard), which routes it to the issuing bank.

**2\. Verification:** The issuing bank verifies the transaction’s validity (verifying security details such as the card’s expiration date and card verification value or CVV), checks for sufficient funds or credit limit, and assesses the risk parameters before approving or declining the transaction. The issuing bank then sends a response back to the business.

**3\. Approval or decline:** Based on the verification, the issuing bank either approves or declines the transaction. If approved, the bank reserves the transaction amount in the cardholder’s account, reducing the available balance or credit. This reserved amount is earmarked for the upcoming transaction. If declined, the business receives a reason from the issuing bank.

**4\. Capture:** The business might not immediately capture the transaction after authorization. This is common in scenarios where the final purchase amount might vary (e.g., at a gas station) or when the goods or services are delivered at a later time (e.g., in online shopping).

When the business is ready to finalize the transaction amount, it initiates the capture process. This can occur at the end of the business day or after the service is provided. The capture request is sent to the [payment processor](https://stripe.com/resources/more/payment-processor-vs-payment-gateway) or acquiring bank, instructing that entity to finalize the transaction for the authorized amount.

**5\. Batching:** Batching is when the business sends all the authorized transactions from that day, grouped together as a batch, to its [payment processor](https://stripe.com/resources/more/payment-processors-101) at the end of the day. It’s the precursor to settlement.

**6\. Clearing and interchange:** The acquiring bank forwards the batched transactions to the [card networks](https://stripe.com/resources/more/how-do-credit-card-networks-work). The networks route these transactions to the respective issuing banks and calculate interchange fees (i.e., fees paid between banks for the acceptance of card-based transactions).

**7\. Settlement:** The issuing bank transfers the appropriate funds to the card networks. The networks transfer these funds to the acquiring bank, typically within one to three business days.

**8\. Funding:** The business’s account is credited with the net transaction amount (gross transaction amount minus interchange fees, acquiring bank fees, and any other applicable charges). While settlement marks the transfer of funds between banks, funding is when the money becomes available in the business’s account.

**9\. Reconciliation:** The business reconciles its account by matching the transaction amount it has recorded with the amount that has been settled and funded. This can identify any discrepancies and help resolve issues related to transaction processing.

![Card processing explained step-by-step - Payment settlement flow chart](https://images.stripeassets.com/3sz5ney9ml0h/4nHBzoBsMp2pDAS8lC2PWm/03505c4bc9c3449a35d718e2ecb87e7f/Card-processing-explained-step-by-step.png?w=2160&q=80)

## Who is involved in payment settlement systems?

Payment settlement involves several different entities with specific responsibilities and functions. These entities collaborate to create secure, reliable payment systems that process [millions of credit card transactions every hour](https://capitaloneshopping.com/research/number-of-credit-card-transactions).

- **Business:** This is the entity that sells goods or services. It accepts payments from customers using a merchant account, a type of bank account that allows businesses to accept debit and credit card payments. A [merchant account](https://stripe.com/resources/more/merchant-accounts-101) holds funds before the money is transferred to the business’s primary business bank account.

- **Customer (cardholder):** This is the individual or entity that purchases goods or services using a payment method such as a credit or debit card.

- **Acquiring bank (business’s bank):** The acquiring bank is the business’s partner in processing credit and debit card transactions. It provides the business with the necessary tools and bank accounts to accept card payments. The acquiring bank passes along the business’s transactions to the applicable issuing banks to receive payment.

- **Issuing bank (customer’s bank):** This is the bank that issued the customer’s credit or debit card. The [issuing bank](https://stripe.com/resources/more/issuing-banks) is responsible for paying the acquiring bank on behalf of the customer and, later, collecting the payment from the customer.

- **Payment processor:** Often a third-party company, the [payment processor](https://stripe.com/resources/more/third-party-payment-processors-explained) is the entity that manages the transaction flow between businesses, acquiring banks, and card networks. It provides the technology and services needed to process transactions, including authorization, batching, and settlement functions.

- **Card networks (payment networks):** These networks facilitate the electronic transfer of financial information and funds between parties. Examples include Visa, Mastercard, American Express, and Discover. Card networks set the rules and standards for card transactions and provide the infrastructure for processing them.

- **Payment gateway:** [Payment gateways](https://stripe.com/resources/more/payment-gateways-101) process credit card payments for businesses. They facilitate the transfer of information between a payment portal (e.g., a website or mobile phone) and the payment processor or acquiring bank.


## Timing and cycles of settlement periods

Payment settlement systems operate along different timelines. In a typical timeline for credit cards, transactions are authorized instantly and then batched and sent out at the end of each business day. Clearing is completed overnight, and settlement is completed within one to three business days after the transaction. Funding takes two to three business days after the transaction to complete.

Settlement time frames for credit card payments can vary depending on the parties involved and the specific processing systems used. With same-day settlement, the funds transfer to the business’s account within the same business day. Next-day or one-day settlement typically processes transactions overnight, with funds available the following business day. With two-day or standard settlement, which is common in the industry, the company receives funds two business days after the transaction. Longer settlement periods might apply to high-risk businesses or international transactions.

Below are the processes and timelines for other transaction types.

### Automated Clearing House (ACH) transactions

- **Initiation:** The originator sends a payment instruction to their bank, which can happen anytime.

- **Batching:** Banks batch and process [ACH payments](https://stripe.com/gb/resources/more/ach-payments-101) at scheduled times—every four to six hours or so—not once a day as with card transactions.

- **Clearing:** The batches are sent to the central ACH operator, which sorts them and sends instructions to the recipient’s bank.

- **Settlement:** Settlement usually occurs for ACH transactions on the next business day after the batch is sent.

- **Funding:** The recipient’s bank credits their account, often on the same day settlement occurs.


### Wire transfers

- **Initiation:** The sender initiates the [wire transfer](https://stripe.com/resources/more/wire-transfers-101) with their bank.

- **Transmission:** The sender’s bank transmits the transaction details through a network, such as the Society for Worldwide Interbank Financial Telecommunication (SWIFT).

- **Settlement:** Wire transfers are usually settled within hours, making them one of the fastest methods for transferring funds internationally.

- **Funding:** The recipient’s account is credited on the same day or within 24 hours, depending on the banks’ cutoff times and time zones.


### Digital wallets

- **Transaction:** The [digital wallet](https://stripe.com/resources/more/digital-wallets-101) (e.g., Apple Pay) provider initiates the transfer process as soon as the payment is made.

- **Processing:** The wallet provider might briefly hold the funds to perform security checks and fraud analysis.

- **Settlement:** Settlement typically occurs within one to three business days for [digital wallet payments](https://stripe.com/resources/more/mobile-payments-explained-a-guide-for-businesses).

- **Funding:** The business can access funds within two to three business days of the initial transaction.


### Global payment networks

- **Initiation:** For an international bank transfer, a bank sends a payment order through the [SWIFT network](https://stripe.com/resources/more/what-is-swift).

- **Processing:** SWIFT forwards the message to the beneficiary’s bank. Banks might use intermediary banks to facilitate the transfer.

- **Settlement:** Settlement times can vary from one to four business days, depending on the global payment network and banks involved.

- **Funding:** Funds are available to the recipient once the receiving bank has processed the payment.


## Security and fraud prevention during settlement

Security and fraud prevention in payment settlement is a collaborative effort. Payment networks like [Visa](https://stripe.com/resources/more/what-is-visa) and [Mastercard](https://stripe.com/resources/more/what-is-mastercard) have strong fraud detection systems, and they share information about suspicious activity with issuing and acquiring banks.

Issuing banks monitor transactions for fraudulent behavior and might contact the cardholder for verification if they detect suspicious activity. Acquiring banks also work with payment networks and issuing banks to prevent fraudulent transactions.

Here are some commonly used security measures and fraud prevention tools that protect the payment process.

### Security measures

- **Encryption:** Sensitive data (e.g., credit card numbers) is [encrypted](https://stripe.com/resources/more/encryption-vs-tokenization-how-they-are-different-and-how-they-work-together) during transmission, making it unreadable to unauthorized parties.

- **Authentication:** Authentication verifies the identities of both the payer and payee. This can involve [multifactor authentication](https://stripe.com/resources/more/secure-payment-systems-explained) or password verification for online transactions.

- **Tokenization:** [Tokenization](https://stripe.com/resources/more/payment-tokenization-101) replaces card details with unique tokens during transactions. This reduces the risk that account information will be compromised, even if a breach occurs.

- **Access controls:** Access to sensitive financial data is limited to authorized personnel and systems.

- **Firewalls and intrusion detection:** These systems prevent unauthorized access to financial systems.


### Fraud prevention measures

- **Transaction monitoring:** Transaction patterns are analyzed for suspicious activity such as large purchases from unusual locations.

- **Velocity checks:** The frequency and volume of transactions are monitored to identify potential attempts to exploit stolen credentials.

- **Address verification service (AVS):** The [billing address](https://stripe.com/resources/more/secure-payment-systems-explained) provided by the payer is compared with the information on file at their bank.

- **CVV:** The payer must verify the [unique CVV code](https://stripe.com/resources/more/what-is-card-verification-value) on the back of their card to confirm they possess the physical card during online transactions.

- **Risk scoring:** Each transaction is assigned a risk score based on factors such as purchase history and location. Transactions with high-risk scores might be flagged for further scrutiny.


## Legal and compliance considerations around payment settlements

Payment settlement processes are subject to a set of laws and regulations that govern their security and compliance with national and global standards. Businesses must comply with these laws and regulations in each jurisdiction where they operate.

For international settlements, businesses must consider the legal requirements of all involved jurisdictions, including currency controls, reporting requirements, and cross-border transaction regulations. All businesses must maintain accurate transaction records and be prepared to report to regulators when necessary.

Compliance requirements regarding payment settlement include standards set by financial authorities and regulatory bodies such as the Financial Crimes Enforcement Network in the [United States](https://stripe.com/resources/more/payments-in-the-united-states-an-in-depth-guide) and the Financial Conduct Authority in the [United Kingdom](https://stripe.com/resources/more/payments-in-the-united-kingdom-an-in-depth-guide). These are the regulated areas related to payment settlement:

- **Anti-Money Laundering (AML):** AML regulations prevent the flow of funds related to illicit activities. Companies must implement effective [AML programs](https://www.cftc.gov/IndustryOversight/AntiMoneyLaundering/dsio_amlprograms.html), including Know Your Customer (KYC) processes, to verify the identities of their customers and monitor transactions for suspicious activities.

- **Countering the Financing of Terrorism (CFT):** [CFT regulations](https://www.fdic.gov/banker-resource-center/anti-money-laundering-countering-financing-terrorism-amlcft) prevent the use of financial systems for funding terrorist activities. Companies must comply with CFT measures to prevent the misuse of payment systems.

- **Data protection and privacy:** Data protection regulations protect the privacy and integrity of customer information. Companies must comply with data protection regulations such as the [General Data Protection Regulation (GDPR)](https://gdpr.eu/) in the European Union.

- **Consumer protection laws:** Consumer protection laws protect customers’ rights in financial transactions. They mandate transparent disclosure of fees, the right to dispute transactions, and protection against unauthorized payments.

- **Payment Card Industry Data Security Standard (PCI DSS):** The PCI DSS ensures the security of card transactions and protects against data breaches. All businesses that handle card payments must comply with it.

- **Contractual obligations:** Agreements with payment processors, banks, and other financial partners often come with their own legal and compliance obligations. These contracts must align with broader regulatory requirements.


## Payment settlement best practices for businesses

Here are some measures you can take to facilitate payment settlement:

- **Payment controls:** Create a solid framework for overseeing payments. Divide responsibilities to assure that no individual has too much control and that every step, from initiation to [reconciliation](https://stripe.com/resources/more/payment-reconciliation-101), is double-checked.

- **Security measures:** Use payments platforms that exceed industry benchmarks such as the PCI DSS. Keep your systems up-to-date with the latest updates and strong [encryption](https://stripe.com/resources/more/point-to-point-encryption) to safeguard every transaction.

- **Reconciliation:** Make it a habit to reconcile your payment activities with your bank statements and financial records. Quick reconciliation helps you spot any mismatches early, combat fraud, and keep your financial data accurate.

- **Compliance:** Continually review any local and global rules that affect your payment activities. From AML to data protection, ensure you’re aware of and fully comply with these regulations.

- **KYC:** Use KYC practices to confirm the identities of your clients and reduce fraud risks.

- **Payment terms:** Clarify your payment conditions in every contract, including payment timelines and penalties if deadlines are missed.

- **Disputes:** Develop a process for handling chargebacks or disputes that allows you to respond promptly and keep a detailed log of what occurred and how it was resolved.

- **Staff training:** Regularly train and update your team on the latest in payment security and compliance to handle new threats and regulatory shifts.

- **Data protection:** Follow all applicable data protection laws and protect your customers’ data with strict access protocols.

- **Transaction monitoring:** Monitor payment transactions for anything unusual. Regular audits help you spot and fix any issues in your payment processes.

- **Vendor assessment:** If you’re working with third-party payment processors, ensure they comply with industry standards and regulations.

- **Backup plans:** Have a backup plan in place so your payment processes keep working even when faced with disruptions such as tech glitches, manual errors, and natural disasters.


## How Stripe Payments can help

[Stripe Payments](https://stripe.com/payments) provides a unified, global payment solution that helps any business—from scaling startups to global enterprises—accept payments online, in person, and around the world.

Stripe Payments can help you:

- **Optimize your checkout experience:** Create a frictionless customer experience and save thousands of engineering hours with prebuilt payment UIs, access to 100+ payment methods, and Link, Stripe’s digital wallet.

- **Expand to new markets faster:** Reach customers worldwide and reduce the complexity and cost of multicurrency management with cross-border payment options, available in 195 countries across 135+ currencies.

- **Unify payments in person and online:** Build a unified commerce experience across online and in-person channels to personalize interactions, reward loyalty, and grow revenue.

- **Improve payment performance:** Increase revenue with a range of customizable, easy-to-configure payment tools, including no-code fraud protection and advanced capabilities to improve authorization rates.

- **Move faster with a flexible, reliable platform for growth:** Build on a platform designed to scale with you, with 99.999% uptime and industry-leading reliability.


Learn more about how [Stripe Payments](https://docs.stripe.com/payments) can power your online and in-person payments, or [get started](https://dashboard.stripe.com/register/payments) today.

The content in this article is for general information and education purposes only and should not be construed as legal or tax advice. Stripe does not warrant or guarantee the accurateness, completeness, adequacy, or currency of the information in the article. You should seek the advice of a competent attorney or accountant licensed to practice in your jurisdiction for advice on your particular situation.

- [<span data-js-target="MoreArticlesSection.label"></span>](about:blank#)
- Something went wrong. Please try again or contact support.

Create an account and start accepting payments—no contracts or banking details required. Or, contact us to design a custom package for your business.


Accept payments online, in person, and around the world with a payments solution built for any business.


Find a guide to integrate Stripe's payments APIs.