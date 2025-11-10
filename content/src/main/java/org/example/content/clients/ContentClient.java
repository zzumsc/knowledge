package org.example.content.clients;

import org.example.content.pojo.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

// 业务服务中的Feign接口（调用用户服务）
@FeignClient(name = "user-service") // user-service是用户服务的注册名
public interface ContentClient {
    // 示例1：调用用户服务的“查询用户详情”接口
    @GetMapping("/user/myInfo")
    UserDTO getMyInfo(); // 无需加@RequestHeader，拦截器自动传递

    // 示例2：调用用户服务的“校验用户权限”接口（需用户ID）
    @PostMapping("/user/check-permission")
    boolean checkPermission(@RequestParam("permission") String permission);
}