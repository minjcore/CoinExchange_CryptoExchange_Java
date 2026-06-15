Why was this page helpful to you?

Solved my problem

Easy to understand

Good example

Another reason

Submit

Why was this page not helpful to you?

Couldn't find what I was looking for

Too complicated / Too many steps

Unclear language or terminology

Problem with the code / errors

Another reason

Submit

Thank you!

Your feedback helps us improve our product.

Why was this page helpful to you?

Solved my problem

Easy to understand

Good example

Another reason

Submit

Why was this page not helpful to you?

Couldn't find what I was looking for

Too complicated / Too many steps

Unclear language or terminology

Problem with the code / errors

Another reason

Submit

Thank you!

Your feedback helps us improve our product.

![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/pay-by-link-plane.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/checkout.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/terminal-2.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/unified-commerce.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/plugins.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/pay-by-link-plane.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/platforms.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/marketplaces.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/accounts.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/capital.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/issuing.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/classic-platforms.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/accounts.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/issuing.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/payout.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/person-circle.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/reporting.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/risk-team.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/pay-by-link-plane.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/credentials.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/event-code.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/warning.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/news.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/tools.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/settings.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/store.svg)![](https://docs.adyen.com/user/themes/adyen/images/illustrations/icons/users-9.svg)

Was this page helpful?

![Payment-method icon](https://docs.adyen.com/user/themes/adyen-docs/assets/icons/payment-methods.svg)

# Napas card for API only

Add Napas card to an existing API-only integration.


[View source](https://docs.adyen.com/payment-methods/napas-card/api-only.md)

Copy as Markdown

Accept Napas card payments using our APIs, and build your own payment form to have full control over the look and feel of your checkout page.

## [Anchor](https://docs.adyen.com/payment-methods/napas-card/api-only\#requirements) Requirements

| Requirement | Description |
| --- | --- |
| **Integration type** | Make sure that you have built an [API-only integration](https://docs.adyen.com/online-payments/build-your-integration/advanced-flow?platform=Web&integration=API%20only). |
| **Setup steps** | Before you begin, [add Napas card in your test Customer Area](https://docs.adyen.com/payment-methods/add-payment-methods). |

## [Anchor](https://docs.adyen.com/payment-methods/napas-card/api-only\#build-your-payment-form-for-napas-card) Build your payment form for Napas card

Include Napas card in the list of available payment methods. You do not need to collect any information from the shopper in your payment form.

We provide logos for Napas card which you can use on your payment form. For more information, refer to [Downloading logos](https://docs.adyen.com/online-payments/build-your-integration/advanced-flow/?platform=Web&integration=API%20only#downloading-logos).

If you are using the [/paymentMethods](https://docs.adyen.com/api-explorer/Checkout/latest/post/paymentMethods) to show available payment methods to the shopper, specify the following:

- [countryCode](https://docs.adyen.com/api-explorer/Checkout/latest/post/paymentMethods): **VN**
- [amount.currency](https://docs.adyen.com/api-explorer/Checkout/latest/post/paymentMethods#request-amount): **VND**
- [amount.value](https://docs.adyen.com/api-explorer/Checkout/latest/post/paymentMethods#request-amount): The value of the payment.
- [channel](https://docs.adyen.com/api-explorer/Checkout/latest/post/paymentMethods#request-channel): Specify **Web**, **iOS**, or **Android**.

The response contains `paymentMethod.type`: **momo\_atm**.

## [Anchor](https://docs.adyen.com/payment-methods/napas-card/api-only\#make-payment) Make a payment

In your [/payments](https://docs.adyen.com/api-explorer/Checkout/latest/post/payments) request, specify:

- `paymentMethod.type`: **momo\_atm**

curlNodeJS (JavaScript)PythonRubycurlNodeJS (JavaScript)PythonRuby

Expand view

Copy link to code block

Copy code

Copy code

```bash

curl https://checkout-test.adyen.com/v68/payments \

-H 'x-api-key: ADYEN_API_KEY' \

-H 'content-type: application/json' \

-d '{

  "merchantAccount":"ADYEN_MERCHANT_ACCOUNT",

  "reference":"YOUR_ORDER_NUMBER",

  "amount":{

    "currency":"VND",

    "value":1000

  },

  "paymentMethod":{

    "type":"momo_atm"

  },

  "returnUrl":"https://your-company.example.com/checkout?shopperOrder=12xy.."

}'
```

In the [/payments](https://docs.adyen.com/api-explorer/Checkout/latest/post/payments) response, check the `action` object for the information that you must use to redirect the shopper.

/payments response

Expand view

Copy link to code block

Copy code

Copy code

```json

{

  "resultCode":"RedirectShopper",

  "action":{

    "paymentMethodType":"momo_atm",

    "method":"GET",

    "url":"https://checkoutshopper-test.adyen.com/checkoutshopper/checkoutPaymentRedirect?redirectData=...",

    "type":"redirect"

  }

}
```

## [Anchor](https://docs.adyen.com/payment-methods/napas-card/api-only\#handle-the-redirect) Handle the redirect

To complete a Napas card payment, you need to redirect the shopper to Napas card's website or app.

1. To complete the payment, redirect the shopper to the `action.url` returned in the [/payments](https://docs.adyen.com/api-explorer/Checkout/latest/post/payments) response, taking into account the following recommendations:
   - **When using the HTTP GET method:**


     For security reasons, when showing the redirect in the app, we recommend that you use [SFSafariViewController](https://developer.apple.com/documentation/safariservices/sfsafariviewcontroller) for iOS or [Chrome Custom Tabs](https://developer.chrome.com/multidevice/android/customtabs) for Android, instead of WebView objects. Also refer to the [security best practices](https://developer.android.com/topic/security/best-practices#webview) for WebView.

   - **Redirection for mobile integrations:**


     For mobile integrations, we strongly recommended that you redirect the shopper to the default browser of their device. Redirecting to the default browser ensures the best compatibility, handling of multi-factor authentication, app-to-app redirection, and error handling.
2. After the shopper is redirected back to your website, check the payment result by making a POST [/payments/details](https://docs.adyen.com/api-explorer/Checkout/latest/post/payments/details) request, specifying:


   - `details`: object that contains the URL-decoded `redirectResult` returned when the shopper was redirected back to your site.

/payments/details request

Expand view

Copy link to code block

Copy code

Copy code

```bash

curl https://checkout-test.adyen.com/v72/payments/details \

-H 'x-api-key: ADYEN_API_KEY' \

-H 'content-type: application/json' \

-d '{

   "details": {

      "redirectResult": "eyJ0cmFuc1N0YXR1cyI6IlkifQ=="

   }

}'
```

3. In the response note the following:


   - `resultCode`: use this to present the result to your shopper.
   - `pspReference`: our unique identifier for the transaction.

/payments/details response

Expand view

Copy link to code block

Copy code

Copy code

```json

{

   "resultCode": "Authorised",

   "pspReference": "V4HZ4RBFJGXXGN82"

}
```

## [Anchor](https://docs.adyen.com/payment-methods/napas-card/api-only\#present-the-payment-result) Present the payment result

Use the  [resultCode](https://docs.adyen.com/api-explorer/Checkout/latest/post/payments/details#responses-200-resultCode) that you received in the [/payments/details](https://docs.adyen.com/api-explorer/Checkout/latest/post/payments/details) response to present the payment result to your shopper.

The `resultCode` values you can receive for Napas card are:

| resultCode | Description | Action to take |
| --- | --- | --- |
| **Authorised** | The payment was successful. | Inform the shopper that the payment has been successful. |
| **Cancelled** | The shopper cancelled the payment while on Napas card or NAPAS's website. | Ask the shopper whether they want to continue with the order, or ask them to select a different payment method. |
| **Pending** or <br>**Received** | The shopper has completed the payment but the final result is not yet known. | Inform the shopper that you have received their order, and are waiting for the payment to be completed. <br> To know the final result of the payment, wait for the **AUTHORISATION** webhook. |
| **Refused** | The payment was refused by the shopper's bank. | Ask the shopper to try the payment again using a different payment method. |

If the shopper closed the browser and failed to return to your website, wait for webhooks to know the outcome of the payment. The webhooks you can receive for Napas card are:

| eventCode | success field | Description | Action to take |
| --- | --- | --- | --- |
| **AUTHORISATION** | **false** | The transaction failed. | Cancel the order and inform the shopper that the payment failed. |
| **AUTHORISATION** | **true** | The shopper successfully completed the payment. | Inform the shopper that the payment has been successful and proceed with the order. |
| **OFFER\_CLOSED** | **true** | The shopper did not complete the payment. | Cancel the order and inform the shopper that the payment timed out. |

## [Anchor](https://docs.adyen.com/payment-methods/napas-card/api-only\#test-and-go-live) Test and go live

To test Napas card payments, select any bank from the list and use the following test cards and credentials:

| Scenario | Card name | Card number | Expiry | One-time password |
| --- | --- | --- | --- | --- |
| Successful | Nguyen Van A | 9704000000000018 | 03/07 | otp |
| Card blocked | Nguyen Van A | 9704000000000026 | 03/07 | otp |
| Insufficient funds | Nguyen Van A | 9704000000000034 | 03/07 | otp |
| Amount exceeded limit | Nguyen Van A | 9704000000000042 | 03/07 | otp |

Check the status of Napas card test payments in your [Customer Area](https://ca-test.adyen.com/) > **Transactions** > **Payments**.

Before you can accept live Napas card payments, you need to submit a request for Napas card in your [live Customer Area](https://ca-live.adyen.com/).

## [Anchor](https://docs.adyen.com/payment-methods/napas-card/api-only\#see-also) See also

- [API only integration](https://docs.adyen.com/online-payments/api-only)
- [Webhooks](https://docs.adyen.com/development-resources/webhooks)
- [API Explorer](https://docs.adyen.com/api-explorer/#/CheckoutService/latest/overview)

Was this page helpful?