package com.ytx.ai.parser;

import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.parser.util.ImageUtils;
import com.ytx.ai.parser.vo.DocumentParseOption;
import com.ytx.ai.parser.vo.DocumentVO;
import com.ytx.ai.parser.vo.ImageVO;
import com.ytx.ai.parser.vo.ScaleOption;


import java.util.List;

public interface DocumentParser {

    public DocumentVO parse(DocumentParseOption parseOption);


    default public void scaleImages(List<ImageVO> images, ScaleOption scaleOption) {
        if (ObjectUtil.isEmpty(images) || ObjectUtil.isEmpty(scaleOption) || !scaleOption.isEnable()) {
            return;
        }
        images.forEach(item->{
            scaleImage(item,scaleOption);
        });

    }
    default public void scaleImage(ImageVO image, ScaleOption scaleOption) {
        if (ObjectUtil.isEmpty(image) || ObjectUtil.isEmpty(scaleOption)) {
            return;
        }
        //确保最终图片存储大小不超过最大值
        if (ObjectUtil.isNotEmpty(scaleOption.getMaxMB())) {
            ImageUtils.resizeImage(image, scaleOption.getMaxMB());
        }
    }

    default public void scaleMergedImages(ImageVO image, ScaleOption scaleOption) {
        if (ObjectUtil.isEmpty(image) || ObjectUtil.isEmpty(scaleOption) || !scaleOption.isEnable()) {
            return;
        }
        //确保最终图片存储大小不超过最大值
        if (ObjectUtil.isNotEmpty(scaleOption.getMaxMB())) {
            ImageUtils.resizeImage(image, scaleOption.getMaxMB());
        }
    }

    default public void convertImgIfNecessary(ImageVO imageVO,DocumentParseOption parseOption){
        //允许的图片类型列表不为空
        if(ObjectUtil.isNotEmpty(parseOption.getAllowedImageTypes()) ){
            //图片不在允许的类型中，则进行类型转换
            if(!parseOption.getAllowedImageTypes().contains(imageVO.getExt())){
                ImageUtils.convertTo(imageVO, parseOption.getParseDefaultImageType());
            }
        }else if(!parseOption.getParseDefaultImageType().equalsIgnoreCase(imageVO.getExt())){
            ImageUtils.convertTo(imageVO, parseOption.getParseDefaultImageType());
        }
    }
}