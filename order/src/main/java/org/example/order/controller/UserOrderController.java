package org.example.order.controller;

import org.example.order.service.IOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserOrderController {
    @Autowired
    IOrderService orderService;
    @GetMapping("/order")
    public Long getMyUserOrderNum(){return orderService.getMyUserOrderNum();}
}
