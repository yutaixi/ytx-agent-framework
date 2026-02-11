package com.ytx.ai.oss.service.impl;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.ytx.ai.oss.config.OssProperties;
import com.ytx.ai.oss.vo.OssUploadRequest;
import com.ytx.ai.oss.service.OssClient;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;
        import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import org.apache.tika.Tika;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * AWS S3 / MinIO / RustFS 客户端实现
 * 基于 AWS SDK v2
 */
@Slf4j
public class S3OssClient implements OssClient {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final OssProperties properties;
    private final Tika tika = new Tika();

    public S3OssClient(OssProperties properties) {
        this.properties = properties;

        // 1. Initialize S3 client configuration
        S3ClientBuilder clientBuilder = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())
                ));

        // 2. Initialize S3 Presigner configuration
        S3Presigner.Builder presignerBuilder = S3Presigner.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())
                ));

        // 3. Configure Endpoint (Required for MinIO / RustFS)
        if (StrUtil.isNotBlank(properties.getEndpoint())) {
            URI endpointUri = URI.create(properties.getEndpoint());
            clientBuilder.endpointOverride(endpointUri);
            presignerBuilder.endpointOverride(endpointUri);
        }

        // 4. Configure Region
        if (StrUtil.isNotBlank(properties.getRegion())) {
            Region region = Region.of(properties.getRegion());
            clientBuilder.region(region);
            presignerBuilder.region(region);
        } else {
            // Default region for S3 compatible storage like RustFS/MinIO
            clientBuilder.region(Region.US_EAST_1);
            presignerBuilder.region(Region.US_EAST_1);
        }

        // 5. Force Path Style (Critical for RustFS and MinIO)
        clientBuilder.forcePathStyle(true);

        this.s3Client = clientBuilder.build();
        this.s3Presigner = presignerBuilder.build();

        createBucketIfNotExists();
    }

    private void createBucketIfNotExists() {
        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(properties.getBucketName()).build());
            log.info("Bucket created: {}", properties.getBucketName());
        } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException e) {
            // Bucket exists
        } catch (Exception e) {
            log.warn("Failed to create bucket: {}", e.getMessage());
        }
    }

    /**
     * 规范化 path，去除开头的 /，防止 S3 Invalid Argument 错误
     */
    private String normalizePath(String path) {
        if (StrUtil.isEmpty(path)) {
            return path;
        }
        if (path.startsWith("/")) {
            return path.substring(1);
        }
        return path;
    }

    @Override
    public String upload(OssUploadRequest request) {
        String path = request.getPath();
        InputStream inputStream = request.getInputStream();

        try {
            String key = normalizePath(path);
            // Read stream to bytes as SDK v2 requires length for streams
            byte[] bytes = IoUtil.readBytes(inputStream);

            // Auto-detect content type if not provided
            String mimeType = null;
            try {
                mimeType = tika.detect(bytes, path);
            } catch (Exception e) {
                log.warn("Failed to detect content type for {}: {}", path, e.getMessage());
            }

            PutObjectRequest.Builder builder = PutObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(key);

            if (StrUtil.isNotBlank(mimeType)) {
                builder.contentType(mimeType);
            }

            // 设置MD5
            if (StrUtil.isNotBlank(request.getMd5())) {
                builder.contentMD5(request.getMd5());
            }

            // --- 设置用户自定义元数据 (User Metadata) ---
            Map<String, String> metadata = new HashMap<>();
            // 1. 存储原始文件名 (注意: S3 User Metadata Key 默认转小写，且不支持非ASCII字符)
            // 修复: S3 Metadata Value 必须是 ASCII 字符。对于包含中文等非 ASCII 字符的文件名，必须进行 URL 编码，否则会导致 403 Forbidden 错误。
            if (StrUtil.isNotBlank(request.getOriginalFilename())) {
                String encodedFilename = URLEncoder.encode(request.getOriginalFilename(), StandardCharsets.UTF_8);
                metadata.put("original-filename", encodedFilename);
            }
            // 2. 存储文件标签
            if (StrUtil.isNotBlank(request.getTag())) {
                String encodedTag = URLEncoder.encode(request.getTag(), StandardCharsets.UTF_8);
                metadata.put("tag", encodedTag);
            }
            // 3. 存储文件大小
            metadata.put("file-size", String.valueOf(bytes.length));
            // ----------------------------------------

            if (!metadata.isEmpty()) {
                builder.metadata(metadata);
            }
            if(ObjectUtil.isNotEmpty(request.getExpires())){
                builder.expires(Instant.now().plusSeconds(request.getExpires()));
            }

            s3Client.putObject(builder.build(), RequestBody.fromBytes(bytes));

            // 获取过期时间，默认3600秒
            long timeout = request.getExpires() != null ? request.getExpires() : 3600L;
            return getUrl(key, timeout, request.getOriginalFilename());
        } catch (Exception e) {
            log.error("S3 upload failed", e);
            throw new RuntimeException("S3 upload failed", e);
        }
    }

    @Override
    public InputStream download(String path) {
        try {
            String key = normalizePath(path);
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(key)
                    .build();
            return s3Client.getObject(getObjectRequest);
        } catch (Exception e) {
            log.error("S3 download failed", e);
            throw new RuntimeException("S3 download failed", e);
        }
    }

    @Override
    public Map<String, String> getObjectMetadata(String path) {
        try {
            String key = normalizePath(path);
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(key)
                    .build();
            HeadObjectResponse response = s3Client.headObject(headObjectRequest);
            return response.metadata();
        } catch (Exception e) {
            log.error("S3 getObjectMetadata failed", e);
            throw new RuntimeException("S3 getObjectMetadata failed", e);
        }
    }

    @Override
    public void delete(String path) {
        try {
            String key = normalizePath(path);
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(key)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            log.error("S3 delete failed", e);
            throw new RuntimeException("S3 delete failed", e);
        }
    }

    @Override
    public String getUrl(String path, long timeout) {
        return getUrl(path, timeout, null);
    }

    @Override
    public String getUrl(String path, long timeout, String downloadFilename) {
        try {
            String key = normalizePath(path);
            GetObjectRequest.Builder getObjectRequestBuilder = GetObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(key);

            // 如果需要重命名下载文件，设置 ResponseContentDisposition
            if (StrUtil.isNotBlank(downloadFilename)) {
                getObjectRequestBuilder.responseContentDisposition("attachment; filename=\"" + downloadFilename + "\"");
            }

            GetObjectRequest getObjectRequest = getObjectRequestBuilder.build();

            // Generate Presigned URL
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .getObjectRequest(getObjectRequest)
                    .signatureDuration(Duration.ofSeconds(timeout))
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (Exception e) {
            log.error("S3 generate URL failed", e);
            // Fallback to simple URL construction
            String endpoint = properties.getEndpoint();
            String key = normalizePath(path);
            if (StrUtil.isBlank(endpoint)) {
                return key;
            }
            if (!endpoint.endsWith("/")) {
                endpoint += "/";
            }
            return endpoint + properties.getBucketName() + "/" + key;
        }
    }

}
