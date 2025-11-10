package org.example.common.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.common.utils.UserContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class UserInfoInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 直接提取网关传递的"user-id"头（值是数字，无需解析JWT）
        String userIdStr = request.getHeader("user-id");

        // 2. 转换为Long类型（避免NumberFormatException）
        if (userIdStr != null && !userIdStr.isEmpty()) {
            try {
                Long userId = Long.valueOf(userIdStr);
                UserContext.setUser(userId); // 存入ThreadLocal
            } catch (NumberFormatException e) {
                log.warn("user-id格式错误：{}", userIdStr);
            }
        }

        // 3. 放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除用户
        UserContext.removeUser();
    }

}