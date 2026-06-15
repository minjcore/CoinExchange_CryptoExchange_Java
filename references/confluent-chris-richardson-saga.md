Enhance your career, get your certificate as a Data Streaming Engineer \| [Get your Certificate](https://developer.confluent.io/certificates/)

December 16, 2020 \| Episode 133

# Choreographing the Saga Pattern in Microservices ft. Chris Richardson

[Subscribe](https://developer.confluent.io/learn-more/podcasts/choreographing-the-saga-pattern-in-microservices-ft-chris-richardson/#moreinfo)

[Back to Podcasts](https://developer.confluent.io/learn-more/podcasts/)

Choreographing the Saga Pattern in Microservices ft. Chris Richardson

![Choreographing the Saga Pattern in Microservices ft. Chris Richardson Artwork](https://www.buzzsprout.com/rails/active_storage/representations/redirect/eyJfcmFpbHMiOnsiZGF0YSI6NDg0MjA3MjcsInB1ciI6ImJsb2JfaWQifX0=--7a6ce4e1eedd98e521bb3e87e0aad583f2f12c3a/eyJfcmFpbHMiOnsiZGF0YSI6eyJmb3JtYXQiOiJqcGciLCJyZXNpemVfdG9fZmlsbCI6WzYwMCw2MDAseyJjcm9wIjoiY2VudHJlIn1dLCJzYXZlciI6eyJxdWFsaXR5Ijo2MH0sImNvbG91cnNwYWNlIjoic3JnYiJ9LCJwdXIiOiJ2YXJpYXRpb24ifX0=--0b154cb6b88107719b1a277923d35676ba5519cc/kafka-podcast-streaming-audio-tim-berglund.jpg)

# Confluent Developer ft. Tim Berglund, Adi Polak & Viktor Gamov

Hi, we’re Tim Berglund, Adi Polak, and Viktor Gamov and we’re excited to bring you the Confluent Developer podcast (formerly “Streaming Audio.”) Our hand-crafted weekly episodes feature in-depth interviews with our community of software developers (actual human beings - not AI) talking about some of the most interesting challenges they’ve faced in their careers. We aim to explore the conditions that gave rise to each person’s technical hurdles, as well as how their experiences transformed their understanding and approach to building systems.

Whether you’re a seasoned open source data streaming engineer, or just someone who’s interested in learning more about Apache Kafka®,  Apache Flink® and real-time data, we hope you’ll appreciate the stories, the discussion, and our effort to bring you a high-quality show worth your time.

Show More

[All Episodes](https://www.buzzsprout.com/186154/episodes)

## Confluent Developer ft. Tim Berglund, Adi Polak & Viktor Gamov

# Choreographing the Saga Pattern in Microservices ft. Chris Richardson

December 16, 2020•Confluent, original creators of Apache Kafka®•Season 1•Episode 133

Share

Use Left/Right to seek, Home/End to jump to start or end. Hold shift to jump forward or backward.

0:00
\|
47:49


Show NotesChaptersTranscript

Chris Richardson, creator of the original Cloud Foundry, maintainer of microservices.io and author of “Microservices Patterns,” discovered cloud computing in 2006 during an Amazon talk about APIs for provisioning servers. At this time, you could provision 20 servers and pay 10 cents per hour. This blew his mind and led him in 2008 to create the original Cloud Foundry, a PaaS for deploying Java applications on EC2.

One of the original Cloud Foundry’s earliest success stories was a digital marketing agency for a beer company that ran a campaign around the Super Bowl. Cloud Foundry actually enabled them to deploy an application on AWS and then adjust its capacity based on the load. They were leveraging the elasticity of the cloud back in the ‘08–‘09 timeframe. SpringSource eventually acquired Cloud Foundry, followed by VMware. It's the origin of the name of today's Cloud Foundry.

Later in the show, Chris explains what choreographed sagas are, reasons to leverage them, and how to measure their efficacy.

EPISODE LINKS

- [The microservices pattern language](https://microservices.io/)
- [Eventuate framework](https://eventuate.io/)
- [Book: The Art of Scalability](https://microservices.io/articles/scalecube.html)
- [Use **podcon19** to get 40% off Microservices Patterns by Chris Richardson](https://www.manning.com/books/microservices-patterns?a_aid=microservices-patterns-chris&a_bid=2d6d8a4d)
- [Join the Confluent Community Slack](https://cnfl.io/confluent-community-slack)
- [Learn more with Kafka tutorials, resources, and guides at Confluent Developer](https://developer.confluent.io/?utm_source=buzzsprout&utm_medium=podcast&utm_campaign=ch.episode-links_type.community_content.developer)
- [Live demo: Kafka streaming in 10 minutes on Confluent Cloud](https://www.confluent.io/online-talks/confluent-cloud-demo/)
- [Use **60PDCAST** to get an additional $60 of free Confluent Cloud usage](https://www.confluent.io/confluent-cloud/?utm_source=buzzsprout&utm_medium=podcast&utm_campaign=ch.oprah_type.community_content.cloud) ( [details](https://www.confluent.io/confluent-cloud-promo-disclaimer/?utm_source=buzzsprout&utm_medium=podcast&utm_campaign=ch.oprah_type.community_content.cloud))

**SEASON 2** Hosted by Tim Berglund, Adi Polak and Viktor Gamov

Produced and Edited by Noelle Gallagher, Peter Furia and Nurie Mohamed

Music by [Coastal Kites](https://soundcloud.com/coastalkites)

Artwork by Phil Vo

-  🎧 [Subscribe](https://confluent.buzzsprout.com/) to Confluent Developer wherever you listen to podcasts.
- ▶️ [Subscribe on YouTube](https://youtube.com/@confluentdeveloper?sub_confirmation=1), and hit the 🔔 to catch new episodes.
- 👍 If you enjoyed this, please leave us a rating.
- 🎧 Confluent also has a podcast for tech leaders: " [Life Is But A Stream](https://www.confluent.io/life-is-but-a-stream-show)" hosted by our friend, Joseph Morais.

Tim Berglund:

Chris Richardson is a bunch of things. He's the creator of the original Cloud Foundry. He's the maintainer of the microservices.io website. He's the author of "Microservices Patterns." He's a guy who talks about microservices in his own description. And he's also a friend of mine. I set out to talk to him today about sagas. But we cover lots more ground than that, on today's episode of Streaming Audio, a podcast about Kafka, Confluent, and the cloud.

Tim Berglund:

Hello, and welcome to another episode of Streaming Audio, I am, as I often say, your host, Tim Berglund. And I'm joined in the virtual studio today by Chris Richardson. Now, Chris Richardson is a longtime friend of mine. And he is, as we discussed before this recording a guy who talks about microservices and writes books about microservices, and even delivers workshops on microservices. That's kind of Chris. Chris, welcome to the show.

Chris Richardson:

Well thanks. It's good to be here.

Tim Berglund:

It is, I'm delighted to have you here. So as I often like to begin, just to get to know you a little bit better. You're the guy, we've discussed that, but how did you get to the place you are? What's the appropriately scoped story of how Chris Richardson became what he is now?

Chris Richardson:

Well, yeah, that's good. It's an interesting thing. It's like, where do I start? Right? I think in a way, like an overriding theme, for many, many years is, could be summed up by sort of this desire or interest or curiosity in how to build better software faster. And I felt like, if I even go way back, back to the late 80s, I was actually... I spent a number of years building Lisp systems. Right? Lisp, which to see does this funny language with all these parentheses, but it was actually an incredibly sophisticated language, it could let you build DSLs. And I feel like that's always influenced my thinking, right? And then more recent years like, say, 2005 2006, I sort of perceived POJOs as a, if anyone can remember those plain old Java objects, right? As a way of building better software faster. And I got interested in it. Now, it's probably 2004, I ultimately wrote the book POJOs In Action that came out in 2006.

Tim Berglund:

Which was really a reaction against EJBs. Right? I mean, that's \[crosstalk 00:02:55\].

Chris Richardson:

Yeah. Oh, my God, that was really something right? You could imagine, like a persistence framework that didn't support queries?

Tim Berglund:

EJBs are now this thing where you get old timer sitting around the fires saying, now let me tell you a story that will scare you kids.

Chris Richardson:

Yeah. But it's an example of how like, otherwise super intelligent community could create something so bad.

Tim Berglund:

And that in itself, boy, somebody needs to do some research and a documentary on that, that it was universally derided as, oh, this was terrible. And the people behind it are better at this than I am. And that's fascinating.

Chris Richardson:

Well, yeah, but it's not only them, it was like then I spent a whole bunch of time, coming up with patterns to mitigate the issues that you have with EJBs right? And someone wrote a book EJB Patterns. So we kind of took it and embraced it and built stuff to just deal with it, and it's just like, Oh, my God. But then I think it's a cautionary tale, right? Think what technologies are we using today that sort of fundamentally flawed in some way?

Tim Berglund:

And the answer is some.

Chris Richardson:

Yes.

Tim Berglund:

It is always some.

Chris Richardson:

Yeah. So what else happened? Oh, yeah, then I discovered cloud computing. That was super fascinating. I run well, it's sort of kind of inactive now, but I run the Oakland Java Users Group in way back in 2006. We had an evangelist come from Google talk about API's for checkout. Actually, a good friend of mine, Patrick Chanezon. And then the next month we had someone show up from AWS and we thought, oh, cool, APIs for buying books. And it was like, did I say AWS? I meant, yeah, Amazon, right? Buying books. It was like, no, it was actually APIs for provisioning servers, storage, S3 queuing, and it was like completely blew my mind. And I just got so excited about that but-

Tim Berglund:

S3. The first time you be held S3.

Chris Richardson:

Yes. But this notion that for, you could with an API call provision 20 servers and pay 10 cents per hour was like, wow, that is so cool.

Tim Berglund:

And the stories we told ourselves about how we would be able to scale applications. I think I've talked about this on the show before, but like, in that's like maybe 2007, when EC2 was released, or whenever it was. And you're explaining it to your friend who hasn't heard about it yet. You're like, "Yeah, so at Christmas, a retailer can just spin up a bunch of whole new instances. And then in January, turn them off, and you only pay for what you use." Which was true but, in terms of your narrative of your career of building better software faster, like you weren't building software, in 2006, in such a way that, that fantasy could be realized, and neither was I like nobody was.

Chris Richardson:

Well, see, so I mean, that led me to create in 2008, the original Cloud Foundry. actually, I had something else called Cloud tools, which might have had its genesis in 2007. And then in 2008, started working on Cloud Foundry. Actually, that was because of the financial crash, and which like, meant my consulting clients went away. So instead of being a zero revenue consulting company, it was like, cool, let's be a zero revenue product company instead. Right? But actually that catastrophe actually gave me the time to work on Cloud Foundry. And then one early success story was a digital marketing agency for, I think it was beer, that had a Super Bowl related campaign.

Chris Richardson:

And it sort of Cloud Foundry or cloud tools actually enabled them to deploy an application on AWS and then adjust its capacity based on the load, right? And I think they're running campaigns and stuff like that, and tied to games and so, they were actually leveraging the elasticity of the cloud back in the 08, 09 timeframe.

Tim Berglund:

Okay. Which was an early adopters, because most... Just thinking in terms of Java applications, they just weren't built to do that. That was a different thing. And the microservices, the direction of your career, tends to bend our architectures in a direction where they are more favorable to scaling with the cloud \[crosstalk 00:08:12\].

Chris Richardson:

Yeah. But you could always take, I mean, classic thing, right? You would take an enterprise Java application, and you'd run it behind a load balancer. What were the cool things back then? F5 was something. Physical-

Tim Berglund:

Which is just cool to say. I feel cool when I say that. \[crosstalk 00:08:28\] fighter jet or a race car or something.

Chris Richardson:

Yeah. Yeah. So there was always the, you could always scale out sort of horizontally, in some ways, but there was obviously things like you might have session state requiring sticky sort of routing and stuff like that. But it was the kind of a standard pattern, but. So ended up creating Cloud Foundry, an intro, the original one, that's where the name of today's Cloud Foundry comes from. Because they got acquired by SpringSource, and VMware and so on. And Cloud Foundry was a monolith. Which is, but it was like... Because-

Tim Berglund:

\[inaudible 00:09:15\] already there.

Chris Richardson:

Yeah. Well, it was like, well, of course you... So I think so many applications, right? On day one, you type make the code, CD, and then you just start typing away, right? And so that was the architecture. But one of the interesting things is unlike a traditional enterprise application, which is like gooey business logic, accessing a database, Cloud Foundry had a whole set of rich functionality, like obviously it had a UI, but then it was monitoring EC2 instances, and then it was provisioning them and logging SSH into them and running configuration scripts, and so on.

Chris Richardson:

So there was all this sort of complex functionality that was packaged bundled up inside the monolith, that actually introduced some real kind of complexities. In particular, the monitoring part was actually just almost embarrassing to say, a stateful singleton. So we couldn't even run multiple instances of it. Don't tell anyone that, by the way, it's kind of awkward.

Tim Berglund:

\[inaudible 00:10:31\] to that with a loud beep.

Chris Richardson:

Yeah, no, I think in the preface to my Microservices Patterns book, I might have admitted to this right? But anyway, but it was like, Okay, that was the best we could do. And we just scaled up by running on a bigger EC2 instance. But then a few years later, to sort of bring this story to an end, I read this book called The Art of Scalability, which was written by some architects, I think, from eBay, PayPal. And in the model in the book, they had this scale cube concept. It was three dimensional model of scalability. On one axis, the X axis was just run a bunch of nodes behind a load balancer, right? So that was clearly one kind of the traditional sort of clustering, scaling, right? And that was good for load and availability. But then they had Y axis scaling, which was functional decomposition.

Chris Richardson:

So in other words, break an application apart by function into many applications. And when I read that, it was like, wow, that's really cool. Let's break some... Just apply functional decomposition. Such an obvious idea, right? Little did I know that when I was like, discovering this concept, AWS or amazon.com, rather, had already migrated to this style of functionally decomposed architecture back in 2002. And eBay had done that in like 2008. But it was like took a while for me to figure that out. And then I look back, and I thought, I had Cloud Foundry, right? Which is actually a relative... It was only developed by basically well by two of us. So tiny team, but it had all this functionality packaged together. Had we implemented it using micro... Well what ultimately became known as a microservice architecture, it would have solved a whole lot of these sort of development and operational issues. Right?

Chris Richardson:

So the fact that one component was a stateful singleton would not have prevented the rest of the application from scaling, for example. And then the fact that we could iterate on the UI without redeploying the component that was statefully provisioning EC2 instances, for example. So there's all kinds of, it was just like, wow, that's really cool. I mean, by that time, I'd moved on, but I got super excited and in terms of what this architecture had to offer. And then to complete the story, I then got ended up by this point, I was working at VMware. Because they had acquired SpringSource, who'd acquired my startup, and then I got the opportunity to be in the developer relations group that did Cloud Foundry developer relations

Tim Berglund:

Outstanding career move right there.

Chris Richardson:

Yeah. But it was like, but this was the new Cloud Foundry, not my Cloud Foundry.

Tim Berglund:

For those who don't know what it is, it just a rearchitected, rebuilt, rebranded-

Chris Richardson:

Oh, yeah, completely. It was done by a totally different group that ultimately adopted the name. So that's the only connection between what I did and the today's Cloud Foundry is, it's purely the name.

Tim Berglund:

Got you.

Chris Richardson:

Obviously, they were both passes but that's where \[inaudible 00:14:27\].

Tim Berglund:

Yes.

Chris Richardson:

And this was like 2012. And part of my role there was really to think about things like what we would now call the microservice architecture, and I just started... I think I gave my first talk on it back in April 2012. We have a name-

Tim Berglund:

\[crosstalk 00:14:51\] using the word... Okay, so you weren't saying microservices.

Chris Richardson:

No. I think the titles were like tile talk titles, decomposing for deployability and scalability or testability. And I had this great phrase, modular polyglot architectures. Just rolls off the tongue.

Tim Berglund:

It does. I want to say it over and over again. But it's polyglot, because you can build the services. And in 2012, saying polyglot was a good way to get your talk accepted.

Chris Richardson:

Yeah. That never caught on. Though, interestingly, I think the term microservices might have already been coined, but just not widely known. At that time, I think it was like Fred George, and some other folks at some event somewhere kind of came up with that term. And but then it was popularized by Martin Fowler. And when he wrote a blog post, right? Back in 2014.

Tim Berglund:

And that's when we had the actual term.

Chris Richardson:

Yeah. Which then... It's funny, you can have a concept floating around. But you need a name so that, once the name has been established, then kind of why I mean, some things slightly cynical, but like marketing efforts, kick into gear at that point.

Tim Berglund:

They do and there's a cynical side of that. But there's something powerful about giving a name to an idea that it seems in \[inaudible 00:16:30\] at one point, and you're just getting it worked out. And everybody is kind of struggling together to make sense of this thing and then it gets a name and it's real and\[crosstalk 00:16:41\]. Not just market-

Chris Richardson:

It's been named becomes a shorthand for a whole kind of graph of concepts at that point. And it's easily communicated and discussed. So before that, it was like, yeah, Amazon, they had a bunch of services and then eBay had a bunch of services, and it was like... But then if you say, well, they both adopted microservices, like retro actively it kind of identifies some commonalities at that point.

Tim Berglund:

Yes. So there's been... I kind of want to ask your opinion of this. And I mean, because you're the guy who talks about microservices and writes about them and was sort of there from the beginning. There's been an evolution of let me put that in a different way. There's a basic problem that you introduce, when you split a program into lots of little programs, right? You get access to a bunch of good things. You mentioned scalability and deployability and I think those are the biggies and also the ability to manage complexity-

Chris Richardson:

Yeah. By the way, I would say that, to me, the primary motivation of the microservice architecture, is that, it's a loosely coupled architecture where the pieces are independently deployable. And that enables you to have a loosely coupled set of teams, right? You sort of apply Conway's Law here. That enables those teams to develop and test and deploy their code independently. As a \[posal 00:18:21\] to contributing to the same monolithic code base, which can create bottlenecks. And I think, honestly believe that the reason companies like Amazon adopted this was not because they were operating at extremely high scale, but it was because they would need it to compete and deliver software very rapidly, frequently and reliably. And my service oriented architecture enables them to do that. But yeah and then you get some other benefits like it can be more scalable as well.

Tim Berglund:

Right. But fair point there, and just in the event that you don't know, if you're listening, and maybe like you're new in this business, you've heard people talk about this Conway's Law thing, and they just say it like everybody knows what it is, but you don't know it. Conway's Law is that, software architectures end up modeling the organizational architectures that produce them. So you'll see your company's org chart reflected in your software architecture. And it's rightfully called a law. This is a pattern that emerges and it's not a law, like the strong force is or something like that. But it's pretty much always true.

Tim Berglund:

So, this is a way of saying given that Conway's Law is a fact, how can we build and deploy our software in a way that doesn't make that a constraint? Like, go ahead, Conway, do your thing, it's fine, but we've got this way of building and deploying software that lets that work itself out and still lets us evolve a large scale application quickly.

Chris Richardson:

Yeah.

Tim Berglund:

Okay, so good things happen when you split your monolith up into pieces. Bad things also happen, like what were calls between modules using stack hardware on a processor and executing as fast as anything can become network calls, which are slow and unreliable. And that's bad. And there's been this history of various ways of resolving that problem. Now, I want to lay out the history as I normally tell it, and I want you to give you a chance to react to that. \[crosstalk 00:20:46\] if you think I'm right. But it seems like at first, we had a monolith and there was a database, it was a an application built around a relational database. Now, we want to build a bunch of little applications, so the initial experiments were with just sharing data in tables. Because hey, the database is there, and we need it anyway, and this seems easy. And generally, that didn't go too well and that's like, universally derided now. Is that fair so far?

Chris Richardson:

Well, yes. So one of the key attributes of the microservice architecture is that, going back to the Conway's Law thing, is one. Is that, services need to be loosely coupled. So there's actually at least two different dimensions to that. So one is from, which is Conway's Law relevant, is from a design time perspective, changes to one service should not require other services to change in lockstep. And then there's also runtime coupling, which is where service A can't respond to a request until it receives a response to a request that it made to service B. Which means that both services have, if you're using synchronous communication, that means that both services need to be available, in order to handle a request. Which given the availability is basically the product of the availabilities of the services that are invoked, you can easily run the risk if you've got lots of chains of cause of having a lower availability.

Chris Richardson:

So anyway, that's runtime coupling, that's a different thing that we will get back to. So talking about design time coupling, if you share a database, like literally, was share database tables, then you're introducing design time coupling. Because, imagine the order service was accessing the customer table that belong to the customer service. Well, if someone on the customer team wanted to change that table, they incompatible in some way, they would have to go coordinate that change with the border service team. And how things tend to get out of hand inside an organization, right? Where if it's freely accessing tables, suddenly you find there's 20 other teams. And then there's an application running somewhere and no one knows who owns it and it's a path that can lead you in the direction of not easily being able to make changes, which given the whole point here is so it's design time coupling. So given the whole point here is to have loosely coupled tables, there's a significant downside to sharing database tables.

Tim Berglund:

Yes. I like the division into design time and runtime coupling, that's extremely helpful.

Chris Richardson:

And by the way, there's also a runtime coupling perspective on this as well. Right? Because if two services are sharing the same piece of infrastructure, then there is the possibility of, should we say, interference, right? Service A could overload the infrastructure and sort of in a sense, do a denial of service attack on service B, right?

Tim Berglund:

Sure. Whether it means to or not.

Chris Richardson:

Yeah, or just more benignly, the ordered service could claim a lock on the customer table, which may prevents the customer service from handling requests. Right? So interestingly, you should be very wary of sharing infrastructure when sort of availability is important.

Tim Berglund:

Right, right. You're always going to have to share something but, you need to do it cautiously.

Chris Richardson:

Yes.

Tim Berglund:

So we went from coupling into the database to coupling through synchronous calls. And you talked a little bit about that, that there's runtime coupling there. And there's still design time coupling. I mean, you have to agree on the schema, as it were, if you're going to be passing objects back and forth. It's probably possible to do that with less, design type coupling.

Chris Richardson:

Well one hopes that... So Yeah. So I mean, that gets into API design. And obviously, if service A is consuming the API of service B, then there is coupling, right? The dependency implies some kind of coupling. But there that gets into the classic issues of like, kind of modularity and encapsulation, where one would hope that the API of a service hides a lot of the details, right? So the idea is that, you have all of these design decisions, and those decisions that are exposed through the API, are difficult to change, because it requires coordination with other teams. And those that are hidden behind the API, are easier to change because no coordination is required. Right?

Tim Berglund:

Right.

Chris Richardson:

And so I like to use, I mean, there's a bunch of metaphors, but like one of them is the iceberg principle. Where you have a relatively small stable API, that encapsulates a tremendous amount of functionality. So I mean, like the classic example of that, I mean, you could like examples of APIs provided by Stripe or by Twilio, right? So Twilio, I mean, sort of simplifying here, but Twilio, there's basically an operation send message and it's like from phone number to phone number, hit message to send. Incredibly simple API, but presumably behind that is a massive amount of complexity to actually get your text message delivered across multiple carriers and-

Tim Berglund:

Dynamic complexity too that software is changing even as the API does not.

Chris Richardson:

Yeah, exactly. So yeah. And same with stripe, right? Like, charged this credit card and presumably behind that is a heck of a lot of functionality that's evolving over time, right?

Tim Berglund:

Right. And that principle of API design and well of encapsulation of trying to hide the things that you don't need to expose behind some API that can remain constant is true. Probably a lot of us aren't as good at API design as we ought to be. And are APIs still evolved? That's a thing that we should acknowledge. But that runtime API coupling really seems to apply to synchronous, microservices that call each other over some RPC interface of some kind, and sort of the Kafka flavor of the asynchronous reactive microservices. And I with the way I started this question or this series of questions, kind of like a saga of questions. That's foreshadowing. You've got, okay, microservices are a thing. Let's use the database. Wait, that sucked. Okay, now let's have them call each other with HTTP or REST or GRPC or whatever. That's a little better, but there's still this synchronous coupling that we don't like. And now we seem to have landed emerging consensus, most people probably seem to be going in the direction of microservices coupled through some kind of event log like Kafka.

Chris Richardson:

Yeah.

Tim Berglund:

\[crosstalk 00:29:32\] exaggerate?

Chris Richardson:

Well, no. I mean, I guess, use slightly different terms, right? So yeah, so sort of the fundamental problem is, well, the content of the situation is, in order for services to be loosely coupled, both from a design time runtime perspective, each service should have its own database. And then those services should only communicate via APIs. So that sounds great, except that you can have operations that need to span services. And so one type of operation is a command which creates or changes data. And that needs to do updates in multiple services. And then you can have queries that retrieve data and they effectively need to do the equivalent of a join across multiple services. Except that because the databases are encapsulated, you can't write select star from list of table names, right?

Chris Richardson:

So you have this class of what I call distributed data management problems, both to do with updates and to do with queries. And there's sort of two separate but to slightly but sort of related problems. And the solution to those problems, is easiest to think of in terms of asynchronous messaging. And I use that term carefully, I'm not saying events, because events are a particular subtype of message and some solutions to distributed data management problems rely on other types of messages.

Tim Berglund:

Expand on that a little bit.

Chris Richardson:

Sure. I was hoping you would ask that. So yeah. So classic... I use this example all the time. So imagine, like, you've got two services, order service that manages orders, customer service that manages information about customers, including their credit limit, and their available credit. So in order to create an order, two things have to happen. An order obviously has to be created but, the customer's available credit must be reduced by the amount of the order total, subject to the constraint that the order that they're available credit can never go below zero, right? So you kind of got to check that there's enough available credit decremented and... So a classic approach would be to use a distributed transaction two phase commit. But that's basically synchronous communication, which, in order for the transaction to commit, both parties have to be available and so that's coupling that we want to avoid.

Chris Richardson:

And so the solution is to use what's known as a SAGA. And that's a sequence of local transactions that are executed in the participating services. And once the whole sequence has been executed, then you've achieved the desired end state. So like to create an order. That so as you create the complete order process creation step would consist of say step one, that creates the order in a pending state, so it's provisionally created. And the HTTP post which does that would actually return immediately, which basically tells the client, "Hey, I've received your request, here's the order ID, I'm working on it. check back later to see what the outcome is."

Tim Berglund:

Which is probably something like what happens when I order something on amazon.com. I mean, a page loads and I get an email, right?

Chris Richardson:

Yeah, you can kind of, yeah. I mean, you can sort of have these optimistic scenarios where you create it and then recover afterwards. So yeah. So then step two is the reserve credit step in the customer service, which actually has two outcomes. It could either succeed because there's sufficient available credit or fail, because there was insufficient available credit. Actually, the third outcome is, the specified customer doesn't exist, right? And then the final step is, either... Finally, after that, either the order is approved if the credit reservation was successful, or the order was rejected or canceled, because either there was insufficient credit or the customer does not exist.

Chris Richardson:

So you break down what would otherwise be a distributed transaction into a series of steps. And then this is where you'll get excited because you haven't had a product that works that can help with this. A good way to coordinate the series of steps to actually make them happen, or specifically, how the order service triggers the credit reservation and how the customer service tells the order service the outcome, is to use asynchronous messaging. So \[crosstalk 00:35:39\].

Tim Berglund:

It sounds like there's Kafka there. So our services are now talking to, running on Kafka and when I \[crosstalk 00:35:49\] an order, I confirm it, I create an idea in my local database, and I produce a message to a topic that says, "Hey, look, I've got a new order."

Chris Richardson:

And Kafka is actually quite well suited to this for a number of reasons. One is that it provides at least ones and ordered delivery of messages. Which is kind... Things really have to arrive in the order that they were published, and there should be a guarantee that they will be delivered, right? Because there it gets too complicated otherwise. And then the other thing I find also very useful, is the fact that you can scale out consumers while preserving message ordering. So that's sort of the competing consumer pattern. And abstractly and more concretely as the Kafka consumer group mechanism. So Kafka's actually quite good. I would say that, I mean, one of the things I work on shameless plug is Eventuate IO. So eventuate.io, which is a distributed data management framework that solves these problems in a microservice architecture. And the very first message broker and sort of the primary message Broker in a way that it uses it is actually Kafka.

Tim Berglund:

Very nice.

Chris Richardson:

So yeah. But then in terms of messaging, we like to think of events, right? But there's been more general than that. So yes, so one way of doing it with messaging is to use events. So the order service says, "I've created an order." So that's an event. Right? The customer service has a message handler, event handler that receives that event, reserves credit, and then it publishes an event saying, "I've reserved credit or the credit reservation attempt failed." for one of some reasons. And then the order service receives that event. So that style of interaction is where services announce what they have done, right? That's event based, and that's known as a choreography based SAGA. But the other form of SAGAs are orchestration based SAGAs. And that's where there is a centralized orchestrator, that is telling the participants what to do.

Chris Richardson:

So you could think of as choreography as sort of passive aggressive style communication. Right? Whereas with orchestration, specifically the order service is being told to reserve credit. You actually, which sounds like request response, but it's sort of a different interaction style, it's request asynchronous response. So you send a command message to the order service, right? So there's actually a Kafka topic that receives these command messages, which are basically requests to do something. Right? An order is an announcement that something has been, an event is an announcement that has something has been done, these are requests to do something. And then the customer service, does it, and then puts a reply message indicating the outcome in the reply topic that is consumed by the order service.

Chris Richardson:

So you've got two different styles of messaging to choose from here. And there's various trade offs. So I like to think that, yes, rather than always thinking of microservices as being event driven, kind of the primary communication pattern is actually asynchronous messaging. And then in certain scenarios, you use events, and then in other scenarios, you use request async response.

Tim Berglund:

Got it, and that request async response. Flush that, I'm just curious. We're getting close to time, this is dangerous for me to ask this. But, and I don't know that it's even all that good of a question to end on. But, I just want to know more about it. So what are the circumstances under which you think that pattern applies the best?

Chris Richardson:

Well, it generally-

Tim Berglund:

Let me tell you why I'm asking. I always describe it as a smell. I always tell people that if you're doing that you're probably... I mean, there's synchronous things outside of your microservices architecture and you have to deal with them being synchronous. But if you're trying to be synchronous on the inside you probably don't have to-

Chris Richardson:

Yeah, this is still very asynchronous. When the order service sends a request to the customer service, it's not sitting there blocked, waiting for a reply to come back. It sends the request message, and then the reply is consumed by a message handler, just as if it was an event. So it's sort of, so don't get misled into thinking that just because it's request response, it's actually blocking in any way. It's all basically still just a bunch of message handlers, which ultimately, there's consumer Kafka client running under many layers below. So I'll give you an example of why. I mean, you think about one of the interesting things about choreography is that the customer service is listening for order events, and then reacting to them. Right?

Chris Richardson:

So it knows, "Oh, order created, I have to reserve credit. Order canceled, ah, I have to release the credit that was previously reserved for that order." Right? So if you think about it, the customer service has to be an expert on how events coming out of the order service impact, the available credit. Which is slightly smelly, right? It's actually another form of coupling in a way. Right? Whereas with orchestration, it's the order service that is telling the customer service to reserve credit, or release credit. So it's, arguably reverses the dependency and means that the customer service has a simpler API, right? Or it just reserve credit, police credit, right? And it doesn't have to know anything about the order service. So it's kind of like this strange thing where choreography is super simple, except for the fact that it can create the strange coupling, which I think is kind of undesirable. And so orchestration, even though it's more complex, it actually can result in a cleaner set of dependencies.

Tim Berglund:

Okay. Okay, so that's a consideration that would push you towards orchestration type \[crosstalk 00:43:55\].

Chris Richardson:

Also, choreography wrap if you've got like many participants, there's all these events bouncing back and forth. And it's very hard to kind of keep what's happening in your head. Whereas orchestration is do this, do that, do the next thing, do the thing after. It's very kind of just linear style of interaction. Plus, there's the orchestration code, which is code that is in one place. And if you want to know what is the SAGA do? Oh, just go look here. Versus choreography where the implementation is scattered around all these event handlers.

Tim Berglund:

And in the orchestration case. So there's... Final question, I know we're almost up against time.

Chris Richardson:

I know.

Tim Berglund:

I can't resist Chris. \[crosstalk 00:44:51\] in orchestration.

Chris Richardson:

Can we have a part two?

Tim Berglund:

I think we need a part two. So you're going to be back. But, in the orchestration case, coming from the perspective of somebody who wants to commit a transaction, right? And know that everything was happy. I don't get to do that here, and in the orchestration case, I can get to the end of the orchestration code and know that everything's happy. In the case of a choreographed SAGA, and there are reasons to want choreographed SAGAs. How do I know when it's done? So I'm this person who has been committing transactions, his whole life, and now you're telling me to do this crazy microservice thing. How do I know it worked?

Chris Richardson:

Yeah, well, it's sort of like the final step gets executed, and some event is published to say that just indicates it's done. Right.

Tim Berglund:

Got it. So we know it's done because the last thing happened. And you can monitor that last thing.

Chris Richardson:

Yeah. And so maybe in the order creation SAGA, the order service published an order approved event. So you now know that or an order rejected event. And at that point, you it's done.

Tim Berglund:

It just seems so simple.

Chris Richardson:

Yeah.

Tim Berglund:

Yeah. And that's a good thing. My guest today has been Chris Richardson, who will be back. Chris, thanks for being a part of streaming audio.

Chris Richardson:

Oh, you're welcome. It's been an interesting discussion. Thank you.

Tim Berglund:

Hey, you know what you get for listening to the end? Some free Confluent Cloud. Use the promo code 60PDCAST. That's 6-0-P-D-C-A-S-T to get an additional $60 of free Confluent Cloud usage. Be sure to activate it by December 31 2021 and use it within 90 days after activation. And any unused promo value on the expiration date will be forfeit and there are a limited number of codes available so don't miss out. Anyway, as always, I hope this podcast was helpful to you. If you want to discuss it or ask a question, you can always reach out to me @tlberglund on Twitter. That's T-L-B-E-R-G-L-U-N-D. Or you can leave a comment on a YouTube video or reach out in our community slack. There's a slack signup link in the show notes if you'd like to join. And while you're at it, please subscribe to our YouTube channel and to this podcast wherever find podcasts are sold. And if you subscribe to Apple podcasts, be sure to leave us a review there. that helps other people discover us which we think is a good thing. So thanks for your support and we'll see you next time.

[![Head Shot](https://www.buzzsprout.com/rails/active_storage/representations/redirect/eyJfcmFpbHMiOnsiZGF0YSI6MTUzNDYzMzUwLCJwdXIiOiJibG9iX2lkIn19--ea0f3a1686a5a867071e1720e4b3646ee258d62d/eyJfcmFpbHMiOnsiZGF0YSI6eyJmb3JtYXQiOiJqcGciLCJyZXNpemVfdG9fbGltaXQiOlsxMDgwLDE0NDAseyJjcm9wIjoiY2VudHJlIn1dLCJzYXZlciI6eyJxdWFsaXR5Ijo2MH0sImNvbG91cnNwYWNlIjoic3JnYiJ9LCJwdXIiOiJ2YXJpYXRpb24ifX0=--355526e0ad8d751b2d7838e573060e1c9e6162cc/tim-host.jpg)\\
\\
**Tim Berglund** Host](https://www.buzzsprout.com/186154/contributors/118637-tim-berglund) [![Head Shot](https://www.buzzsprout.com/rails/active_storage/representations/redirect/eyJfcmFpbHMiOnsiZGF0YSI6MTUzNDYzNDcxLCJwdXIiOiJibG9iX2lkIn19--2ed1a456574d636d7b325b73556ee271ef838aff/eyJfcmFpbHMiOnsiZGF0YSI6eyJmb3JtYXQiOiJqcGciLCJyZXNpemVfdG9fbGltaXQiOlsxMDgwLDE0NDAseyJjcm9wIjoiY2VudHJlIn1dLCJzYXZlciI6eyJxdWFsaXR5Ijo2MH0sImNvbG91cnNwYWNlIjoic3JnYiJ9LCJwdXIiOiJ2YXJpYXRpb24ifX0=--355526e0ad8d751b2d7838e573060e1c9e6162cc/adi-host.jpg)\\
\\
**Adi Polak** Co-host](https://www.buzzsprout.com/186154/contributors/118638-adi-polak) [![Head Shot](https://www.buzzsprout.com/rails/active_storage/representations/redirect/eyJfcmFpbHMiOnsiZGF0YSI6MTUzNDYzMzc2LCJwdXIiOiJibG9iX2lkIn19--3e9b8dcce1c8bf840b81b11d6ab4dc3af29d2c39/eyJfcmFpbHMiOnsiZGF0YSI6eyJmb3JtYXQiOiJqcGciLCJyZXNpemVfdG9fbGltaXQiOlsxMDgwLDE0NDAseyJjcm9wIjoiY2VudHJlIn1dLCJzYXZlciI6eyJxdWFsaXR5Ijo2MH0sImNvbG91cnNwYWNlIjoic3JnYiJ9LCJwdXIiOiJ2YXJpYXRpb24ifX0=--355526e0ad8d751b2d7838e573060e1c9e6162cc/viktor-host.jpg)\\
\\
**Viktor Gamov** Co-host](https://www.buzzsprout.com/186154/contributors/118639-viktor-gamov)

- Transcript
- Notes

### Tim Berglund:

Chris Richardson is a bunch of things. He's the creator of the original Cloud Foundry. He's the maintainer of the microservices.io website. He's the author of "Microservices Patterns." He's a guy who talks about microservices in his own description. And he's also a friend of mine. I set out to talk to him today about sagas. But we cover lots more ground than that, on today's episode of Streaming Audio, a podcast about Kafka, Confluent, and the cloud.

### Tim Berglund:

Hello, and welcome to another episode of Streaming Audio, I am, as I often say, your host, Tim Berglund. And I'm joined in the virtual studio today by Chris Richardson. Now, Chris Richardson is a longtime friend of mine. And he is, as we discussed before this recording a guy who talks about microservices and writes books about microservices, and even delivers workshops on microservices. That's kind of Chris. Chris, welcome to the show.

### Chris Richardson:

Well thanks. It's good to be here.

### Tim Berglund:

It is, I'm delighted to have you here. So as I often like to begin, just to get to know you a little bit better. You're the guy, we've discussed that, but how did you get to the place you are? What's the appropriately scoped story of how Chris Richardson became what he is now?

### Chris Richardson:

Well, yeah, that's good. It's an interesting thing. It's like, where do I start? Right? I think in a way, like an overriding theme, for many, many years is, could be summed up by sort of this desire or interest or curiosity in how to build better software faster. And I felt like, if I even go way back, back to the late 80s, I was actually... I spent a number of years building Lisp systems. Right? Lisp, which to see does this funny language with all these parentheses, but it was actually an incredibly sophisticated language, it could let you build DSLs. And I feel like that's always influenced my thinking, right? And then more recent years like, say, 2005 2006, I sort of perceived POJOs as a, if anyone can remember those plain old Java objects, right? As a way of building better software faster. And I got interested in it. Now, it's probably 2004, I ultimately wrote the book POJOs In Action that came out in 2006.

### Tim Berglund:

Which was really a reaction against EJBs. Right? I mean, that's \[crosstalk 00:02:55\].

### Chris Richardson:

Yeah. Oh, my God, that was really something right? You could imagine, like a persistence framework that didn't support queries?

### Tim Berglund:

EJBs are now this thing where you get old timer sitting around the fires saying, now let me tell you a story that will scare you kids.

### Chris Richardson:

Yeah. But it's an example of how like, otherwise super intelligent community could create something so bad.

### Tim Berglund:

And that in itself, boy, somebody needs to do some research and a documentary on that, that it was universally derided as, oh, this was terrible. And the people behind it are better at this than I am. And that's fascinating.

### Chris Richardson:

Well, yeah, but it's not only them, it was like then I spent a whole bunch of time, coming up with patterns to mitigate the issues that you have with EJBs right? And someone wrote a book EJB Patterns. So we kind of took it and embraced it and built stuff to just deal with it, and it's just like, Oh, my God. But then I think it's a cautionary tale, right? Think what technologies are we using today that sort of fundamentally flawed in some way?

### Tim Berglund:

And the answer is some.

### Chris Richardson:

Yes.

### Tim Berglund:

It is always some.

### Chris Richardson:

Yeah. So what else happened? Oh, yeah, then I discovered cloud computing. That was super fascinating. I run well, it's sort of kind of inactive now, but I run the Oakland Java Users Group in way back in 2006. We had an evangelist come from Google talk about API's for checkout. Actually, a good friend of mine, Patrick Chanezon. And then the next month we had someone show up from AWS and we thought, oh, cool, APIs for buying books. And it was like, did I say AWS? I meant, yeah, Amazon, right? Buying books. It was like, no, it was actually APIs for provisioning servers, storage, S3 queuing, and it was like completely blew my mind. And I just got so excited about that but-

### Tim Berglund:

S3. The first time you be held S3.

### Chris Richardson:

Yes. But this notion that for, you could with an API call provision 20 servers and pay 10 cents per hour was like, wow, that is so cool.

### Tim Berglund:

And the stories we told ourselves about how we would be able to scale applications. I think I've talked about this on the show before, but like, in that's like maybe 2007, when EC2 was released, or whenever it was. And you're explaining it to your friend who hasn't heard about it yet. You're like, "Yeah, so at Christmas, a retailer can just spin up a bunch of whole new instances. And then in January, turn them off, and you only pay for what you use." Which was true but, in terms of your narrative of your career of building better software faster, like you weren't building software, in 2006, in such a way that, that fantasy could be realized, and neither was I like nobody was.

### Chris Richardson:

Well, see, so I mean, that led me to create in 2008, the original Cloud Foundry. actually, I had something else called Cloud tools, which might have had its genesis in 2007. And then in 2008, started working on Cloud Foundry. Actually, that was because of the financial crash, and which like, meant my consulting clients went away. So instead of being a zero revenue consulting company, it was like, cool, let's be a zero revenue product company instead. Right? But actually that catastrophe actually gave me the time to work on Cloud Foundry. And then one early success story was a digital marketing agency for, I think it was beer, that had a Super Bowl related campaign.

### Chris Richardson:

And it sort of Cloud Foundry or cloud tools actually enabled them to deploy an application on AWS and then adjust its capacity based on the load, right? And I think they're running campaigns and stuff like that, and tied to games and so, they were actually leveraging the elasticity of the cloud back in the 08, 09 timeframe.

### Tim Berglund:

Okay. Which was an early adopters, because most... Just thinking in terms of Java applications, they just weren't built to do that. That was a different thing. And the microservices, the direction of your career, tends to bend our architectures in a direction where they are more favorable to scaling with the cloud \[crosstalk 00:08:12\].

### Chris Richardson:

Yeah. But you could always take, I mean, classic thing, right? You would take an enterprise Java application, and you'd run it behind a load balancer. What were the cool things back then? F5 was something. Physical-

### Tim Berglund:

Which is just cool to say. I feel cool when I say that. \[crosstalk 00:08:28\] fighter jet or a race car or something.

### Chris Richardson:

Yeah. Yeah. So there was always the, you could always scale out sort of horizontally, in some ways, but there was obviously things like you might have session state requiring sticky sort of routing and stuff like that. But it was the kind of a standard pattern, but. So ended up creating Cloud Foundry, an intro, the original one, that's where the name of today's Cloud Foundry comes from. Because they got acquired by SpringSource, and VMware and so on. And Cloud Foundry was a monolith. Which is, but it was like... Because-

### Tim Berglund:

\[inaudible 00:09:15\] already there.

### Chris Richardson:

Yeah. Well, it was like, well, of course you... So I think so many applications, right? On day one, you type make the code, CD, and then you just start typing away, right? And so that was the architecture. But one of the interesting things is unlike a traditional enterprise application, which is like gooey business logic, accessing a database, Cloud Foundry had a whole set of rich functionality, like obviously it had a UI, but then it was monitoring EC2 instances, and then it was provisioning them and logging SSH into them and running configuration scripts, and so on.

### Chris Richardson:

So there was all this sort of complex functionality that was packaged bundled up inside the monolith, that actually introduced some real kind of complexities. In particular, the monitoring part was actually just almost embarrassing to say, a stateful singleton. So we couldn't even run multiple instances of it. Don't tell anyone that, by the way, it's kind of awkward.

### Tim Berglund:

\[inaudible 00:10:31\] to that with a loud beep.

### Chris Richardson:

Yeah, no, I think in the preface to my Microservices Patterns book, I might have admitted to this right? But anyway, but it was like, Okay, that was the best we could do. And we just scaled up by running on a bigger EC2 instance. But then a few years later, to sort of bring this story to an end, I read this book called The Art of Scalability, which was written by some architects, I think, from eBay, PayPal. And in the model in the book, they had this scale cube concept. It was three dimensional model of scalability. On one axis, the X axis was just run a bunch of nodes behind a load balancer, right? So that was clearly one kind of the traditional sort of clustering, scaling, right? And that was good for load and availability. But then they had Y axis scaling, which was functional decomposition.

### Chris Richardson:

So in other words, break an application apart by function into many applications. And when I read that, it was like, wow, that's really cool. Let's break some... Just apply functional decomposition. Such an obvious idea, right? Little did I know that when I was like, discovering this concept, AWS or amazon.com, rather, had already migrated to this style of functionally decomposed architecture back in 2002. And eBay had done that in like 2008. But it was like took a while for me to figure that out. And then I look back, and I thought, I had Cloud Foundry, right? Which is actually a relative... It was only developed by basically well by two of us. So tiny team, but it had all this functionality packaged together. Had we implemented it using micro... Well what ultimately became known as a microservice architecture, it would have solved a whole lot of these sort of development and operational issues. Right?

### Chris Richardson:

So the fact that one component was a stateful singleton would not have prevented the rest of the application from scaling, for example. And then the fact that we could iterate on the UI without redeploying the component that was statefully provisioning EC2 instances, for example. So there's all kinds of, it was just like, wow, that's really cool. I mean, by that time, I'd moved on, but I got super excited and in terms of what this architecture had to offer. And then to complete the story, I then got ended up by this point, I was working at VMware. Because they had acquired SpringSource, who'd acquired my startup, and then I got the opportunity to be in the developer relations group that did Cloud Foundry developer relations

### Tim Berglund:

Outstanding career move right there.

### Chris Richardson:

Yeah. But it was like, but this was the new Cloud Foundry, not my Cloud Foundry.

### Tim Berglund:

For those who don't know what it is, it just a rearchitected, rebuilt, rebranded-

### Chris Richardson:

Oh, yeah, completely. It was done by a totally different group that ultimately adopted the name. So that's the only connection between what I did and the today's Cloud Foundry is, it's purely the name.

### Tim Berglund:

Got you.

### Chris Richardson:

Obviously, they were both passes but that's where \[inaudible 00:14:27\].

### Tim Berglund:

Yes.

### Chris Richardson:

And this was like 2012. And part of my role there was really to think about things like what we would now call the microservice architecture, and I just started... I think I gave my first talk on it back in April 2012. We have a name-

### Tim Berglund:

\[crosstalk 00:14:51\] using the word... Okay, so you weren't saying microservices.

### Chris Richardson:

No. I think the titles were like tile talk titles, decomposing for deployability and scalability or testability. And I had this great phrase, modular polyglot architectures. Just rolls off the tongue.

### Tim Berglund:

It does. I want to say it over and over again. But it's polyglot, because you can build the services. And in 2012, saying polyglot was a good way to get your talk accepted.

### Chris Richardson:

Yeah. That never caught on. Though, interestingly, I think the term microservices might have already been coined, but just not widely known. At that time, I think it was like Fred George, and some other folks at some event somewhere kind of came up with that term. And but then it was popularized by Martin Fowler. And when he wrote a blog post, right? Back in 2014.

### Tim Berglund:

And that's when we had the actual term.

### Chris Richardson:

Yeah. Which then... It's funny, you can have a concept floating around. But you need a name so that, once the name has been established, then kind of why I mean, some things slightly cynical, but like marketing efforts, kick into gear at that point.

### Tim Berglund:

They do and there's a cynical side of that. But there's something powerful about giving a name to an idea that it seems in \[inaudible 00:16:30\] at one point, and you're just getting it worked out. And everybody is kind of struggling together to make sense of this thing and then it gets a name and it's real and\[crosstalk 00:16:41\]. Not just market-

### Chris Richardson:

It's been named becomes a shorthand for a whole kind of graph of concepts at that point. And it's easily communicated and discussed. So before that, it was like, yeah, Amazon, they had a bunch of services and then eBay had a bunch of services, and it was like... But then if you say, well, they both adopted microservices, like retro actively it kind of identifies some commonalities at that point.

### Tim Berglund:

Yes. So there's been... I kind of want to ask your opinion of this. And I mean, because you're the guy who talks about microservices and writes about them and was sort of there from the beginning. There's been an evolution of let me put that in a different way. There's a basic problem that you introduce, when you split a program into lots of little programs, right? You get access to a bunch of good things. You mentioned scalability and deployability and I think those are the biggies and also the ability to manage complexity-

### Chris Richardson:

Yeah. By the way, I would say that, to me, the primary motivation of the microservice architecture, is that, it's a loosely coupled architecture where the pieces are independently deployable. And that enables you to have a loosely coupled set of teams, right? You sort of apply Conway's Law here. That enables those teams to develop and test and deploy their code independently. As a \[posal 00:18:21\] to contributing to the same monolithic code base, which can create bottlenecks. And I think, honestly believe that the reason companies like Amazon adopted this was not because they were operating at extremely high scale, but it was because they would need it to compete and deliver software very rapidly, frequently and reliably. And my service oriented architecture enables them to do that. But yeah and then you get some other benefits like it can be more scalable as well.

### Tim Berglund:

Right. But fair point there, and just in the event that you don't know, if you're listening, and maybe like you're new in this business, you've heard people talk about this Conway's Law thing, and they just say it like everybody knows what it is, but you don't know it. Conway's Law is that, software architectures end up modeling the organizational architectures that produce them. So you'll see your company's org chart reflected in your software architecture. And it's rightfully called a law. This is a pattern that emerges and it's not a law, like the strong force is or something like that. But it's pretty much always true.

### Tim Berglund:

So, this is a way of saying given that Conway's Law is a fact, how can we build and deploy our software in a way that doesn't make that a constraint? Like, go ahead, Conway, do your thing, it's fine, but we've got this way of building and deploying software that lets that work itself out and still lets us evolve a large scale application quickly.

### Chris Richardson:

Yeah.

### Tim Berglund:

Okay, so good things happen when you split your monolith up into pieces. Bad things also happen, like what were calls between modules using stack hardware on a processor and executing as fast as anything can become network calls, which are slow and unreliable. And that's bad. And there's been this history of various ways of resolving that problem. Now, I want to lay out the history as I normally tell it, and I want you to give you a chance to react to that. \[crosstalk 00:20:46\] if you think I'm right. But it seems like at first, we had a monolith and there was a database, it was a an application built around a relational database. Now, we want to build a bunch of little applications, so the initial experiments were with just sharing data in tables. Because hey, the database is there, and we need it anyway, and this seems easy. And generally, that didn't go too well and that's like, universally derided now. Is that fair so far?

### Chris Richardson:

Well, yes. So one of the key attributes of the microservice architecture is that, going back to the Conway's Law thing, is one. Is that, services need to be loosely coupled. So there's actually at least two different dimensions to that. So one is from, which is Conway's Law relevant, is from a design time perspective, changes to one service should not require other services to change in lockstep. And then there's also runtime coupling, which is where service A can't respond to a request until it receives a response to a request that it made to service B. Which means that both services have, if you're using synchronous communication, that means that both services need to be available, in order to handle a request. Which given the availability is basically the product of the availabilities of the services that are invoked, you can easily run the risk if you've got lots of chains of cause of having a lower availability.

### Chris Richardson:

So anyway, that's runtime coupling, that's a different thing that we will get back to. So talking about design time coupling, if you share a database, like literally, was share database tables, then you're introducing design time coupling. Because, imagine the order service was accessing the customer table that belong to the customer service. Well, if someone on the customer team wanted to change that table, they incompatible in some way, they would have to go coordinate that change with the border service team. And how things tend to get out of hand inside an organization, right? Where if it's freely accessing tables, suddenly you find there's 20 other teams. And then there's an application running somewhere and no one knows who owns it and it's a path that can lead you in the direction of not easily being able to make changes, which given the whole point here is so it's design time coupling. So given the whole point here is to have loosely coupled tables, there's a significant downside to sharing database tables.

### Tim Berglund:

Yes. I like the division into design time and runtime coupling, that's extremely helpful.

### Chris Richardson:

And by the way, there's also a runtime coupling perspective on this as well. Right? Because if two services are sharing the same piece of infrastructure, then there is the possibility of, should we say, interference, right? Service A could overload the infrastructure and sort of in a sense, do a denial of service attack on service B, right?

### Tim Berglund:

Sure. Whether it means to or not.

### Chris Richardson:

Yeah, or just more benignly, the ordered service could claim a lock on the customer table, which may prevents the customer service from handling requests. Right? So interestingly, you should be very wary of sharing infrastructure when sort of availability is important.

### Tim Berglund:

Right, right. You're always going to have to share something but, you need to do it cautiously.

### Chris Richardson:

Yes.

### Tim Berglund:

So we went from coupling into the database to coupling through synchronous calls. And you talked a little bit about that, that there's runtime coupling there. And there's still design time coupling. I mean, you have to agree on the schema, as it were, if you're going to be passing objects back and forth. It's probably possible to do that with less, design type coupling.

### Chris Richardson:

Well one hopes that... So Yeah. So I mean, that gets into API design. And obviously, if service A is consuming the API of service B, then there is coupling, right? The dependency implies some kind of coupling. But there that gets into the classic issues of like, kind of modularity and encapsulation, where one would hope that the API of a service hides a lot of the details, right? So the idea is that, you have all of these design decisions, and those decisions that are exposed through the API, are difficult to change, because it requires coordination with other teams. And those that are hidden behind the API, are easier to change because no coordination is required. Right?

### Tim Berglund:

Right.

### Chris Richardson:

And so I like to use, I mean, there's a bunch of metaphors, but like one of them is the iceberg principle. Where you have a relatively small stable API, that encapsulates a tremendous amount of functionality. So I mean, like the classic example of that, I mean, you could like examples of APIs provided by Stripe or by Twilio, right? So Twilio, I mean, sort of simplifying here, but Twilio, there's basically an operation send message and it's like from phone number to phone number, hit message to send. Incredibly simple API, but presumably behind that is a massive amount of complexity to actually get your text message delivered across multiple carriers and-

### Tim Berglund:

Dynamic complexity too that software is changing even as the API does not.

### Chris Richardson:

Yeah, exactly. So yeah. And same with stripe, right? Like, charged this credit card and presumably behind that is a heck of a lot of functionality that's evolving over time, right?

### Tim Berglund:

Right. And that principle of API design and well of encapsulation of trying to hide the things that you don't need to expose behind some API that can remain constant is true. Probably a lot of us aren't as good at API design as we ought to be. And are APIs still evolved? That's a thing that we should acknowledge. But that runtime API coupling really seems to apply to synchronous, microservices that call each other over some RPC interface of some kind, and sort of the Kafka flavor of the asynchronous reactive microservices. And I with the way I started this question or this series of questions, kind of like a saga of questions. That's foreshadowing. You've got, okay, microservices are a thing. Let's use the database. Wait, that sucked. Okay, now let's have them call each other with HTTP or REST or GRPC or whatever. That's a little better, but there's still this synchronous coupling that we don't like. And now we seem to have landed emerging consensus, most people probably seem to be going in the direction of microservices coupled through some kind of event log like Kafka.

### Chris Richardson:

Yeah.

### Tim Berglund:

\[crosstalk 00:29:32\] exaggerate?

### Chris Richardson:

Well, no. I mean, I guess, use slightly different terms, right? So yeah, so sort of the fundamental problem is, well, the content of the situation is, in order for services to be loosely coupled, both from a design time runtime perspective, each service should have its own database. And then those services should only communicate via APIs. So that sounds great, except that you can have operations that need to span services. And so one type of operation is a command which creates or changes data. And that needs to do updates in multiple services. And then you can have queries that retrieve data and they effectively need to do the equivalent of a join across multiple services. Except that because the databases are encapsulated, you can't write select star from list of table names, right?

### Chris Richardson:

So you have this class of what I call distributed data management problems, both to do with updates and to do with queries. And there's sort of two separate but to slightly but sort of related problems. And the solution to those problems, is easiest to think of in terms of asynchronous messaging. And I use that term carefully, I'm not saying events, because events are a particular subtype of message and some solutions to distributed data management problems rely on other types of messages.

### Tim Berglund:

Expand on that a little bit.

### Chris Richardson:

Sure. I was hoping you would ask that. So yeah. So classic... I use this example all the time. So imagine, like, you've got two services, order service that manages orders, customer service that manages information about customers, including their credit limit, and their available credit. So in order to create an order, two things have to happen. An order obviously has to be created but, the customer's available credit must be reduced by the amount of the order total, subject to the constraint that the order that they're available credit can never go below zero, right? So you kind of got to check that there's enough available credit decremented and... So a classic approach would be to use a distributed transaction two phase commit. But that's basically synchronous communication, which, in order for the transaction to commit, both parties have to be available and so that's coupling that we want to avoid.

### Chris Richardson:

And so the solution is to use what's known as a SAGA. And that's a sequence of local transactions that are executed in the participating services. And once the whole sequence has been executed, then you've achieved the desired end state. So like to create an order. That so as you create the complete order process creation step would consist of say step one, that creates the order in a pending state, so it's provisionally created. And the HTTP post which does that would actually return immediately, which basically tells the client, "Hey, I've received your request, here's the order ID, I'm working on it. check back later to see what the outcome is."

### Tim Berglund:

Which is probably something like what happens when I order something on amazon.com. I mean, a page loads and I get an email, right?

### Chris Richardson:

Yeah, you can kind of, yeah. I mean, you can sort of have these optimistic scenarios where you create it and then recover afterwards. So yeah. So then step two is the reserve credit step in the customer service, which actually has two outcomes. It could either succeed because there's sufficient available credit or fail, because there was insufficient available credit. Actually, the third outcome is, the specified customer doesn't exist, right? And then the final step is, either... Finally, after that, either the order is approved if the credit reservation was successful, or the order was rejected or canceled, because either there was insufficient credit or the customer does not exist.

### Chris Richardson:

So you break down what would otherwise be a distributed transaction into a series of steps. And then this is where you'll get excited because you haven't had a product that works that can help with this. A good way to coordinate the series of steps to actually make them happen, or specifically, how the order service triggers the credit reservation and how the customer service tells the order service the outcome, is to use asynchronous messaging. So \[crosstalk 00:35:39\].

### Tim Berglund:

It sounds like there's Kafka there. So our services are now talking to, running on Kafka and when I \[crosstalk 00:35:49\] an order, I confirm it, I create an idea in my local database, and I produce a message to a topic that says, "Hey, look, I've got a new order."

### Chris Richardson:

And Kafka is actually quite well suited to this for a number of reasons. One is that it provides at least ones and ordered delivery of messages. Which is kind... Things really have to arrive in the order that they were published, and there should be a guarantee that they will be delivered, right? Because there it gets too complicated otherwise. And then the other thing I find also very useful, is the fact that you can scale out consumers while preserving message ordering. So that's sort of the competing consumer pattern. And abstractly and more concretely as the Kafka consumer group mechanism. So Kafka's actually quite good. I would say that, I mean, one of the things I work on shameless plug is Eventuate IO. So eventuate.io, which is a distributed data management framework that solves these problems in a microservice architecture. And the very first message broker and sort of the primary message Broker in a way that it uses it is actually Kafka.

### Tim Berglund:

Very nice.

### Chris Richardson:

So yeah. But then in terms of messaging, we like to think of events, right? But there's been more general than that. So yes, so one way of doing it with messaging is to use events. So the order service says, "I've created an order." So that's an event. Right? The customer service has a message handler, event handler that receives that event, reserves credit, and then it publishes an event saying, "I've reserved credit or the credit reservation attempt failed." for one of some reasons. And then the order service receives that event. So that style of interaction is where services announce what they have done, right? That's event based, and that's known as a choreography based SAGA. But the other form of SAGAs are orchestration based SAGAs. And that's where there is a centralized orchestrator, that is telling the participants what to do.

### Chris Richardson:

So you could think of as choreography as sort of passive aggressive style communication. Right? Whereas with orchestration, specifically the order service is being told to reserve credit. You actually, which sounds like request response, but it's sort of a different interaction style, it's request asynchronous response. So you send a command message to the order service, right? So there's actually a Kafka topic that receives these command messages, which are basically requests to do something. Right? An order is an announcement that something has been, an event is an announcement that has something has been done, these are requests to do something. And then the customer service, does it, and then puts a reply message indicating the outcome in the reply topic that is consumed by the order service.

### Chris Richardson:

So you've got two different styles of messaging to choose from here. And there's various trade offs. So I like to think that, yes, rather than always thinking of microservices as being event driven, kind of the primary communication pattern is actually asynchronous messaging. And then in certain scenarios, you use events, and then in other scenarios, you use request async response.

### Tim Berglund:

Got it, and that request async response. Flush that, I'm just curious. We're getting close to time, this is dangerous for me to ask this. But, and I don't know that it's even all that good of a question to end on. But, I just want to know more about it. So what are the circumstances under which you think that pattern applies the best?

### Chris Richardson:

Well, it generally-

### Tim Berglund:

Let me tell you why I'm asking. I always describe it as a smell. I always tell people that if you're doing that you're probably... I mean, there's synchronous things outside of your microservices architecture and you have to deal with them being synchronous. But if you're trying to be synchronous on the inside you probably don't have to-

### Chris Richardson:

Yeah, this is still very asynchronous. When the order service sends a request to the customer service, it's not sitting there blocked, waiting for a reply to come back. It sends the request message, and then the reply is consumed by a message handler, just as if it was an event. So it's sort of, so don't get misled into thinking that just because it's request response, it's actually blocking in any way. It's all basically still just a bunch of message handlers, which ultimately, there's consumer Kafka client running under many layers below. So I'll give you an example of why. I mean, you think about one of the interesting things about choreography is that the customer service is listening for order events, and then reacting to them. Right?

### Chris Richardson:

So it knows, "Oh, order created, I have to reserve credit. Order canceled, ah, I have to release the credit that was previously reserved for that order." Right? So if you think about it, the customer service has to be an expert on how events coming out of the order service impact, the available credit. Which is slightly smelly, right? It's actually another form of coupling in a way. Right? Whereas with orchestration, it's the order service that is telling the customer service to reserve credit, or release credit. So it's, arguably reverses the dependency and means that the customer service has a simpler API, right? Or it just reserve credit, police credit, right? And it doesn't have to know anything about the order service. So it's kind of like this strange thing where choreography is super simple, except for the fact that it can create the strange coupling, which I think is kind of undesirable. And so orchestration, even though it's more complex, it actually can result in a cleaner set of dependencies.

### Tim Berglund:

Okay. Okay, so that's a consideration that would push you towards orchestration type \[crosstalk 00:43:55\].

### Chris Richardson:

Also, choreography wrap if you've got like many participants, there's all these events bouncing back and forth. And it's very hard to kind of keep what's happening in your head. Whereas orchestration is do this, do that, do the next thing, do the thing after. It's very kind of just linear style of interaction. Plus, there's the orchestration code, which is code that is in one place. And if you want to know what is the SAGA do? Oh, just go look here. Versus choreography where the implementation is scattered around all these event handlers.

### Tim Berglund:

And in the orchestration case. So there's... Final question, I know we're almost up against time.

### Chris Richardson:

I know.

### Tim Berglund:

I can't resist Chris. \[crosstalk 00:44:51\] in orchestration.

### Chris Richardson:

Can we have a part two?

### Tim Berglund:

I think we need a part two. So you're going to be back. But, in the orchestration case, coming from the perspective of somebody who wants to commit a transaction, right? And know that everything was happy. I don't get to do that here, and in the orchestration case, I can get to the end of the orchestration code and know that everything's happy. In the case of a choreographed SAGA, and there are reasons to want choreographed SAGAs. How do I know when it's done? So I'm this person who has been committing transactions, his whole life, and now you're telling me to do this crazy microservice thing. How do I know it worked?

### Chris Richardson:

Yeah, well, it's sort of like the final step gets executed, and some event is published to say that just indicates it's done. Right.

### Tim Berglund:

Got it. So we know it's done because the last thing happened. And you can monitor that last thing.

### Chris Richardson:

Yeah. And so maybe in the order creation SAGA, the order service published an order approved event. So you now know that or an order rejected event. And at that point, you it's done.

### Tim Berglund:

It just seems so simple.

### Chris Richardson:

Yeah.

### Tim Berglund:

Yeah. And that's a good thing. My guest today has been Chris Richardson, who will be back. Chris, thanks for being a part of streaming audio.

### Chris Richardson:

Oh, you're welcome. It's been an interesting discussion. Thank you.

### Tim Berglund:

Hey, you know what you get for listening to the end? Some free Confluent Cloud. Use the promo code 60PDCAST. That's 6-0-P-D-C-A-S-T to get an additional $60 of free Confluent Cloud usage. Be sure to activate it by December 31 2021 and use it within 90 days after activation. And any unused promo value on the expiration date will be forfeit and there are a limited number of codes available so don't miss out. Anyway, as always, I hope this podcast was helpful to you. If you want to discuss it or ask a question, you can always reach out to me @tlberglund on Twitter. That's T-L-B-E-R-G-L-U-N-D. Or you can leave a comment on a YouTube video or reach out in our community slack. There's a slack signup link in the show notes if you'd like to join. And while you're at it, please subscribe to our YouTube channel and to this podcast wherever find podcasts are sold. And if you subscribe to Apple podcasts, be sure to leave us a review there. that helps other people discover us which we think is a good thing. So thanks for your support and we'll see you next time.

Chris Richardson, creator of the original Cloud Foundry, maintainer of microservices.io and author of “Microservices Patterns,” discovered cloud computing in 2006 during an Amazon talk about APIs for provisioning servers. At this time, you could provision 20 servers and pay 10 cents per hour. This blew his mind and led him in 2008 to create the original Cloud Foundry, a PaaS for deploying Java applications on EC2.

One of the original Cloud Foundry’s earliest success stories was a digital marketing agency for a beer company that ran a campaign around the Super Bowl. Cloud Foundry actually enabled them to deploy an application on AWS and then adjust its capacity based on the load. They were leveraging the elasticity of the cloud back in the ‘08–‘09 timeframe. SpringSource eventually acquired Cloud Foundry, followed by VMware. It's the origin of the name of today's Cloud Foundry.

Later in the show, Chris explains what choreographed sagas are, reasons to leverage them, and how to measure their efficacy.

EPISODE LINKS

- [The microservices pattern language](https://microservices.io/)
- [Eventuate framework](https://eventuate.io/)
- [Book: The Art of Scalability](https://microservices.io/articles/scalecube.html)
- [Use **podcon19** to get 40% off Microservices Patterns by Chris Richardson](https://www.manning.com/books/microservices-patterns?a_aid=microservices-patterns-chris&a_bid=2d6d8a4d)
- [Join the Confluent Community Slack](https://cnfl.io/confluent-community-slack)
- [Learn more with Kafka tutorials, resources, and guides at Confluent Developer](https://developer.confluent.io/?utm_source=buzzsprout&utm_medium=podcast&utm_campaign=ch.episode-links_type.community_content.developer)
- [Live demo: Kafka streaming in 10 minutes on Confluent Cloud](https://www.confluent.io/online-talks/confluent-cloud-demo/)
- [Use **60PDCAST** to get an additional $60 of free Confluent Cloud usage](https://www.confluent.io/confluent-cloud/?utm_source=buzzsprout&utm_medium=podcast&utm_campaign=ch.oprah_type.community_content.cloud) ( [details](https://www.confluent.io/confluent-cloud-promo-disclaimer/?utm_source=buzzsprout&utm_medium=podcast&utm_campaign=ch.oprah_type.community_content.cloud))

## Continue Listening

Episode 134December 21, 2020 \| 10 min

### [Apache Kafka 2.7 - Overview of Latest Features, Updates, and KIPs](https://developer.confluent.io/learn-more/podcasts/apache-kafka-27-overview-of-latest-features-updates-and-kips/)

Apache Kafka® 2.7 is here! Here are the key Kafka Improvement Proposals (KIPs) and updates in this release. Find out what’s new with the Kafka broker, producer, and consumer, and what’s new with Kafka Streams in today’s episode.

[Listen Now](https://developer.confluent.io/learn-more/podcasts/apache-kafka-27-overview-of-latest-features-updates-and-kips/)

Episode 135December 22, 2020 \| 46 min

### [Mastering DevOps with Apache Kafka, Kubernetes, and Confluent Cloud ft. Rick Spurgeon and Allison Walther](https://developer.confluent.io/learn-more/podcasts/mastering-devops-with-apache-kafka-kubernetes-and-confluent-cloud-ft-rick-spurgeon-and-allison-walther/)

How do you use Apache Kafka®, Confluent Platform, and Confluent Cloud for DevOps? Integration Architects Rick Spurgeon and Allison Walther share how, including a custom tool they’ve developed for this very purpose.

[Listen Now](https://developer.confluent.io/learn-more/podcasts/mastering-devops-with-apache-kafka-kubernetes-and-confluent-cloud-ft-rick-spurgeon-and-allison-walther/)

Episode 136December 28, 2020 \| 43 min

### [How to Become a Certified Apache Kafka Expert ft. Niamh O’Byrne and Barry Ballard](https://developer.confluent.io/learn-more/podcasts/how-to-become-a-certified-apache-kafka-expert-ft-niamh-obyrne-and-barry-ballard/)

Niamh O’Byrne and Barry Ballard discuss Confluent’s Certification program, including sample test questions, bootcamp, exam details, Kafka training, and getting the necessary practical hands-on experience.

[Listen Now](https://developer.confluent.io/learn-more/podcasts/how-to-become-a-certified-apache-kafka-expert-ft-niamh-obyrne-and-barry-ballard/)

#### Got questions?

If there's something you want to know about Apache Kafka, Confluent or event streaming, please send us an email with your question and we'll hope to answer it on the next episode of Ask Confluent.

[Email Us](mailto:podcast@confluent.io)

#### Never miss an episode!

- [![Apple Podcasts](https://developer.confluent.io/images/icons/icon-apple-podcast.png)](https://podcasts.apple.com/us/podcast/streaming-audio-kafka-confluent-cloud-tim-berglund/id1401509765?mt=2)
- [![Spotify](https://developer.confluent.io/images/icons/icon-spotify.png)](https://open.spotify.com/show/65WRDvSFQ2tkdk1GXlRPqR)
- [![Google Podcasts](https://developer.confluent.io/images/icons/icon-google-podcast.png)](https://podcasts.google.com/?feed=aHR0cHM6Ly9mZWVkcy5idXp6c3Byb3V0LmNvbS8xODYxNTQucnNz)
- [![Stitcher](https://developer.confluent.io/images/icons/icon-stitcher.png)](https://www.stitcher.com/s?fid=200576)
- [![iheart Radio](https://developer.confluent.io/images/icons/icon-iheart-radio.png)](https://www.iheart.com/podcast/256-streaming-audio-a-confluen-31081990/)
- [![TuneIn](https://developer.confluent.io/images/icons/icon-tune-in.png)](https://tunein.com/podcasts/Technology-Podcasts/Streaming-Audio-a-Confluent-podcast-about-Apache--p1264640/)
- [![Overcast](https://developer.confluent.io/images/icons/icon-overcast.png)](https://overcast.fm/itunes1401509765)
- [![Pocket Cast](https://developer.confluent.io/images/icons/icon-pocket-cast.png)](https://pca.st/itunes/1401509765)
- [![Castro](https://developer.confluent.io/images/icons/icon-castro.png)](https://castro.fm/itunes/1401509765)
- [![Castbox](https://developer.confluent.io/images/icons/icon-castbox.png)](https://castbox.fm/vic/1401509765)
- [![Podchaser](https://developer.confluent.io/images/icons/icon-podchaser.png)](https://www.podchaser.com/podcasts/streaming-audio-a-confluent-po-693739)
- [![Podcast Addict](https://developer.confluent.io/images/icons/icon-podcast-addict.png)](https://podcastaddict.com/podcast/2189051)
- [![Deezer](https://developer.confluent.io/images/icons/icon-deezer.png)](https://www.deezer.com/show/1372592)
- [![Listen Notes](https://developer.confluent.io/images/icons/icon-listen-notes.png)](https://www.listennotes.com/c/976cedf0679648cbacac5f06eabd93c7/)
- [![RSS Feed](https://developer.confluent.io/images/icons/icon-rss-feed.png)](https://feeds.buzzsprout.com/186154.rss)
- [![Amazon](https://developer.confluent.io/images/icons/icon-amazon-music.png)](https://music.amazon.com/podcasts/be1ba503-a4b4-4022-867c-87417b4d2a95/Streaming-Audio-A-Confluent-podcast-about-Apache-Kafka)
- [![Pandora](https://developer.confluent.io/images/icons/icon-pandora.png)](https://www.pandora.com/podcast/streaming-audio-a-confluent-podcast-about-apache-kafka/PC:35818?)
- [![Player FM](https://developer.confluent.io/images/icons/icon-player-fm.png)](https://player.fm/series/streaming-audio-a-confluent-podcast-about-apache-kafka)
- [![Podcast Index](https://developer.confluent.io/images/icons/icon-podcast-index.png)](https://podcastindex.org/podcast/333944)
- [![Podfriend](https://developer.confluent.io/images/icons/icon-podfriend.png)](https://web.podfriend.com/podcast/1401509765)

### Confluent Cloud is a fully managed Apache Kafka service available on all three major clouds. Try it for free today.

[Try it for free](https://www.confluent.io/confluent-cloud/tryfree/?session_ref=https%3A%2F%2Fwww.google.com%2F)

Feedback

By clicking “Accept All Cookies”, you agree to the storing of cookies on your device to enhance site navigation, analyze site usage, and assist in our marketing efforts.

Accept All Cookies

Reject All

Cookies Settings