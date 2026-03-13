package com.ytx.ai.parser;

import cn.hutool.core.io.IoUtil;
import com.aspose.cells.*;
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
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
public class CellsParser extends BaseOfficeParser{

    private static final int MAX_ROW=100;

    @Override
    public DocumentVO parse(DocumentParseOption parseOption) {
        DocumentVO document=DocumentVO.of(parseOption);
        List<ImageVO> pageImages=new ArrayList<>();
        document.setPageImages(pageImages);
        Workbook workbook =null;
        AtomicInteger pageCount=new AtomicInteger(0);
        try (InputStream inputStream = IoUtil.toStream(parseOption.getFileBytes())) {
            // 加载 Excel 文件
            workbook = new Workbook(inputStream);
            // 遍历所有工作表
            for (int sheetIndex = 0; sheetIndex < workbook.getWorksheets().getCount(); sheetIndex++) {
                Worksheet sheet = workbook.getWorksheets().get(sheetIndex);
                // 获取工作表中的 Cells 对象
                Cells cells = sheet.getCells();
                // 获取数据的最大行号和最大列号
                int maxDataRow = cells.getMaxDataRow();
                int dpi=calculateDpi(sheet);
                if(maxDataRow>MAX_ROW){
                    dpi=(int)Math.round(dpi* Math.sqrt(MAX_ROW*1.0/maxDataRow));
                }

                // 设置图片保存选项
                ImageOrPrintOptions options = new ImageOrPrintOptions();
                int imageType=getImageType(parseOption.getParseDefaultImageType());
                options.setImageType(imageType);
                options.setOnePagePerSheet(true);
                options.setQuality(100);
                options.setHorizontalResolution(dpi);
                options.setVerticalResolution(dpi);
                options.setOnlyArea(true);
                options.setCellAutoFit(true);
//                options.setDesiredSize(1920,1080);

                // 渲染为图片
                SheetRender render = new SheetRender(sheet, options);
                // 遍历当前工作表的所有页面
                for (int pageIndex = 0; pageIndex < render.getPageCount(); pageIndex++) {
                    // 创建一个ByteArrayOutputStream来存储图片数据
                    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                        // 将当前页的图片写入输出流
                        render.toImage(pageIndex, byteArrayOutputStream);
                        ImageVO pageImage=new ImageVO();
                        pageImage.setExt(getName(imageType));
                        pageImage.setIndex(pageCount.get());
                        pageImage.setBytes(byteArrayOutputStream.toByteArray());

                        convertImgIfNecessary(pageImage,parseOption);
                        scaleImage(pageImage,parseOption.getScaleOption());
                        pageImages.add(pageImage);
                        pageCount.getAndIncrement();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                if(parseOption.isAutoMerge()){
                    ImageVO mergedImg= ImageUtils.mergeImage(pageImages,parseOption.getParseDefaultImageType());
                    scaleMergedImages(mergedImg, parseOption.getScaleOption());
                    document.setMergedImage(mergedImg);
                }

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            if(workbook!=null){
                workbook.dispose();
            }
        }
        return document;
    }

    // java
    private int calculateDpi(Worksheet sheet) {
        if (sheet == null) {
            return (int) ParseConstants.IMG_DPI;
        }
        double widthInches = -1;
        // 1) 尝试从 PageSetup 获取页面宽度（一些 API 返回英寸，有些返回点）
        try {
            PageSetup ps = sheet.getPageSetup();
            double pageWidth = ps.getPaperWidth(); // 可能是英寸或点
            if (pageWidth > 0) {
                // 如果值很大（>100），很可能是以 points(pts) 返回，转为英寸
                if (pageWidth > 100) {
                    widthInches = pageWidth / 72.0;
                } else {
                    widthInches =pageWidth>=10? pageWidth*0.75:pageWidth;
                }
            }
        } catch (Exception ignored) {
        }

        // 2) 如果 pageWidth 不可用，累加列宽（优先使用像素方法，回退到字符宽度估算）
        if (widthInches <= 0) {
            try {
                Cells cells = sheet.getCells();
                int maxCol = Math.max(0, cells.getMaxDataColumn());
                double totalPixels = 0;
                for (int c = 0; c <= maxCol; c++) {
                    try {
                        // 尝试使用像素宽度（若 API 可用）
                        totalPixels += cells.getColumnWidthPixel(c);
                    } catch (Exception ex) {
                        // 回退：使用字符宽度乘以常数估算像素（经验值）
                        totalPixels += cells.getColumnWidth(c) * 7.0;
                    }
                }
                if (totalPixels > 0) {
                    final double DEFAULT_SCREEN_DPI = 96.0; // 常用屏幕像素密度，作为像素->英寸转换
                    widthInches = totalPixels / DEFAULT_SCREEN_DPI;
                }
            } catch (Exception ignored) {
            }
        }

        // 3) 最终 fallback
        if (widthInches <= 0) {
            return (int) ParseConstants.IMG_DPI;
        }

        // 4) 按 Pdf 同样的公式计算 dpi，并限制范围
        double dpi = ParseConstants.DEFAULT_IMAGE_WIDTH / widthInches;
        dpi = Math.max(dpi, 72);   // 最低 72 DPI
        dpi = Math.min(dpi, 300);  // 最高 300 DPI
        return (int) Math.round(dpi);
    }

    private int getImageType(String imageType){

        switch (imageType){
            case "png":
                return ImageType.PNG;
            case "jpeg":
            case "jpg":
                return ImageType.JPEG;
            case "bmp":
                return ImageType.BMP;
            case "gif":
                return ImageType.GIF;
            case "tiff":
                return ImageType.TIFF;
            case "webp":
                return ImageType.JPEG;
            default:
                return ImageType.JPEG;
        }
    }

    private String getName(int type){
        return switch (type) {
            case ImageType.PNG -> "png";
            case ImageType.JPEG -> "jpeg";
            case ImageType.BMP -> "bmp";
            case ImageType.GIF -> "gif";
            case ImageType.TIFF -> "tiff";
            case ImageType.WEB_P -> "webp";
            default -> "unknown";
        };
    }
}
