package com.ytx.ai.parser.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.codec.Base64;
import com.ytx.ai.base.util.FileUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ImageDataURISchemeMapper {
    private static Map<String, String> scheme2Extension = new HashMap<String, String>();
    private static Map<String, String> extension2Scheme = new HashMap<String, String>();

    static {
        initSchemeSupported();
    }

    public static String getSchemeByExt(String ext){
        String result = extension2Scheme.get(ext.toLowerCase());
        return result == null ? "" : result;
    }

    public static String getSchemeByUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        // Remove query parameters if present
        int queryParamIndex = url.indexOf("?");
        if (queryParamIndex != -1) {
            url = url.substring(0, queryParamIndex);
        }
        String extName=FileUtil.extName(url);
        String result = extension2Scheme.get(extName.toLowerCase());
        return result == null ? "" : result;
    }

    public static String getScheme(File image) {
        if (image == null) {
            return "";
        }
        String name = image.getName();
        int lastPointIndex = name.lastIndexOf(".");
        String ext = lastPointIndex < 0 ? "" : name.substring(lastPointIndex + 1);
        return getSchemeByExt(ext);
    }

    public static String getContentType(String fileExtension) {
        String scheme = extension2Scheme.get(fileExtension.toLowerCase());
        if (scheme == null || scheme.isEmpty()) {
            return "application/octet-stream";
        }
        return scheme.substring(5, scheme.indexOf(";"));
    }

    public static String getExtension(String dataUrlScheme) {
        return scheme2Extension.get(dataUrlScheme);
    }

    public static String getExtensionFromImageBase64(String imageBase64, String defaultExtension) {
        int firstComma = imageBase64.indexOf(",");
        if (firstComma < 0) {
            return defaultExtension;
        }
        return scheme2Extension.get(imageBase64.subSequence(0, firstComma + 1));
    }

    public static String toBase64WithScheme(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        String ext = FileUtils.getExt(bytes);
        String scheme = getSchemeByExt(ext);
        return scheme + Base64.encode(bytes);
    }

    private static void initSchemeSupported() {
        addScheme("jpg", "data:image/jpeg;base64,");
        addScheme("jpeg", "data:image/jpeg;base64,");
        addScheme("png", "data:image/png;base64,");
        addScheme("gif", "data:image/gif;base64,");
        addScheme("icon", "data:image/x-icon;base64,");
        addScheme("jfif", "data:image/jfif;base64,");
        // 新增 WebP 支持
        addScheme("webp", "data:image/webp;base64,");
        addScheme("bmp", "data:image/bmp;base64,");

        addScheme("pdf", "data:application/pdf;base64,");
        // HTML 网页
        addScheme("html", "data:text/html;base64,");

        // Word 文档 (老版本 .doc 和 新版本 .docx)
        addScheme("doc", "data:application/msword;base64,");
        addScheme("docx", "data:application/vnd.openxmlformats-officedocument.wordprocessingml.document;base64,");

        // Excel 表格 (老版本 .xls 和 新版本 .xlsx)
        addScheme("xls", "data:application/vnd.ms-excel;base64,");
        addScheme("xlsx", "data:application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;base64,");

        // PowerPoint 演示文稿
        addScheme("ppt", "data:application/vnd.ms-powerpoint;base64,");
        addScheme("pptx", "data:application/vnd.openxmlformats-officedocument.presentationml.presentation;base64,");

        // 文本文件
        addScheme("txt", "data:text/plain;base64,");
        addScheme("csv", "data:text/csv;base64,");
        addScheme("md", "data:text/markdown;base64,");

        // 数据格式
        addScheme("json", "data:application/json;base64,");
        addScheme("xml", "data:application/xml;base64,");

        // 压缩包
        addScheme("zip", "data:application/zip;base64,");
        addScheme("rar", "data:application/x-rar-compressed;base64,");
        addScheme("7z", "data:application/x-7z-compressed;base64,");
        addScheme("tar", "data:application/x-tar;base64,");
        addScheme("gz", "data:application/gzip;base64,");

        // 音频/视频
        addScheme("mp3", "data:audio/mpeg;base64,");
        addScheme("wav", "data:audio/wav;base64,");
        addScheme("mp4", "data:video/mp4;base64,");
        addScheme("webm", "data:video/webm;base64,");

        // 矢量图
        addScheme("svg", "data:image/svg+xml;base64,");
    }

    private static void addScheme(String extension, String dataUrl) {
        scheme2Extension.put(dataUrl, extension);
        extension2Scheme.put(extension, dataUrl);
    }
}
