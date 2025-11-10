package org.example.order.clients;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient("user-service")
public interface UserClient {

}
