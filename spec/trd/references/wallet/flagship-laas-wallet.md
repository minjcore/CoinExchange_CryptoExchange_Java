[Skip to content](https://flagshipadvisorypartners.com/insights/ledger-as-a-service-an-emerging-infrastructure-layer-powering-fintech-innovation/#content)

Image Credit: FreePik

Innovation in financial services is at an all-time high and expanding to include an increasingly diverse range of product concepts, company types, and delivery models. Yet one thing remains constant: businesses that store, move, convert, or otherwise handle customer or partner money must keep track of those funds. As they focus on building new products, delivering exceptional customer experiences, and breaking the mold, many underestimate the complexity of fulfilling that core business requirement. In this article, we unpack why application ledgers are essential for banks, fintechs, and any other company moving money, explore the growing ecosystem of Ledger-as-a-Service (“LaaS”) providers, and highlight how LaaS is emerging as a core infrastructure layer enabling the next wave of fintech innovation.

## What is an Application Ledger, and Why Use One?

In an ideal world, every business that needs to maintain accurate records of customer or partner money — tracking balances and transaction activity — would use a robust application ledger system. At a high level, an application ledger is a purpose-built system that records the movement and balances of money at the transaction level, typically embedded directly into a company’s product or operational stack to serve as the source of truth for financial state. It sits between the business’s core product logic and its external financial infrastructure (e.g., banks, processors), enabling accurate, real-time tracking of balances and transactions that underpin operational workflows like payments, reconciliation, and customer account management. At their core, such systems should incorporate double-entry recordkeeping, ensure immutability and auditability, and function independently from the general ledger. However, the reality is often quite different, especially for early- and growth-stage companies. Instead of implementing such systems, many businesses resort to workarounds like manually tracking funds in tools like Excel, using their general ledger for dual purposes, relying on transaction logs from their payment engines, or depending on the systems and reporting provided by payment processors or partner banks (e.g., BIN sponsors).

These approaches can seem sufficient when transaction volumes and operational complexities are relatively low, but often become significant liabilities as businesses scale and evolve. For instance, expanding into new geographic markets, launching more sophisticated products, handling complex transactions like splitting settlements across multiple accounts, or engaging additional bank partners can introduce complexities that expose the shortcomings of these workarounds. In many cases, businesses do not realize the full consequences of these inadequacies until it’s too late — such as in the case of Synapse, where gaps in ledgering reportedly contributed to millions of dollars in unreconciled customer funds and a highly publicized operational collapse in 2024.

##### Figure 1: Why Application Ledgers Matter: Key Challenges & Solutions

_![Slide2-Jul-02-2025-11-35-53-1079-AM-1](https://flagshipadvisorypartners.com/wp-content/uploads/2026/03/Slide2-Jul-02-2025-11-35-53-1079-AM-1-1024x976.png)_

## Why Not Just Use a General Ledger?

Before we dig into the various options companies have for implementing an application ledger, it’s important to address a natural question: “Why can’t businesses just use their existing general ledger system for these purposes?” The short answer is that while it’s technically possible, it is far from best practice.

As summarized in Figure 2, general ledger systems also track financial transactions but are designed around GAAP principles to meet core treasury, tax, and financial reporting needs. These systems typically ingest data in batches, distilling it into normalized accounts (e.g., assets, liabilities, revenue, etc.) for financial reporting. Attempting to use the general ledger for operational purposes introduces latency, lacks transactional context, and risks corrupting financial reporting integrity.

In contrast, application ledgers are tailored to the practical, day-to-day activities of a business, such as payment processing and customer service. They handle detailed transaction-level data (e.g., timestamp, status, counterparties, etc.) in real time, and integrate seamlessly with other financial tools, platforms, and vendors (e.g., banks). These characteristics generally make application ledgers the best option for enabling businesses to scale and evolve while maintaining accuracy and efficiency in their financial operations.

##### Figure 2: General vs. Application Ledger: Different Tools for Different Jobs

_![Slide3-Jul-02-2025-11-37-05-2094-AM-1](https://flagshipadvisorypartners.com/wp-content/uploads/2026/03/Slide3-Jul-02-2025-11-37-05-2094-AM-1-1024x937.png)_

## What are the Options for Implementing an Application Ledger?

Building an in-house application ledger is a significant undertaking, requiring both substantial financial investment and specialized engineering talent, which can be difficult to find. For example, Uber likely invested millions of dollars to develop its in-house ledger system, LedgerStore, to handle the massive scale and complexity of its transactional data. Uber reported it expects to save around $6 million in costs annually as a result of its migration from DynamoDB to Ledgerstore.

However, for most businesses, especially those in their early stages, the level of investment required to build a ledger system fully in-house is simply not feasible. Recognizing this challenge, an expanding field of providers has emerged to help businesses solve their ledgering needs in more accessible and cost-effective ways. As depicted in Figure 3, these providers can be grouped into three primary categories: Domain-Specific Solutions, Ledger-as-a-Service (LaaS), and Ledger Infrastructure.

- Domain-Specific Solutions offer end-to-end systems designed to address specific workflows and business needs — such as stock trading or reconciliation and financial close — while incorporating an application ledger as a core component of the solution.
- Ledger-as-a-Service (LaaS) providers enable businesses to implement customizable, cloud-based application ledger systems that help update balances, synchronize with other ledgers, and support workflows like automated reconciliation.
- Ledger Infrastructure providers deliver the foundational technology needed to store and update balances, with performance characteristics specifically optimized for ledger use cases. These solutions provide businesses with the flexibility to build highly customized applications on top of the infrastructure.

##### Figure 3: Application Ledger Options: Purpose-Built to Platform-Based

##### __![Slide4-Jul-02-2025-11-38-18-0365-AM-1](https://flagshipadvisorypartners.com/wp-content/uploads/2026/03/Slide4-Jul-02-2025-11-38-18-0365-AM-1-1024x969.png)__

## What are the Capabilities of Ledger-as-a-Service Solutions?

As summarized in Figures 4 and 5, there is a core set of ~20 functionalities that LaaS solutions generally cover. At the heart of each lies the most fundamental features of double-entry recordkeeping, transaction status tracking, balance caching, and real-time data ingestion (from internal and external sources). Providers’ coverage of more advanced capabilities like automated reconciliation, ledger event handlers, and event (transaction) linking varies.

###### Figure 4: Application Ledger Options: Purpose-Built to Platform-Based  (not exhaustive)

##### __![Slide5-Jul-02-2025-11-39-22-0841-AM-1](https://flagshipadvisorypartners.com/wp-content/uploads/2026/03/Slide5-Jul-02-2025-11-39-22-0841-AM-1-1024x953.png)__

###### Figure 5: Application Ledger Options: Purpose-Built to Platform-Based (continued)  (not exhaustive)

##### __![Slide6-Jul-02-2025-11-40-53-4596-AM-1](https://flagshipadvisorypartners.com/wp-content/uploads/2026/03/Slide6-Jul-02-2025-11-40-53-4596-AM-1-1024x951.png)__

As one of the early developers of LaaS technology, Modern Treasury stands out as a relatively full-featured provider, serving banks, marketplaces, issuers, and other fintechs and ‘money movers’ around the world with its ledgering solutions since 2018. The LaaS market is far from a monopoly, however, with several other providers — such as Twisp, Formance, and Fragment — emerging over the past ~5 years with competing solutions. As captured in Figures 6 and 7, these newer entrants vary in their depth of functionality, but in some cases deliver niche or advanced capabilities that help differentiate their products. For instance, Twisp supports event linking, enabling multiple transactions across accounts to succeed or fail as a single atomic unit — a powerful feature for fintechs orchestrating complex fund flows. Such differentiation signals an increasingly mature and competitive ecosystem, where newer providers push the boundaries of solution design and encourage innovation across the category.

###### Figure 6: Comparison of Select Ledger-as-a-Service Providers  (as marketed on providers’ websites)

##### __![Slide7-Jul-02-2025-02-05-59-5631-PM-1](https://flagshipadvisorypartners.com/wp-content/uploads/2026/03/Slide7-Jul-02-2025-02-05-59-5631-PM-1-1024x954.png)__

###### Figure 7: Comparison of Select Ledger-as-a-Service Providers (continued)  (as marketed on providers’ websites)

##### __![Slide8-Jul-02-2025-02-06-39-9325-PM-1](https://flagshipadvisorypartners.com/wp-content/uploads/2026/03/Slide8-Jul-02-2025-02-06-39-9325-PM-1-1024x948.png)__

Historically, fintechs have long used Domain-Specific Solutions to fill the application ledger role. For instance, many core banking (e.g., Thought Machine, Finxact), card processing (e.g., Galileo), and embedded finance platforms (e.g., Atelio by FIS, Synctera, Unit) offer some form of application ledger functionality as part of their broader offerings. While effective for single-product use cases, these systems can struggle to support companies operating hybrid models — such as combining card issuing with lending, or digital wallets with investing — where funds flow across different product types and business logic. As fintechs build increasingly integrated and multi-faceted platforms, the need for a more flexible, product-agnostic ledgering layer will continue to grow.

This shift in needs has fueled the rise of general-purpose LaaS platforms that are designed to serve a broad array of use cases and deployment models. One of the clearest indicators of momentum in the LaaS space is the diversity of applications it now supports. From digital wallets and neobanks to investment platforms and vertical SaaS providers, businesses across the fintech landscape are adopting LaaS solutions to power mission-critical ledgering needs.

As shown in Figure 8, businesses across verticals are leveraging LaaS platforms to manage complex operational workflows such as balance tracking, automated reconciliation, virtual account management, and cross-border fund flows. These deployments often go live in just a few months and yield meaningful operational improvements. For example, one company eliminated its reliance on Excel to manage over 125,000 ledger accounts, while another used its LaaS implementation to accelerate a full pivot to a cross-border payments platform. These outcomes speak not only to deployment speed, but also to the strategic role LaaS can play in enabling business transformation.

###### Figure 8: Examples of Ledger-as-a-Service Implementations  (based on publicly available information)

##### __![Slide9-Jul-02-2025-11-44-33-1004-AM-1](https://flagshipadvisorypartners.com/wp-content/uploads/2026/03/Slide9-Jul-02-2025-11-44-33-1004-AM-1-1024x943.png)__

This diversity of adoption also underscores the flexibility of modern LaaS offerings. Today’s platforms can support a wide range of product models — ranging from focused, single-use-case applications to complex, multi-party financial ecosystems — while enabling rapid customization through APIs, configuration layers, and cloud-native deployment options. As a result, more companies are choosing to “buy” rather than build their ledger infrastructure, accelerating innovation while avoiding the cost and complexity of maintaining it in-house.

## What’s Next for Ledger-as-a-Service?

As fintech companies continue to build increasingly complex, real-time, and global financial products, the importance of robust, scalable ledgering infrastructure will only grow. LaaS platforms have already demonstrated their value through faster launches, cleaner audits, and more efficient operations — but their strategic importance in the ecosystem is only beginning to unfold.

Increasingly, application ledgers are becoming the connective tissue across the modern financial tech stack, interfacing with payment processors, bank APIs, orchestration engines, compliance tools, and analytics platforms. As fintechs push further into embedded finance, multi-party money movement, and cross-border flows, the need for ledgers that are not only accurate but also programmable, composable, and interoperable will become essential.

The next wave of LaaS innovation will likely focus on deeper integration with surrounding systems. Expect advances in areas such as automated reconciliation with bank feeds, webhook-based orchestration, ledger-aware compliance workflows, and event-driven observability for operational finance teams. As innovation moves in this direction, differentiation will increasingly hinge on how effectively providers deliver these capabilities — making ease of integration, developer experience, and interoperability with adjacent infrastructure key battlegrounds.

As LaaS adoption accelerates, regulatory dynamics are also shaping its evolution. From safeguarding obligations and auditability mandates to growing scrutiny of third-party dependencies, regulators are sharpening their focus on how fintechs track, reconcile, and report customer funds. In response, LaaS platforms are increasingly seen as strategic infrastructure that can help companies meet compliance obligations with greater speed, precision, and auditability.

More broadly, the rise of LaaS reflects a structural shift in fintech infrastructure: the move from hardcoded financial logic to modular, cloud-native primitives. Just as cloud data warehouses transformed how companies handle analytics, and modern payment APIs reinvented money movement, LaaS is redefining how financial systems keep score.

At the same time, emerging technologies like AI, distributed ledgers, and stablecoins are reshaping the expectations placed on modern financial systems. AI is creating new demands for real-time data availability and intelligent automation across finance workflows, while stablecoins and tokenized assets introduce new types of value transfer and custody models that require flexible, interoperable accounting frameworks. And while many DLT-based systems embed their own ledgers, traditional platforms increasingly need to bridge these new paradigms with conventional financial infrastructure. In this environment, LaaS platforms have an opportunity to serve as the connective layer — standardizing recordkeeping, enabling composability, and supporting innovation at the edges.

That connective role underscores a broader truth — LaaS is not just a utility; it’s an enabler of innovation. By offloading complexity, reducing operational risk, and making core ledgering capabilities more accessible, LaaS empowers product teams to focus on what they do best: inventing the future of fintech.

Please do not hesitate to contact Stephen Ford at [Stephen@FlagshipAP.com](mailto:Stephen@FlagshipAP.com) with your comments or questions.