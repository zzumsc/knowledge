package org.example.order.controller;

import org.example.order.service.IOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("content")
public class ContentOrderController {
    @Autowired
    IOrderService orderService;
    @GetMapping("/order")
    public List<Long> getMyOrderContent() {
        return orderService.getMyOrderContent();
    }
}
