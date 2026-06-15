In Fintech domain, especially when handling transactions Optimistic Locking & Pessimistic Locking mechanisms play a pivotal role in ensuring data integrity and preventing concurrency issues.

So, I’m here to share my knowledge and insights on these two types of locking.

Let’ assume a scenario, I have a bank account with a balance of Rs. 5000. I can make payments through mobile banking application and debit card. I given my debit card to my mother to buy goods at home. Then, she went to the supermarket and came to pay Rs. 4500 for goods. At the same time, I pay my mobile phone bill which is Rs. 3000.

Then the two transactions happened at the same time.

**Transaction 1 – Rs. 4500 – through debit card**

**Transaction 2 – Rs. 3000 – through mobile banking app**

**Account Balance – Rs. 5000.**

If both transactions are completed, it leads to a drop of account balance below zero or data inconsistency.

The solution to this problem is to use locking. There are two types of locking mechanisms.

1. Pessimistic Locking
2. Optimistic Locking

## Pessimistic Locking

It is a locking strategy when a transaction reads a record, it should be locked. Then other transactions cannot read or modify that record until the lock is released. This approach is used in high concurrency environments.

Types of Pessimistic Locks:

1. PESSIMISTIC\_READ – Allows reading but it blocks writing.
2. PESSIMISTIC\_WRITE – Blocks both reading and writing.
3. PESSIMISTIC\_FORCE\_INCREMENT – like PESSIMISTIC\_WRITE but also increment the version number even if no changes made.

**PESSIMISTIC\_READ**

PESSIMISTIC\_READ is the locking strategy shared access which means all transactions can read the record but cannot modify it while the lock is held. It blocks the write operation.

`@Lock(value = LockModeType.PESSIMISTIC_READ)

Optional<AccountBalance> findById(Integer id);`

**PESSIMISTIC\_WRITE**

Transactions cannot do read write operation until the lock release. No transaction can perform read write operations until the current transaction finishes.

`@Lock(value = LockModeType.PESSIMISTIC_WRITE)

Optional<AccountBalance> findById(Integer id);`

**PESSIMISTIC\_FORCE\_INCREMENT**

It is like PESSIMISTIC\_WRITE, but the only one difference is it forces the version field of the entity to increment.

`@Lock(value = LockModeType.PESSIMISTIC_FORCE_INCREMENT)

Optional<AccountBalance> findById(Integer id);`

## Optimistic Locking

It allows multiple transactions to read and write data simultaneously, without holding locks.

Types of Optimistic Locks:

1. OPTIMISTIC
2. OPTIMISTIC\_FORCE\_INCREMENT

**OPTIMISTIC**

It increases the version number when a record changes.

`@Lock(value = LockModeType.OPTIMISTIC)

Optional<AccountBalance> findById(Integer id);`

**OPTIMISTIC\_FORCE\_INCREMENT**

It increases the version number when transactions perform read or write operations.

`@Lock(value = LockModeType.OPTIMISTIC_FORCE_INCREMENT)

Optional<AccountBalance> findById(Integer id);`

