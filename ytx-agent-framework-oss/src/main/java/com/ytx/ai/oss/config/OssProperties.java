package com.ytx.ai.oss.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OSS配置类
 * 用于接收application.yml/properties中的oss配置信息
 */
@Data
@ConfigurationProperties(prefix = "oss")
public class OssProperties {

    /**
     * 是否启用OSS
     */
    private boolean enabled = true;

    /**
     * 存储类型
     */
    private OssType type = OssType.LOCAL;

    /**
     * 端点 (例如: oss-cn-hangzhou.aliyuncs.com 或 http://minio:9000)
     */
    private String endpoint;

    /**
     * 访问Key ID
     */
    private String accessKey;

    /**
     * 访问Key Secret
     */
    private String secretKey;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 区域 (AWS S3需要)
     */
    private String region;

    /**
     * 基础路径 (Local模式使用)
     */
    private String basePath;
}