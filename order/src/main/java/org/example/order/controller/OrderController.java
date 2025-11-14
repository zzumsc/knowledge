package org.example.order.controller;

import org.example.common.utils.Result;
import org.example.order.service.IOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
public class OrderController {
    @Autowired
    IOrderService orderService;
    @GetMapping
    public Result getMyOrders() {
        return orderService.getMyOrders();
    }
    @GetMapping("/{id}")
    public Result getOrderById(@PathVariable("id") Long id) {
        return orderService.getOrderById(id);
    }
    @PostMapping("/create/{id}")
    public Result createOrder(@PathVariable("id") Long id) {
        return orderService.createOrder(id);
    }
    @PostMapping("/pay/{id}")
    public Result payOrder(@PathVariable("id") Long id) {
        return orderService.payOrder(id);
    }
}
