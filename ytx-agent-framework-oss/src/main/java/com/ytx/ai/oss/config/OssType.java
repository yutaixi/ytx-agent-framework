package com.ytx.ai.oss.config;

/**
 * OSS存储类型枚举
 */
public enum OssType {
    /**
     * 阿里云OSS
     */
    ALIYUN,
    /**
     * AWS S3 / MinIO
     */
    S3,
    /**
     * MinIO (Explicit, mapped to S3 usually)
     */
    MINIO,
    /**
     * 本地文件系统 / RustFS
     */
    RUSTFS,
    /**
     * 本地
     */
    LOCAL
}