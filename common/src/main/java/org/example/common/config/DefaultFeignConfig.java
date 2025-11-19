package org.example.common.config;

import feign.RequestInterceptor;
import org.example.common.utils.UserContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class DefaultFeignConfig {
    @Bean
    public RequestInterceptor userInfoRequestInterceptor(){
        return template -> {
            Long userId = UserContext.getCurrentUser();
            if(userId == null) {
                return;
            }
            template.header("user-id", userId.toString());
        };
    }
}