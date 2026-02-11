package com.ytx.ai.parser;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjectUtil;
import com.aspose.email.*;
import com.aspose.words.FileFormatUtil;
import com.aspose.words.PageInfo;
import com.aspose.words.*;
import com.ytx.ai.parser.util.ImageUtils;
import com.ytx.ai.parser.vo.DocumentParseOption;
import com.ytx.ai.parser.vo.DocumentVO;
import com.ytx.ai.parser.vo.ImageVO;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ytx.ai.parser.constant.ParseConstants.*;

@Slf4j
public class OutlookMsgParser extends BaseOfficeParser{
    String[] ignoreStrings={
            "<center>This is an evaluation copy of Aspose.Email for Java.</center><br><a href=\"http://www.aspose.com/corporate/purchase/end-user-license-agreement.aspx\"><center>View EULA Online</center></a>",
    };

    @Override
    public DocumentVO parse(DocumentParseOption parseOption) {
        DocumentVO document = DocumentVO.of(parseOption);
        List<ImageVO> pageImages = new ArrayList<>();
        document.setPageImages(pageImages);
        MailMessage mail = null;

        // 检测文件格式
        detectFileFormat(parseOption);

        try (InputStream inputStream = IoUtil.toStream(parseOption.getFileBytes())) {
            // 解析 .msg 文件内容
            mail = MailMessage.load(inputStream);
            String content = mail.getBody();

            // 准备 HTML 内容
            String htmlContent = prepareHtmlContent(mail);

            // 缓存图片资源数据
            Map<String, byte[]> imageDataCache = new HashMap<>();
            Map<String, String> imageExtCache = new HashMap<>();
            cacheLinkedResourceImages(mail.getLinkedResources(), imageDataCache, imageExtCache);

            // 处理 HTML 中的图片引用，将 cid: 引用替换为 base64 data URI
            htmlContent = replaceCidImagesWithBase64(htmlContent, imageDataCache, imageExtCache);

            // 将 HTML 转换为图片
            convertHtmlToImages(htmlContent, pageImages, parseOption);

            // 处理邮件附件中的图片
            AtomicInteger imageIndex = new AtomicInteger(pageImages.size());
            processAttachments(mail.getAttachments(), pageImages, imageIndex, parseOption);

            // 缩放图片
            scaleImages(pageImages, parseOption.getScaleOption());

            // 合并图片（如果需要）
            if (parseOption.isAutoMerge() && ObjectUtil.isNotEmpty(pageImages)) {
                ImageVO mergedImg = ImageUtils.mergeImage(pageImages, parseOption.getParseDefaultImageType());
                scaleMergedImages(mergedImg, parseOption.getScaleOption());
                document.setMergedImage(mergedImg);
            }
            document.setText(content);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (mail != null) {
                mail.dispose();
            }
        }
        return document;
    }

    /**
     * 检测文件格式
     */
    private void detectFileFormat(DocumentParseOption parseOption) {
        try (InputStream inputStream = IoUtil.toStream(parseOption.getFileBytes())) {
            FileFormatUtil.detectFileFormat(inputStream);
        } catch (Exception e) {
            log.error("detect file {} format error:{}", parseOption.getName(), e.getMessage());
        }
    }

    /**
     * 准备 HTML 内容，包括获取 HTML 内容和清理不需要的字符串
     */
    private String prepareHtmlContent(MailMessage mail) {
        String htmlContent = mail.getHtmlBody();
        if (htmlContent == null || htmlContent.isEmpty()) {
            htmlContent = mail.getBody(); // 如果没有 HTML 内容，使用纯文本
        }

        // 清理不需要的字符串
        if (ObjectUtil.isNotEmpty(ignoreStrings)) {
            for (String ignoreString : ignoreStrings) {
                htmlContent = htmlContent.replace(ignoreString, "");
            }
        }
        return htmlContent;
    }

