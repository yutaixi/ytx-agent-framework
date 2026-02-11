package com.ytx.ai.hack;

import javassist.*;

import java.io.IOException;

public class TestHackCells {


    public static void main(String[] args) throws NotFoundException, CannotCompileException, IOException {
        crackAsposeCells("E:\\test\\aspose/aspose-cells-21.8.jar");
    }

    public static void crackAsposeCells(String JarPath) throws NotFoundException, CannotCompileException, IOException {
        // 这个是得到反编译的池
        ClassPool pool = ClassPool.getDefault();

        // 取得需要反编译的jar文件，设定路径
        pool.insertClassPath(JarPath);

        CtClass ctClass = pool.get("com.aspose.cells.License");

        CtMethod method_isLicenseSet = ctClass.getDeclaredMethod("isLicenseSet");
        method_isLicenseSet.setBody("return true;");
        CtMethod method_setLicense = ctClass.getDeclaredMethod("setLicense");
        method_setLicense.setBody("{    a = new com.aspose.cells.License();\n" +
                "    com.aspose.cells.zbkl.a();}");
        CtMethod methodL = ctClass.getDeclaredMethod("l");
        methodL.setBody("return new java.util.Date(Long.MAX_VALUE);");

        ctClass.writeFile("E:\\test\\aspose\\21_8");
    }

}
