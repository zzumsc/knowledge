package org.example.common.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.common.config.UserContextInterceptorProperties;
import org.example.common.utils.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 通用用户上下文拦截器（所有服务复用）
 */
@Component
public class CommonUserContextInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(CommonUserContextInterceptor.class);
    private final UserContextInterceptorProperties properties;

    // 构造器注入配置项（避免 @Value 硬编码）
    public CommonUserContextInterceptor(UserContextInterceptorProperties properties) {
        this.properties = properties;
    }

    public static final String JWT_BLACK_LIST = "jwt:blacklist:";
    @Autowired
    RedisTemplate<String,Object> redisTemplate;
    /**
     * 接口执行前：读取请求头 userId，存入 ThreadLocal
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 从配置中获取请求头字段名（支持自定义）
        String userIdHeader = properties.getUserIdHeader();
        // 2. 读取请求头中的 user-id
        String userIdStr = request.getHeader(userIdHeader);
        log.debug("{}: {}", userIdHeader, userIdStr);

        // 3. 校验并转换 userId（非空 + 数字格式）
        if (userIdStr == null || userIdStr.trim().isEmpty()) {
            log.warn("请求头缺少 {}，未经过 Gateway 认证", userIdHeader);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 未授权
            return false;
        }
        Long userId = Long.parseLong(userIdStr);
        if(redisTemplate.opsForValue().get(JWT_BLACK_LIST + userId)!=null) {return false;};
        try {
            UserContext.setUser(userId); // 存入 ThreadLocal
            log.debug("ThreadLocal 存入 userId: {}", userId);
        } catch (NumberFormatException e) {
            log.error("{} 格式错误：{}", userIdHeader, userIdStr, e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 未授权
            return false;
        }

        return true; // 放行，继续执行接口
    }

    /**
     * 接口执行后：清除 ThreadLocal（必须！避免线程池复用导致内存泄漏）
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        log.debug("清除 ThreadLocal 中的 userId");
        UserContext.removeUser();
    }
}