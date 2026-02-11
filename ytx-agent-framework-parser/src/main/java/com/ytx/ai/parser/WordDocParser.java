package com.ytx.ai.parser;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjectUtil;
import com.aspose.words.*;
import com.ytx.ai.parser.constant.ParseConstants;
import com.ytx.ai.parser.util.ImageUtils;
import com.ytx.ai.parser.vo.DocumentParseOption;
import com.ytx.ai.parser.vo.DocumentVO;
import com.ytx.ai.parser.vo.ImageVO;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.ytx.ai.parser.constant.ParseConstants.DEFAULT_IMAGE_WIDTH;

@Slf4j
public class WordDocParser extends BaseOfficeParser {

    @Override
    public DocumentVO parse(DocumentParseOption parseOption) {
        DocumentVO documentVO = DocumentVO.of(parseOption);
        List<ImageVO> pageImages = new ArrayList<>();
        documentVO.setPageImages(pageImages);
        Document doc = null;

        try (InputStream inputStream = IoUtil.toStream(parseOption.getFileBytes())) {
            FileFormatInfo formatInfo = FileFormatUtil.detectFileFormat(inputStream);
        } catch (Exception e) {
            log.error("detect file {} format error:{}", parseOption.getName(), e.getMessage());
        }

        try (InputStream inputStream = IoUtil.toStream(parseOption.getFileBytes())) {

            // 加载docx文档
            // 同步创建 Aspose Document，避免并发引起的内部状态异常
            doc = new Document(inputStream);
            documentVO.setText(doc.getText());
            FileFormatInfo formatInfo = FileFormatUtil.detectFileFormat(inputStream);
            // 指定输出格式为图片
            int saveFormat = getSaveFormat(parseOption.getParseDefaultImageType());
            ImageSaveOptions options = new ImageSaveOptions(saveFormat);
            /**Aspose.Words 内部使用的是 “点”（point） 作为文档度量单位。
             * 1 英寸（inch） = 72 points
             *页面宽度通常以 points 表示，比如 A4 宽度约为 595 points。
             */
            double widthInches = doc.getPageInfo(0).getWidthInPoints() / ParseConstants.INCH_1_POINTS;
            //DPI（Dots Per Inch），即每英寸像素点数量。
            //如果页面宽度是 8 英寸，设置 DPI = 300，则像素宽度 = 8 × 300 = 2400px。
            int dpi = (int) (DEFAULT_IMAGE_WIDTH / widthInches);
            options.setResolution(dpi);
            options.setMemoryOptimization(true);
            options.setOptimizeOutput(true);
            options.setJpegQuality(95);
            // 遍历文档的每一页，保存为独立的图片
            for (int i = 0; i < doc.getPageCount(); i++) {
                // 使用 ByteArrayOutputStream 保存图片
                try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                    options.setPageSet(new PageSet(i));
                    doc.save(byteArrayOutputStream, options);
                    ImageVO imageVO = new ImageVO();
                    imageVO.setIndex(i);
                    imageVO.setExt(getExtName(saveFormat));
                    imageVO.setBytes(byteArrayOutputStream.toByteArray());
                    convertImgIfNecessary(imageVO, parseOption);
                    pageImages.add(imageVO);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            scaleImages(pageImages, parseOption.getScaleOption());
            if (parseOption.isAutoMerge() && ObjectUtil.isNotEmpty(pageImages)) {
                ImageVO mergedImg = ImageUtils.mergeImage(pageImages, parseOption.getParseDefaultImageType());
                scaleMergedImages(mergedImg, parseOption.getScaleOption());
                documentVO.setMergedImage(mergedImg);
            }

        } catch (Exception e) {
            log.error("parse file {} error:{}", parseOption.getName(), e.getMessage());
            throw new RuntimeException(e);
        } finally {
            if (doc != null) {
                try {
                    doc.cleanup();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return documentVO;
    }
}