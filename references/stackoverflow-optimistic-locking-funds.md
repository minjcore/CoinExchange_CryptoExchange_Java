##### Collectives™ on Stack Overflow

Find centralized, trusted content and collaborate around the technologies you use most.

[Learn more about Collectives](https://stackoverflow.com/collectives)

**Stack Internal**

Knowledge at work

Bring the best of human thought and AI automation together at your work.

[Explore Stack Internal](https://stackoverflow.co/internal/?utm_medium=referral&utm_source=stackoverflow-community&utm_campaign=side-bar&utm_content=explore-teams-compact-popover)

# [Is optimistic locking enough to secure operations such as funds transfer?](https://stackoverflow.com/questions/68236336/is-optimistic-locking-enough-to-secure-operations-such-as-funds-transfer)

[Ask Question](https://stackoverflow.com/questions/ask)

Asked4 years, 11 months ago

Modified [4 years, 11 months ago](https://stackoverflow.com/questions/68236336/is-optimistic-locking-enough-to-secure-operations-such-as-funds-transfer?lastactivity "2021-07-07 19:57:19Z")

Viewed
463 times


This question shows research effort; it is useful and clear

2

This question does not show any research effort; it is unclear or not useful

Save this question.

[Timeline](https://stackoverflow.com/posts/68236336/timeline)

Show activity on this post.

In the following example we may run into a concurrency error where if the sender decides to send funds to 2 different receivers in the same time then both receivers may get the money while the sender will only be charged once.

```csharp
Copy
public async Task SendMoney(int amount, int sender, int receiver)
{
    await using var dbContext = new ApplicationDbContext();

    var person1 = await dbContext.Persons.FirstOrDefaultAsync(p => p.Id == sender);
    var person2 = await dbContext.Persons.FirstOrDefaultAsync(p => p.Id == receiver);

    if (person1.Balance >= amount)
    {
        person1.Balance -= amount;
        person2.Balance += amount;
    }

    await dbContext.SaveChangesAsync();
}
```

What would be the best way to avoid such scenario and make this operation secure?

Is introducing optimistic locking by adding timestamp property `[Timestamp] public byte[] Version { get; set; }` to the `Person` model enough to make sure there is no way of duplicating funds?

I mostly care about the security of avoiding duplication. Properly handling exceptions such as `DbUpdateConcurrencyException` in order to provide smooth user experience is not part of my question.

- [c#](https://stackoverflow.com/questions/tagged/c%23 "show questions tagged 'c#'")
- [database](https://stackoverflow.com/questions/tagged/database "show questions tagged 'database'")
- [entity-framework-core](https://stackoverflow.com/questions/tagged/entity-framework-core "show questions tagged 'entity-framework-core'")
- [locking](https://stackoverflow.com/questions/tagged/locking "show questions tagged 'locking'")

[Share](https://stackoverflow.com/q/68236336)

Share a link to this question

Copy link [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/ "The current license for this post: CC BY-SA 4.0")

Short permalink to this question

[Improve this question](https://stackoverflow.com/posts/68236336/edit "")

Follow



Follow this question to receive notifications

[edited Jul 3, 2021 at 17:33](https://stackoverflow.com/posts/68236336/revisions "show all edits to this post")

asked Jul 3, 2021 at 12:37

[![QmlnR2F5's user avatar](https://www.gravatar.com/avatar/f09b77fd8a33c1326c522daab4dab444?s=64&d=identicon&r=PG&f=y&so-version=2)](https://stackoverflow.com/users/8131107/qmlnr2f5)

[QmlnR2F5](https://stackoverflow.com/users/8131107/qmlnr2f5)

1,3891515 silver badges2424 bronze badges

14

- 1





Yes, that would ensure consistency



ErikEJ


–
[ErikEJ](https://stackoverflow.com/users/183934/erikej "42,100 reputation")



2021-07-03 13:25:18 +00:00

[CommentedJul 3, 2021 at 13:25](https://stackoverflow.com/questions/68236336/is-optimistic-locking-enough-to-secure-operations-such-as-funds-transfer#comment120598848_68236336)


- 1





unless you operate inside a database transaction, there is no guarantee that another application might change your data.



Mitch Wheat


–
[Mitch Wheat](https://stackoverflow.com/users/16076/mitch-wheat "301,795 reputation")



2021-07-04 12:41:23 +00:00

[CommentedJul 4, 2021 at 12:41](https://stackoverflow.com/questions/68236336/is-optimistic-locking-enough-to-secure-operations-such-as-funds-transfer#comment120613302_68236336)

- 3





"Optimistic" doesn't mean that you fire the thing and forget/pray. It means if an error happens, you'll have to do something (ie: handle errors/exceptions), as opposed to "Pessimistic" where you try to lock everything that must be (which can cost lots in terms of transaction, etc.) _beforehand_, then do it. It's just two ways of doing thing, but the net results is always supposed to be correct (whether it succeeded or failed)



Simon Mourier


–
[Simon Mourier](https://stackoverflow.com/users/403671/simon-mourier "141,838 reputation")



2021-07-08 12:35:37 +00:00

[CommentedJul 8, 2021 at 12:35](https://stackoverflow.com/questions/68236336/is-optimistic-locking-enough-to-secure-operations-such-as-funds-transfer#comment120712921_68236336)

- 2





I agree with @SimonMourier. You don't seem to understand that the whole transfer operation fails if _one_ of the Balances changes in the mean time. It's impossible that "both receivers may get the money while the sender will only be charged once." If the sender is charged once only one receiver will actually have received payment. That's the trouble with this question, seemingly it's based on a misconception, and if that's been cleared away the question "is it enough" is still opinion-based.



Gert Arnold


–
[Gert Arnold](https://stackoverflow.com/users/861716/gert-arnold "110,133 reputation")



2021-07-08 21:24:45 +00:00

[CommentedJul 8, 2021 at 21:24](https://stackoverflow.com/questions/68236336/is-optimistic-locking-enough-to-secure-operations-such-as-funds-transfer#comment120725871_68236336)

- 4





Properly implemented, Optimistic vs Pessimistic has nothing to do with reliability or data integrity, it has to do with whether your transaction conflicts happen at the beginning (preventative) or at the end (corrective). Data integrity/risk is determined by the isolation level.



RBarryYoung


–
[RBarryYoung](https://stackoverflow.com/users/109122/rbarryyoung "57,217 reputation")



2021-07-12 20:13:35 +00:00

[CommentedJul 12, 2021 at 20:13](https://stackoverflow.com/questions/68236336/is-optimistic-locking-enough-to-secure-operations-such-as-funds-transfer#comment120803790_68236336)


[Use comments to ask for more information or suggest improvements. Avoid answering questions in comments.](https://stackoverflow.com/questions/68236336/is-optimistic-locking-enough-to-secure-operations-such-as-funds-transfer# "Use comments to ask for more information or suggest improvements. Avoid answering questions in comments.") \| [Show **9** more comments](https://stackoverflow.com/questions/68236336/is-optimistic-locking-enough-to-secure-operations-such-as-funds-transfer# "Expand to show all comments on this post")

## 1 Answer 1

Sorted by:
[Reset to default](https://stackoverflow.com/questions/68236336/is-optimistic-locking-enough-to-secure-operations-such-as-funds-transfer?answertab=scoredesc#tab-top)

Highest score (default)

Trending (recent votes count more)

Date modified (newest first)

Date created (oldest first)


This answer is useful

2

This answer is not useful

Save this answer.

Loading when this answer was accepted…

[Timeline](https://stackoverflow.com/posts/68244665/timeline)

Show activity on this post.

In a concurrent environment `[Timestamp]` will ensure the row is saved only if it was not changed since loaded from DB, so it will work.

Also for the case you have provided, it is possible to set `Balance` field as [Concurrency Token](https://learn.microsoft.com/en-us/ef/core/modeling/concurrency?tabs=data-annotations).

[Share](https://stackoverflow.com/a/68244665)

Share a link to this answer

Copy link [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/ "The current license for this post: CC BY-SA 4.0")

Short permalink to this answer

[Improve this answer](https://stackoverflow.com/posts/68244665/edit "")

Follow



Follow this answer to receive notifications

answered Jul 4, 2021 at 12:20

[![FireAlkazar's user avatar](https://i.sstatic.net/wWkHs.png?s=64)](https://stackoverflow.com/users/1479335/firealkazar)

[FireAlkazar](https://stackoverflow.com/users/1479335/firealkazar)

1,89011 gold badge1616 silver badges3030 bronze badges

Sign up to request clarification or add additional context in comments.


## 1 Comment

Add a comment

[![](https://www.gravatar.com/avatar/f09b77fd8a33c1326c522daab4dab444?s=48&d=identicon&r=PG&f=y&so-version=2)](https://stackoverflow.com/users/8131107/qmlnr2f5)

QmlnR2F5

[QmlnR2F5](https://stackoverflow.com/users/8131107/qmlnr2f5) [Over a year ago](https://stackoverflow.com/questions/68236336/is-optimistic-locking-enough-to-secure-operations-such-as-funds-transfer#comment120613185_68244665)

Extra kudos to you, sir for mentioning Concurrency Token. Fantastic answer.

2021-07-04T12:30:39.09Z+00:00

0

Reply

- Copy link

## Your Answer

Draft saved

Draft discarded

### Sign up or [log in](https://stackoverflow.com/users/login?ssrc=question_page&returnurl=https%3a%2f%2fstackoverflow.com%2fquestions%2f68236336%2fis-optimistic-locking-enough-to-secure-operations-such-as-funds-transfer%23new-answer)

Sign up using Google


Sign up using Email and Password


Submit

### Post as a guest

Name

Email

Required, but never shown

Post Your Answer

Discard


By clicking “Post Your Answer”, you agree to our [terms of service](https://stackoverflow.com/legal/terms-of-service/public) and acknowledge you have read our [privacy policy](https://stackoverflow.com/legal/privacy-policy).


Start asking to get answers

Find the answer to your question by asking.

[Ask question](https://stackoverflow.com/questions/ask)

Explore related questions

- [c#](https://stackoverflow.com/questions/tagged/c%23 "show questions tagged 'c#'")
- [database](https://stackoverflow.com/questions/tagged/database "show questions tagged 'database'")
- [entity-framework-core](https://stackoverflow.com/questions/tagged/entity-framework-core "show questions tagged 'entity-framework-core'")
- [locking](https://stackoverflow.com/questions/tagged/locking "show questions tagged 'locking'")

See similar questions with these tags.

lang-cs