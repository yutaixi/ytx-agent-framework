package com.ytx.ai.parser;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjectUtil;
import com.aspose.words.Document;
import com.aspose.words.ImageSaveOptions;
import com.aspose.words.PageSet;
import com.ytx.ai.parser.constant.ParseConstants;
import com.ytx.ai.parser.util.ImageUtils;
import com.ytx.ai.parser.vo.DocumentParseOption;
import com.ytx.ai.parser.vo.DocumentVO;
import com.ytx.ai.parser.vo.ImageVO;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class HtmlParser extends BaseOfficeParser{

    @Autowired
    private TextDocumentParser textDocumentParser;


    @Override
    public DocumentVO parse(DocumentParseOption parseOption) {
        DocumentVO document = DocumentVO.of(parseOption);
        List<ImageVO> pageImages = new ArrayList<>();
        document.setPageImages(pageImages);

        try (InputStream inputStream = IoUtil.toStream(parseOption.getFileBytes())) {
            dev.langchain4j.data.document.Document htmlDocument = textDocumentParser.parse(inputStream);
            // 1. HTML 字符串
            String htmlContent = htmlDocument.text();
            document.setText(htmlContent);
            Document wordDocument =null;
            // 使用 Aspose.Words 将 HTML 转换为文档
            try (ByteArrayInputStream htmlStream = new ByteArrayInputStream(htmlContent.getBytes(StandardCharsets.UTF_16))) {
                wordDocument = new Document(htmlStream);

                // 创建 ImageSaveOptions 实例并设置图片质量
                int saveFormat=getSaveFormat(parseOption.getParseDefaultImageType());
                ImageSaveOptions options = new ImageSaveOptions(saveFormat);
                options.setResolution(ParseConstants.IMG_DPI);
                for (int index = 0; index < wordDocument.getPageCount(); index++) {
                    options.setPageSet(new PageSet(index));
                    // 将文档保存为图片格式
                    try (ByteArrayOutputStream imageOutputStream = new ByteArrayOutputStream()) {
                        wordDocument.save(imageOutputStream, options); // 保存为 PNG 图片
                        byte[] imgBytes = imageOutputStream.toByteArray();
                        ImageVO imageVO = new ImageVO();
                        imageVO.setExt(getExtName(saveFormat));
                        imageVO.setIndex(index);
                        imageVO.setBytes(imgBytes);

                        convertImgIfNecessary(imageVO,parseOption);
                        pageImages.add(imageVO); // 将图片加入到列表
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                scaleImages(pageImages,parseOption.getScaleOption());

                // 整合为一张图片
                if (parseOption.isAutoMerge() && ObjectUtil.isNotEmpty(pageImages)) {
                    ImageVO mergedImg = ImageUtils.mergeImage(pageImages,parseOption.getParseDefaultImageType());
                    scaleMergedImages(mergedImg, parseOption.getScaleOption());
                    document.setMergedImage(mergedImg);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            finally {
                if(wordDocument!=null){
                    wordDocument.cleanup();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return document;
    }



    public HtmlParser setTextDocumentParser(TextDocumentParser textDocumentParser) {
        this.textDocumentParser = textDocumentParser;
        return this;
    }

}
