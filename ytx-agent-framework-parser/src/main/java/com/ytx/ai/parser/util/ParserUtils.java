package com.ytx.ai.parser.util;

import com.aspose.cells.License;
import sun.misc.Unsafe;

import java.io.InputStream;
import java.lang.reflect.Field;

public class ParserUtils {

    public static void removeWaterMark() throws Exception {
        Class<?> aClass = Class.forName("com.aspose.words.zzXyu");
        Field zzZXG = aClass.getDeclaredField("zzZXG");
        zzZXG.setAccessible(true);

        // 使用 Unsafe 修改 final 字段
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);

        // 获取静态字段的内存地址并修改值
        unsafe.putObjectVolatile(
                unsafe.staticFieldBase(zzZXG),
                unsafe.staticFieldOffset(zzZXG),
                new byte[]{76, 73, 67, 69, 78, 83, 69, 68}
        );
    }

    public static void setCellsLicense() throws Exception {
        InputStream inputStream = ParserUtils.class.getResourceAsStream("/com.aspose.cells.lic_2999.xml");
        License license=new License();
        license.setLicense(inputStream);
    }


}