package com.ytx.ai.oss.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;

/**
 * OSS上传请求参数对象
 * 封装上传所需的所有参数，便于扩展
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OssUploadRequest {

    /**
     * 文件输入流 (必填)
     */
    private InputStream inputStream;

    /**
     * 文件存储路径 (包含文件名) (必填)
     * 例如: images/avatar.jpg
     */
    private String path;

    /**
     * 文件MD5校验值 (可选)
     * 用于校验文件完整性
     */
    private String md5;

    /**
     * 链接过期时间 (单位: 秒) (可选)
     * 上传成功后返回的访问链接的有效期
     * 默认为 3600秒 (1小时)
     */
    private Integer expires;

    /**
     * 文件标签/分类 (可选)
     * 用于文件的分类标识或元数据标记
     */
    private String tag;

    private String originalFilename;

    private String bucketName;

}