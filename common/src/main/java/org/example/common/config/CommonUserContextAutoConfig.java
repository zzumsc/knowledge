package org.example.common.config;

import org.example.common.interceptors.CommonUserContextInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CommonUserContextAutoConfig implements WebMvcConfigurer {

    private final CommonUserContextInterceptor commonUserContextInterceptor;
    private final UserContextInterceptorProperties properties;

    // 注入拦截器和配置项
    public CommonUserContextAutoConfig(CommonUserContextInterceptor commonUserContextInterceptor,
                                       UserContextInterceptorProperties properties) {
        this.commonUserContextInterceptor = commonUserContextInterceptor;
        this.properties = properties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(commonUserContextInterceptor)
                // 拦截配置的路径（业务服务通过 application.yml 指定）
                .addPathPatterns(properties.getIncludePaths())
                // 排除配置的路径（如登录、注册接口）
                .excludePathPatterns(properties.getExcludePaths());
    }
}