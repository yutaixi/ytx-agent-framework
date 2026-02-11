package com.ytx.ai.base.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpUtil;
import org.apache.tika.Tika;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypes;

public class FileUtils {

    private static final String HTTP_PROTOCOL="http://";
    private static final String HTTPS_PROTOCOL="https://";
    private static final String FTP_PROTOCOL="ftp://";


    public static byte[] toByteArray(String url){
        if(isRemoteURL(url)){
            return HttpUtil.downloadBytes(url);
        }
        return FileUtil.readBytes(url);
    }

    public static boolean isRemoteURL(String path) {
        try {
            String formatedUrl=path.toLowerCase();
            return formatedUrl.startsWith(HTTP_PROTOCOL) || formatedUrl.startsWith(HTTPS_PROTOCOL) || formatedUrl.startsWith(FTP_PROTOCOL);
        } catch (Exception e) {
            return false;
        }
    }

    public static String extName(String filePath){
        if (ObjectUtil.isEmpty(filePath)) {
            return "";
        }

        // 1. 优先处理 URL 参数问题
        // 如果路径中包含 '?'，直接截取 '?' 之前的部分，确保后续提取不受参数内容干扰
        int queryIndex = filePath.indexOf('?');
        if (queryIndex != -1) {
            filePath = filePath.substring(0, queryIndex);
        }

        // 2. 如果考虑得更周全，也可以处理 '#' (锚点)
        int anchorIndex = filePath.indexOf('#');
        if (anchorIndex != -1) {
            filePath = filePath.substring(0, anchorIndex);
        }
        // 3. 此时 filePath 已经是纯净的路径，再提取后缀
        String ext = FileUtil.extName(filePath);

        return ObjectUtil.isEmpty(ext) ? "" : ext;
    }

    public static String getName(String filePath){
        String name= FileUtil.getName(filePath);
        if(name.contains("?")){
            name=name.substring(0,name.indexOf("?"));
        }
        return name;
    }

    /**
     * 计算byte数组占用内存大小，单位MB
     */
    public static double calculateByteArraySizeMB(byte[] bytes) {
        if (ObjectUtil.isEmpty(bytes)) {
            return 0.0;
        }
        double sizeInBytes = bytes.length;
        return sizeInBytes / (1024 * 1024);
    }

    /**
     * 根据byte数组获取文件后缀
     * @param bytes 文件字节数组
     * @return 文件后缀，例如：pdf, png等
     */
    public static String getExt(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        try {
            Tika tika = new Tika();
            String mimeType = tika.detect(bytes);
            MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
            MimeType type = allTypes.forName(mimeType);
            String extension = type.getExtension();
            if (ObjectUtil.isEmpty(extension)) {
                return "";
            }
            // extension usually contains dot, e.g. ".pdf"
            return extension.startsWith(".") ? extension.substring(1) : extension;
        } catch (Exception e) {
            return "";
        }
    }
}