    /**
     * 缓存链接资源中的图片数据，避免重复读取流导致对象被释放
     */
    private void cacheLinkedResourceImages(LinkedResourceCollection resourceCollection,
                                           Map<String, byte[]> imageDataCache,
                                           Map<String, String> imageExtCache) {
        if (ObjectUtil.isEmpty(resourceCollection) || resourceCollection.size() == 0) {
            return;
        }

        for (int i = 0; i < resourceCollection.size(); i++) {
            LinkedResource resource = resourceCollection.get_Item(i);
            String mediaType = resource.getContentType().getMediaType();
            if (!mediaType.startsWith("image/")) {
                continue;
            }

            try {
                String contentId = resource.getContentId();
                String contentLink = resource.getContentLink() != null ? resource.getContentLink().toString() : null;

                // 读取图片数据并缓存
                byte[] imageBytes;
                try (InputStream resourceStream = resource.getContentStream()) {
                    imageBytes = resourceStream.readAllBytes();
                }

                if (imageBytes != null && imageBytes.length > 0) {
                    String ext = getExt(mediaType);
                    // 使用 ContentId 作为 key 缓存
                    if (ObjectUtil.isNotEmpty(contentId)) {
                        imageDataCache.put(contentId, imageBytes);
                        imageExtCache.put(contentId, ext);
                    }
                    // 使用 ContentLink 作为 key 缓存（如果不同）
                    if (ObjectUtil.isNotEmpty(contentLink) && !contentLink.equals(contentId)) {
                        imageDataCache.put(contentLink, imageBytes);
                        imageExtCache.put(contentLink, ext);
                    }
                }
            } catch (Exception e) {
                log.error("Error caching linked resource image: {}", e.getMessage(), e);
                // 继续处理其他资源
            }
        }
    }

