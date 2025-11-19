package org.example.order.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

@FeignClient("content-service")
public interface ContentClient {
    @GetMapping("/order/price/{id}")
    BigDecimal getPriceById(@PathVariable Long id);
    @GetMapping("/order/{id}")
    Integer getStatusById(@PathVariable Long id);
}
