package com.ytx.ai.oss.service;

import com.ytx.ai.oss.vo.OssUploadRequest;

import java.io.InputStream;
import java.util.Map;

/**
 * OSS客户端接口
 * 定义文件存储的通用操作
 */
public interface OssClient {

    /**
     * 上传文件
     *
     * @param request 上传请求参数对象 (包含流、路径、MD5、过期时间等)
     * @return 文件访问URL
     */
    String upload(OssUploadRequest request);

    /**
     * 获取文件访问URL (支持自定义过期时间)
     *
     * @param path    文件路径
     * @param timeout 超时时间 (秒)
     * @return URL字符串
     */
    String getUrl(String path, long timeout);

    /**
     * 获取文件访问URL (支持自定义下载文件名)
     * 最佳实践: 通过OSS预签名URL的Content-Disposition头实现下载时重命名
     *
     * @param path             文件路径
     * @param timeout          超时时间 (秒)
     * @param downloadFilename 下载时显示的文件名 (包含扩展名)
     * @return 带签名的URL字符串
     */
    String getUrl(String path, long timeout, String downloadFilename);

    /**
     * 下载文件
     *
     * @param path 文件路径
     * @return 文件流
     */
    InputStream download(String path);

    /**
     * 获取文件元数据 (User Metadata)
     *
     * @param path 文件路径
     * @return 元数据Map (key: 属性名, value: 属性值)
     */
    Map<String, String> getObjectMetadata(String path);

    /**
     * 删除文件
     *
     * @param path 文件路径
     */
    void delete(String path);


}