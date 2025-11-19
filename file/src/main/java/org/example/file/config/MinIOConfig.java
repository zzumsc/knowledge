package org.example.file.config;

import io.minio.MinioClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * MinIO 配置类（自动读取application.yml中的minio配置）
 */
@Configuration
@EnableConfigurationProperties(MinIOConfig.class) // 显式启用配置绑定（关键）
@ConfigurationProperties(prefix = "minio") // 绑定yml中minio前缀的配置
@Data // lombok注解，生成getter/setter（需确保lombok依赖生效）
public class MinIOConfig {

    private String endpoint;       // MinIO API地址（已包含端口：http://192.168.208.129:9000）
    private String accessKey;      // 访问密钥
    private String secretKey;      // 密钥
    private String bucketName;     // 默认存储桶
    private boolean secure;        // 是否HTTPS

    /**
     * 注入MinioClient实例（全局唯一，可直接@Autowired使用）
     */
    @Bean
    public MinioClient minioClient() {
        // 非空校验：提前拦截配置错误，便于排查
        if (!StringUtils.hasText(endpoint)) {
            throw new IllegalArgumentException("MinIO endpoint 配置不能为空，请检查 application.yml");
        }
        if (!StringUtils.hasText(accessKey) || !StringUtils.hasText(secretKey)) {
            throw new IllegalArgumentException("MinIO access-key/secret-key 配置不能为空");
        }

        // 修复：endpoint已包含端口，无需重复指定（避免端口冲突）
        return MinioClient.builder()
                .endpoint(endpoint) // 直接使用完整endpoint（含端口）
                .credentials(accessKey, secretKey)
                .build();
    }
}