package com.ytx.ai.parser.service;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.ytx.ai.parser.DocumentParser;
import com.ytx.ai.parser.config.DocumentParserProperties;
import com.ytx.ai.parser.ocr.OcrAgent;
import com.ytx.ai.parser.util.ImageDataURISchemeMapper;
import com.ytx.ai.parser.vo.DocumentParseOption;
import com.ytx.ai.parser.vo.DocumentVO;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;

/**
 * @author taixi.yu
 * @version 1.0.0
 * @className
 * @date 2024/05/29
 */
@Slf4j
public class DocumentHandleServiceImpl implements DocumentHandleService {

    @Autowired
    private DocumentParserProperties documentParserProperties;

    @Autowired
    private OcrAgent ocrAgent;

    /**
     * 文档解析(有异常，调用者处理)
     *
     * @param parseOption 解析对象
     * @return {@code Document }
     * @date 2024/05/30
     */
    @Override
    public DocumentVO documentParser(DocumentParseOption parseOption) {
        StopWatch stopWatch=new StopWatch();
        stopWatch.start();
        String ext=parseOption.getExt();
        String className = documentParserProperties.getClassNames().get(ext);
        DocumentVO document;
        if (className != null) {
            document = SpringUtil.getBean(className, DocumentParser.class).parse(parseOption);
        } else {
            document=defaultParser(parseOption);
        }
        stopWatch.stop();
        log.info("文档解析，耗时{}ms",stopWatch.getLastTaskTimeMillis());

        if (parseOption.isOcrIfContentEmpty()) {
            String content = document.getText();
            if (ObjectUtil.isEmpty(content) && ObjectUtil.isNotEmpty(document.getMergedImage())) {
                stopWatch.start();
                String imgContent = ocrAgent.doBiz(document.getMergedImage());
                document.setText(imgContent);
                stopWatch.stop();
                log.info("文档内容OCR，耗时{}ms", stopWatch.getLastTaskTimeMillis());
            }
        }
        return document;
    }

    private DocumentVO defaultParser(DocumentParseOption parseOption){
        try (InputStream inputStream = IoUtil.toStream(parseOption.getFileBytes())){
            Document document = new TextDocumentParser().parse(inputStream);
            DocumentVO documentVO =new DocumentVO();
            documentVO.setText(document.text());
            return documentVO;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将字节数组转换为带scheme的base64字符串
     * @param bytes 字节数组
     * @return 带scheme的base64字符串
     */
    @Override
    public String toBase64WithScheme(byte[] bytes) {
        return ImageDataURISchemeMapper.toBase64WithScheme(bytes);
    }

}