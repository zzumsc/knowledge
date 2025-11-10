package org.example.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "gateway.auth") // 配置前缀
public class AuthProperties {

    // 不需要拦截的路径（Ant风格匹配，如/api/auth/**）
    private List<String> excludePaths = new ArrayList<>();

    // Getter + Setter
    public List<String> getExcludePaths() {
        return excludePaths;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }
}