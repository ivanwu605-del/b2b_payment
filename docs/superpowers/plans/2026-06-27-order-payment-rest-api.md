# Order Payment REST API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add real `POST /orders` and `POST /payments` REST APIs that persist to MySQL and update Redis payment ratio statistics.

**Architecture:** Add `spring-boot-starter-web`, keep controllers thin, and put business behavior in application services. Repositories continue to own JDBC access; `B2bPaymentRatioService` continues to own Redis statistic writes.

**Tech Stack:** Java 17, Spring Boot 4, Spring Web MVC, Spring JDBC, Spring Data Redis, JUnit 5, Mockito.

---

## File Structure

- Modify `pom.xml`: add `spring-boot-starter-web`.
- Modify `src/main/java/com/example/demo/db/OrderJdbcRepository.java`: add `findByOrderId(String orderId)`.
- Create `src/main/java/com/example/demo/api/CreateOrderRequest.java`: order create payload.
- Create `src/main/java/com/example/demo/api/CreatePaymentRequest.java`: payment create payload.
- Create `src/main/java/com/example/demo/api/OrderController.java`: `POST /orders`.
- Create `src/main/java/com/example/demo/api/PaymentController.java`: `POST /payments`.
- Create `src/main/java/com/example/demo/api/ApiErrorResponse.java`: JSON error body.
- Create `src/main/java/com/example/demo/api/ApiExceptionHandler.java`: maps app exceptions to HTTP.
- Create `src/main/java/com/example/demo/order/OrderApplicationService.java`: create order behavior.
- Create `src/main/java/com/example/demo/payment/PaymentApplicationService.java`: create payment behavior.
- Create `src/main/java/com/example/demo/payment/OrderNotFoundException.java`: missing order error.
- Create `src/main/java/com/example/demo/payment/MerchantMismatchException.java`: merchant mismatch error.
- Create `src/test/java/com/example/demo/order/OrderApplicationServiceTest.java`: order service behavior.
- Create `src/test/java/com/example/demo/payment/PaymentApplicationServiceTest.java`: payment service behavior.

### Task 1: Order Creation Service

**Files:**
- Create: `src/main/java/com/example/demo/api/CreateOrderRequest.java`
- Create: `src/main/java/com/example/demo/order/OrderApplicationService.java`
- Test: `src/test/java/com/example/demo/order/OrderApplicationServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void createOrderPersistsOrderAndRecordsRedisStatistic() {
    OrderJdbcRepository orderRepository = mock(OrderJdbcRepository.class);
    B2bPaymentRatioService ratioService = mock(B2bPaymentRatioService.class);
    OrderApplicationService service = new OrderApplicationService(orderRepository, ratioService);

    OrderRecord order = service.createOrder(new CreateOrderRequest("merchant-1", "out-1", 100L, "CNY"));

    assertEquals("merchant-1", order.merchantId());
    assertEquals("out-1", order.outTradeNo());
    assertEquals(100L, order.amount());
    assertEquals("CNY", order.currency());
    assertEquals("CREATED", order.status());
    assertNotNull(order.orderId());
    assertNotNull(order.createTime());
    verify(orderRepository).insert(order);
    verify(ratioService).onOrderCreated(order.orderId(), order.createTime());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=OrderApplicationServiceTest`
Expected: compile failure because `OrderApplicationService` and `CreateOrderRequest` do not exist.

- [ ] **Step 3: Write minimal implementation**

Create `CreateOrderRequest` as a record with `merchantId`, `outTradeNo`, `amount`, `currency`.
Create `OrderApplicationService#createOrder(CreateOrderRequest)` that generates an `ord_` UUID, writes `OrderRecord`, and calls `B2bPaymentRatioService.onOrderCreated`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=OrderApplicationServiceTest`
Expected: PASS.

### Task 2: Payment Creation Service

**Files:**
- Modify: `src/main/java/com/example/demo/db/OrderJdbcRepository.java`
- Create: `src/main/java/com/example/demo/api/CreatePaymentRequest.java`
- Create: `src/main/java/com/example/demo/payment/PaymentApplicationService.java`
- Create: `src/main/java/com/example/demo/payment/OrderNotFoundException.java`
- Create: `src/main/java/com/example/demo/payment/MerchantMismatchException.java`
- Test: `src/test/java/com/example/demo/payment/PaymentApplicationServiceTest.java`

- [ ] **Step 1: Write failing tests**

```java
@Test
void createSuccessfulPaymentPersistsPaymentAndRecordsRedisStatistic() {
    OrderJdbcRepository orderRepository = mock(OrderJdbcRepository.class);
    PaymentJdbcRepository paymentRepository = mock(PaymentJdbcRepository.class);
    B2bPaymentRatioService ratioService = mock(B2bPaymentRatioService.class);
    PaymentApplicationService service = new PaymentApplicationService(orderRepository, paymentRepository, ratioService);
    OrderRecord order = new OrderRecord("order-1", "merchant-1", "out-1", 100L, "CNY", "CREATED", LocalDateTime.now(), LocalDateTime.now());
    when(orderRepository.findByOrderId("order-1")).thenReturn(Optional.of(order));

    PaymentRecord payment = service.createPayment(new CreatePaymentRequest("order-1", "merchant-1", "WECHAT", "SUCCESS"));

    assertEquals("order-1", payment.orderId());
    assertEquals("merchant-1", payment.merchantId());
    assertEquals("WECHAT", payment.channel());
    assertEquals("SUCCESS", payment.status());
    verify(paymentRepository).insert(payment);
    verify(ratioService).onPaymentSuccess(payment.transactionId(), payment.createTime());
}

