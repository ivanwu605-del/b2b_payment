# Merchant CRUD And Association Design

## Goal

Add merchant management to the b2b_payment project. Merchants become the source of truth for
`merchant_id` values used by orders and payment transactions.

## Scope

Implement a merchant table and REST APIs for merchant CRUD:

- `POST /merchants`
- `GET /merchants/{merchantId}`
- `GET /merchants`
- `PUT /merchants/{merchantId}`
- `DELETE /merchants/{merchantId}`

Also update order and payment creation so both require an existing active merchant.

## Data Model

Add table `t_merchant`:

- `merchant_id varchar(64) primary key`
- `merchant_name varchar(128) not null`
- `status varchar(32) not null`
- `create_time datetime(6) not null`
- `update_time datetime(6) not null`

Supported merchant statuses:

- `ACTIVE`: merchant can create orders and payments.
- `DISABLED`: merchant exists but cannot create new orders or payments.

Update existing tables:

- `t_order.merchant_id` references `t_merchant.merchant_id`.
- `t_payment_transaction.merchant_id` references `t_merchant.merchant_id`.
- Keep existing order uniqueness constraint `uk_merchant_out`.
- Keep existing payment uniqueness constraint `uk_channel_txn`.

Because existing local databases may already contain order/payment rows, schema setup should create
or seed merchants before adding foreign keys when applying the SQL manually.

## API Contract

### Create Merchant

`POST /merchants`

Request:

- `merchantName`: required merchant display name.
- `status`: optional; defaults to `ACTIVE` when absent.

Behavior:

- Generate `merchantId` on the server with prefix `mch_`.
- Persist the merchant.
- Return the created merchant.

### Get Merchant

`GET /merchants/{merchantId}`

Behavior:

- Return the merchant when found.
- Return `404` when not found.

### List Merchants

`GET /merchants`

Behavior:

- Return merchants ordered by `create_time`.
- No pagination in the first version because the demo data set is small.

### Update Merchant

`PUT /merchants/{merchantId}`

Request:

- `merchantName`: required merchant display name.
- `status`: required status, `ACTIVE` or `DISABLED`.

Behavior:

- Return `404` when the merchant does not exist.
- Update `merchant_name`, `status`, and `update_time`.
- Return the updated merchant.

### Delete Merchant

`DELETE /merchants/{merchantId}`

Behavior:

- Return `404` when the merchant does not exist.
- If any `t_order` or `t_payment_transaction` row references the merchant, return `400` and do not delete it.
- Otherwise delete the merchant and return `204`.

## Order And Payment Integration

Order creation:

- Before inserting `t_order`, load the merchant by `merchantId`.
- If missing, return `404`.
- If status is not `ACTIVE`, return `400`.
- Existing order creation behavior remains unchanged after the merchant check: insert order, then update Redis order statistics.

Payment creation:

- Before inserting `t_payment_transaction`, validate the request merchant exists and is `ACTIVE`.
- Continue to load the order and reject merchant mismatch.
- If the order does not exist, return `404`.
- If request merchant does not match the order merchant, return `400`.
- Existing payment creation behavior remains unchanged after validation: insert payment, then update Redis payment success statistics when status is `SUCCESS`.

## Architecture

Follow the current application structure:

- `MerchantRecord` in `db` represents a row in `t_merchant`.
- `MerchantJdbcRepository` owns SQL for merchant CRUD and reference checks.
- Merchant request records live in `api`.
- `MerchantApplicationService` owns merchant CRUD business rules.
- `MerchantController` exposes HTTP endpoints.
- Existing `OrderApplicationService` and `PaymentApplicationService` depend on merchant lookup/validation before writes.
- `ApiExceptionHandler` maps merchant errors to JSON HTTP responses.

## Error Handling

Use the existing JSON error shape:

```json
{
  "message": "Merchant not found: mch_123"
}
```

Expected statuses:

- `404 Not Found` for missing merchants or missing orders.
- `400 Bad Request` for disabled merchants, merchant mismatch, invalid merchant status, or deleting merchants that are already referenced.

## Testing

Add focused service and controller tests:

- Creating a merchant persists and returns generated `merchantId`.
- Getting a missing merchant fails with `404`.
- Listing merchants returns repository results.
- Updating a missing merchant fails with `404`.
- Deleting a referenced merchant fails with `400`.
- Deleting an unreferenced merchant succeeds.
- Creating an order with a missing merchant fails before writing an order.
- Creating an order with a disabled merchant fails before writing an order.
- Creating a payment with a missing or disabled merchant fails before writing payment data.
- Merchant REST endpoints return the expected HTTP statuses and JSON bodies.

Existing Redis/MySQL demo tests should continue to pass when local Redis and MySQL are available.
