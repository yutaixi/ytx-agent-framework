package com.ytx.ai.parser;

import com.aspose.words.SaveFormat;

public abstract class BaseOfficeParser implements DocumentParser{


    protected int getSaveFormat(String ext){
        String lowerExt=ext.toLowerCase();
        switch (lowerExt){
            case "png":
                return SaveFormat.PNG;
            case "svg":
                return SaveFormat.SVG;
            case "tiff":
                return SaveFormat.TIFF;
            case "bmp":
                return SaveFormat.BMP;
            case "jpeg":
            case "jpg":
                return SaveFormat.JPEG;
            case "gif":
                return SaveFormat.GIF;
        }
        return SaveFormat.JPEG;
    }

    protected String getExtName(int format){
        String name=SaveFormat.getName(format);
        return name.toLowerCase();
    }
}