package com.ytx.ai.oss.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ResponseHeaderOverrides;
import com.ytx.ai.oss.config.OssProperties;
import com.ytx.ai.oss.vo.OssUploadRequest;
import com.ytx.ai.oss.service.OssClient;
import lombok.extern.slf4j.Slf4j;

import com.aliyun.oss.model.ObjectMetadata;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.io.IoUtil;
import org.apache.tika.Tika;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;


/**
 * 阿里云OSS客户端实现
 */
@Slf4j
public class AliyunOssClient implements OssClient {

    private final OSS ossClient;
    private final OssProperties properties;
    private final Tika tika = new Tika();

    public AliyunOssClient(OssProperties properties) {
        this.properties = properties;
        this.ossClient = new OSSClientBuilder().build(
                properties.getEndpoint(),
                properties.getAccessKey(),
                properties.getSecretKey()
        );
    }

    @Override
    public String upload(OssUploadRequest request) {
        String path = request.getPath();
        InputStream inputStream = request.getInputStream();

        try {
            // Read stream to bytes to allow reusing stream for detection and upload
            // Also Aliyun SDK can take InputStream directly, but if we need to detect type, we might need bytes or mark/reset
            // Here we read to bytes to be safe and consistent with S3 implementation (which needs length)
            byte[] bytes = IoUtil.readBytes(inputStream);

            // Auto-detect content type if not provided
            String mimeType = null;
            try {
                mimeType = tika.detect(bytes, path);
            } catch (Exception e) {
                log.warn("Failed to detect content type for {}: {}", path, e.getMessage());
            }

            ObjectMetadata metadata = new ObjectMetadata();
            if (StrUtil.isNotBlank(mimeType)) {
                metadata.setContentType(mimeType);
            }

            // 设置MD5
            if (StrUtil.isNotBlank(request.getMd5())) {
                metadata.setContentMD5(request.getMd5());
            }

            // --- 设置用户自定义元数据 (User Metadata) ---
            // 1. 存储原始文件名 (注意: HTTP Header通常不支持非ASCII字符，如有中文建议URL编码)
            if (StrUtil.isNotBlank(request.getOriginalFilename())) {
                metadata.addUserMetadata("original-filename", request.getOriginalFilename());
            }
            // 2. 存储文件标签
            if (StrUtil.isNotBlank(request.getTag())) {
                metadata.addUserMetadata("tag", request.getTag());
            }
            // 3. 存储文件大小
            metadata.addUserMetadata("file-size", String.valueOf(bytes.length));
            // ----------------------------------------

            ossClient.putObject(properties.getBucketName(), path, new ByteArrayInputStream(bytes), metadata);

            // 获取过期时间，默认3600秒
            long timeout = request.getExpires() != null ? request.getExpires() : 3600L;
            return getUrl(path, timeout, request.getOriginalFilename());
        } catch (Exception e) {
            log.error("Aliyun OSS upload failed", e);
            throw new RuntimeException("Aliyun OSS upload failed", e);
        }
    }

    @Override
    public InputStream download(String path) {
        try {
            return ossClient.getObject(properties.getBucketName(), path).getObjectContent();
        } catch (Exception e) {
            log.error("Aliyun OSS download failed", e);
            throw new RuntimeException("Aliyun OSS download failed", e);
        }
    }

    @Override
    public Map<String, String> getObjectMetadata(String path) {
        try {
            ObjectMetadata objectMetadata = ossClient.getObjectMetadata(properties.getBucketName(), path);
            // 优先返回用户自定义元数据
            return objectMetadata.getUserMetadata();
        } catch (Exception e) {
            log.error("Aliyun OSS getObjectMetadata failed", e);
            throw new RuntimeException("Aliyun OSS getObjectMetadata failed", e);
        }
    }

    @Override
    public void delete(String path) {
        try {
            ossClient.deleteObject(properties.getBucketName(), path);
        } catch (Exception e) {
            log.error("Aliyun OSS delete failed", e);
            throw new RuntimeException("Aliyun OSS delete failed", e);
        }
    }


    @Override
    public String getUrl(String path, long timeout) {
        return getUrl(path, timeout, null);
    }

    @Override
    public String getUrl(String path, long timeout, String downloadFilename) {
        try {
            Date expiration = new Date(System.currentTimeMillis() + timeout * 1000);

            if (StrUtil.isNotBlank(downloadFilename)) {
                GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(properties.getBucketName(), path);
                request.setExpiration(expiration);

                ResponseHeaderOverrides overrides = new ResponseHeaderOverrides();
                overrides.setContentDisposition("attachment; filename=\"" + downloadFilename + "\"");
                request.setResponseHeaders(overrides);

                return ossClient.generatePresignedUrl(request).toString();
            } else {
                return ossClient.generatePresignedUrl(properties.getBucketName(), path, expiration).toString();
            }
        } catch (Exception e) {
            log.error("Aliyun OSS getUrl failed", e);
            // 降级为简单拼接 (不支持下载重命名)
            String endpoint = properties.getEndpoint();
            if (endpoint.startsWith("http://")) {
                endpoint = endpoint.substring(7);
            } else if (endpoint.startsWith("https://")) {
                endpoint = endpoint.substring(8);
            }
            return "https://" + properties.getBucketName() + "." + endpoint + "/" + path;
        }
    }

}

