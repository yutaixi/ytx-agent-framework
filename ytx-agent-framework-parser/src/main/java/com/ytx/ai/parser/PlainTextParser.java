package com.ytx.ai.parser;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.parser.constant.ParseConstants;
import com.ytx.ai.parser.vo.DocumentParseOption;
import com.ytx.ai.parser.vo.DocumentVO;
import com.ytx.ai.parser.vo.ImageVO;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.TranscodingHints;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.JPEGTranscoder;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class PlainTextParser implements DocumentParser{

    @Override
    public DocumentVO parse(DocumentParseOption parseOption) {
        DocumentVO parsedDocument=DocumentVO.of(parseOption);
        List<ImageVO> pageImages=new ArrayList<>();
        parsedDocument.setPageImages(pageImages);
        try(InputStream inputStream = IoUtil.toStream(parseOption.getFileBytes());
            ByteArrayOutputStream buffer = new ByteArrayOutputStream()){
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            String text = buffer.toString(StandardCharsets.UTF_8);
            parsedDocument.setText(text);
            if(ObjectUtil.isEmpty(text)){
                return parsedDocument;
            }
            ImageVO image=new ImageVO();
            image.setExt(ParseConstants.IMG_JPEG);
            image.setIndex(0);
            byte[] bytes= convertTextToJpeg(text,"Microsoft YaHei, SimSun, Arial, sans-serif",14);
            image.setBytes(bytes);

            convertImgIfNecessary(image,parseOption);
            scaleImage(image,parseOption.getScaleOption());
            pageImages.add(image);
            if(parseOption.isAutoMerge()){
                parsedDocument.setMergedImage(image);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return parsedDocument;
    }


    public static byte[] convertTextToJpeg(String content, String fontFamily, int fontSize) throws Exception {
        try  {
            String svgContent = createSvgFromText(content, fontFamily, fontSize);
            // 获取实际计算宽度
            int actualWidth = Integer.parseInt(svgContent.substring(
                    svgContent.indexOf("width=\"") + 7,
                    svgContent.indexOf("\"", svgContent.indexOf("width=\"") + 7)
            ));
            // 新增：获取实际计算高度
            int totalHeight = Integer.parseInt(svgContent.substring(
                    svgContent.indexOf("height=\"") + 8,
                    svgContent.indexOf("\"", svgContent.indexOf("height=\"") + 8)
            ));

            // 增加Transcoder内存配置
            TranscodingHints hints = new TranscodingHints();
            // 增加DPI配置（300dpi）
            hints.put(JPEGTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER, 0.08466666666666667f); // 300dpi
            hints.put(JPEGTranscoder.KEY_AOI, new Rectangle(0, 0, actualWidth, totalHeight));
            // 添加DOM实现配置
            hints.put(JPEGTranscoder.KEY_DOM_IMPLEMENTATION,
                    GenericDOMImplementation.getDOMImplementation());
            // 新增SVG命名空间配置
            hints.put(JPEGTranscoder.KEY_DOCUMENT_ELEMENT_NAMESPACE_URI,
                    "http://www.w3.org/2000/svg");
            hints.put(JPEGTranscoder.KEY_DOCUMENT_ELEMENT, "svg");
            hints.put(ImageTranscoder.KEY_WIDTH, (float)actualWidth);
            hints.put(ImageTranscoder.KEY_EXECUTE_ONLOAD, true);
            hints.put(JPEGTranscoder.KEY_FORCE_TRANSPARENT_WHITE, Boolean.TRUE);

            // 关键：设置JPEG输出质量
            hints.put(JPEGTranscoder.KEY_QUALITY, 1.0f); // 最大质量，取值范围0.0~1.0

            try (InputStream svgStream = new ByteArrayInputStream(svgContent.getBytes(StandardCharsets.UTF_8));
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

                JPEGTranscoder transcoder = new JPEGTranscoder();
                transcoder.setTranscodingHints(hints);
                transcoder.transcode(new TranscoderInput(svgStream), new TranscoderOutput(baos));
                return baos.toByteArray();
            }
        }catch (Exception e){
            throw new RuntimeException("Batik转换失败", e); // 改进异常处理
        }
    }

    // 改进版SVG生成（动态计算高度）
    private static String createSvgFromText(String text, String fontFamily, int fontSize) {
        BufferedImage dummyImage =null;
        String result=null;
        try{
            Font font = new Font(fontFamily, Font.PLAIN, fontSize);
            dummyImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            FontMetrics fm = dummyImage.getGraphics().getFontMetrics(font);
            // 新增自动换行逻辑
            int maxOriginalWidth = 0;
            for (String originalLine : text.split("\n")) {
                maxOriginalWidth = Math.max(maxOriginalWidth, fm.stringWidth(originalLine));
            }
            int actualWidth = Math.min(maxOriginalWidth + 20, 1090); // 左右各留10像素边距
            int maxLineWidth = actualWidth - 20;

            List<String> wrappedLines = new ArrayList<>();
            for (String originalLine : text.split("\n")) {
                wrappedLines.addAll(wrapText(originalLine, fm, maxLineWidth));
            }

            int lineHeight = fm.getHeight();
            int totalHeight = lineHeight * wrappedLines.size() + 20;
            StringBuilder svg = new StringBuilder()
                    .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                    .append("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\"")
                    .append(String.format(" width=\"%d\" height=\"%d\">",actualWidth, totalHeight))
                    .append(" shape-rendering=\"crispEdges\"") // 添加渲染优化
                    .append(" text-rendering=\"geometricPrecision\">") // 文本渲染优化
                    .append("<rect width=\"100%\" height=\"100%\" fill=\"white\"/>")
                    .append(String.format("<g font-family=\"%s\" font-size=\"%d\">",
                            fontFamily.replace("\"", "&quot;"), fontSize));

            for (int i = 0; i < wrappedLines.size(); i++) {
                String escapedLine = wrappedLines.get(i)
                        .replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;");
                svg.append(String.format("<text x=\"10\" y=\"%d\">%s</text>",
                        (i * lineHeight) + fm.getAscent() + 10,
                        escapedLine));
            }
            result=svg.append("</g></svg>").toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            if(ObjectUtil.isNotEmpty(dummyImage)){
                dummyImage.flush();
            }
        }

        return result;
    }

    // 新增自动换行方法
    private static List<String> wrapText(String text, FontMetrics fm, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();

        for (String word : text.split(" ")) {
            if (fm.stringWidth(currentLine + word) < maxWidth) {
                currentLine.append(word).append(" ");
            } else {
                // 处理超长单词
                if (currentLine.length() == 0) {
                    splitLongWord(word, fm, maxWidth, lines);
                } else {
                    lines.add(currentLine.toString().trim());
                    currentLine = new StringBuilder(word + " ");
                }
            }
        }
        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString().trim());
        }
        return lines;
    }

    // 处理无空格超长单词
    private static void splitLongWord(String word, FontMetrics fm, int maxWidth, List<String> lines) {
        int currentIndex = 0;
        while (currentIndex < word.length()) {
            int endIndex = currentIndex + 1;
            while (endIndex <= word.length() &&
                    fm.stringWidth(word.substring(currentIndex, endIndex)) < maxWidth) {
                endIndex++;
            }
            if (endIndex > currentIndex + 1) {
                endIndex--; // 回退到可显示的位置
                lines.add(word.substring(currentIndex, endIndex));
                currentIndex = endIndex;
            } else {
                // 单个字符超过宽度的情况
                lines.add(word.substring(currentIndex, currentIndex + 1));
                currentIndex++;
            }
        }
    }
}