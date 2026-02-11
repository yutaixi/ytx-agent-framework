package com.ytx.ai.parser;

import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.parser.util.ImageUtils;
import com.ytx.ai.parser.vo.DocumentParseOption;
import com.ytx.ai.parser.vo.DocumentVO;
import com.ytx.ai.parser.vo.ImageVO;
import org.apache.tika.Tika;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ImageParser implements DocumentParser{

    private static final Map<String,String> mimeTypeMap=new HashMap<>();
    static {
        mimeTypeMap.put("image/jpeg","jpeg");
        mimeTypeMap.put("image/png","png");
        mimeTypeMap.put("image/gif","gif");
        mimeTypeMap.put("image/webp","webp");
        mimeTypeMap.put("image/bmp","bmp");
        mimeTypeMap.put("image/tiff","tiff");
        mimeTypeMap.put("image/heif","heif");
        mimeTypeMap.put("image/heic","heic");
        mimeTypeMap.put("image/svg+xml","svg");
        mimeTypeMap.put("image/x-icon","ico");
        mimeTypeMap.put("image/avif","avif");
        mimeTypeMap.put("image/x-xbitmap","xbm");
    }

    @Override
    public DocumentVO parse(DocumentParseOption parseOption) {
        DocumentVO documentVO =DocumentVO.of(parseOption);
        if(ObjectUtil.isEmpty(parseOption.getFileBytes())){
            return documentVO;
        }
        List<ImageVO> pageImages=new ArrayList<>();
        documentVO.setPageImages(pageImages);
        ImageVO image=new ImageVO();
        image.setIndex(0);
        image.setBytes(parseOption.getFileBytes());
        String ext=getFileType(parseOption);
        image.setExt(ext);
        if(!parseOption.getAllowedImageTypes().contains(image.getExt())){
            ImageUtils.convertTo(image,parseOption.getParseDefaultImageType());
        }
        pageImages.add(image);
        scaleImages(pageImages,parseOption.getScaleOption());
        if(parseOption.isAutoMerge()){
            documentVO.setMergedImage(image);
        }
        return documentVO;
    }



    private String getFileType(DocumentParseOption parseOption){
        if(ObjectUtil.isEmpty(parseOption.getFileBytes())){
            return parseOption.getExt();
        }
        Tika tika = new Tika();
        String mimeType = tika.detect(parseOption.getFileBytes());
        return mimeTypeMap.getOrDefault(mimeType,parseOption.getExt());
    }



}
