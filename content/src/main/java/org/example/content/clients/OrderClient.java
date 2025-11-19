package org.example.content.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient("order-service")
public interface OrderClient {
    @GetMapping("/content/order")
    List<Long> getMyOrderContent();
}
