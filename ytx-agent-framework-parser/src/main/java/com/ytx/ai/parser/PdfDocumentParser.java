package com.ytx.ai.parser;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.ytx.ai.parser.constant.ParseConstants;
import com.ytx.ai.parser.util.ImageUtils;
import com.ytx.ai.parser.vo.DocumentParseOption;
import com.ytx.ai.parser.vo.DocumentVO;
import com.ytx.ai.parser.vo.ImageVO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.ytx.ai.parser.constant.ParseConstants.INCH_1_POINTS;

public class PdfDocumentParser implements DocumentParser{

    private static final int MAX_WIDTH=1024;

    @Override
    public DocumentVO parse(DocumentParseOption parseOption) {
        DocumentVO documentVO =DocumentVO.of(parseOption);
        List<ImageVO> pageImages=new ArrayList<>();
        documentVO.setPageImages(pageImages);

        String parseDefaultImageType=parseOption.getParseDefaultImageType();


        try(InputStream inputStream = IoUtil.toStream(parseOption.getFileBytes());
            PDDocument pdfDocument = PDDocument.load(inputStream)) {
            //提取文字
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // 可能对复杂字体有帮助
            String text = stripper.getText(pdfDocument);
            if(StrUtil.isBlank(text)){
                text="";
            }
            documentVO.setText(text);

            PDFRenderer pdfRenderer = new PDFRenderer(pdfDocument);
            int pageCount = pdfDocument.getNumberOfPages();
            int maxPages=parseOption.getMaxPages()>0?parseOption.getMaxPages():pageCount;
            for (int page = 0; page < pageCount && page<maxPages; page++) {
                // 渲染页面为图片
                BufferedImage image =null;
                try {
                    float dpi=calculateDpi(pdfDocument.getPage(page));
                    image = pdfRenderer.renderImageWithDPI(page, dpi,ImageType.RGB);
                    // 将图片转换为字节数组
                    byte[] bytes = ImageUtils.toBytes(image, parseDefaultImageType);
                    // 构造 ImageVO 对象
                    ImageVO imageVO = new ImageVO();
                    imageVO.setIndex(page);
                    imageVO.setBytes(bytes);
                    imageVO.setExt(parseDefaultImageType);
                    // 添加到集合
                    pageImages.add(imageVO);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 显式释放 BufferedImage 对象
                    if (image != null) {
                        image.flush();
                    }
                }
            }
            scaleImages(pageImages,parseOption.getScaleOption());
            if(parseOption.isAutoMerge() && ObjectUtil.isNotEmpty(pageImages)){
                ImageVO mergedImg= ImageUtils.mergeImage(pageImages,parseOption.getParseDefaultImageType());
                scaleMergedImages(mergedImg, parseOption.getScaleOption());
                documentVO.setMergedImage(mergedImg);
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return documentVO;
    }



//    private float calculateScaleFactor(PDPage pdPage,DocumentParseOption parseOption) {
//        float scaleFactor = 1.0F;
//        if(ObjectUtil.isEmpty(pdPage) || ObjectUtil.isEmpty(parseOption.getScaleOption())
//        || ObjectUtil.isEmpty(parseOption.getScaleOption().getMaxDimension()) ){
//            return scaleFactor;
//        }
//        // 获取页面的尺寸，单位是点（points）
//        PDRectangle mediaBox = pdPage.getMediaBox();
//        if(ObjectUtil.isEmpty(mediaBox)){
//            return scaleFactor;
//        }
//        float widthPoints = mediaBox.getWidth();
//        float heightPoints = mediaBox.getHeight();
//        // 计算缩放比例
//        float scaleWidth = parseOption.getScaleOption().getMaxDimension() / widthPoints;
////        float scaleHeight = parseOption.getScaleOption().getMaxDimension() / heightPoints;
////        scaleFactor = Math.min(scaleWidth, scaleHeight);
//        scaleFactor=scaleWidth;
//        if(scaleFactor>1){
//            scaleFactor=1;
//        }
//        return scaleFactor;
//    }
    /**
     * 根据 PDF 页面宽度计算渲染使用的 DPI。
     * 参考 Word 实现：widthPoints / 72 -> 宽度英寸，dpi = DEFAULT_IMAGE_WIDTH / widthInches
     */
    private float calculateDpi(PDPage pdPage) {
        if (ObjectUtil.isEmpty(pdPage)) {
            return ParseConstants.IMG_DPI;
        }
        PDRectangle mediaBox = pdPage.getMediaBox();
        if (ObjectUtil.isEmpty(mediaBox)) {
            return ParseConstants.IMG_DPI;
        }
        float widthPoints = mediaBox.getWidth();
        if (widthPoints <= 0) {
            return ParseConstants.IMG_DPI;
        }
        float widthInches = widthPoints / INCH_1_POINTS; // 1 inch = 72 points
        float dpi =(ParseConstants.DEFAULT_IMAGE_WIDTH / widthInches);
        // 限制 DPI 合理范围，防止极端值（可根据需求调整上下限）
        dpi = Math.max(dpi, 47);    // 最低 72 DPI
        dpi = Math.min(dpi, 300);  // 最高 300 DPI
        return dpi;
    }
    private float calculateScaleFactor(PDPage pdPage) {
        float scaleFactor = 1.0F;
        if(ObjectUtil.isEmpty(pdPage) ){
            return scaleFactor;
        }
        // 获取页面的尺寸，单位是点（points）
        PDRectangle mediaBox = pdPage.getMediaBox();
        if(ObjectUtil.isEmpty(mediaBox)){
            return scaleFactor;
        }
        float widthPoints = mediaBox.getWidth();
        // 计算缩放比例
        scaleFactor = MAX_WIDTH / widthPoints;
        if(scaleFactor>1){
            scaleFactor=1;
        }
        return scaleFactor;
    }



}
