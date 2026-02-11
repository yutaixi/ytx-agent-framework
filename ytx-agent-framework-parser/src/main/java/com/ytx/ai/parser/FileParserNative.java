package com.ytx.ai.parser;

import com.ytx.ai.base.util.JniLibraryLoader;
import com.ytx.ai.parser.vo.DocumentParseOption;
import com.ytx.ai.parser.vo.DocumentVO;
import lombok.extern.slf4j.Slf4j;

/**
 * Java JNI 包装类，用于调用 Rust file-parser 库
 *
 * 该类提供了与 Rust 实现的文件解析器交互的接口
 * 使用对象传递方式，与 llm 模块保持一致
 */
@Slf4j
public class FileParserNative {

    // 加载本地库
    static {
        JniLibraryLoader.load("file_parser");
    }

    /**
     * 解析文档
     *
     * @param parseOption 文档解析选项
     * @return DocumentVO 对象，包含解析结果。如果解析失败，返回基于parseOption创建的默认DocumentVO
     */
    public static DocumentVO parse(DocumentParseOption parseOption) {
        try {
            // 直接传递Java对象给native方法
            DocumentVO result = parseNative(parseOption);

            if (result == null) {
                log.error("Native parse returned null");
                return DocumentVO.of(parseOption);
            }

            return result;
        } catch (Exception e) {
            log.error("Failed to parse document: {}", e.getMessage(), e);
            return DocumentVO.of(parseOption);
        }
    }

    /**
     * 解析文档 - JNI 原生方法
     *
     * @param parseOption Java DocumentParseOption 对象
     * @return Java DocumentVO 对象，如果失败返回 null
     */
    private static native DocumentVO parseNative(DocumentParseOption parseOption);
}

