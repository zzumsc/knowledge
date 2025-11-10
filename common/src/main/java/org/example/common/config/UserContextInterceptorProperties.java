package org.example.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "user.context.interceptor") // 配置前缀
public class UserContextInterceptorProperties {

    private String userIdHeader = "user-id";

    private List<String> includePaths = new ArrayList<>();

    private List<String> excludePaths = new ArrayList<>();
}