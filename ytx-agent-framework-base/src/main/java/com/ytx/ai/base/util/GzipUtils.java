package com.ytx.ai.base.util;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Slf4j
public class GzipUtils {

    private GzipUtils() {
    }
    public static final String GZIP_ENCODE_UTF_8 = "UTF-8";
    public static final String GZIP_ENCODE_ISO_8859_1 = "ISO-8859-1";
    public static final String GZIP="gzip";

    /**
     * 字符串压缩为GZIP字节数组
     *
     * @param str
     * @return
     */
    public static byte[] compress(String str) {
        return compress(str, GZIP_ENCODE_UTF_8);
    }

    /**
     * 字符串压缩为GZIP字节数组
     *
     * @param str
     * @param encoding
     * @return
     */
    public static byte[] compress(String str, String encoding) {
        if (str == null || str.length() == 0) {
            return new byte[0];
        }
        byte[] result = null;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(out);
        ) {
            gzip.write(str.getBytes(encoding));
            gzip.finish();
            result = out.toByteArray();
        } catch (IOException e) {
            log.error("", e);
        }
        return result;
    }

    public static byte[] compress(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return new byte[0];
        }
        byte[] result = null;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(out);
        ) {
            gzip.write(bytes);
            gzip.finish();
            result = out.toByteArray();
        } catch (IOException e) {
            log.error("gzip failed", e);
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * GZIP解压缩
     *
     * @param bytes
     * @return
     */
    public static byte[] uncompress(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return new byte[0];
        }
        if(!isGzip(bytes)){
            return bytes;
        }
        byte[] result = null;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ByteArrayInputStream in = new ByteArrayInputStream(bytes);
             GZIPInputStream ungzip = new GZIPInputStream(in)) {
            byte[] buffer = new byte[256];
            int n;
            while ((n = ungzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
            result = out.toByteArray();
        } catch (IOException e) {
            log.error("unzip failed", e);
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * 解压并返回String
     *
     * @param bytes
     * @return
     */
    public static String uncompressToString(byte[] bytes) {
        return uncompressToString(bytes, GZIP_ENCODE_UTF_8);
    }

    /**
     * 解压
     *
     * @param bytes
     * @param encoding
     * @return
     */
    public static String uncompressToString(byte[] bytes, String encoding) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        String result=null;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ByteArrayInputStream in = new ByteArrayInputStream(bytes);
             GZIPInputStream ungzip = new GZIPInputStream(in)
        ) {
            byte[] buffer = new byte[256];
            int n;
            while ((n = ungzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
            result= out.toString(encoding);
        } catch (IOException e) {
            log.error("", e);
        }
        return result;
    }

    public static boolean isGzip(byte[] bytes)
    {
        if(bytes==null || bytes.length<2)
        {
            return false;
        }
        int b0 = bytes[0];
        int b1 = bytes[1];
        int magicNumber = (b1&0xFF)<<8 | b0&0xFF;
        boolean isGzip=false;
        if(magicNumber == GZIPInputStream.GZIP_MAGIC)
        {
            isGzip=true;
        }
        return isGzip;
    }

    /**
     public static void main(String[] args) {
     String str = "%5B%7B%22lastUpdateTime%22%3A%222011-10-28+9%3A39%3A41%22%2C%22smsList%22%3A%5B%7B%22liveState%22%3A%221";
     System.out.println("原长度：" + str.length());
     System.out.println("压缩后字符串：" + GzipUtils.compress(str).toString().length());
     System.out.println("解压缩后字符串：" + StringUtils.newStringUtf8(GzipUtils.uncompress(GzipUtils.compress(str))));
     System.out.println("解压缩后字符串：" + GzipUtils.uncompressToString(GzipUtils.compress(str)));
     }*/
}
