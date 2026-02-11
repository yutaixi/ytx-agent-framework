package com.ytx.ai.oss.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.ytx.ai.oss.config.OssProperties;
import com.ytx.ai.oss.vo.OssUploadRequest;
import com.ytx.ai.oss.service.OssClient;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

/**
 * 本地文件系统存储实现
 * 也可用作 RustFS 的模拟/占位实现
 */
@Slf4j
public class LocalOssClient implements OssClient {

    private final String basePath;
    private final String endpoint; // 用于生成访问URL

    public LocalOssClient(OssProperties properties) {
        this.basePath = properties.getBasePath();
        this.endpoint = properties.getEndpoint();
        if (StrUtil.isBlank(basePath)) {
            throw new IllegalArgumentException("Local OSS basePath cannot be empty");
        }
        if (!FileUtil.exist(basePath)) {
            FileUtil.mkdir(basePath);
        }
    }

    @Override
    public String upload(OssUploadRequest request) {
        String path = request.getPath();
        InputStream inputStream = request.getInputStream();

        try {
            File dest = FileUtil.file(basePath, path);
            FileUtil.writeFromStream(inputStream, dest);

            // 获取过期时间，默认3600秒
            long timeout = request.getExpires() != null ? request.getExpires() : 3600L;
            return getUrl(path, timeout);
        } catch (Exception e) {
            log.error("Local upload failed", e);
            throw new RuntimeException("Local upload failed", e);
        }
    }

    @Override
    public InputStream download(String path) {
        try {
            File file = FileUtil.file(basePath, path);
            if (!file.exists()) {
                throw new RuntimeException("File not found: " + path);
            }
            return FileUtil.getInputStream(file);
        } catch (Exception e) {
            log.error("Local download failed", e);
            throw new RuntimeException("Local download failed", e);
        }
    }

    @Override
    public Map<String, String> getObjectMetadata(String path) {
        // 本地文件系统暂不支持存储/读取自定义元数据
        return Collections.emptyMap();
    }

    @Override
    public void delete(String path) {
        try {
            FileUtil.del(FileUtil.file(basePath, path));
        } catch (Exception e) {
            log.error("Local delete failed", e);
            throw new RuntimeException("Local delete failed", e);
        }
    }

    @Override
    public String getUrl(String path, long timeout) {
        return getUrl(path, timeout, null);
    }

    @Override
    public String getUrl(String path, long timeout, String downloadFilename) {
        // 如果提供了endpoint，则拼接；否则返回本地路径
        // 注意：Local模式下的静态资源服务器通常不支持通过URL参数动态修改Content-Disposition
        // 因此这里忽略 downloadFilename 参数
        if (StrUtil.isNotBlank(endpoint)) {
            String ep = endpoint;
            if (!ep.endsWith("/")) {
                ep += "/";
            }
            return ep + path;
        }
        return path;
    }
}
