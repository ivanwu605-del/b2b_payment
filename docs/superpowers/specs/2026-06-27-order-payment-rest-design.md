# Order And Payment REST API Design

## Goal

Add real HTTP REST APIs for creating orders and payments in the b2b_payment project.
Both APIs persist data to MySQL and update the existing Redis sliding-window payment statistics.

## Scope

Implement two endpoints:

- `POST /orders` creates an order in `t_order`.
- `POST /payments` creates a payment in `t_payment_transaction`.

The APIs should follow the existing project model: JDBC repositories own database writes, and
`B2bPaymentRatioService` owns Redis statistic updates.

## API Contract

### Create Order

`POST /orders`

Request:

- `merchantId`: merchant identifier.
- `outTradeNo`: merchant-side trade number.
- `amount`: order amount in the smallest currency unit.
- `currency`: currency code.

Behavior:

- Generate `orderId` on the server.
- Persist the order with status `CREATED`.
- Set `createTime` and `updateTime` to the current time.
- Call `B2bPaymentRatioService.onOrderCreated(orderId, createTime)` after the database write.
- Return the created order.

### Create Payment

`POST /payments`

Request:

- `orderId`: order to pay.
- `merchantId`: merchant identifier.
- `channel`: payment channel.
- `status`: payment status.

Behavior:

- Look up the order before creating the payment.
- If the order does not exist, return `404` and do not write a payment record.
- If the order exists but belongs to another merchant, return `400` and do not write a payment record.
- Generate `transactionId` on the server.
- Persist the payment.
- If `status` is `SUCCESS`, call `B2bPaymentRatioService.onPaymentSuccess(transactionId, createTime)`.
- Return the created payment.

## Architecture

Use a thin web layer and small application services:

- Controllers handle HTTP request/response mapping.
- Request records represent input payloads.
- Application services perform validation, ID generation, database writes, and Redis statistic updates.
- Repositories remain focused on JDBC access.
- A lightweight exception handler maps business errors to JSON HTTP responses.

`spring-boot-starter-web` is required because the project currently has no Web starter.

## Error Handling

Return JSON errors in the shape:

```json
{
  "message": "Order not found"
}
```

Expected status codes:

- `400 Bad Request` for malformed business input, including merchant mismatch.
- `404 Not Found` for missing orders.

## Testing

Add focused tests around the application service behavior:

- Creating an order writes MySQL and updates Redis order statistics.
- Creating a successful payment writes MySQL and updates Redis payment statistics.
- Creating a payment for a missing order fails and does not write payment data.
- Creating a payment with a merchant mismatch fails and does not write payment data.

Existing Redis/MySQL b2b_payment tests remain valid and should continue to pass when the required local services are running.
