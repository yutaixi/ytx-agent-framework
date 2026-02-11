package com.ytx.ai.oss.config;

import com.ytx.ai.oss.service.OssClient;
import com.ytx.ai.oss.service.impl.AliyunOssClient;
import com.ytx.ai.oss.service.impl.LocalOssClient;
import com.ytx.ai.oss.service.impl.S3OssClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OSS自动配置类
 */
@Configuration
@EnableConfigurationProperties(OssProperties.class)
@ConditionalOnProperty(prefix = "oss", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class OssConfig {

    private final OssProperties ossProperties;

    @Bean
    public OssClient ossClient() {
        OssType type = ossProperties.getType();
        log.info("Initializing OSS Client with type: {}", type);
        switch (type) {
            case ALIYUN:
                return new AliyunOssClient(ossProperties);
            case S3:
            case MINIO:
            case RUSTFS:
                return new S3OssClient(ossProperties);
            case LOCAL:
                return new LocalOssClient(ossProperties);
            default:
                throw new IllegalArgumentException("Unsupported OSS type: " + type);
        }
    }
}