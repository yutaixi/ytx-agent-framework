package com.ytx.ai.parser.vo;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.base.util.FileUtils;
import com.ytx.ai.base.util.GzipUtils;
import com.ytx.ai.parser.constant.ParseConstants;
import com.ytx.ai.parser.util.ImageUtils;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.ytx.ai.parser.constant.ParseConstants.IMAGE_TYPE_LIST;


@Getter
@Setter
public class DocumentParseOption {

    private String name;
    private String path;
    private String ext;
    private byte[] fileBytes;
    private int maxPages=-1;

    private boolean ocrIfContentEmpty;
    private boolean sharpImage=true;
    private boolean autoMerge=true;

    //项目中目录，非磁盘上的目录
    private boolean projectPath=false;

    private List<String> allowedImageTypes=IMAGE_TYPE_LIST;
    private String parseDefaultImageType= ParseConstants.DEFAULT_IMAGE_TYPE;
    private ScaleOption scaleOption;


    public byte[] getFileBytes() {
        return GzipUtils.uncompress(fileBytes);
    }

    public void setFileBytes(byte[] fileBytes) {
        this.fileBytes =GzipUtils.compress(fileBytes);
    }

    public void init() {
        if (ObjectUtil.isNotEmpty(this.getFileBytes())) {
            return;
        }
        if(ObjectUtil.isEmpty(this.getPath())){
            return;
        }
        if (ImageUtils.isBase64DataURI(this.getPath())) {
            // 处理 base64 data URI
            parseBase64DataURI(this, this.getPath());
        } else {
            // 处理普通路径（HTTP/HTTPS、本地文件路径等）
            String ext = FileUtils.extName(this.getPath());
            this.setName(FileUtils.getName(this.getPath()));
            this.setExt(ext.toLowerCase());
            if (this.isProjectPath()) {
                try (InputStream in = DocumentParseOption.class.getResourceAsStream(this.getPath())) {
                    this.setFileBytes(IoUtil.readBytes(in));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                this.setFileBytes(FileUtils.toByteArray(this.getPath()));
            }
        }

    }

    public static DocumentParseOption of(String url){
        return of(url,false);
    }
    public static DocumentParseOption of(String url,boolean projectPath){

        DocumentParseOption parseOption=new DocumentParseOption();

        // 检查是否是 base64 data URI 格式
        if(ImageUtils.isBase64DataURI(url)){
            // 处理 base64 data URI
            parseBase64DataURI(parseOption, url);
        } else {
            // 处理普通路径（HTTP/HTTPS、本地文件路径等）
            String ext=FileUtils.extName(url);
            parseOption.setName(FileUtils.getName(url));
            parseOption.setPath(url);
            parseOption.setExt(ext.toLowerCase());
            if(projectPath){
                try(InputStream in = DocumentParseOption.class.getResourceAsStream(url)){
                    parseOption.setFileBytes(IoUtil.readBytes(in));
                    parseOption.setProjectPath(true);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }else{
                parseOption.setFileBytes(FileUtils.toByteArray(url));
                parseOption.setProjectPath(false);
            }
        }

        return parseOption;
    }



    /**
     * 解析 base64 data URI
     * @param parseOption 解析选项对象
     * @param dataURI base64 data URI 字符串，格式：data:image/png;base64,xxx
     */
    private static void parseBase64DataURI(DocumentParseOption parseOption, String dataURI) {
        try {
            ImageVO image= ImageUtils.base64WithSchemeToImage(dataURI);

            // 设置解析选项
            parseOption.setPath(dataURI);
            parseOption.setExt(image.getExt());
            parseOption.setName("image." + image.getExt()); // 生成一个默认的文件名
            parseOption.setFileBytes(image.getBytes());
            parseOption.setProjectPath(false);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse base64 data URI: " + e.getMessage(), e);
        }
    }

    /**
     * 从 MIME 类型字符串中提取文件扩展名
     * 例如：data:image/png;base64, -> png
     *      data:image/jpeg;base64, -> jpeg
     */
    private static String extractExtensionFromMimeType(String mimePart) {
        if (ObjectUtil.isEmpty(mimePart)) {
            return null;
        }

        // 提取 MIME 类型部分（data:image/png;base64, -> image/png）
        int colonIndex = mimePart.indexOf(":");
        int semicolonIndex = mimePart.indexOf(";");

        if (colonIndex < 0 || semicolonIndex < 0 || semicolonIndex <= colonIndex) {
            return null;
        }

        String mimeType = mimePart.substring(colonIndex + 1, semicolonIndex);
        // mimeType 格式：image/png, image/jpeg 等

        // 提取扩展名
        int slashIndex = mimeType.indexOf("/");
        if (slashIndex < 0 || slashIndex >= mimeType.length() - 1) {
            return null;
        }

        String ext = mimeType.substring(slashIndex + 1);

        // 处理一些特殊情况
        if ("jpeg".equalsIgnoreCase(ext)) {
            ext = "jpg";
        }

        return ext;
    }

    public static DocumentParseOption withScaleOption(String url){
        DocumentParseOption parseOption=DocumentParseOption.of(url);
        parseOption.setScaleOption(new ScaleOption());
        return parseOption;
    }

    public DocumentParseOption scale(double scale){
        ScaleOption scaleOption=this.scaleOption;
        if(scaleOption==null){
            scaleOption=new ScaleOption();
        }
        scaleOption.setScale(scale);
        this.scaleOption= scaleOption;
        return this;
    }

    public DocumentParseOption scale(int  maxDimension){
        ScaleOption scaleOption=this.scaleOption;
        if(scaleOption==null){
            scaleOption=new ScaleOption();
        }
        scaleOption.setMaxDimension(maxDimension);
        this.scaleOption= scaleOption;
        return this;
    }

    public DocumentParseOption maxMB(double maxMB){
        ScaleOption scaleOption=this.scaleOption;
        if(scaleOption==null){
            scaleOption=new ScaleOption();
        }
        scaleOption.setMaxMB(maxMB);
        this.scaleOption= scaleOption;
        return this;
    }

    public DocumentParseOption maxPages(int maxPages){
        this.maxPages=maxPages;
        return this;
    }

    public DocumentParseOption autoMerge(boolean autoMerge){
        this.autoMerge=autoMerge;
        return this;
    }

}