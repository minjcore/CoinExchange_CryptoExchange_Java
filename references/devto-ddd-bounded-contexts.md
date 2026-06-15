## Introduction

When modeling complex systems using **Domain-Driven Design (DDD)**, one of the most common dilemmas architects and engineers face is:

> _“If two bounded contexts use the same concept — like a `Customer` — should they share a common model or have separate ones?”_

Take, for example, **Sales** and **Support** systems within an organization. Both deal with a `Customer`, but they use that concept differently. So what’s the right way to model this scenario in line with DDD principles?

![Bounded Context of Sales and Support](https://media2.dev.to/dynamic/image/width=800%2Cheight=%2Cfit=scale-down%2Cgravity=auto%2Cformat=auto/https%3A%2F%2Fdev-to-uploads.s3.amazonaws.com%2Fuploads%2Farticles%2Fax8f996efd0s61g8y37w.png)

_original context map image courtesy: [https://martinfowler.com/bliki/BoundedContext.html](https://martinfowler.com/bliki/BoundedContext.html)_

Let’s explore.

* * *

## Understanding the Problem: Shared Concepts, Divergent Needs

In many organizations:

- The **Sales** team cares about a customer's _lead score_, _purchase intent_, and _assigned sales rep_.
- The **Support** team, on the other hand, is focused on _support ticket history_, _SLA agreements_, and _incident timelines_.

Clearly, while the term "Customer" is shared, its **meaning** and **usage** diverge across these contexts. This is a textbook case of **polysemy** in DDD — the same term having multiple meanings in different bounded contexts.

* * *

## What Does DDD Recommend?

### ✅ Model Each Bounded Context Independently

Rather than forcing a single `Customer` entity to serve multiple masters, DDD encourages modeling the concept **according to the specific domain's needs**.

This means creating:

- `SalesCustomer` in the Sales context.
- `SupportCustomer` in the Support context.

Each entity will only contain the attributes and behavior relevant to its respective domain. They can be kept in sync using **integration events** or APIs.

* * *

## Why Not Share a Single Customer Entity?

Sharing a single customer model across contexts may seem DRY and clean at first, but it introduces tight coupling, cross-team dependencies, and often leads to bloated models full of irrelevant attributes.

### Problems with a Shared Model:

- Code becomes harder to maintain.
- Changes in one context impact others.
- The model becomes a compromise that satisfies no one well.

* * *

## Integration Through Events: The DDD Way

In a properly decoupled system, Sales might **publish domain events** such as `CustomerCreated`, `CustomerUpdated`, or `LeadConverted`.

Support consumes these events and updates its own `SupportCustomer` read model.

This keeps the systems loosely coupled and allows each to evolve independently.

* * *

## Diagram: Visualizing the Pattern

![DDD Context Map of Sales and Support](https://media2.dev.to/dynamic/image/width=800%2Cheight=%2Cfit=scale-down%2Cgravity=auto%2Cformat=auto/https%3A%2F%2Fdev-to-uploads.s3.amazonaws.com%2Fuploads%2Farticles%2Fo466y53t24l2i97zagod.png)

- Each **bounded context** maintains its own `Customer` model.
- **Integration events** (via Kafka, EventBridge, etc.) keep the models in sync.
- The system respects **context boundaries** and encourages **autonomy**.

* * *

## Other Modeling Strategies

In DDD, this scenario can also be approached through different integration patterns depending on organizational and technical constraints:

| Pattern | Description |
| --- | --- |
| **Shared Kernel** (discouraged but still valid in rare scenarios) | Shared subset of the model agreed upon and versioned by both teams |
| **Customer-as-a-Service** | One context exposes an API or event feed; others consume with translation |
| **Anti-Corruption Layer** | Context-specific adapter to isolate internal models from external contracts |

* * *

## Practical Guidelines

- **Avoid modeling based on database schemas**. Model based on domain **semantics**.
- **Treat duplication as an acceptable trade-off** for autonomy and clarity.
- **Use ubiquitous language** in each context; don’t force uniform terminology.
- **Synchronize data through events**, not shared objects.

* * *

## Final Thoughts

In Domain-Driven Design, **context is king**. Just because different domains refer to a `Customer` doesn’t mean they are talking about the same concept. Forcing a unified model creates fragility and limits agility.

Instead, **embrace duplication**, **model explicitly**, and **integrate intentionally**. That’s how you build robust, evolvable systems.

[![profile](https://media2.dev.to/dynamic/image/width=64,height=64,fit=cover,gravity=auto,format=auto/https%3A%2F%2Fdev-to-uploads.s3.amazonaws.com%2Fuploads%2Forganization%2Fprofile_image%2F140%2F9639a040-3c27-4b99-b65a-85e100016d3c.png)\\
MongoDB](https://dev.to/mongodb) Promoted

Dropdown menu

- [What's a billboard?](https://dev.to/billboards)
- [Manage preferences](https://dev.to/settings/customization#sponsors)

* * *

- [Report billboard](https://dev.to/report-abuse?billboard=241235)

[![Build seamlessly, securely, and flexibly with MongoDB Atlas. Try free.](https://media2.dev.to/dynamic/image/width=775%2Cheight=%2Cfit=scale-down%2Cgravity=auto%2Cformat=auto/https%3A%2F%2Fi.imgur.com%2FVYTIlUE.png)](https://www.mongodb.com/cloud/atlas/lp/try3?utm_campaign=display_devto-broad_pl_flighted_atlas_tryatlaslp_prosp_gic-null_ww-all_dev_dv-all_eng_leadgen&utm_source=devto&utm_medium=display&utm_content=runappsanywhere-v1&bb=241235)

## [Build seamlessly, securely, and flexibly with MongoDB Atlas. Try free.](https://www.mongodb.com/cloud/atlas/lp/try3?utm_campaign=display_devto-broad_pl_flighted_atlas_tryatlaslp_prosp_gic-null_ww-all_dev_dv-all_eng_leadgen&utm_source=devto&utm_medium=display&utm_content=runappsanywhere-v1&bb=241235)

MongoDB Atlas lets you build and run modern apps in 125+ regions across AWS, Azure, and Google Cloud. Multi-cloud clusters distribute data seamlessly and auto-failover between providers for high availability and flexibility. Start free!

[Learn More](https://www.mongodb.com/cloud/atlas/lp/try3?utm_campaign=display_devto-broad_pl_flighted_atlas_tryatlaslp_prosp_gic-null_ww-all_dev_dv-all_eng_leadgen&utm_source=devto&utm_medium=display&utm_content=runappsanywhere-v1&bb=241235)

Read More


![pic](https://media2.dev.to/dynamic/image/width=256,height=,fit=scale-down,gravity=auto,format=auto/https%3A%2F%2Fdev-to-uploads.s3.amazonaws.com%2Fuploads%2Farticles%2F8j7kvp660rqzt99zui8e.png)

[Create template](https://dev.to/settings/response-templates)

Templates let you quickly answer FAQs or store snippets for re-use.

SubmitPreview [Dismiss](https://dev.to/404.html)

Are you sure you want to hide this comment? It will become hidden in your post, but will still be visible via the comment's [permalink](https://dev.to/aws-builders/modeling-shared-entities-across-bounded-contexts-in-domain-driven-design-5hih#).


Hide child comments as well

Confirm


For further actions, you may consider blocking this person and/or [reporting abuse](https://dev.to/report-abuse)

[![profile](https://media2.dev.to/dynamic/image/width=64,height=64,fit=cover,gravity=auto,format=auto/https%3A%2F%2Fdev-to-uploads.s3.amazonaws.com%2Fuploads%2Forganization%2Fprofile_image%2F2794%2F88da75b6-aadd-4ea1-8083-ae2dfca8be94.png)\\
AWS Community Builders](https://dev.to/aws-builders)

Dropdown menu

- [What's a billboard?](https://dev.to/billboards)
- [Manage preferences](https://dev.to/settings/customization#sponsors)

* * *

- [Report billboard](https://dev.to/report-abuse?billboard=59819)

[![Best Practices for Running  Container WordPress on AWS (ECS, EFS, RDS, ELB) using CDK cover image](https://media2.dev.to/dynamic/image/width=775%2Cheight=%2Cfit=scale-down%2Cgravity=auto%2Cformat=auto/https%3A%2F%2Fdev-to-uploads.s3.amazonaws.com%2Fi%2Fisx76i4lh7o13c7twd2c.png)](https://dev.to/aws-builders/best-practices-for-running-wordpress-on-aws-using-cdk-aj9?bb=59819)

## [Best Practices for Running Container WordPress on AWS (ECS, EFS, RDS, ELB) using CDK](https://dev.to/aws-builders/best-practices-for-running-wordpress-on-aws-using-cdk-aj9?bb=59819)

This post discusses the process of migrating a growing WordPress eShop business to AWS using AWS CDK for an easily scalable, high availability architecture. The detailed structure encompasses several pillars: Compute, Storage, Database, Cache, CDN, DNS, Security, and Backup.

**[Read full post](https://dev.to/aws-builders/best-practices-for-running-wordpress-on-aws-using-cdk-aj9?bb=59819)**

DEV Education Tracks

Dropdown menu

- [What's a billboard?](https://dev.to/billboards)
- [Manage preferences](https://dev.to/settings/customization#sponsors)

* * *

- [Report billboard](https://dev.to/report-abuse?billboard=238629)

x

### Announcing the First DEV Education Track: "Build Apps with Google AI Studio"

The moment is here! We recently announced DEV Education Tracks, our new initiative to bring you structured learning paths directly from industry experts.


[Dive in and Learn](https://dev.to/devteam/announcing-the-first-dev-education-track-build-apps-with-google-ai-studio-ej7?bb=238629)

DEV is bringing Education Tracks to the community. Dismiss if you're not interested. ❤️


![DEV Community](https://media2.dev.to/dynamic/image/width=190,height=,fit=scale-down,gravity=auto,format=auto/https%3A%2F%2Fdev-to-uploads.s3.amazonaws.com%2Fuploads%2Farticles%2F8j7kvp660rqzt99zui8e.png)

We're a place where coders share, stay up-to-date and grow their careers.


[Log in](https://dev.to/enter?signup_subforem=1) [Create account](https://dev.to/enter?signup_subforem=1&state=new-user)

![](https://assets.dev.to/assets/sparkle-heart-5f9bee3767e18deb1bb725290cb151c25234768a0e9a2bd39370c382d02920cf.svg)![](https://assets.dev.to/assets/multi-unicorn-b44d6f8c23cdd00964192bedc38af3e82463978aa611b4365bd33a0f1f4f3e97.svg)![](https://assets.dev.to/assets/exploding-head-daceb38d627e6ae9b730f36a1e390fca556a4289d5a41abb2c35068ad3e2c4b5.svg)![](https://assets.dev.to/assets/raised-hands-74b2099fd66a39f2d7eed9305ee0f4553df0eb7b4f11b01b6b1b499973048fe5.svg)![](https://assets.dev.to/assets/fire-f60e7a582391810302117f987b22a8ef04a2fe0df7e3258a5f49332df1cec71e.svg)