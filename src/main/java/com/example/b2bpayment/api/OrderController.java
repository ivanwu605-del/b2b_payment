package com.example.b2bpayment.api;

import com.example.b2bpayment.db.OrderRecord;
import com.example.b2bpayment.order.OrderApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    private final OrderApplicationService orderService;

    public OrderController(OrderApplicationService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderRecord createOrder(@RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }
}
