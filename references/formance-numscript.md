Numscript

# Numscript

Numscript is a Domain-Specific Language (DSL) designed to help you model complex financial transactions, replacing complex and error-prone custom code with easy-to-read, declarative scripts.

## [Financial transactions\#](https://docs.formance.com/modules/numscript?deployment=cloud&license=ee\#financial-transactions)

We define a financial transaction as a series of discrete value movements between abstract accounts. Each movement represents transfer of value from one account to another, with an associated amount and asset denomination.

Assets being transferred can represent any kind of value, from traditional currencies like USD or JPY to custom tokens or commodities.

Accounts involved in a transaction can represent anything, from a bank account to a voucher, a virtual wallet or an order that has yet to be paid out.

You can read more about the structure of programs and transactions [here](https://docs.formance.com/modules/numscript/program-structure).

## [Design principles\#](https://docs.formance.com/modules/numscript?deployment=cloud&license=ee\#design-principles)

### [Readability\#](https://docs.formance.com/modules/numscript?deployment=cloud&license=ee\#readability)

The intent of a Numscript program should always be clear and easy to understand. Numscript programs should be readable by both developers and non-technical financial users, providing a shared, executable definition of money movements.

### [Correctness\#](https://docs.formance.com/modules/numscript?deployment=cloud&license=ee\#correctness)

Monetary computations in Numscript should always yield correct results, avoiding common currency rounding errors and accidental money creation or destruction. Execution is atomic, ensuring that either all modeled transactions are committed or none.

### [Finiteness\#](https://docs.formance.com/modules/numscript?deployment=cloud&license=ee\#finiteness)

Numscript programs are deterministic, always terminating with a predictable output. This ensures that the behavior of Numscript programs can be reliably predicted and controlled.

These principles are the guiding light behind Numscript, and they are reflected in the design of the language itself. By using Numscript, you can model complex financial transactions in a way that is clear, accurate, and predictable.

## [Example\#](https://docs.formance.com/modules/numscript?deployment=cloud&license=ee\#example)

Here is a simple transaction example of what a Numscript transaction can look like. We use multiple `send` statements, moving USD through a series of accounts, and splitting the final amount between a driver, a charity, and platform fees.

Numscript

[Run in Numscript playground](https://playground.numscript.org/?data=eyJzY3JpcHQiOiJzZW5kIFtVU0QvMiA1OTldIChcbiAgc291cmNlID0gQHdvcmxkXG4gIGRlc3RpbmF0aW9uID0gQHBheW1lbnRzOjAwMVxuKVxuXG5zZW5kIFtVU0QvMiA1OTldIChcbiAgc291cmNlID0gQHBheW1lbnRzOjAwMVxuICBkZXN0aW5hdGlvbiA9IEByaWRlczowMjM0XG4pXG5cbnNlbmQgW1VTRC8yIDU5OV0gKFxuICBzb3VyY2UgPSBAcmlkZXM6MDIzNFxuICBkZXN0aW5hdGlvbiA9IHtcbiAgICA4NSUgdG8gQGRyaXZlcnM6MDQyXG4gICAgcmVtYWluaW5nIHRvIHtcbiAgICAgIDEwJSB0byBAY2hhcml0eVxuICAgICAgcmVtYWluaW5nIHRvIEBwbGF0Zm9ybTpmZWVzXG4gICAgfVxuICB9XG4pIiwiaW5wdXRzIjoie1xuICBcInZhcmlhYmxlc1wiOiB7fSxcbiAgXCJiYWxhbmNlc1wiOiB7XG4gICAgXCJ3b3JsZFwiOiB7XG4gICAgICBcIlVTRC8yXCI6IDEwMDAwMDBcbiAgICB9LFxuICAgIFwicGF5bWVudHM6MDAxXCI6IHtcbiAgICAgIFwiVVNELzJcIjogMTAwMDAwMFxuICAgIH0sXG4gICAgXCJyaWRlczowMjM0XCI6IHtcbiAgICAgIFwiVVNELzJcIjogMTAwMDAwMFxuICAgIH0sXG4gICAgXCJkcml2ZXJzOjA0MlwiOiB7XG4gICAgICBcIlVTRC8yXCI6IDEwMDAwMDBcbiAgICB9LFxuICAgIFwiY2hhcml0eVwiOiB7XG4gICAgICBcIlVTRC8yXCI6IDEwMDAwMDBcbiAgICB9LFxuICAgIFwicGxhdGZvcm06ZmVlc1wiOiB7XG4gICAgICBcIlVTRC8yXCI6IDEwMDAwMDBcbiAgICB9XG4gIH0sXG4gIFwibWV0YWRhdGFcIjoge31cbn0ifQ%3D%3D&run=1 "Run in Numscript playground")

```
send [USD/2 599] (
  source = @world
  destination = @payments:001
)

send [USD/2 599] (
  source = @payments:001
  destination = @rides:0234
)

send [USD/2 599] (
  source = @rides:0234
  destination = {
    85% to @drivers:042
    remaining to {
      10% to @charity
      remaining to @platform:fees
    }
  }
)
```

Executed by the Numscript interpreter, the above script will result in the following transaction:

JSON

```
{
  "postings": [\
      {\
          "source": "world",\
          "destination": "payments:001",\
          "amount": 599,\
          "asset": "USD/2"\
      },\
      {\
          "source": "payments:001",\
          "destination": "rides:0234",\
          "amount": 599,\
          "asset": "USD/2"\
      },\
      {\
          "source": "rides:0234",\
          "destination": "drivers:042",\
          "amount": 510,\
          "asset": "USD/2"\
      },\
      {\
          "source": "rides:0234",\
          "destination": "charity",\
          "amount": 9,\
          "asset": "USD/2"\
      },\
      {\
          "source": "rides:0234",\
          "destination": "platform:fees",\
          "amount": 80,\
          "asset": "USD/2"\
      }\
  ]
}
```

A VSCode extension for Numscript is available [here](https://marketplace.visualstudio.com/items?itemName=formance.formance-vscode&ssr=false#overview).