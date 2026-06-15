[Skip to main content](https://api.napas.com.vn/page/introduce#main-wrapper)

[![NAPAS](https://api.napas.com.vn/files/assets/img/logo_white.png)](https://api.napas.com.vn/)

- [Trang chủ](https://api.napas.com.vn/)
- [Về Napas](https://api.napas.com.vn/page/about)
- [Sản phẩm của Napas](https://api.napas.com.vn/page/introduce)
- [Tài liệu](https://api.napas.com.vn/page/docs)
- [Liên hệ](https://api.napas.com.vn/page/contact)
- [Hỏi đáp](https://api.napas.com.vn/page/faq)

CỔNG TÍCH HỢP SẢN PHẨM NAPAS


Danh sách sản phẩm:

Napas Core

v 1.2.1


## Napas Digital Payment Platform

v1.2

### Napas Digital Payment Platform

v1.4

### DPP API Specification For Issuer (Part 01 - Payment)

v1.0.0

### Napas API Specification DPP Token Vault (Part 02 - Payment)

## Others

v1.2.1

### Napas Core

v0.3.6

### Open Banking

API LIST

#### Nhóm Napas Core

Nhóm các API hỗ trợ truy vấn thông tin giao dịch, thông tin cài đặt Merchant, lấy file đối soát, tạo mã QR.

Endpoints on this page

|     |     |
| --- | --- |
| POST [/api/core/v1/oauth/token](https://api.napas.com.vn/page/introduce#oauth2-v20) | OAUTH2 |
| POST [/api/core/v1/oauth/refresh-token](https://api.napas.com.vn/page/introduce#refresh-token) | API Refresh Token |
| POST [/api/core/v1/transaction](https://api.napas.com.vn/page/introduce#api-gettransaction) | API Get Transaction |
| POST [/api/core/v1/transactions](https://api.napas.com.vn/page/introduce#api-gettransactions) | API Get Transactions |
| POST [/api/core/v1/files](https://api.napas.com.vn/page/introduce#api-listfiles) | API List File |
| POST [/api/core/v1/file](https://api.napas.com.vn/page/introduce#api-getfile) | API Get File |
| POST [/api/core/v1/merchant](https://api.napas.com.vn/page/introduce#api-getmerchant) | API Get Merchant |
| POST [/api/core/v1/merchants](https://api.napas.com.vn/page/introduce#api-getmerchants) | API Get Merchants |
| POST [/api/core/v1/gen-viet-qr](https://api.napas.com.vn/page/introduce#api-generateqr) | API Generate QR |
| POST [/api/core/v1/get-vietqr-info](https://api.napas.com.vn/page/introduce#api-getvietqrinfo) | API Get VietQR Info |

![N|Solid](https://api.napas.com.vn/files/assets/files/api-landing18.svg)

## Signature

Ký số toàn bộ các trường đầu vào theo thứ tự alpha beta của tên biến input

## Sample API Oauth2

| STT | Field Name | Content |
| --- | --- | --- |
| 1 | client\_id | rkvnjv6gcy8bu9wdegrx8xr3 |
| 2 | client\_secret | MBRpDEysZRffuPMWJKAvbC83svjNh2zX |
| 3 | grant\_type | password |
| 4 | password | gW3Zk39v7c |
| 5 | username | t43mz48s3jgyfccyfdqdsa46 |

> Signature input : rkvnjv6gcy8bu9wdegrx8xr3MBRpDEysZRffuPMWJKAvbC83svjNh2zXpasswordgW3Zk39v7ct43mz48s3jgyfccyfdqdsa46

## Oauth2 V2.0

```js
POST: /api/core/v1/oauth/token
```

## Mục đích

Tạo Token cho Merchant để sử dụng các dịch vụ API để đảm bảo tính bảo mật.

## Header Parameters

| Parameters | Type | Description |
| --- | --- | --- |
| Authorization | string `required` | Mã hóa base64 (username:password) |

## Query Parameters

| Parameters | Type | Description |
| --- | --- | --- |
| grant\_type | string `required` | Để giá trị “password” |
| client\_id | string `required` | Mã Merchant |
| client\_secret | string `required` | Thông tin client\_secret của Merchant |
| username | string `required` | Thông tin username của Merchant |
| password | string `required` | Thông tin password của Merchant |
| Signature | string `required` | Ký số |

## Sample Request

```js
curl --location --request POST 'https://api.napas.com.vn/api/core/v1/oauth/token'
--header 'Content-Type: application/json' --data-raw '{
"grant_type": "password",
"client_id": "PSP_9PAYTEST",
"client_secret": "Napas$01#2021!",
"username": "userPSP_9PAYTEST",
"password": "12345",
"Signature": "dQsI6ytwNrY45EngFzMkrJR82xZM0t_TW3DrvBg9bhsEQU4zTRvH7KkC24oEO45KSr39mwV09haMYFDuMu0_bh1-OIprlD3KxqRmx7ltjWuzNUsjh28Wi301sgngiuNSP_SRsixUMh9rjEvjWRG_byW_Pb0f5b0x1jxuc51TAnwkPP1Ft1JL1679lbGTZakPLroplPiwcagyx1_qgPWsAEXTErpAJXSJiMhW8e5B8EcIw95-qKhvrzKsHfVtgiCS06gdJoJoPr6Dujcf3uWiXYnbyM9OmbZS4WIAsP2q7pWCwc4-rRDrO7C1Ltqcycn1Yc643TvSre3JxyGwTx-4YA"
}'
```

## Example Response

```javascript
{
  "result": "SUCCESS",
  "access_token": "3cxfrnc8wjanxt6g3w972382",
  "token_type": "bearer",
  "refresh_token": "jgwnejv8cdn76a9uupcewnea",
  "expies_in": "300",
  "Signature": "AuB6Rtha2f4dLmqwdwkQedsOUbulfmke_cGQzlTIgymauGQUTWZSVdxhaDGik4KhGIfbVZklRfHoOvz0QLiF2rhU2SKNWzySDmQ9BJMwffe1Ib4livhNIIIXqZzP2TkUmkOAMqmGzFMn101f6v3_hyHrdo7ZRoUJM0-t7CdfQgeAKtPb0ED4s9iYPoxZ4MF9XshC4HPyMC1ervfl4h57QFbU32vToMMfEkK62rjU1qVCSAjKlgp_uIVoSDOLmyf6jAA0ke_XJj1FxuoAFvDKlW1OlmTcwDXsXN7J8bpDTdpXgEMsi5rggsYEeuqBu-JR6sSmQ6y-NfkAYp2A8y28cw"
}
```

## Note

Để ký số đối tác sắp xếp giá trị truyền vào theo thứ tự dưới để tạo thành một chuỗi giá tri. Ký chuỗi theo SHA256 RSA key được cung cấp.

| STT | Field Name |
| --- | --- |
| 1 | client\_id |
| 2 | client\_secret |
| 3 | grant\_type |
| 4 | password |
| 5 | username |

## API Response Codes

#### 200 Success

Thành công.

| Field | Type | Description |
| --- | --- | --- |
| result | string `required` | Trả về SUCCESS nếu không có phát sinh lỗi hệ thống/lỗi nghiệp vụ, còn lại trả về ERROR |
| ErrCode | string `optional` | Mã lỗi phản hồi nếu có phát sinh lỗi |
| ErrDesc | string `optional` | Mô tả lỗi phản hồi nếu có phát sinh lỗi |
| access\_token | Object `optional` | Được sinh ra bởi hệ thống của Napas |
| token\_type | string `optional` | Trả giá trị "bearer" |
| refresh\_token | string `required` | Được sinh ra bởi hệ thống của Napas |
| expires\_in | string `optional` | Thời gian sống của token, đơn vị là giây. Mặc định = 300 |
| scope | string `required` | Mặc định = "read write trust" |
| Signature | string `required` | Ký số toàn bộ các trường đầu ra theo thứ tự alpha beta của tên biến output. Trường hợp output là mảng/đối tượng thì so sánh tên mảng/đối tượng với các biến cùng mức |

#### 400 Bad Request

Thao tác này trả về nội dung lỗi của hệ thống.

#### 401 Unauthorized

Token hết hạn hoặc không tồn tại.

#### 404 Not Found

Thao tác này trả về nội dung lỗi của hệ thống.

#### 500 Internal Server Error

Thao tác này trả về nội dung lỗi của hệ thống.

## Refresh Token

```js
POST: /api/core/v1/oauth/refresh-token
```

## Mục đích

Lấy mã access token từ refresh token đã được tạo. Access Token là duy nhất và có hiệu lực trong vòng 15 phút, Refresh Token là duy nhất và có hiệu lực trong vòng 7 ngày.

## Header Parameters

| Parameters | Type | Description |
| --- | --- | --- |
| Authorization | string `required` | Mã hóa base64 (username:password) |

## Query Parameters

| Parameters | Type | Description |
| --- | --- | --- |
| grant\_type | string `required` | Để giá trị “password” |
| client\_id | string `required` | Mã Merchant |
| client\_secret | string `required` | Thông tin client\_secret của Merchant |
| refresh\_token | string `required` | Được sinh ra bởi hệ thống của Napas |
| Signature | string `required` | Ký số |

## Sample Request

```js
curl --location 'https://developer.napas.com.vn/api/core/v1/oauth/refresh-token' \
--header 'Content-Type: application/json' \
--data '{
  "Signature": "nEijR0IbdzoVPKMVpAI3fzqv41x16y1HgMlrDHrQHiw5s8yPoK7cjx6R6ltqNak8hWSPWxsc8yGQ69s8cjtWw7HR0wD/+xl6fEW91YaXjNYZwOIRYEkW2FSTL/r+EMr7QhJyYSly4vCyz3Z7sqtGTyRpYcRd1UCQEkJzPVDYhuuXEkakCPKrbft/fbpYKkuWUBTc/lR8EWF+tAOlUYV9zUDGwQAoH/BBMuzrBy7zZD5DYJL2j1b8cSHPptZsu6hux3KcdPxStP7BZ3NDZfc+i6pSX3bd7rB19xgBDPKFGu/Xc+ISKCtyNpbAvItZEK4MSSBtwjWh3l7KcMkOGkWmdQ==",
  "client_id": "VIETPAY",
  "client_secret": "X4F73C1102A0CE915BD97506211XA74B",
  "grant_type": "refresh_token",
  "refresh_token": "v3effdnffmnyw9z8xnv5h8ky"
}'
```

## Example Response

```javascript
{
  "result": "SUCCESS",
  "access_token": "3cxfrnc8wjanxt6g3w972382",
  "token_type": "bearer",
  "refresh_token": "jgwnejv8cdn76a9uupcewnea",
  "expies_in": "300",
  "Signature": "AuB6Rtha2f4dLmqwdwkQedsOUbulfmke_cGQzlTIgymauGQUTWZSVdxhaDGik4KhGIfbVZklRfHoOvz0QLiF2rhU2SKNWzySDmQ9BJMwffe1Ib4livhNIIIXqZzP2TkUmkOAMqmGzFMn101f6v3_hyHrdo7ZRoUJM0-t7CdfQgeAKtPb0ED4s9iYPoxZ4MF9XshC4HPyMC1ervfl4h57QFbU32vToMMfEkK62rjU1qVCSAjKlgp_uIVoSDOLmyf6jAA0ke_XJj1FxuoAFvDKlW1OlmTcwDXsXN7J8bpDTdpXgEMsi5rggsYEeuqBu-JR6sSmQ6y-NfkAYp2A8y28cw"
}
```

## Note

Để ký số đối tác sắp xếp giá trị truyền vào theo thứ tự dưới để tạo thành một chuỗi giá tri. Ký chuỗi theo SHA256 RSA key được cung cấp.

| STT | Field Name |
| --- | --- |
| 1 | client\_id |
| 2 | client\_secret |
| 3 | grant\_type |
| 4 | refresh\_token |

## API Response Codes

#### 200 Success

Thành công.

| Field | Type | Description |
| --- | --- | --- |
| result | string `required` | "SUCCESS" |
| access\_token | Object `optional` | Được sinh ra bởi hệ thống của Napas |
| token\_type | string `optional` | Trả giá trị "bearer" |
| refresh\_token | string `required` | Được sinh ra bởi hệ thống của Napas |
| expires\_in | string `optional` | Thời gian sống của token, đơn vị là giây. Mặc định = 300 |
| Signature | string `required` | Ký số toàn bộ các trường đầu ra theo thứ tự alpha beta của tên biến output. Trường hợp output là mảng/đối tượng thì so sánh tên mảng/đối tượng với các biến cùng mức |

#### 400 Bad Request

| Field | Description |
| --- | --- |
| statusCode | 400 |
| message | ${Filed Name} exceeds the max length |
| \-\-\---- | ${Filed Name} wrong formats |
| \-\-\---- | ${Filed Name}is null |
| \-\-\---- | Signature could not be verified |
| \-\-\---- | Request API Over Quota Limit |
| \-\-\---- | Invalid JSON Request |

#### 500 Internal Server Error

| Field | Description |
| --- | --- |
| statusCode | 500 |
| message | Internal server error |

## API GetTransaction

```js
POST: /api/core/v1/transaction
```

## Mục đích

Trả về chi tiết 1 giao dịch ecom trên hệ thống backend TTDT.

## Path Parameters

| Parameters | Type | Description |
| --- | --- | --- |
| merchantId | string `required` | MerchantId của Merchant. Duy nhất với mỗi Merchant. Dữ liệu gồm các ký tự: 0-9, a-z, A-Z |
| orderid | string `required` | Mã đơn hàng là duy nhất trong suốt quá trình sử dụng API |
| transactionid | string `required` | Id giao dịch là duy nhất trong suốt quá trình sử dụng API |

## Header Parameters

| Parameters | Type | Description |
| --- | --- | --- |
| Authorization | string `required` | Mã hóa base64 (username:password) |

## Query Parameters

| Parameters | Type | Description |
| --- | --- | --- |
| TranxCode | string `optional` | Theo mã giao dịch Napas trả về đối tác. Bắt buộc TranxCode nếu trường PartnerTranxCode không nhập thông tin. |
| ServiceCode | string `required` | Loại dịch vụ. (Tạm thời mặc định ECOM ). |
| PartnerTranxCode | string `required` | Mã giao dịch ở hệ thống của Partner. Bắt buộc nếu trường TranxCode không nhập thông tin. |
| CardScheme | string `required` | Gồm 2 giá trị: CreditCard: là thẻ quốc tế, AtmCard: là thẻ nội địa |
| GatewayOrderId | string `required` | Loại cổng thanh toán. Bắt buộc truyền với giao dịch quốc tế. Gồm các cổng sau: <br>• MIGS <br>• DC <br>• MPGS <br>• KCP, UPOP |
| Signature | string `required` | Ký số toàn bộ các trường đầu vào theo thứ tự alpha beta của tên biến input |

## Sample Request

```js
curl --location --request POST 'https://api.napas.com.vn/api/core/v1/transaction' --header 'Content-Type: application/json'
--data-raw '{
  "TranxInfo": {
  "ServiceCode": "ECOM",
  "CardScheme": "AtmCard",
  "Signature": "GdOrfFnmTdaZEalBl0MaLRehnVP3qnbZowlDkuqAc7Dv8jAhrmK-zglF61kP_xmF-IdNZ-KSkilLlE_-9xQk7PqDPWjekgw4vaN_jRwyKIjPrurOaIA5aiyfRe-HSV4GwyWGAz_Es7U3Kn9CsKk5yGrY5P_q7jUOCZv6QJwjwFG7s-7x_Prx7wyi0Po8Sj2N5yCK8F3jOQ_WlgxDAQhyJ99V4wPgHnkIpXsO1NFgzNxOKpxhr4DXXcn_CGvY6O5qmm8GX47nnQstKkgIxYQnja7Lt3s6953T6Ac4BaP2MMxgB7UViUbgYi3_Gr1XJa7r0pnBmvpaeEQrycllSfjsmQ",
  "TranxCode": "835599909",
  "PartnerTranxCode": "",
  "GatewayOrderId": ""
  }
}'
```

## Example Response

```javascript
{
  "ResponseInf": {
    "Status": "SUCCESS"
  },
  "DomTransaction": {
    "TransactionID": "835599909",
    "AcquirerCode": "STB",
    "CardNumber": "rReq1h3zqmUN7JUh4mZHze8kH+KtXZJR5SZJlgR/VxsnMiFwHR7MLu5qbNRcq8EuEmIJxiEDTiJOaGmw",
    "Amount": "2100000",
    "IssuerCode": "SML",
    "CardName": "x0c0zsG7JiAf55YKiToq8DVP13nrMu+HN6yLCUfDXPmcYV6oxIt027CI/TacrzJ1eAcn2I25WVw=",
    "SettleAmount": "2100000",
    "ExpDate": "gylumvHtgsgnNhg8VfbisC7L47r0XhSfrbf2JBQYIYHW9i/sYwaH3kiYPW/Nr5zH5DYQ",
    "Currency": "VND",
    "TransactionRef": 7980173,
    "ResponseCode": "0",
    "MerchantCode": "BEGROUP_M_STG",
    "TransactionInfo": "Thanh toan hoa don",
    "IP": "27.71.207.170",
    "ServiceType": "ECOM"
  },
  "IntTransaction": {
    "CreatedDate": "",
    "UpdateDate": ""
  },
  "Timestamp": "07/01/2022 11:26:50",
  "Signature": "PAwT0uQJKDhhwFL77VHLgrrTxAgqBTGGCktuoEzWWfbZsKfpS0QtS-j-UKLUkbbDajVTBYye3rMi0iq8ERHI0yjOKgXp2qnT_wUJxp2PL-bpinAUh4ma7LJ8vZl1qYpislsBVM05CK07P6ejsjQS4PrbEomitHtp2-LpryxCza5UtgoY66dESdzZaaJ7Tu75Re__S4TxR2JXRQXkzRSmDtY2clRU5kythicg_DGSo0Qnp_IZLvY3qiTrZrPOxkcjD2q0mv9laJ9kZcxhMLLYe0k-Ipp6hr9HD7SIXlds8zC-MDLmFS-nCOhPli-RS2WR_CSewDi-2BT08YLeIj20Bw"
}
```

## Note

Để ký số đối tác sắp xếp giá trị truyền vào theo thứ tự dưới để tạo thành một chuỗi giá tri. Ký chuỗi theo SHA256 RSA key được cung cấp.

| STT | Field Name |
| --- | --- |
| 1 | CardScheme |
| 2 | GatewayOrderId |
| 3 | PartnerTranxCode |
| 4 | ServiceCode |
| 5 | TranxCode |

## API Response Codes

#### 200 Success

Thành công.

| Parameters | Type | Description |
| --- | --- | --- |
| ResponseInfo.Status | string `required` | Trả về SUCCESS nếu không có phát sinh lỗi hệ thống/lỗi nghiệp vụ, còn lại trả về ERROR |
| ResponseInfo.ErrCode | string `optional` | Mã lỗi phản hồi nếu có phát sinh lỗi |
| ResponseInfo.ErrDesc | string `optional` | Mô tả lỗi phản hồi nếu có phát sinh lỗi |
| DomTransaction | Object `optional` | Thông tin output nếu là giao dịch giao dịch nội địa. |
| DomTransaction.TransactionID | string `optional` | Id giao dịch. |
| DomTransaction.AcquirerCode | string `required` | Mã ngân hàng thanh toán. |
| DomTransaction.CardNumber | string `optional` | Số thẻ. Mã hóa sử dụng thuật toán mã hóa AES-GCM (256bit trở lên). Tham chiếu tại mục 3.9 Cách giải mã trường thông tin |
| DomTransaction.Amount | string `optional` | Số tiền thanh toán. |
| DomTransaction.IssuerCode | string `optional` | Mã ngân hàng phát hành thẻ. |
| DomTransaction.CardName | string `optional` | Tên chủ tài khoản. Mã hóa sử dụng thuật toán mã hóa AES-GCM (256 bit trở lên) . Tham chiếu tại mục 3.9 Cách giải mã trường thông tin |
| DomTransaction.SettleAmount | string `optional` | Số tiền quyết toán. |
| DomTransaction.BankID | string `optional` | ID ngân hàng. |
| DomTransaction.ExpDate | string `optional` | Tháng, năm hết hạn thẻ, MM/YYYY. Mã hóa sử dụng thuật toán mã hóa AES-GCM (256 bit trở lên). Tham chiếu tại mục 3.9 Cách giải mã trường thông tin |
| DomTransaction.Currency | string `optional` | Loại tiền giao dịch. |
| DomTransaction.TransactionRef | string `optional` | Ghi chú giao dịch. |
| DomTransaction.ResponseCode | string `optional` | Mã trạng thái của giao dịch. |
| DomTransaction.MerchantCode | string `optional` | Mã merchant. |
| DomTransaction.TransactionInfo | string `optional` | Thông tin giao dịch |
| DomTransaction.IP | string `optional` | Địa chỉ IP Local. |
| DomTransaction.ServiceType | string `optional` | Service type. Loại dịch vụ. |
| DomTransaction.CardFundMethod | string `optional` | Phương thức cấp vốn bằng thẻ. |
| IntTransaction | Object `optional` | Thông tin output nếu là giao dịch quốc tế. |
| IntTransaction.MerchantId | string `optional` | ID người bán |
| IntTransaction.AcquirerId | string `optional` | ID người mua. |
| IntTransaction.TransactionId | string `optional` | ID giao dịch. |
| IntTransaction.OrdId | string `optional` | ID đặt hàng. |
| IntTransaction.GroupId | string `optional` | Nhóm ID |
| IntTransaction.GatewayTranxId | string `optional` | ID cổng giao dịch. |
| IntTransaction.GatewayOrdId | string `optional` | ID cổng đơn đặt hàng. |
| IntTransaction.CreatedDate | Date `optional` | Định dạng DD/MM/YYYY HH24:MM:SS. |
| IntTransaction.UpdateDate | Date `optional` | Định dạng DD/MM/YYYY HH24:MM:SS. |
| IntTransaction.TransactionType | string `optional` | Loại giao dịch |
| IntTransaction.PaymentType | string `optional` | Hình thức thanh toán. |
| IntTransaction.ServiceCode | string `optional` | Mã dịch vụ. |
| IntTransaction.Amount | Number `optional` | Số tiền giao dịch. |
| IntTransaction.RefundAmount | Number `optional` | Số tiền hoàn lại vẫn còn. |
| IntTransaction.Currency | string `optional` | Loại tiền giao dịch. |
| IntTransaction.OrderRef | string `optional` | Đơn hàng tham khảo. |
| IntTransaction.BatchNumber | string `optional` | Sô chuyến |
| IntTransaction.AuthorizationCode | string `optional` | Mã ủy quyền. |
| IntTransaction.IP | string `optional` | Vị trí IP. |
| IntTransaction.Result | string `optional` | Kết quả |
| IntTransaction.GatewayCode | string `optional` | Mã cổng. |
| IntTransaction.ResponseCode | string `optional` | Mã phản hồi. |
| IntTransaction.ResponseDesc | string `optional` | Mô tả phản hồi. |
| IntTransaction.CardFundMethod | string `optional` | Phương thức cấp vốn bằng thẻ. |
| IntTransaction.CardNumber | string `optional` | Mã hóa sử dụng thuật toán mã hóa AES-GCM (256 bit trở lên). Tham chiếu tại mục 3.9 Cách giải mã trường thông tin |
| IntTransaction.CardBrand | string `optional` | Nhãn hiệu thẻ. |
| IntTransaction.ExpDate | string `optional` | Tháng, năm hết hạn thẻ, MM/YYYY. Mã hóa sử dụng thuật toán mã hóa AES-GCM (256 bit trở lên). Tham chiếu tại mục 3.9 Cách giải mã trường thông tin |
| IntTransaction.CSC | string `optional` | Kết quả CSC |
| IntTransaction.CSCResp | string `optional` | Phản hồi CSC |
| IntTransaction.Token | string `optional` | Mã Token |
| IntTransaction.TokenExp | string `optional` | Mã thông báo hết hạn. |
| IntTransaction.AuthStatus | string `optional` | Trạng thái xác thực |
| IntTransaction.EnrollStatus | string `optional` | Tình trạng tuyển sinh. |
| IntTransaction.XID | string `optional` | 3-D Secure XID. |
| IntTransaction.ECI | string `optional` | 3-D Secure ECI. |
| Timestamp | Date `required` | Ngày giờ hệ thống. DD/MM/YYYY HH24:MM:SS |
| Signature | string `required` | Ký số toàn bộ các trường đầu ra theo thứ tự alpha beta của tên biến output. Trường hợp output là mảng/đối tượng thì so sánh tên mảng/đối tượng với các biến cùng mức |

#### 400 Bad Request

Thao tác này trả về nội dung lỗi của hệ thống.

#### 401 Unauthorized

Token hết hạn hoặc không tồn tại.

#### 404 Not Found

Thao tác này trả về nội dung lỗi của hệ thống.

#### 500 Internal Server Error

Thao tác này trả về nội dung lỗi của hệ thống.

## API GetTransactions

```js
POST: /api/core/v1/transactions
```

## Mục đích

Trả về chi tiết nhiều giao dịch ecom trên hệ thống backend TTDT.

## Header Parameters

| Parameters | Type | Description |
| --- | --- | --- |
| Authorization | string `required` | Mã thông báo ủy quyền gần đây nhất. Điều này sẽ có định dạng Bearer + {space} + {accessToken}. Ví dụ: Bearer KGNsaWVudF9pZDpjbGllbnRfc2VjcmV0KQ==. |

## Query Parameters

| Parameters | Type | Description |
| --- | --- | --- |
| FromDate | string `required` | Ngày từ định dạng DD/MM/YYYY HH24:MM:SS |
| ToDate | string `required` | Ngày đến định dạng DD/MM/YYYY HH24:MM:SS Thời gian từ ngày đến ngày giới hạn trong 1 tháng (tham số cấu hình) Thời gian tìm kiếm trong vòng 360 ngày cho thời điểm giao dịch (tham số cấu hình) |
| TransactionStatus | string `optional` | Trạng thái giao dịch. Gồm 2 giái trị là SUCCESS/ERROR |
| CardScheme | string `required` | Gồm 2 giá trị: CreditCard: là thẻ quốc tế, AtmCard: là thẻ nội địa |
| MerchantCode | string `optional` | Đơn vị chấp nhận thẻ |
| IssuerCode | string `optional` | Ngân hàng phát hành. |
| AccquireCode | string `optional` | Ngân hàng thanh toán. |
| OrderId | string `optional` | Mã giao dịch của hệ thống đối tác |
| OrderRef | string `optional` | Ghi chú của đối tác khi thực hiện giao dịch |
| PSPCode | string `optional` | Mã trung gian thanh toán |
| Signature | string `required` | Ký số toàn bộ các trường đầu vào theo thứ tự alpha beta của tên biến input |

## Sample Request

```js
curl --location --request POST 'https://api.napas.com.vn/api/core/v1/transactions' --header 'Content-Type: application/json'
--data-raw '{
  "QueryInfo": {
    "FromDate": "14/12/2020 00:00:59",
    "ToDate": "16/12/2020 01:00:00",
    "TransactionStatus": "SUCCESS",
    "CardScheme": "AtmCard",
    "MerchantCode": "",
    "IssuerCode": "",
    "AccquireCode": "ACQ_VCB",
    "OrderId": "835560515",
    "OrderRef": "ORDER-2000010530",
    "PSPCode": "MOCA",
    "Signature": "AvJm1fRrsfeX3yZDnhZbrv5CPdU9TVUO7sSgEbglCh4_YhAh5kAn36mdauYucutqzp43DKbys2kaIbmx6cBsrGLr7gClOs1ecAgBbOQ7Sr6Kks2rTC5TUbGc0-QsHsiCPW9PnHr_ESUriv66y4b74ExIA7EWBKxx81XxG83RycPS97peiS0tUZeba0Gs4bU7SBjDSAoLzYu9e4fRgSJmMFnzK8BKIpd_hb1G2STPYoPyUMq1jj2sYS5XOv63X-qwSR1ljX83PYEKt8fY9oB2N7P34bMwd9y6h5yH_MyoT0UuIq_GTCY2rxhivS7D8-D-mhB-zHt9er4yphjSguJtcA"
    }
}'
```

## Example Response

```javascript
{
  "ResponseInf": {
    "Status": "SUCCESS"
  },
  "IntTranxList": [\
    {\
      "MerchantId": "9PAYWL"\
    }, {\
      "MerchantId": "9PAYWL",\
      "CardNumber": "KYtDwD1Tu4b2AqrXxoliFbnkFdAgvxl2JIxkVUuyUVHHCbQ23dLvIdDZypEmxk25/BwkJkNqMahYME6V",\
    }, {\
      "MerchantId": "9PAYWL",\
      "Token": "9704000177960018"\
    }\
  ],
  "Timestamp": "08/01/2022 14:35:51",
  "Signature": "G6sB0wyBJi_LdHXvmaW4DjSODI3unfyqTX38DD_SrkbANkPwZpwvq-iqw4t0hMFbwF4khziyR_jixoMIPj1ss8X5mTML1PES8nOaugzMVYVSTx1RPVUyTnc4iuMOdHNPZErieXWNHUv-LV3Xz8-AT44iylfsxPJkSzdJ7tujwgOUIWi21jurM4WUvxRaF97qnbtONFwbrCqWB6B2DVv86-2ML5lpPHlvdYYwrIMGwrV1s2dQkofPmEfgusWI1Xus3kQ0hagJWNxGwAI5BdU7D-leP8muly1H6isddozBZp5qlMk8OVnZ-Wg5W4zjdtut_kWcgRkGGp78vecFLcrgSw"
}
```

## Note

Để ký số đối tác sắp xếp giá trị truyền vào theo thứ tự dưới để tạo thành một chuỗi giá tri. Ký chuỗi theo SHA256 RSA key được cung cấp.

| STT | Field Name |
| --- | --- |
| 1 | AccquireCode |
| 2 | CardScheme |
| 3 | FromDate |
| 3 | IssuerCode |
| 4 | MerchantCode |
| 5 | OrderId |
| 6 | OrderRef |
| 7 | PSPCode |
| 8 | ToDate |
| 9 | TransactionStatus |

## API Response Codes

#### 200 Success

| Field | Type | Description |
| --- | --- | --- |
| ResponseInfo.Status | string `required` | Trả về SUCCESS nếu không có phát sinh lỗi hệ thống/lỗi nghiệp vụ, còn lại trả về ERROR |
| ResponseInfo.ErrCode | string `optional` | Mã lỗi phản hồi nếu có phát sinh lỗi |
| ResponseInfo.ErrDesc | string `optional` | Mô tả lỗi phản hồi nếu có phát sinh lỗi |
| DomTranxList.CardNumber | string `optional` | Số thẻ. Mã hóa sử dụng thuật toán mã hóa AES-GCM (256bit trở lên). Tham chiếu tại mục 3.9 Cách giải mã trường thông tin |
| DomTranxList.Amount | string `optional` | Số tiền thanh toán. |
| DomTranxList.IssuerCode | string `optional` | Mã ngân hàng phát hành thẻ. |
| DomTranxList.CardName | string `optional` | Tên chủ tài khoản. Mã hóa sử dụng thuật toán mã hóa AES-GCM (256 bit trở lên) . Tham chiếu tại mục 3.9 Cách giải mã trường thông tin |
| DomTranxList.SettleAmount | string `optional` | Số tiền quyết toán. |
| DomTranxList.BankID | string `optional` | ID ngân hàng. |
| DomTranxList.ExpDate | string `optional` | Tháng, năm hết hạn thẻ, MM/YYYY. Mã hóa sử dụng thuật toán mã hóa AES-GCM (256 bit trở lên). Tham chiếu tại mục 3.9 Cách giải mã trường thông tin |
| DomTranxList.Currency | string `optional` | Loại tiền giao dịch. |
| DomTranxList.TransactionRef | string `optional` | Ghi chú giao dịch. |
| DomTranxList.ResponseCode | string `optional` | Mã trạng thái của giao dịch. |
| DomTranxList.MerchantCode | string `optional` | Mã merchant. |
| DomTranxList.TransactionInfo | string `optional` | Thông tin giao dịch |
| DomTranxList.IP | string `optional` | Địa chỉ IP Local. |
| DomTranxList.ServiceType | string `optional` | Service type. Loại dịch vụ. |
| DomTranxList.CardFundMethod | string `optional` | Phương thức cấp vốn bằng thẻ. |
| IntTranxList | Object `optional` | Thông tin output nếu là giao dịch quốc tế. |
| IntTranxList.MerchantId | string `optional` | ID người bán |
| IntTranxList.AcquirerId | string `optional` | ID người mua. |
| IntTranxList.TransactionId | string `optional` | ID giao dịch. |
| IntTranxList.OrdId | string `optional` | ID đặt hàng. |
| IntTranxList.GroupId | string `optional` | Nhóm ID |
| IntTranxList.GatewayTranxId | string `optional` | ID cổng giao dịch. |
| IntTranxList.GatewayOrdId | string `optional` | ID cổng đơn đặt hàng. |
| IntTranxList.CreatedDate | Date `optional` | Định dạng DD/MM/YYYY HH24:MM:SS. |
| IntTranxList.UpdateDate | Date `optional` | Định dạng DD/MM/YYYY HH24:MM:SS. |
| IntTranxList.TransactionType | string `optional` | Loại giao dịch |
| IntTranxList.PaymentType | string `optional` | Hình thức thanh toán. |
| IntTranxList.ServiceCode | string `optional` | Mã dịch vụ. |
| IntTranxList.Amount | Number `optional` | Số tiền giao dịch. |
| IntTranxList.RefundAmount | Number `optional` | Số tiền hoàn lại vẫn còn. |
| IntTranxList.Currency | string `optional` | Loại tiền giao dịch. |
| IntTranxList.OrderRef | string `optional` | Đơn hàng tham khảo. |
| IntTranxList.BatchNumber | string `optional` | Sô chuyến |
| IntTranxList.AuthorizationCode | string `optional` | Mã ủy quyền. |
| IntTranxList.IP | string `optional` | Vị trí IP. |
| IntTranxList.Result | string `optional` | Kết quả |
| IntTranxList.GatewayCode | string `optional` | Mã cổng. |
| IntTranxList.ResponseCode | string `optional` | Mã phản hồi. |
| IntTranxList.ResponseDesc | string `optional` | Mô tả phản hồi. |
| IntTranxList.CardFundMethod | string `optional` | Phương thức cấp vốn bằng thẻ. |
| IntTranxList.CardNumber | string `optional` | Mã hóa sử dụng thuật toán mã hóa AES-GCM (256 bit trở lên). Tham chiếu tại mục 3.9 Cách giải mã trường thông tin |
| IntTranxList.CardBrand | string `optional` | Nhãn hiệu thẻ. |
| IntTranxList.ExpDate | string `optional` | Tháng, năm hết hạn thẻ, MM/YYYY. Mã hóa sử dụng thuật toán mã hóa AES-GCM (256 bit trở lên). Tham chiếu tại mục 3.9 Cách giải mã trường thông tin |
| IntTranxList.CSC | string `optional` | Kết quả CSC |
| IntTranxList.CSCResp | string `optional` | Phản hồi CSC |
| IntTranxList.Token | string `optional` | Mã Token |
| IntTranxList.TokenExp | string `optional` | Mã thông báo hết hạn. |
| IntTranxList.AuthStatus | string `optional` | Trạng thái xác thực |
| IntTranxList.EnrollStatus | string `optional` | Tình trạng tuyển sinh. |
| IntTranxList.XID | string `optional` | 3-D Secure XID. |
| IntTranxList.ECI | string `optional` | 3-D Secure ECI. |
| Timestamp | Date `required` | Ngày giờ hệ thống. DD/MM/YYYY HH24:MM:SS |
| Signature | string `required` | Ký số toàn bộ các trường đầu ra theo thứ tự alpha beta của tên biến output. Trường hợp output là mảng/đối tượng thì so sánh tên mảng/đối tượng với các biến cùng mức |

#### 400 Bad Request

Thao tác này trả về nội dung lỗi của hệ thống.

#### 401 Unauthorized

Token hết hạn hoặc không tồn tại.

#### 404 Not Found

Thao tác này trả về nội dung lỗi của hệ thống.

#### 500 Internal Server Error

Thao tác này trả về nội dung lỗi của hệ thống.

## API ListFiles

```js
POST: /api/core/v1/files
```

## Mục đích

Trả về danh sách tên file đối soát của đối tác.

## Header Parameters

| Parameters | Type | Description |
| --- | --- | --- |
| Authorization | string `required` | Mã thông báo ủy quyền gần đây nhất. Điều này sẽ có định dạng Bearer + {space} + {accessToken}. Ví dụ: Bearer KGNsaWVudF9pZDpjbGllbnRfc2VjcmV0KQ==. |

## Query Parameters

| Parameters | Type | Description |
| --- | --- | --- |
| ServiceCode | string `required` | Mã dịch vụ. Mặc định là ECOM |
| CreatedDate | string `required` | Ngày tạo file, định dạng DD/MM/YYYY |
| Signature | string `required` | Ký số toàn bộ các trường đầu vào theo thứ tự alpha beta của tên biến input |

## Sample Request

```js
curl --location --request POST 'https://api.napas.com.vn/api/core/v1/files' --header 'Content-Type: application/json'
--data-raw '{
  "InputInfo": {
  "ServiceCode": "ECOM",
  "Signature": "D4zHVsP1V9hMnKrwmlnx0iKH46rcFnaWThEFwIjsipFKWYpNzmeC8ykgPmPFdCCdXyDV3XC0isazN4b3xJFcjZ5_Qi2y1YEGnaVgb400Ejcrw9aeY5a1jEchdRkeEYLdi4NqMTue3VI5gdPS0m5X3KbodBo6iyXjGeXtP7Rbq6syI3-V4UEsLogyuUvqey1L4nbulnMsW85MDra_pEn7p55gQ06RbChMDWhpt-tOks7pxfaHImYdhiZLap4QRG-kCWZgsGRAwIaC1g4wtSegeasozeD7KmeLXSeccgpBwkQWHgkMjkQP15exEEun414F2o4a31hvQXoydUiIaGbTvQ",
  "CreatedDate": "17/12/2021"
  }
}'
```

## Example Response

```javascript
{
  "ResponseInf": {
    "Status": "SUCCESS"
  },
  "FileNames": {
    "FileName": [\
      ""\
    ]
  },
  "Timestamp": "07/01/2022 10:05:10",
  "Signature": "FcotlYePRGhrmGl7TI7mWd7VSHOMKzqkjEcCF7uNhBRJW8kjA86eYLZb-iv6pZzu9oFV0EpEZGGyEaVk6l_ViNBWpRyfnLNXfHKr1cqJcK6emQnBT5auwUN1DbDZk0gVjV_GQ6d08_2dCwpKrRhFCbRZ-FipkKxhScbQfp6ImhV0Xyz-GaUR20If6JswE0lIlxhh71aBBAUKSQSoorkpPRaOvwpR-KBYBItLZlGiW51EkLP-LPd5X600yJxKbhUxjV2PoE4g5Tj8EuJQ2AJMWevmUaUXlOPfUYeBC6Gdus5Uikn7Ac1H131z_GaDq0l1KVKatmynYo_rkI3mqw48fA"
}
```

## Note

Để ký số đối tác sắp xếp giá trị truyền vào theo thứ tự dưới để tạo thành một chuỗi giá tri. Ký chuỗi theo SHA256 RSA key được cung cấp.

| STT | Field Name |
| --- | --- |
| 1 | CreatedDate |
| 2 | ServiceCode |

## API Response Codes

#### 200 Success

| Field | Type | Description |
| --- | --- | --- |
| ResponseInfo.Status | string `required` | Trả về SUCCESS nếu không có phát sinh lỗi hệ thống/lỗi nghiệp vụ, còn lại trả về ERROR |
| ResponseInfo.ErrCode | string `optional` | Mã lỗi phản hồi nếu có phát sinh lỗi |
| ResponseInfo.ErrDesc | string `optional` | Mô tả lỗi phản hồi nếu có phát sinh lỗi |
| FileNames | Object `optional` | Danh sách tên file |
| FileNames.FileName | string `optional` | Tên file |
| Timestamp | Date `required` | Ngày giờ hệ thống. DD/MM/YYYY HH24:MM:SS |
| Signature | string `required` | Ký số toàn bộ các trường đầu ra theo thứ tự alpha beta của tên biến output. Trường hợp output là mảng/đối tượng thì so sánh tên mảng/đối tượng với các biến cùng mức |

#### 400 Bad Request

Thao tác này trả về nội dung lỗi của hệ thống.

#### 401 Unauthorized

Token hết hạn hoặc không tồn tại.

#### 404 Not Found

Thao tác này trả về nội dung lỗi của hệ thống.

#### 500 Internal Server Error

Thao tác này trả về nội dung lỗi của hệ thống.

## API GetFile

```js
POST: /api/core/v1/file
```

## Mục đích

Trả về file đối soát của đối tác.

## Header Parameters

| Parameters | Type | Description |
| --- | --- | --- |
| Authorization | string `required` | Mã thông báo ủy quyền gần đây nhất. Điều này sẽ có định dạng Bearer + {space} + {accessToken}. Ví dụ: Bearer KGNsaWVudF9pZDpjbGllbnRfc2VjcmV0KQ==. |

## Query Parameters

| Parameters | Type | Description |
| --- | --- | --- |
| FileName | string `required` | Tên file |
| Signature | string `required` | Ký số toàn bộ các trường đầu vào theo thứ tự alpha beta của tên biến input |

## Sample Request

```js
curl --location --request POST 'https://api.napas.com.vn/api/core/v1/file'
--header 'Content-Type: application/json'
--data-raw ' {
  "FileName": "512MB.zip",
  "Signature": "OqBuljg0iMDMSTgJhvyUTIVRWKtLnZ3jw6CWMleAE7ADQ9iWars9-eBH-GfehfDIGHdXzcnIX7IWNhU8qiXylAnyZyjatZIIxEKb_cQlN8OGGj4XjsCyUrP1PfRSnSQeTi1SgtWg69J1Ydp7QZZuGhjEb1DQDEzK3d_1YwDHHgRNvyJm8AfF1iCqFpMTRGd-Mg0LgwiErmQWs0mXCPjZLh6Lg71cmAOhYWzdIidz6SPk2R9MHvm1NLcLcl2Vdd-3T_ZHeNBv04UzDJlX8DzhaPPuspH5VjutWgXsCK3ekIN5vRRzq5eAMaY8ahpTs_VKEOUVkXm56roAq0QkeMFUag"
} '
```

## Example Response

```javascript
{
  "ResponseInf": {
    "Status": "SUCCESS"
  },
  "FileName": "10MB.zip.zip",
  "FileContent": ""nội dung base 64 của file (do nội dung file dài nên không thể copy được vào đây)"",
  "Timestamp": "07/01/2022 10:29:11",
  "Signature": "Y7mbwNt2K28daEcyYUxha7RFNvIgJ3ivlL3LUB6Eh7S2pVpcKPopF4besHMGgQdDbsh_OgbmII8YBl2pMf6N3azvMb_xY0rYYrjF_hJr21Re9bbLH1n6QmtV1dgkkLTVgfdLR8-L8rniFrdKqEZLC1m_hFJ9iFbGK-DjAglHqc4LMs5zG3sm7_AzlAixqDxqi106PcKDRMNkhLV9EoVUPdHhRLbBMvSH5h_IgpgmT377SjauA54Ls6xRJcgAMoC76m09wk7L8ALqbRhIjRxzHHqSAu9stZ0Jj8BBz2Bbiz2T9NzWwt9FOfiQqdlO7ShTw6snzX2_cJM_nye_Fxsy-Q"
}
```

## Note

Để ký số đối tác sắp xếp giá trị truyền vào theo thứ tự dưới để tạo thành một chuỗi giá tri. Ký chuỗi theo SHA256 RSA key được cung cấp.

| STT | Field Name |
| --- | --- |
| 1 | FileName |

## API Response Codes

#### 200 Success

| Field | Type | Description |
| --- | --- | --- |
| ResponseInfo.Status | string `required` | Trả về SUCCESS nếu không có phát sinh lỗi hệ thống/lỗi nghiệp vụ, còn lại trả về ERROR |
| ResponseInfo.ErrCode | string `optional` | Mã lỗi phản hồi nếu có phát sinh lỗi |
| ResponseInfo.ErrDesc | string `optional` | Mô tả lỗi phản hồi nếu có phát sinh lỗi |
| FileName | Object `optional` | Tên file |
| FileContent | string `optional` | Nội dung file base 64 đã được nén kiểu .zip để giảm tải dung lượng |
| Timestamp | Date `required` | Ngày giờ hệ thống. DD/MM/YYYY HH24:MM:SS |
| Signature | string `required` | Ký số toàn bộ các trường đầu ra theo thứ tự alpha beta của tên biến output, trừ trường FileContent |

#### 400 Bad Request

Thao tác này trả về nội dung lỗi của hệ thống.

#### 401 Unauthorized

Token hết hạn hoặc không tồn tại.

#### 404 Not Found

Thao tác này trả về nội dung lỗi của hệ thống.

#### 500 Internal Server Error

Thao tác này trả về nội dung lỗi của hệ thống.

## API GetMerchant

```js
POST: /api/core/v1/merchant
```

## Mục đích

Lấy thông tin cài đặt merchant.

## Header Parameters

| Parameters | Type | Description |
| --- | --- | --- |
| Authorization | string `required` | Mã thông báo ủy quyền gần đây nhất. Điều này sẽ có định dạng Bearer + {space} + {accessToken}. Ví dụ: Bearer KGNsaWVudF9pZDpjbGllbnRfc2VjcmV0KQ==. |

## Query Parameters

| Parameters | Type | Description |
| --- | --- | --- |
| MerchantCode | string `required` | Mã merchant, hệ thống kiểm tra đối tác truyền Merchant Code không phải của đối tác |
| Signature | string `required` | Ký số toàn bộ các trường đầu vào theo thứ tự alpha beta của tên biến input |

## Sample Request

```js
curl --location --request POST 'https://api.napas.com.vn/api/core/v1/merchant'
--header 'Content-Type: application/json'
--data-raw '{
  "MerchantCode": "merchantTest",
  "Signature": "O-gl4k4sfTwqRwE1ehz3nyR1h_v9J6OJixxDsNkEf-sJOzdye8zghYKzCMOKzRAEP8gsFEV2NX5FmUVbzSzLACECUxj5hdxsbWKZZhMWmxLg8hVaYfS3YphPdQQF8_bwaRpbb4os3eGaiD9sdgSrvDllLnbHFLbCRC5YDOFiqpvxqHpgLpFcbH69viQpWKamYw7Y9MBEUPjTkH0rHjxno0NbL0fIGEft4w_CY5BvKiHJDNaQlT00Pmri4eogf-Bjybu6nYuOecvdqnW9hD5gnefKkDo4-y6RQKoBPIdsss8u9XSjCiTVgG43IbPQt8ixcFWvRrbKI_QscVHCYKHsDg"
}'
```

## Example Response

```javascript
{
  "ResponseInf": {
    "Status": "SUCCESS"
  },
  "BasicInfo": {
    "MerchantName": "TEST TCTT WL",
    "IntAcquirer": "ACQ_STB"
  },
  "ServiceDetails": {
    "DomVersion": "ECOM3.0",
    "MerchantUsername": "TCTTWL",
    "PPT": "DMS",
    "DomCardToken": "MANY",
    "IntCardToken": "MANY"
  },
  "Domestic": {
    "ModelDom": "SERVER_HOSTED",
    "MerchantCode": "0002",
    "LocalAcqId": "06800000686",
    "EcomTranxType": "PURCHASE, REFUND, VERIFY_CARD, VERIFY_OTP",
    "TokeTranxType": "DELETE_TOKEN, PURCHASE, RETRIEVE, TOKEN, VERIFY_CARD, VERIFY_OTP",
    "Currency": "VND",
    "PostPay": "NONE"
  },
  "EcomIssuer": {
    "IssuerCode": "CTG",
    "AccountInfo": "6868"
  },
  "International": {
    "DeloyInt": "ALL",
    "MerchantCode": "4784",
    "MpgsId": "TESTTCTTWL",
    "TransactionType": "AUTHORIZE, CAPTURE, CHECK_3DS_ENROLLMENT, DELETE_TOKEN, PAY, PROCESS_ACS_RESULT, REFUND, RETRIEVE, TOKEN, VOID",
    "AuthService": "AUTOPAY, ONE-TIME",
    "AuthTransaction": "PayCard, PayCreate, PaySave, PayToken",
    "MpgsMso": ",,,,,,,"
  },
  "Timestamp": "07/01/2022 17:16:10",
  "Signature": "h-05GuwBXny5V9B4mGr98i-j6dCZkzHShV8jDGNDwtMLq2HtUpttMyeLHXWrBhS6mC2cfjpLXDOqbH53Ts7vtKvmQwyS7bb5z2BsUu9RsQ2yNPZfVqpLjqIfGJr44PLSQNmwpdfBIEJinv0CdEracPcZR7D80nk6yR7Mq-XgH6KGS-7VrNOZCthfSaAuF1ivk9Q7TdEAxPjEfF-TgBz5iiC8cfMEYBXABB1EYX4-2Wiy2skp4fd1c0RZqROIUxEDGNvYnuuGnTDkTdmWeuZvIlmReq5xDJFGvI-cIa-iDUR6HQa3QqtAqpGA1SpC2BWTrYoDfY5XWEqh4hUnnyQ2Nw"
}
```

## Note

Để ký số đối tác sắp xếp giá trị truyền vào theo thứ tự dưới để tạo thành một chuỗi giá tri. Ký chuỗi theo SHA256 RSA key được cung cấp.

| STT | Field Name |
| --- | --- |
| 1 | MerchantCode |

## API Response Codes

#### 200 Success

Thành công.

| Field | Type | Description |
| --- | --- | --- |
| ResponseInfo.Status | string `required` | Trả về SUCCESS nếu không có phát sinh lỗi hệ thống/lỗi nghiệp vụ, còn lại trả về ERROR |
| ResponseInfo.ErrCode | string `optional` | Mã lỗi phản hồi nếu có phát sinh lỗi |
| ResponseInfo.ErrDesc | string `optional` | Mô tả lỗi phản hồi nếu có phát sinh lỗi |
| BasicInfo.MerchantName | Object `optional` | Tên đối tác |
| BasicInfo.PSPCode | string `optional` | Nhà cung cấp dịch vụ thanh toán |
| BasicInfo.MerCode | string `optional` | Mã Merchant |
| BasicInfo.BusinessName | string `optional` | Tên doanh nghiệp |
| BasicInfo.Page | string `optional` | Trang chủ |
| BasicInfo.Tel | string `optional` | Điện thoại |
| BasicInfo.Fax | string `optional` | Số Fax |
| BasicInfo.Email | string `optional` | Email |
| BasicInfo.GoliveDate | string `optional` | Ngày triển khai |
| BasicInfo.Category | string `optional` | Loại |
| BasicInfo.EncryStatus | string `optional` | Trạng thái mã hóa |
| BasicInfo.City | string `optional` | Thành phố |
| BasicInfo.PartnerID | string `optional` | ID đối tác |
| BasicInfo.DomesAcq | string `optional` | Khách hàng trong nước |
| BasicInfo.IntAcquirer | string `optional` | Khách hàng quốc tế |
| BasicInfo.Service | string `optional` | Dịch vụ |
| BasicInfo.MerchantContractName | string `optional` | Tên người bán hợp đồng |
| BasicInfo.MerchantContractCode | string `optional` | Mã người bán hợp đồng |
| ServiceDetails.DomVersion | string `optional` | Phiên bản Dom |
| ServiceDetails.MerchantUsername | string `optional` | Tên người dùng của người bán |
| ServiceDetails.ClientSecret | string `optional` | Mật mã máy khách |
| ServiceDetails.PPN3.0 | string `optional` | IPN 3.0 |
| ServiceDetails.IPNUrl | string `optional` | IPN url |
| ServiceDetails.LocalDefault | string `optional` | Mặc định cục bộ |
| ServiceDetails.UrlLogoDesk | string `optional` | Biểu tượng máy tính bàn |
| ServiceDetails.UrlLogoMobi | string `optional` | Biểu tượng di động |
| ServiceDetails.UrlIssuer | string `optional` | Biểu trưng của nhà phát hành url |
| ServiceDetails.IpUrlRsa | string `optional` | Ipn url Rsa |
| ServiceDetails.Currency | string `optional` | Tiền tệ |
| ServiceDetails.ReportSubAcc | string `optional` | Báo cáo bằng tài khoản phụ |
| ServiceDetails.CardType | string `optional` | Loại thẻ |
| ServiceDetails.MerAlias | string `optional` | Bí danh người bán |
| ServiceDetails.OrderExp | string `optional` | Đơn đặt hàng đã hết hạn |
| ServiceDetails.TokenStatus | string `optional` | Mã thông báo sử dụng Trạng thái |
| ServiceDetails.TokenFormat | string `optional` | Định dạng mã thông báo |
| ServiceDetails.VerificationType | string `optional` | Loại xác minh |
| ServiceDetails.VerificationStrategy | string `optional` | Chiến lược xác minh |
| ServiceDetails.Respository | string `optional` | Kho chứa |
| ServiceDetails.PPT | string `optional` | PPT |
| ServiceDetails.DomCardToken | string `optional` | Mã thông báo thẻ nội địa |
| ServiceDetails.IntCardToken | string `optional` | Mã thông báo thẻ quốc tế |
| Domestic | Object `optional` |  |
| Domestic.ModelDom | string `optional` | Triển khai mô hình dom |
| Domestic.MerchantCode | string `optional` | Mã danh mục người bán |
| Domestic.LocalAcqId | string `optional` | ID ACQ cục bộ |
| Domestic.EcomTranxType | string `optional` | Loại giao dịch ecom |
| Domestic.TokeTranxType | string `optional` | Loại giao dịch mã thông báo |
| Domestic.Currency | string `optional` | Tiền tệ mặc định |
| Domestic.WhitelistCard | string `optional` | Kiểm tra thẻ danh sách trắng |
| Domestic.PostPay | string `optional` | Trả tiền sau |
| Domestic.FastpayAmount | string `optional` | Fastpay Số tiền tối đa |
| Domestic.EcomIssuer.IssuerCode | string `optional` | Người phát hành |
| Domestic.EcomIssuer.AccountInfo | string `optional` | Thông tin tài khoản |
| Domestic.EcomIssuer.WhilelistCard | string `optional` | Thẻ danh sách trắng |
| International | Object `optional` |  |
| International.DeloyInt | string `optional` | Triển khai mô hình Int |
| International.MerchantCode | string `optional` | Mã danh mục người bán |
| International.MpgsId | string `optional` | ID người bán MPGS |
| International.MpgsAccount | string `optional` | ID tài khoản người mua MPGS |
| International.TransactionType | string `optional` | Loại giao dịch |
| Domestic.AuthService | string `optional` | Dịch vụ xác thực |
| Domestic.AuthTransaction | string `optional` | Xác thực truyền tải |
| Domestic.MpgsMso | string `optional` | MPGS MSO |
| Timestamp | Date `required` | Ngày giờ hệ thống. DD/MM/YYYY HH24:MM:SS |
| Signature | string `required` | Ký số toàn bộ các trường đầu ra theo thứ tự alpha beta của tên biến output. Trường hợp output là mảng/đối tượng thì so sánh tên mảng/đối tượng với các biến cùng mức |

#### 400 Bad Request

Thao tác này trả về nội dung lỗi của hệ thống.

#### 401 Unauthorized

Token hết hạn hoặc không tồn tại.

#### 404 Not Found

Thao tác này trả về nội dung lỗi của hệ thống.

#### 500 Internal Server Error

Thao tác này trả về nội dung lỗi của hệ thống.

## API GetMerchants

```js
POST: /api/core/v1/merchants
```

## Mục đích

Lấy thông tin cài đặt nhiều merchant dựa trên thông tin merchant cha.

## Path Parameters

| Parameters | Type | Description |
| --- | --- | --- |
| merchantId | string `required` | MerchantId của Merchant. Duy nhất với mỗi Merchant. Dữ liệu gồm các ký tự: 0-9, a-z, A-Z |
| orderid | string `required` | Mã đơn hàng là duy nhất trong suốt quá trình sử dụng API |

## Header Parameters

| Parameters | Type | Description |
| --- | --- | --- |
| Authorization | string `required` | Mã thông báo ủy quyền gần đây nhất. Điều này sẽ có định dạng Bearer + {space} + {accessToken}. Ví dụ: Bearer KGNsaWVudF9pZDpjbGllbnRfc2VjcmV0KQ==. |

## Query Parameters

| Parameters | Type | Description |
| --- | --- | --- |
| Signature | string `required` | Ký số token gọi API |

## Sample Request

```js
curl --location --request POST 'https://api.napas.com.vn/api/core/v1/merchants'
--header 'Content-Type: application/json'
--data-raw '{
  "Signature": "O-gl4k4sfTwqRwE1ehz3nyR1h_v9J6OJixxDsNkEf-sJOzdye8zghYKzCMOKzRAEP8gsFEV2NX5FmUVbzSzLACECUxj5hdxsbWKZZhMWmxLg8hVaYfS3YphPdQQF8_bwaRpbb4os3eGaiD9sdgSrvDllLnbHFLbCRC5YDOFiqpvxqHpgLpFcbH69viQpWKamYw7Y9MBEUPjTkH0rHjxno0NbL0fIGEft4w_CY5BvKiHJDNaQlT00Pmri4eogf-Bjybu6nYuOecvdqnW9hD5gnefKkDo4-y6RQKoBPIdsss8u9XSjCiTVgG43IbPQt8ixcFWvRrbKI_QscVHCYKHsDg"
}'
```

## Example Response

```javascript
{
  "ResponseInf": {
    "Status": "SUCCESS"
  },
  "MerchantList": [\
    {\
      "BasicInfo": {\
        "DomAcquier": "ACQ_VCB"\
      },\
      "ServiceDetails": {\
        "DomCardToken": "MANY",\
        "IntCardToken": "MANY"\
      },\
      "Domestic": {\
        "ModelDom": "SERVER_HOSTED",\
        "MerchantCode": "0002"\
      },\
      "EcomIssuer": {\
        "WhitelistCard": ""\
      },\
      "International": {\
        "MerchantCode": "4784"\
      }\
    }\
  ],
  "Timestamp": "08/01/2022 19:03:02",
  "Signature": "JWSiz-h0YAiS4bAzxLBjPGisVCQdoCx16skQ9gs2PGWoC_cT5UJyUfK_TMOMKagdEisxW4eEa5MkV984NJbHIbKvXHGCpTqwceI2GRbXjq6fhOeG51Uj4jfQuAo-4rUE61_r0qU7JHiHOmQTUWEDwtUWyuPqCMa-aLX9SsBWVd8Qki50AWhfKZkS1HEwODG1P7h9CcRq2r6jMA1BchZcnSgIfhj2KnyMKdiK02jZ9FouJMEv5eKkM9C0jHhq_TSXCie142KkF_9YlH_cK_6_YVSnpgGZzcWPSHvcCwAzxvUuPXbxQQx4YMZc9o73u34zsL_eRt7jpixRnSYvJDCXkQ"
}
```

## API Response Codes

#### 200 Success

| Field | Type | Description |
| --- | --- | --- |
| ResponseInfo.Status | string `required` | Trả về SUCCESS nếu không có phát sinh lỗi hệ thống/lỗi nghiệp vụ, còn lại trả về ERROR |
| ResponseInfo.ErrCode | string `optional` | Mã lỗi phản hồi nếu có phát sinh lỗi |
| ResponseInfo.ErrDesc | string `optional` | Mô tả lỗi phản hồi nếu có phát sinh lỗi |
| BasicInfo.MerchantName | Object `optional` | Tên đối tác |
| BasicInfo.PSPCode | string `optional` | Nhà cung cấp dịch vụ thanh toán |
| BasicInfo.MerchantCode | string `optional` | Mã Merchant |
| BasicInfo.BusinessName | string `optional` | Tên doanh nghiệp |
| BasicInfo.Page | string `optional` | Trang chủ |
| BasicInfo.Tel | string `optional` | Điện thoại |
| BasicInfo.Fax | string `optional` | Số Fax |
| BasicInfo.Email | string `optional` | Email |
| BasicInfo.GoliveDate | string `optional` | Ngày triển khai |
| BasicInfo.Category | string `optional` | Loại |
| BasicInfo.City | string `optional` | Thành phố |
| BasicInfo.PartnerID | string `optional` | ID đối tác |
| BasicInfo.DomAcquier | string `optional` | Khách hàng trong nước |
| BasicInfo.IntAcquirer | string `optional` | Khách hàng quốc tế |
| BasicInfo.Service | string `optional` | Dịch vụ |
| BasicInfo.MerchantContractName | string `optional` | Tên người bán hợp đồng |
| BasicInfo.MerchantContractCode | string `optional` | Mã người bán hợp đồng |
| ServiceDetails.DomVersion | string `optional` | Phiên bản Dom |
| ServiceDetails.MerchantUser | string `optional` | Tên người dùng của người bán |
| ServiceDetails.PPN3.0 | string `optional` | IPN 3.0 |
| ServiceDetails.IPNUrl | string `optional` | IPN url |
| ServiceDetails.LocalDefault | string `optional` | Mặc định cục bộ |
| ServiceDetails.UrlLogoDesk | string `optional` | Biểu tượng máy tính bàn |
| ServiceDetails.UrlLogoMobi | string `optional` | Biểu tượng di động |
| ServiceDetails.UrlIssuer | string `optional` | Biểu trưng của nhà phát hành url |
| ServiceDetails.IpUrlRsa | string `optional` | Ipn url Rsa |
| ServiceDetails.Currency | string `optional` | Tiền tệ |
| ServiceDetails.ReportSubAcc | string `optional` | Báo cáo bằng tài khoản phụ |
| ServiceDetails.CardType | string `optional` | Loại thẻ |
| ServiceDetails.MerchantAlias | string `optional` | Bí danh người bán |
| ServiceDetails.OrderExp | string `optional` | Đơn đặt hàng đã hết hạn |
| ServiceDetails.TokenStatus | string `optional` | Mã thông báo sử dụng Trạng thái |
| ServiceDetails.TokenFormat | string `optional` | Định dạng mã thông báo |
| ServiceDetails.VerificationType | string `optional` | Loại xác minh |
| ServiceDetails.VerificationStrategy | string `optional` | Chiến lược xác minh |
| ServiceDetails.Respository | string `optional` | Kho chứa |
| ServiceDetails.PPT | string `optional` | PPT |
| ServiceDetails.DomCardToken | string `optional` | Mã thông báo thẻ nội địa |
| ServiceDetails.IntCardToken | string `optional` | Mã thông báo thẻ quốc tế |
| Domestic | Object `optional` |  |
| Domestic.ModelDom | string `optional` | Triển khai mô hình dom |
| Domestic.MerchantCode | string `optional` | Mã danh mục người bán |
| Domestic.LocalAcqId | string `optional` | ID ACQ cục bộ |
| Domestic.EcomTranxType | string `optional` | Loại giao dịch ecom |
| Domestic.TokeTranxType | string `optional` | Loại giao dịch mã thông báo |
| Domestic.Cur | string `optional` | Tiền tệ mặc định |
| Domestic.WhitelistCard | string `optional` | Kiểm tra thẻ danh sách trắng |
| Domestic.PostPay | string `optional` | Trả tiền sau |
| Domestic.FastpayAmount | string `optional` | Fastpay Số tiền tối đa |
| Domestic.EcomIssuer.IssuerCode | string `optional` | Người phát hành |
| Domestic.EcomIssuer.AccountInfo | string `optional` | Thông tin tài khoản |
| Domestic.EcomIssuer.WhilelistCard | string `optional` | Thẻ danh sách trắng |
| International | Object `optional` |  |
| International.DeloyInt | string `optional` | Triển khai mô hình Int |
| International.MerchantCode | string `optional` | Mã danh mục người bán |
| International.MpgsId | string `optional` | ID người bán MPGS |
| International.MpgsAccount | string `optional` | ID tài khoản người mua MPGS |
| International.TransactionType | string `optional` | Loại giao dịch |
| Domestic.AuthService | string `optional` | Dịch vụ xác thực |
| Domestic.AuthTransaction | string `optional` | Xác thực truyền tải |
| Domestic.MpgsMso | string `optional` | MPGS MSO |
| Timestamp | Date `required` | Ngày giờ hệ thống. DD/MM/YYYY HH24:MM:SS |
| Signature | string `required` | Ký số toàn bộ các trường đầu ra theo thứ tự alpha beta của tên biến output. Trường hợp output là mảng/đối tượng thì so sánh tên mảng/đối tượng với các biến cùng mức |

#### 400 Bad Request

Thao tác này trả về nội dung lỗi của hệ thống.

#### 401 Unauthorized

Token hết hạn hoặc không tồn tại.

#### 404 Not Found

Thao tác này trả về nội dung lỗi của hệ thống.

#### 500 Internal Server Error

Thao tác này trả về nội dung lỗi của hệ thống.

## API GenerateQR

```js
POST: /api/core/v1/gen-viet-qr
```

## Mục đích

Tạo mã QR.

## Query Parameters

| Parameters | Type | Description |
| --- | --- | --- |
| BenId | Number `required` | Mã ngân hàng thụ hưởng. Chỉ cho số.(0-9) |
| AcountNumber | Number `required` | Số tài khoản thụ hưởng. Chỉ cho số |
| AccountName | string `required` | Tên tài khoản thụ hưởng a-Z,0-9 Có dấu cách, không dấu |
| Amount | Number `optional` | Số tiền chuyển. Chỉ số, không có dấu thập phân |
| Remark | string `optional` | Nội dung chuyển khoản a-Z,0-9. Cho dấu cách |
| IsMask | Boolean `required` | Option cho phép ẩn số tài khoản. True: ẩn. False: không ẩn |
| Logo | `optional` | Truyền logo ảnh dạng form data. Logo ảnh dung lượng <1MB. Theo chuẩn định dạng ảnh JPG/PNG/JPEG. |
| Signature | string `required` | Ký số toàn bộ các trường đầu vào theo thứ tự alpha beta của tên biến input. Logo ký trên nội dung base 64 của logo |

## Sample Request

```js
curl --location --request POST --url https://api.napas.com.vn/api/core/v1/gen-viet-qr
  --header `Content-Type: application/json`
  --data-raw `--form BenId="970416"
  --form AcountNumber="123456"
  --form AccountName="nguyen"
  --form Amount=""
  --form Remark="ck tien hang"
  --form IsMask="true"
  --form Logo=@"/path/to/file"
  --form Signature="EgaBfzesQYiJ_x-1ddoZ5ychTK4Egkyc6gRBOvzpmgKeVriW9te_7H5gfvWansH4d6uKuvq4WjdnJrFqGrdi3RDlIyiC8rr2YqU85ZyUtwGiHppJWV1AnX_7XBq-81AmclenTHEAz156iQevx0r3IFfq-sHO5j5DeYWXqpq-jmQ9hC7wIuIA5hCrPhhIklpYkZAxHYg9CZ7MoAzYsF0iltLd-s1MZwiNJu7qdT-smQ9HrJln9hIBfhQqnBX2r6XD3YMzxdpWpXP_hcxvQNNyussl86MlqzkbFIsKlzHqSzeOJL6mVPN8FaXV1JWZL6R0MAPv1i-CrVJLJeWrdAdzeeQ"`
```

## Example Response

```javascript
{
  "ResponseInf": {
    "Status": "SUCCESS"
  },
  "Result": "nội dung base 64 của ảnh Qrcode (do nội dung Qrcode dài nên không thể copy được vào đây)",
  "Timestamp": "12/01/2022 09:56:19",
  "Signature": "fSaWuPgNNdaxiao4cV4c0X2gWEjAKv7JHMjuwN2b8ZaAJqJYvfhKpm8xkCjvTRaQxh_EXZ15PwOJefirpv796bQeW7gP5YJltxt1WNGFOHcdEc1wCvmBoj2x92GZciP5lUhXhDCSZsWLad7Xc2_s9pri3nnCZ9XksM9mwg3P0OfvcbplDh2kNhhaYQD2RBCv_SLvGUSvqVWV2gY4sr6EcWKTQIa2ZrSXKZrFHA4sYJ4eErgHyCc5MfpcwaYWkRWj-Agylgz6fzhMY5WuRV8h5Gf2vYwBW8E1FXNPTmY9F-21kHBb78D8VaAE1lLsU91Le4ixeRY9mAV7KzphsbvlNw"
}
```

## Note

Để ký số đối tác sắp xếp giá trị truyền vào theo thứ tự dưới để tạo thành một chuỗi giá tri. Ký chuỗi theo SHA256 RSA key được cung cấp.

| STT | Field Name |
| --- | --- |
| 1 | AccountName |
| 2 | AcountNumber |
| 3 | Amount |
| 4 | BenId |
| 5 | IsMask |
| 6 | Logo |
| 6 | Remark |

## API Response Codes

#### 200 Success

| Field | Type | Description |
| --- | --- | --- |
| ResponseInfo.Status | string `required` | Trả về SUCCESS nếu không có phát sinh lỗi hệ thống/lỗi nghiệp vụ, còn lại trả về ERROR |
| ResponseInfo.ErrCode | string `optional` | Mã lỗi phản hồi nếu có phát sinh lỗi |
| ResponseInfo.ErrDesc | string `optional` | Mô tả lỗi phản hồi nếu có phát sinh lỗi |
| Kết quả | Base64 `required` |  |
| Timestamp | Date `required` | Ngày giờ hệ thống. DD/MM/YYYY HH24:MM:SS |
| Signature | string `required` | Ký số toàn bộ các trường đầu ra theo thứ tự alpha beta của tên biến output, trừ trường Result |

#### 400 Bad Request

Thao tác này trả về nội dung lỗi của hệ thống.

#### 401 Unauthorized

Token hết hạn hoặc không tồn tại.

#### 404 Not Found

Thao tác này trả về nội dung lỗi của hệ thống.

#### 500 Internal Server Error

Thao tác này trả về nội dung lỗi của hệ thống.

## API GetVietQRInfo

```js
POST: /api/core/v1/get-vietqr-info
```

## Mục đích

Trả về danh sách thông tin tài khoản mà khách hàng đã tạo mã VietQR trên trang vietqr.net theo khung thời gian xác định.

## Query Parameters

| Parameters | Type | Description |
| --- | --- | --- |
| BenId | string `required` | Mã ngân hàng thụ hưởng. Chỉ cho số.(0-9) |
| FromDate | string `required` | Ngày từ định dạng DD/MM/YYYY HH24:MM:SS |
| ToDate | string `required` | Ngày đến định dạng DD/MM/YYYY HH24:MM:SS Thời gian từ ngày đến ngày giới hạn trong 1 tháng (tham số cấu hình) Thời gian tìm kiếm trong vòng 360 ngày cho thời điểm giao dịch (tham số cấu hình) |
| Signature | string `required` | Ký số toàn bộ các trường đầu vào theo thứ tự alpha beta của tên biến input |

## Sample Request

```js
curl --location 'https://developer.napas.com.vn/api/core/v1/get-vietqr-info' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer p8tn8m4gqkeeak2r7wwq7y8n' \
--data '{
  "BenId" : "970434",
  "FromDate": "01/06/2023 06:40:00",
  "ToDate": "01/06/2023 06:50:01",
  "Signature": "S4NZ5xMsZzxJZXcmczKbcVPLBSXQWwGKEi6TTQ0wqVJ7v0KHvNS//Y+ssdq9fktyqQuMtxW2yQqS68AjgO3sTVUiPYPLnoqA+bxm93kUAjSM8BteWyqk19yvNnQi6fqEmvbDH+i/jrP3yeCgUt6L328Dh5rb8z1iCywLynJiCyGsMhSvkxFQnUlsPGuf9yQaK8CzE/fr4uKE81x+yDqofhzrZm583ntwG+3ICzo0BG5Tsbv+XH/J5kLc//EOS/mHC7FvG/ZiMYG74S3fMNkwBGOfGhDuDbD+wwxTiBmEIO4Lo3Xbm4AE9oIYqmP6BzctAmfhT8e9urMaCqk4dfbkAg=="
}'
```

## Example Response

```javascript
{
  "ResponseInfo": {
  "Status": "SUCCESS"
},
  "Results": [\
  {\
    "VietQRInfo": {\
      "Bin": "970434",\
      "AccountNo": "xxxxxx9001",\
      "AccountName": "CTY CP GIAI PHAP THANH TOAN VIET NAM - C/A - VND",\
      "CreateDate": "2023-06-01T06:41:19+07:00"\
    }\
  },\
  {\
    "VietQRInfo": {\
      "Bin": "970434",\
      "AccountNo": "xxxxxx9005",\
      "AccountName": "CTY CP GIAI PHAP THANH TOAN VN-C/A-VND",\
      "CreateDate": "2023-06-01T06:46:01+07:00"\
    }\
  },\
  {\
    "VietQRInfo": {\
      "Bin": "970434",\
      "AccountNo": "xxxxxx9006",\
      "AccountName": "CTY CP GIAI PHAP THANH TOAN VN-VND-C/A",\
      "CreateDate": "2023-06-01T06:47:12+07:00"\
    }\
  }\
],
  "Timestamp": "02/08/2023 09:50:22",
  "Signature": "b+5/tNFaS33+e0HxaXu95liHx95bgZsLdf5JtYTYwpA7BtpvtMDTZ3gh6mOUjcQQ6xLBJF4DhGr52mip8L8Qf+x1bPCE4fSgMmSair6CI5sayfLE3djGpCn1VUlH+TRiMUa4KBmFSUW0p/9tF4HR4sTI7IHGcWMWabq7+xfXgPXrSs5nly7h7/+hE6NumjfZ2KXZd8p9IePF2a2GWgslTNEx5/+QyaesKUwn4fcuz0y5GN/6Zu4WnaaQPzxmUcJ83Q06+m0NoamJ4o5ZPu230nyAyQlynTSDkwsctuO8fprDr26YetKHEvCdkeIRtd3qK1d/Ix4WMZpaYgqtKSaUdw=="
}
```

## Note

Để ký số đối tác sắp xếp giá trị truyền vào theo thứ tự dưới để tạo thành một chuỗi giá tri. Ký chuỗi theo SHA256 RSA key được cung cấp.

| STT | Field Name |
| --- | --- |
| 1 | BenId |
| 2 | FromDate |
| 3 | ToDate |

## API Response Codes

#### 200 Success

Thành công.

| Field | Type | Description |
| --- | --- | --- |
| ResponseInfo |  |  |
| ResponseInfo.Status | string `required` | "SUCCESS" |
| Results |  |  |
| Results.VietQRInfo |  |  |
| VietQRInfo.Bin | string `required` | Mã ngân hàng thụ hưởng. |
| VietQRInfo.AccountNo | string `required` | Số tài khoản thụ hưởng. |
| VietQRInfo.AccountName | string `required` | Tên tài khoản thụ hưởng. |
| VietQRInfo.Amount | string `optional` | Số tiền chuyển. |
| VietQRInfo.AddInfo | string `optional` | Nội dung chuyển khoản. |
| VietQRInfo.CreateDate | string `required` | Ngày tạo. Định dạng DD/MM/YYYY HH24:MM:SS. |
| Timestamp | Date `required` | Ngày giờ hệ thống. DD/MM/YYYY HH24:MM:SS |
| Signature | string `required` | Ký số toàn bộ các trường đầu ra theo thứ tự alpha beta của tên biến output. Trường hợp output là mảng/đối tượng thì so sánh tên mảng/đối tượng với các biến cùng mức |

#### 400 Bad Request

| Field | Description |
| --- | --- |
| statusCode | 400 |
| message | FromDate is null |
| \-\-\---- | FromDate wrong formats DD/MM/YYYY HH24:MM:SS |
| \-\-\---- | FromDate over 3 months with today |
| \-\-\---- | FromDate cannot be a future date |
| \-\-\---- | ToDate is null |
| \-\-\---- | ToDate wrong formats DD/MM/YYYY HH24:MM:SS |
| \-\-\---- | ToDate over 3 months with today |
| \-\-\---- | ToDate cannot be a future date |
| \-\-\---- | ToDate must not be less than or equal to FromDate |
| \-\-\---- | Signature could not be verified |
| \-\-\---- | No authorization |
| \-\-\---- | BenId does not exist |
| \-\-\---- | BenId wrong formats (0-9) |
| \-\-\---- | BenId exceeds the max length |

#### 500 Internal Server Error

| Field | Description |
| --- | --- |
| statusCode | 500 |
| message | Internal server error |

#### Nhóm Napas Digital Payment Platform

![](https://api.napas.com.vn/files/assets/img/pp_security_2.png)

Bạn cần đăng nhập hoặc không có quyền để xem tài liệu này

![](https://api.napas.com.vn/files/assets/img/must-login.png)

#### Nhóm Napas Digital Payment Platform

![](https://api.napas.com.vn/files/assets/img/pp_security_2.png)

Bạn cần đăng nhập hoặc không có quyền để xem tài liệu này

![](https://api.napas.com.vn/files/assets/img/must-login.png)

#### Nhóm Napas Digital Payment Platform

![](https://api.napas.com.vn/files/assets/img/pp_security_2.png)

Bạn cần đăng nhập hoặc không có quyền để xem tài liệu này

![](https://api.napas.com.vn/files/assets/img/must-login.png)

#### Nhóm DPP API Specification For Issuer (Part 01 - Payment)

![](https://api.napas.com.vn/files/assets/img/pp_security_2.png)

Bạn cần đăng nhập hoặc không có quyền để xem tài liệu này

![](https://api.napas.com.vn/files/assets/img/must-login.png)

#### Nhóm DPP API Specification For Issuer (Part 01 - Payment)

![](https://api.napas.com.vn/files/assets/img/pp_security_2.png)

Bạn cần đăng nhập hoặc không có quyền để xem tài liệu này

![](https://api.napas.com.vn/files/assets/img/must-login.png)

#### Nhóm Napas API Specification DPP Token Vault (Part 02 - Payment)

![](https://api.napas.com.vn/files/assets/img/pp_security_2.png)

Bạn cần đăng nhập hoặc không có quyền để xem tài liệu này

![](https://api.napas.com.vn/files/assets/img/must-login.png)

#### Nhóm Open Banking

![](https://api.napas.com.vn/files/assets/img/pp_security_2.png)

Bạn cần đăng nhập hoặc không có quyền để xem tài liệu này

![](https://api.napas.com.vn/files/assets/img/must-login.png)