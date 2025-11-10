package org.example.content.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.example.content.util.UserContextUtil;
import org.springframework.stereotype.Component;

@Component // 交给Spring管理，自动生效
public class FeignUserHeaderInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        try {
            // 1. 业务服务中获取当前用户信息（用之前的工具类）
            Long userId = UserContextUtil.getCurrentUserId();
            String userName = UserContextUtil.getCurrentUserName();
            // 2. 把用户信息放入Feign请求头，传递给用户服务
            template.header("X-User-Id", String.valueOf(userId));
            template.header("X-User-Name", userName);
        } catch (Exception e) {
            // 非用户请求场景（如定时任务调用），可根据需求抛异常或忽略
            throw new RuntimeException("Feign调用用户服务失败：未获取到当前用户信息", e);
        }
    }
}