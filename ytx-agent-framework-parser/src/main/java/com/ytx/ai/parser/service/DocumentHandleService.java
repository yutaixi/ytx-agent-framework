package com.ytx.ai.parser.service;


import com.ytx.ai.parser.vo.DocumentParseOption;
import com.ytx.ai.parser.vo.DocumentVO;

/**
 * @author taixi.yu
 * @version 1.0.0
 * @className
 * @date 2024/05/29
 */
public interface DocumentHandleService {

    /**
     * 文档解析(有异常，调用者处理)
     *
     * @param parseOption
     * @return {@code Document }
     * @date 2024/05/30
     */
    DocumentVO documentParser(DocumentParseOption parseOption);

    /**
     * 将字节数组转换为带scheme的base64字符串
     * @param bytes 字节数组
     * @return 带scheme的base64字符串
     */
    String toBase64WithScheme(byte[] bytes);


}