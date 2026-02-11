package com.ytx.ai.oss.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

import java.util.Date;

/**
 * OSS路径生成工具类
 * 提供标准化的路径生成规则，解决文件重名覆盖问题
 */
public class OssPathUtil {

    /**
     * 生成基于日期和UUID的唯一路径 (不保留原文件名)
     * 格式: yyyy/MM/dd/{uuid}.{ext}
     * 适用场景: 对文件名保密性有要求，或者文件名包含特殊字符可能导致问题的场景
     *
     * @param originalFilename 原始文件名 (用于获取扩展名)
     * @return 生成的唯一路径
     */
    public static String generateDateUuidPath(String originalFilename) {
        String ext = FileUtil.extName(originalFilename);
        String uuid = IdUtil.simpleUUID();
        String datePath = DateUtil.format(new Date(), "yyyy/MM/dd");

        if (StrUtil.isNotBlank(ext)) {
            return StrUtil.format("{}/{}.{}", datePath, uuid, ext);
        } else {
            return StrUtil.format("{}/{}", datePath, uuid);
        }
    }

    /**
     * 生成基于日期和UUID的唯一路径 (保留原文件名)
     * 格式: yyyy/MM/dd/{uuid}-{originalFilename}
     * 适用场景: 需要保留原始文件名以便于识别和管理的场景
     * 注意: 会自动清理原始文件名中的特殊字符
     *
     * @param originalFilename 原始文件名
     * @return 生成的唯一路径
     */
    public static String generateDateUuidOriginalPath(String originalFilename) {
        String uuid = IdUtil.simpleUUID();
        String datePath = DateUtil.format(new Date(), "yyyy/MM/dd");

        // 简单的文件名清理，防止路径遍历或特殊字符问题
        String cleanName = FileUtil.getName(originalFilename);
        // 替换掉可能影响URL的字符
        cleanName = cleanName.replaceAll("[^a-zA-Z0-9._-]", "_");

        return StrUtil.format("{}/{}-{}", datePath, uuid, cleanName);
    }

    /**
     * 生成纯UUID路径
     * 格式: {uuid}.{ext}
     *
     * @param originalFilename 原始文件名
     * @return 生成的唯一路径
     */
    public static String generateUuidPath(String originalFilename) {
        String ext = FileUtil.extName(originalFilename);
        String uuid = IdUtil.simpleUUID();

        if (StrUtil.isNotBlank(ext)) {
            return StrUtil.format("{}.{}", uuid, ext);
        } else {
            return uuid;
        }
    }
}

