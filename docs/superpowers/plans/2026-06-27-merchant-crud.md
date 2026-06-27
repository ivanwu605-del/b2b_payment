# Merchant CRUD Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add merchant CRUD APIs and make orders/payments require an active merchant.

**Architecture:** Keep the existing thin-controller/application-service/JDBC-repository style. Add merchant persistence under `db`, merchant business rules under `merchant`, HTTP request/controller types under `api`, and extend existing order/payment services with merchant validation.

**Tech Stack:** Java 21, Spring Boot 4, Spring Web MVC, Spring JDBC, JUnit 5, Mockito.

---

## File Structure

- Modify `docs/b2b_payment.sql`: add `t_merchant` and foreign keys from order/payment merchant columns.
- Create `src/main/java/com/example/b2bpayment/db/MerchantRecord.java`.
- Create `src/main/java/com/example/b2bpayment/db/MerchantJdbcRepository.java`.
- Create `src/main/java/com/example/b2bpayment/merchant/MerchantStatus.java`.
- Create `src/main/java/com/example/b2bpayment/merchant/MerchantApplicationService.java`.
- Create merchant exceptions under `src/main/java/com/example/b2bpayment/merchant/`.
- Create `CreateMerchantRequest`, `UpdateMerchantRequest`, and `MerchantController`.
- Modify `OrderApplicationService` and `PaymentApplicationService` to require active merchants.
- Modify `ApiExceptionHandler` to map merchant errors.
- Add service and controller tests for merchant CRUD and order/payment merchant validation.

## Tasks

### Task 1: Merchant Service Core

- [ ] Write `MerchantApplicationServiceTest` covering create/get/list/update/delete and referenced delete failure.
- [ ] Run `./mvnw test -Dtest=MerchantApplicationServiceTest` and confirm it fails because merchant classes are missing.
- [ ] Add merchant record, repository contract methods, status enum, exceptions, and service implementation.
- [ ] Re-run the test and confirm it passes.

### Task 2: Merchant REST API

- [ ] Write `MerchantControllerTest` covering create, get missing, list, update, delete, and referenced delete errors.
- [ ] Run `./mvnw test -Dtest=MerchantControllerTest` and confirm it fails because controller/error mappings are missing.
- [ ] Add merchant request records, controller, and exception handler mappings.
- [ ] Re-run the test and confirm it passes.

### Task 3: Order And Payment Merchant Validation

- [ ] Extend `OrderApplicationServiceTest` to reject missing and disabled merchants.
- [ ] Extend `PaymentApplicationServiceTest` to reject missing and disabled merchants before writing payment data.
- [ ] Run both tests and confirm they fail because services do not validate merchants.
- [ ] Inject `MerchantApplicationService` into order/payment services and validate `merchantId` before writes.
- [ ] Re-run both tests and confirm they pass.

### Task 4: Schema And Verification

- [ ] Update `docs/b2b_payment.sql` with `t_merchant`, indexes, and foreign keys.
- [ ] Run focused tests: `./mvnw clean test -Dtest=MerchantApplicationServiceTest,MerchantControllerTest,OrderApplicationServiceTest,PaymentApplicationServiceTest,OrderControllerTest,PaymentControllerTest,B2bPaymentApplicationTests`.
- [ ] Run lints for edited files.

## Self-Review

- Spec coverage: covers merchant CRUD, table design, order/payment active merchant validation, delete restrictions, HTTP errors, and tests.
- Placeholder scan: no placeholders remain.
- Type consistency: record, repository, service, controller, exception, and test names are consistent.
