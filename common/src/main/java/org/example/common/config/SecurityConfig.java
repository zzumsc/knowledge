package org.example.common.config;


import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // 1. 密码编码器（必选）
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 关闭CSRF保护
                .csrf(csrf -> csrf.disable())
                // 允许所有请求访问，无需认证
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                .httpBasic(httpBasic -> httpBasic.disable())
                .requestCache(cache -> cache.disable())
                // 禁用表单登录
                .formLogin(form -> form.disable())
                // 禁用HTTP基本认证
                .httpBasic(basic -> basic.disable())
                // 设置无状态会话（可选，视情况而定）
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }

//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .authorizeHttpRequests(auth -> auth
//                                // 放行不需要认证的接口
//                                .requestMatchers("/user/login", "/user/logout").permitAll()
//// 其他接口需认证
//                                .anyRequest().authenticated()
//                )
//                // 添加登出配置
//                .logout(logout -> logout
//                        .logoutUrl("/user/logout")
//                        .invalidateHttpSession(true)
//                        .clearAuthentication(true)
//                        .logoutSuccessHandler((request, response, authentication) -> {
//                            // 自定义登出成功响应
//                            response.setContentType("application/json;charset=UTF-8");
//                            response.getWriter().write("{\"code\":200,\"msg\":\"登出成功\"}");
//                        })
//                )
//                .sessionManagement(session -> session
//                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
//                )
//                // 关闭 CSRF
//                .csrf(csrf -> csrf.disable());
//        return http.build();
//    }

}