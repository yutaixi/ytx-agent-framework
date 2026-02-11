package com.ytx.ai.parser.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DocumentVO {

    // 文档名称
    private String name;
    // 文档路径
    private String path;
    // 文档原始路径
    private String originalPath;
    // 文档后缀
    private String ext;
    // 文档内容byte数组
    private byte[] fileBytes;
    //文档页面转图片，每一页一张图片
    private List<ImageVO> pageImages;
    //文档所有页面合并成一张图片
    private ImageVO mergedImage;
    //文档文本
    private String text;
    private List<ImageVO> images;


    public static DocumentVO of(DocumentParseOption parseOption){
        DocumentVO documentVO=new DocumentVO();
        documentVO.setName(parseOption.getName());
        documentVO.setExt(parseOption.getExt());
        documentVO.setFileBytes(parseOption.getFileBytes());
        documentVO.setPath(parseOption.getPath());
        return documentVO;
    }
}