    /**
     * 将 HTML 内容转换为图片列表
     */
    private void convertHtmlToImages(String htmlContent, List<ImageVO> pageImages, DocumentParseOption parseOption) {
        Document htmlDocument = null;
        AtomicInteger imageIndex = new AtomicInteger(0);

        try (ByteArrayInputStream htmlStream = new ByteArrayInputStream(htmlContent.getBytes(StandardCharsets.UTF_16))) {
            htmlDocument = new Document(htmlStream);
            int saveFormat = getSaveFormat(parseOption.getParseDefaultImageType());
            ImageSaveOptions options = createImageSaveOptions(saveFormat);

            // 处理每一页
            for (int index = 0; index < htmlDocument.getPageCount(); index++) {
                ImageVO imageVO = processHtmlPage(htmlDocument, index, options, saveFormat, imageIndex, parseOption);
                if (imageVO != null) {
                    pageImages.add(imageVO);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (htmlDocument != null) {
                try {
                    htmlDocument.cleanup();
                } catch (Exception e) {
                    log.error("Error cleaning up HTML document: {}", e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 创建图片保存选项
     */
    private ImageSaveOptions createImageSaveOptions(int saveFormat) {
        ImageSaveOptions options = new ImageSaveOptions(saveFormat);
        options.setJpegQuality(95);
        options.setOptimizeOutput(true);
        options.setMemoryOptimization(true);
        return options;
    }

    /**
     * 处理 HTML 文档的单个页面，将其转换为图片
     */
    private ImageVO processHtmlPage(Document htmlDocument, int pageIndex, ImageSaveOptions options,
                                    int saveFormat, AtomicInteger imageIndex, DocumentParseOption parseOption) {
        try {
            options.setPageSet(new PageSet(pageIndex));
            PageInfo pageInfo = htmlDocument.getPageInfo(pageIndex);
            float dpi = calcDpi(pageInfo);
            options.setResolution(dpi);

            // 将文档保存为图片格式
            try (ByteArrayOutputStream imageOutputStream = new ByteArrayOutputStream()) {
                htmlDocument.save(imageOutputStream, options);
                byte[] imgBytes = imageOutputStream.toByteArray();

                ImageVO imageVO = new ImageVO();
                imageVO.setExt(getExtName(saveFormat));
                imageVO.setIndex(imageIndex.getAndIncrement());
                imageVO.setBytes(imgBytes);
                convertImgIfNecessary(imageVO, parseOption);
                return imageVO;
            }
        } catch (Exception e) {
            log.error("Error processing HTML page {}: {}", pageIndex, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 计算 DPI
     */
    private float calcDpi(PageInfo pageInfo) {
        float inches = pageInfo.getWidthInPoints() / INCH_1_POINTS;
        return DEFAULT_IMAGE_WIDTH / inches;
    }

    /**
     * 根据媒体类型获取文件扩展名
     */
    private String getExt(String mediaType) {

        switch (mediaType){
            case "image/jpeg":
            case "image/jpg":
                return "jpeg";
            case "image/png":
                return "png";
            case "image/gif":
                return "gif";
            case "image/webp":
                return "webp";
            case "image/bmp":
                return "bmp";
        }
        return DEFAULT_IMAGE_TYPE;
    }

    /**
     * 处理邮件附件中的图片
     * @param attachments 附件集合
     * @param pageImages 图片列表
     * @param imageIndex 图片索引计数器
     * @param parseOption 解析选项
     */
    private void processAttachments(AttachmentCollection attachments, List<ImageVO> pageImages,
                                    AtomicInteger imageIndex, DocumentParseOption parseOption) {
        if (ObjectUtil.isEmpty(attachments) || attachments.size() == 0) {
            return;
        }

        for (int i = 0; i < attachments.size(); i++) {
            Attachment attachment = attachments.get_Item(i);
            try {
                String mediaType = attachment.getContentType().getMediaType();

                // 只处理图片附件
                if (!mediaType.startsWith("image/")) {
                    continue;
                }

                // 读取附件内容
                byte[] attachmentBytes;
                try (InputStream attachmentStream = attachment.getContentStream()) {
                    attachmentBytes = attachmentStream.readAllBytes();
                }

                if (attachmentBytes == null || attachmentBytes.length == 0) {
                    log.warn("Attachment image is empty, name: {}", attachment.getName());
                    continue;
                }

                // 获取图片扩展名
                String ext = getExt(mediaType);

                // 创建 ImageVO
                ImageVO imageVO = new ImageVO();
                imageVO.setIndex(imageIndex.getAndIncrement());
                imageVO.setBytes(attachmentBytes);
                imageVO.setExt(ext);

                // 转换图片格式（如果需要）
                convertImgIfNecessary(imageVO, parseOption);

                // 添加到图片列表
                pageImages.add(imageVO);

                log.debug("Processed attachment image, name: {}, size: {}, ext: {}",
                        attachment.getName(), attachmentBytes.length, ext);

            } catch (Exception e) {
                log.error("Error processing attachment {}: {}", attachment.getName(), e.getMessage(), e);
                // 继续处理其他附件，不中断整个流程
            }
        }
    }

    /**
     * 将 HTML 中的 cid: 图片引用替换为 base64 data URI
     * @param htmlContent HTML 内容
     * @param imageDataCache 图片数据缓存（key: ContentId 或 ContentLink, value: 图片字节数组）
     * @param imageExtCache 图片扩展名缓存（key: ContentId 或 ContentLink, value: 扩展名）
     * @return 替换后的 HTML 内容
     */
    private String replaceCidImagesWithBase64(String htmlContent, Map<String, byte[]> imageDataCache, Map<String, String> imageExtCache) {
        if (htmlContent == null || htmlContent.isEmpty() || imageDataCache == null || imageDataCache.isEmpty()) {
            return htmlContent;
        }

        String result = htmlContent;

        // 遍历缓存中的所有图片数据
        for (Map.Entry<String, byte[]> entry : imageDataCache.entrySet()) {
            String key = entry.getKey(); // ContentId 或 ContentLink
            byte[] imageBytes = entry.getValue();
            String ext = imageExtCache.get(key);
            if (imageBytes == null || imageBytes.length == 0 || ObjectUtil.isEmpty(ext)) {
                continue;
            }
            try {
                // 转换为 base64 data URI
                String base64DataUri = ImageUtils.toBase64WithScheme(imageBytes, ext);

                // 替换 HTML 中的 cid: 引用
                // 匹配格式：cid:xxx 或 cid:xxx@xxx 等，可能出现在 src="cid:xxx" 或 src='cid:xxx' 中
                // 转义特殊字符用于正则表达式
                String escapedKey = Pattern.quote(key);
                // 匹配 src="cid:key" 或 src='cid:key' 或 src=cid:key
                Pattern pattern = Pattern.compile(
                        "(?i)(src\\s*=\\s*[\"']?)cid:" + escapedKey + "([\"']?)",
                        Pattern.CASE_INSENSITIVE
                );
                Matcher matcher = pattern.matcher(result);
                result = matcher.replaceAll("$1" + base64DataUri + "$2");

                log.debug("Replaced cid image reference, key: {}", key);

            } catch (Exception e) {
                log.error("Error replacing cid image reference for key {}: {}", key, e.getMessage(), e);
                // 继续处理其他资源，不中断整个流程
            }
        }

        return result;
    }

}