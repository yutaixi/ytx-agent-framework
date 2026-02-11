package com.ytx.ai.base.util;

import lombok.extern.slf4j.Slf4j;

/**
 * JNI 库加载工具类
 *
 * 提供跨平台的 JNI 本地库加载功能，支持 Windows、Linux 和 macOS 系统。
 * 该类封装了操作系统检测和库加载逻辑，避免在各个模块中重复编写类似的代码。
 *
 * 使用示例：
 * <pre>
 * static {
 *     JniLibraryLoader.load("ytx_sandbox");
 * }
 * </pre>
 */
@Slf4j
public class JniLibraryLoader {

    /**
     * 加载 JNI 本地库
     *
     * 根据当前操作系统自动选择正确的库文件名：
     * - Windows: {libName}.dll
     * - Linux: lib{libName}.so
     * - macOS: lib{libName}.dylib
     *
     * @param libName 库名称（不包含平台特定的前缀和后缀）
     * @throws UnsatisfiedLinkError 如果库加载失败或操作系统不支持
     */
    public static void load(String libName) {
        if (libName == null || libName.trim().isEmpty()) {
            throw new IllegalArgumentException("Library name cannot be null or empty");
        }

        try {
            String osName = System.getProperty("os.name").toLowerCase();
            String fullLibName = getFullLibraryName(osName, libName);

            System.loadLibrary(libName);
            log.info("Successfully loaded native library: {}", fullLibName);
        } catch (UnsatisfiedLinkError e) {
            String osName = System.getProperty("os.name").toLowerCase();
            String fullLibName = getFullLibraryName(osName, libName);
            log.error("Failed to load native library: {}", fullLibName, e);
            log.error("Please ensure the native library is in the library path (java.library.path or system PATH).");
            throw e;
        }
    }

    /**
     * 获取完整的库文件名（用于日志显示）
     *
     * @param osName 操作系统名称（小写）
     * @param libName 库名称
     * @return 完整的库文件名
     */
    private static String getFullLibraryName(String osName, String libName) {
        if (osName.contains("win")) {
            return libName + ".dll";
        } else if (osName.contains("linux")) {
            return "lib" + libName + ".so";
        } else if (osName.contains("mac")) {
            return "lib" + libName + ".dylib";
        } else {
            return libName + " (unsupported OS: " + osName + ")";
        }
    }

    /**
     * 检查当前操作系统是否支持
     *
     * @return 如果操作系统支持则返回 true，否则返回 false
     */
    public static boolean isSupportedOS() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("win") || osName.contains("linux") || osName.contains("mac");
    }
}

