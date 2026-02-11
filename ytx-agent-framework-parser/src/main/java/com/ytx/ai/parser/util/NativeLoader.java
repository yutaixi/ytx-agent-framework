package com.ytx.ai.parser.util;

import org.opencv.core.Core;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class NativeLoader {

    private static final String NATIVE_LIB_PATH = "/opt/data/lib/opencv-4110";

    private static boolean NATIVE_LIBRARY_LOADED = false;

    public static synchronized void loadNativeLibrary() {
        if (!NATIVE_LIBRARY_LOADED) {
            try {
                System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
                NATIVE_LIBRARY_LOADED = true;
                System.out.println("########################## Native library loaded successfully from existing java.library.path. ##################################");
            } catch (UnsatisfiedLinkError e) {
                System.out.println("Could not find native library in java.library.path. Attempting to load from JAR...");
                try {
                    loadLibrariesFromJar(Core.NATIVE_LIBRARY_NAME);
                    NATIVE_LIBRARY_LOADED = true;
                    System.out.println("########################## Native library loaded successfully from JAR. ##################################");
                } catch (Exception loadEx) {
                    throw new RuntimeException("Could not load native library from JAR.", loadEx);
                }
            }
        }
    }

    /**
     * 【已修改】从 JAR 包中提取所有平台的本地库到一个临时目录，并让 JVM 从该目录加载。
     *
     * @param mainLibName 要加载的主库名 (例如 "opencv_java411")
     * @throws IOException 如果文件操作失败
     */
    private static void loadLibrariesFromJar(String mainLibName) throws IOException {
        // 1. 判断操作系统
        String os = System.getProperty("os.name").toLowerCase();
        String platform;
        String mainLibFullName;
        if (os.contains("win")) {
            platform = "windows";
            mainLibFullName = mainLibName + ".dll";
        } else if (os.contains("mac")) {
            platform = "mac";
            mainLibFullName = mainLibName + ".dylib";
        } else {
            platform = "linux";
            mainLibFullName = "lib" + mainLibName + ".so";
        }
        String nativeResourcePathPrefix = "/native/" + platform + "/";

        if("windows".equals(platform)) {
            loadWindowsNativeLibrary(mainLibFullName);
            return;
        }

        // 2. 读取该平台目录下的索引文件，获取所有库文件名
        List<String> libFileNames = new ArrayList<>();
        String indexFilePath = nativeResourcePathPrefix + "index.list";
        try (
                InputStream indexStream = NativeLoader.class.getResourceAsStream(indexFilePath);
                BufferedReader reader = new BufferedReader(new InputStreamReader(indexStream))
        ) {
            if (indexStream == null) {
                throw new FileNotFoundException("Could not find the index file for native libraries: " + indexFilePath);
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    libFileNames.add(line.trim());
                }
            }
        }

        if (libFileNames.isEmpty()) {
            throw new RuntimeException("No native libraries listed in the index file for platform: " + platform);
        }
        System.out.println("Found " + libFileNames.size() + " native libraries to load for " + platform);


        // 3. 创建一个目录，如果父目录不存在，则创建
        Path libPath = Files.createDirectories(Path.of(NATIVE_LIB_PATH));
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            try {
//                Files.walk(libPath)
//                        .sorted(Collections.reverseOrder())
//                        .map(Path::toFile)
//                        .forEach(File::delete);
//            } catch (IOException e) {
//                System.err.println("Error deleting temporary native library directory: " + libPath);
//            }
//        }));

        // 4. 遍历文件名列表，将每个库文件从 JAR 提取到临时目录
        for (String libFileName : libFileNames) {
            String libResourcePath = nativeResourcePathPrefix + libFileName;
            try (InputStream in = NativeLoader.class.getResourceAsStream(libResourcePath)) {
                if (in == null) {
                    System.err.println("Warning: Could not find library " + libFileName + " in JAR.");
                    continue; // 跳过缺失的文件
                }
                Path destPath = libPath.resolve(libFileName);
                // 检查目标文件是否不存在
                if (Files.notExists(destPath)) {
                    // 如果不存在，才执行拷贝操作
                    Files.copy(in, destPath);
                    System.out.println("文件 " + libFileName + " 拷贝成功！");
                } else {
                    // 如果文件已存在，打印一条信息并跳过
                    System.out.println("文件 " + libFileName + " 已存在，跳过拷贝。");
                }
                System.out.println("Extracted " + libFileName + " to " + destPath);
            }
        }
        System.loadLibrary(mainLibName);
        System.out.println("########################## NativeLoader succeeded by extracting from JAR. ##################################");
    }

    private static void loadWindowsNativeLibrary(String libFileName) {
        try (InputStream in = NativeLoader.class.getResourceAsStream(String.format("/native/windows/%s", libFileName))) {
            if (in == null) {
                throw new RuntimeException(String.format("Could not find %s native library for windows in the jar",libFileName));
            }
            File tmpLibraryFile = Files.createTempFile("", libFileName).toFile();
            tmpLibraryFile.deleteOnExit();
            try (FileOutputStream out = new FileOutputStream(tmpLibraryFile)) {
                byte[] buffer = new byte[8 * 1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            Runtime.getRuntime().addShutdownHook(new Thread(tmpLibraryFile::delete));
            System.load(tmpLibraryFile.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Could not load native library:"+libFileName, e);
        }
    }


}