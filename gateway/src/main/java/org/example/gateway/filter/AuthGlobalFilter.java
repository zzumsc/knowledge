package org.example.gateway.filter;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gateway.config.AuthProperties;
import org.example.common.utils.JwtTool;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AuthProperties.class)
@SentinelResource(
        value = "AuthGlobalFilter", // 资源名（与静态规则中 resource 一致）
        blockHandler = "handleBlock", // 限流/熔断处理
        fallback = "handleFallback" // 业务异常处理
)
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtTool jwtTool;
    private final AuthProperties authProperties;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher(); // Ant路径匹配器

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        // 1. 校验是否为放行路径，是则直接放行
        if (isExcludePath(request.getPath().toString())) {
            return chain.filter(exchange);
        }
        // 2. 提取Authorization请求头（处理Bearer前缀）
        String token = extractToken(request);

        // 3. 解析JWT令牌，获取用户ID
        Long userId;
        try {
            userId = jwtTool.parseToken(token);
        } catch (Exception e) {
            // 4. 令牌无效，返回401未授权响应
            return handleUnauthorizedResponse(exchange, e.getMessage());
        }
        if(userId == null){return null;}
        // 5. 传递用户信息：添加请求头user-id（下游服务可通过该头获取用户ID）
        ServerHttpRequest modifiedRequest = request.mutate()
                .header("user-id", userId.toString())
                .build();
        ServerWebExchange modifiedExchange = exchange.mutate()
                .request(modifiedRequest)
                .build();

        // 6. 放行到下一个过滤器/目标服务
        return chain.filter(modifiedExchange);
    }

    private boolean isExcludePath(String requestPath) {
        for (String excludePath : authProperties.getExcludePaths()) {
            if (antPathMatcher.match(excludePath, requestPath)) {
                return true;
            }
        }
        return false;
    }

    private String extractToken(ServerHttpRequest request) {
        // 从Authorization头提取令牌（兼容前端传递格式）
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim(); // 去掉"Bearer "前缀，trim避免空格问题
        }
        // 兼容前端可能直接传token（无Bearer前缀）的情况
        return authHeader;
    }

    private Mono<Void> handleUnauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        // 设置响应状态码
        response.setRawStatusCode(401);
        // 设置响应头（JSON格式）
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        // 构建响应体
        String errorBody = String.format("{\"code\":401,\"message\":\"%s\"}", message);
        DataBuffer dataBuffer = response.bufferFactory().wrap(errorBody.getBytes(StandardCharsets.UTF_8));
        // 返回响应
        return response.writeWith(Mono.just(dataBuffer));
    }

    @Override
    public int getOrder() {
        return 0;
    }
}