@Test
void createPaymentFailsWhenOrderDoesNotExist() {
    when(orderRepository.findByOrderId("missing")).thenReturn(Optional.empty());

    assertThrows(OrderNotFoundException.class, () -> service.createPayment(new CreatePaymentRequest("missing", "merchant-1", "WECHAT", "SUCCESS")));

    verifyNoInteractions(paymentRepository);
}

@Test
void createPaymentFailsWhenMerchantDoesNotMatchOrder() {
    when(orderRepository.findByOrderId("order-1")).thenReturn(Optional.of(orderWithMerchant("merchant-2")));

    assertThrows(MerchantMismatchException.class, () -> service.createPayment(new CreatePaymentRequest("order-1", "merchant-1", "WECHAT", "SUCCESS")));

    verifyNoInteractions(paymentRepository);
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw test -Dtest=PaymentApplicationServiceTest`
Expected: compile failure because payment service, request, exceptions, and repository lookup are missing.

- [ ] **Step 3: Write minimal implementation**

Add `OrderJdbcRepository#findByOrderId`, create the request and exception classes, and implement payment creation with order existence and merchant checks. Only call Redis success statistic when `status` equals `PaymentJdbcRepository.STATUS_SUCCESS`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./mvnw test -Dtest=PaymentApplicationServiceTest`
Expected: PASS.

### Task 3: REST Controllers And Errors

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/com/example/demo/api/OrderController.java`
- Create: `src/main/java/com/example/demo/api/PaymentController.java`
- Create: `src/main/java/com/example/demo/api/ApiErrorResponse.java`
- Create: `src/main/java/com/example/demo/api/ApiExceptionHandler.java`

- [ ] **Step 1: Add web starter**

Add `spring-boot-starter-web` so Spring MVC annotations and HTTP runtime are available.

- [ ] **Step 2: Implement controllers**

`OrderController#createOrder` maps `POST /orders` to `OrderApplicationService#createOrder`.
`PaymentController#createPayment` maps `POST /payments` to `PaymentApplicationService#createPayment`.

- [ ] **Step 3: Implement error mapping**

Map `OrderNotFoundException` to `404` and `MerchantMismatchException` to `400` with `ApiErrorResponse`.

- [ ] **Step 4: Run focused tests**

Run: `./mvnw test -Dtest=OrderApplicationServiceTest,PaymentApplicationServiceTest`
Expected: PASS.

### Task 4: Verification

**Files:**
- Read lints for edited files.
- Run Maven tests that do not require local MySQL/Redis where possible.

- [ ] **Step 1: Lint check**

Run IDE lint check for edited Java and YAML files.

- [ ] **Step 2: Test command**

Run: `./mvnw test -Dtest=OrderApplicationServiceTest,PaymentApplicationServiceTest`
Expected: PASS.

- [ ] **Step 3: Manual API smoke examples**

Use these requests when the app is running with MySQL and Redis:

```bash
curl -X POST http://localhost:8080/orders \
  -H 'Content-Type: application/json' \
  -d '{"merchantId":"merchant-1","outTradeNo":"out-1","amount":100,"currency":"CNY"}'

curl -X POST http://localhost:8080/payments \
  -H 'Content-Type: application/json' \
  -d '{"orderId":"<orderId>","merchantId":"merchant-1","channel":"WECHAT","status":"SUCCESS"}'
```

## Self-Review

- Spec coverage: the plan covers both endpoints, MySQL writes, Redis synchronization, missing order handling, merchant mismatch handling, and JSON error responses.
- Placeholder scan: no TBD/TODO placeholders remain.
- Type consistency: request record names, service method names, repository method names, and exception names are consistent across tasks.
