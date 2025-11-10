package org.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityDisableConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // 禁用所有默认的安全校验（如Basic认证、表单登录）
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().permitAll() // 所有请求都放行（由AuthGlobalFilter控制）
                )
                .csrf(csrf -> csrf.disable()) // 禁用CSRF
                .build();
    }
}