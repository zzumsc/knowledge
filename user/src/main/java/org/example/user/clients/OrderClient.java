package org.example.user.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("order-service")
public interface OrderClient {
    @GetMapping("/user/order")
    Long getMyUserOrderNum();
}