[![profile](https://media2.dev.to/dynamic/image/width=64,height=64,fit=cover,gravity=auto,format=auto/https%3A%2F%2Fdev-to-uploads.s3.amazonaws.com%2Fuploads%2Forganization%2Fprofile_image%2F634%2Fc6bfc78f-136d-456b-96dc-bcc4be1c88f9.jpg)\\
Auth0](https://dev.to/auth0) Promoted

Dropdown menu

- [What's a billboard?](https://dev.to/billboards)
- [Manage preferences](https://dev.to/settings/customization#sponsors)

* * *

- [Report billboard](https://dev.to/report-abuse?billboard=261648)

[![Auth0 image](https://media2.dev.to/dynamic/image/width=775%2Cheight=%2Cfit=scale-down%2Cgravity=auto%2Cformat=auto/https%3A%2F%2Fi.imgur.com%2F7HwHPTA.png)](https://auth0.com/signup?utm_source=devto&utm_campaign=amer_namer_usa_all_ciam-dev_dg-plg_auth0_native_devto_native_aud_native-q12026_utm2&utm_medium=cpc&utm_id=aNKWR000002xVcv4AE&bb=261648)

## [Flexible pricing for all your apps and AI agents from user 1 to 1,000,000+.](https://auth0.com/signup?utm_source=devto&utm_campaign=amer_namer_usa_all_ciam-dev_dg-plg_auth0_native_devto_native_aud_native-q12026_utm2&utm_medium=cpc&utm_id=aNKWR000002xVcv4AE&bb=261648)

[Get started today](https://auth0.com/signup?utm_source=devto&utm_campaign=amer_namer_usa_all_ciam-dev_dg-plg_auth0_native_devto_native_aud_native-q12026_utm2&utm_medium=cpc&utm_id=aNKWR000002xVcv4AE&bb=261648)

Read More


![pic](https://media2.dev.to/dynamic/image/width=256,height=,fit=scale-down,gravity=auto,format=auto/https%3A%2F%2Fdev-to-uploads.s3.amazonaws.com%2Fuploads%2Farticles%2F8j7kvp660rqzt99zui8e.png)

[Create template](https://dev.to/settings/response-templates)

Templates let you quickly answer FAQs or store snippets for re-use.

SubmitPreview [Dismiss](https://dev.to/404.html)

Are you sure you want to hide this comment? It will become hidden in your post, but will still be visible via the comment's [permalink](https://dev.to/hirushi_nethmini_41168bb8/optimistic-locking-pessimistic-locking-2p30#).


Hide child comments as well

Confirm


For further actions, you may consider blocking this person and/or [reporting abuse](https://dev.to/report-abuse)

[![profile](https://media2.dev.to/dynamic/image/width=64,height=64,fit=cover,gravity=auto,format=auto/https%3A%2F%2Fdev-to-uploads.s3.amazonaws.com%2Fuploads%2Forganization%2Fprofile_image%2F1%2Fd908a186-5651-4a5a-9f76-15200bc6801f.jpg)\\
The DEV Team](https://dev.to/devteam) Promoted

Dropdown menu

- [What's a billboard?](https://dev.to/billboards)
- [Manage preferences](https://dev.to/settings/customization#sponsors)

* * *

- [Report billboard](https://dev.to/report-abuse?billboard=263439)

[![Google article image](https://media2.dev.to/dynamic/image/width=775%2Cheight=%2Cfit=scale-down%2Cgravity=auto%2Cformat=auto/https%3A%2F%2Fdev-to-uploads.s3.amazonaws.com%2Fuploads%2Farticles%2Fi6mj0yymgm9gmhlz7l4y.png)](https://dev.to/googleai/architect-a-personalized-multi-agent-system-with-long-term-memory-3o15?bb=263439)

## [Architect A Personalized Multi-Agent System with Long-Term Memory](https://dev.to/googleai/architect-a-personalized-multi-agent-system-with-long-term-memory-3o15?bb=263439)

In support of our mission to accelerate the developer journey on Google Cloud, we built Dev Signal — a multi-agent system designed to transform raw community signals into reliable technical guidance by automating the path from discovery to expert creation.

[Read more →](https://dev.to/googleai/architect-a-personalized-multi-agent-system-with-long-term-memory-3o15?bb=263439)

👋 Kindness is contagious

Dropdown menu

- [What's a billboard?](https://dev.to/billboards)
- [Manage preferences](https://dev.to/settings/customization#sponsors)

* * *

- [Report billboard](https://dev.to/report-abuse?billboard=236879)

x

Dive into this thoughtful piece, beloved in the supportive DEV Community. **Coders of every background** are invited to share and elevate our collective know-how.

A sincere "thank you" can brighten someone's day—leave your appreciation below!

On DEV, **sharing knowledge smooths our journey** and tightens our community bonds. Enjoyed this? A quick thank you to the author is hugely appreciated.

### [Okay](https://dev.to/enter?state=new-user&bb=236879)

![DEV Community](https://media2.dev.to/dynamic/image/width=190,height=,fit=scale-down,gravity=auto,format=auto/https%3A%2F%2Fdev-to-uploads.s3.amazonaws.com%2Fuploads%2Farticles%2F8j7kvp660rqzt99zui8e.png)

We're a place where coders share, stay up-to-date and grow their careers.


[Log in](https://dev.to/enter?signup_subforem=1) [Create account](https://dev.to/enter?signup_subforem=1&state=new-user)

![](https://assets.dev.to/assets/sparkle-heart-5f9bee3767e18deb1bb725290cb151c25234768a0e9a2bd39370c382d02920cf.svg)![](https://assets.dev.to/assets/multi-unicorn-b44d6f8c23cdd00964192bedc38af3e82463978aa611b4365bd33a0f1f4f3e97.svg)![](https://assets.dev.to/assets/exploding-head-daceb38d627e6ae9b730f36a1e390fca556a4289d5a41abb2c35068ad3e2c4b5.svg)![](https://assets.dev.to/assets/raised-hands-74b2099fd66a39f2d7eed9305ee0f4553df0eb7b4f11b01b6b1b499973048fe5.svg)![](https://assets.dev.to/assets/fire-f60e7a582391810302117f987b22a8ef04a2fe0df7e3258a5f49332df1cec71e.svg)