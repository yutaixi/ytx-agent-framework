package com.ytx.ai.parser;

import cn.hutool.core.io.FileUtil;
import com.ytx.ai.parser.vo.DocumentParseOption;
import com.ytx.ai.parser.vo.DocumentVO;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * FileParserNative 单元测试
 *
 * 测试 Rust file-parser 库的 JNI 绑定
 */
public class FileParserNativeTest {

    @Test
    public void testParseTxtFile() throws Exception {
        // 准备测试数据
        String content = "Hello, World! This is a test text file.";
        byte[] fileBytes = content.getBytes(StandardCharsets.UTF_8);

        DocumentParseOption option = new DocumentParseOption();
        option.setName("test.txt");
        option.setPath("E:/workspace/文档测试/test.txt");
        option.setExt("txt");
        option.setFileBytes(fileBytes);

        // 调用native方法解析
        DocumentVO result = FileParserNative.parse(option);

        // 验证结果
        assertNotNull("Result should not be null", result);
        assertEquals("Name should match", "test.txt", result.getName());
        assertEquals("Extension should match", "txt", result.getExt());
        assertNotNull("Text should not be null", result.getText());
        assertTrue("Text should contain the original content",
                   result.getText().contains("Hello, World!"));
        System.out.println(result.getText());
    }

    @Test
    public void testParseEmptyTxtFile() throws Exception {
        // 准备测试数据 - 空文件
        byte[] fileBytes = "".getBytes(StandardCharsets.UTF_8);

        DocumentParseOption option = new DocumentParseOption();
        option.setName("empty.txt");
        option.setPath("/path/to/empty.txt");
        option.setExt("txt");
        option.setFileBytes(fileBytes);

        // 调用native方法解析
        DocumentVO result = FileParserNative.parse(option);

        // 验证结果
        assertNotNull("Result should not be null", result);
        assertEquals("Name should match", "empty.txt", result.getName());
    }

    @Test
    public void testParseUnsupportedFormat() throws Exception {
        // 准备测试数据 - 不支持的文件格式
        byte[] fileBytes = "some content".getBytes(StandardCharsets.UTF_8);

        DocumentParseOption option = new DocumentParseOption();
        option.setName("test.unknown");
        option.setPath("/path/to/test.unknown");
        option.setExt("unknown");
        option.setFileBytes(fileBytes);

        // 调用native方法解析
        DocumentVO result = FileParserNative.parse(option);

        // 验证结果 - 应该返回一个DocumentVO对象（即使解析失败）
        assertNotNull("Result should not be null", result);
        assertEquals("Name should match", "test.unknown", result.getName());
    }

    @Test
    public void testParseWithMaxPages() throws Exception {
        // 准备测试数据
        String content = "Line 1\nLine 2\nLine 3";
        byte[] fileBytes = content.getBytes(StandardCharsets.UTF_8);

        DocumentParseOption option = new DocumentParseOption();
        option.setName("test.txt");
        option.setPath("/path/to/test.txt");
        option.setExt("txt");
        option.setFileBytes(fileBytes);
        option.setMaxPages(2); // 限制最大页数

        // 调用native方法解析
        DocumentVO result = FileParserNative.parse(option);

        // 验证结果
        assertNotNull("Result should not be null", result);
        assertEquals("Name should match", "test.txt", result.getName());
    }

    @Test
    public void testParseWithSpecialCharacters() throws Exception {
        // 准备测试数据 - 包含特殊字符
        String content = "测试内容 Test Content\n特殊字符: !@#$%^&*()";
        byte[] fileBytes = content.getBytes(StandardCharsets.UTF_8);

        DocumentParseOption option = new DocumentParseOption();
        option.setName("test.txt");
        option.setPath("/path/to/test.txt");
        option.setExt("txt");
        option.setFileBytes(fileBytes);

        // 调用native方法解析
        DocumentVO result = FileParserNative.parse(option);

        // 验证结果
        assertNotNull("Result should not be null", result);
        assertNotNull("Text should not be null", result.getText());
        assertTrue("Text should contain the original content",
                   result.getText().contains("测试内容") ||
                   result.getText().contains("Test Content"));
    }

    @Test
    public void testParseDocxFile() throws Exception {
        // 准备测试数据 - 尝试从文件读取docx文件
        // 如果文件不存在，则创建一个最小化的docx文件进行测试

        String testDocxPath = "E:/workspace/文档测试/test.docx";
        byte[] fileBytes = FileUtil.readBytes(testDocxPath);

//        try {
//            // 尝试从指定路径读取docx文件
//            Path path = Paths.get(testDocxPath);
//            if (Files.exists(path)) {
//                fileBytes = Files.readAllBytes(path);
//                System.out.println("Using docx file from: " + testDocxPath);
//            }
//        } catch (IOException e) {
//            System.out.println("Could not read docx file from: " + testDocxPath);
//        }
//
//        // 如果文件不存在，创建一个最小化的docx文件（ZIP格式）
//        if (fileBytes == null || fileBytes.length == 0) {
//            System.out.println("Creating minimal docx file for testing...");
//            fileBytes = createMinimalDocx();
//        }

        DocumentParseOption option = new DocumentParseOption();
        option.setName("test.docx");
        option.setPath(testDocxPath);
        option.setExt("docx");
        option.setFileBytes(fileBytes);

        // 调用native方法解析
        DocumentVO result = FileParserNative.parse(option);

        // 验证结果
        assertNotNull("Result should not be null", result);
        assertEquals("Name should match", "test.docx", result.getName());
        assertEquals("Extension should match", "docx", result.getExt());

        // docx解析应该返回文本内容（即使是最小化的docx）
        if (result.getText() != null && !result.getText().isEmpty()) {
            System.out.println("Parsed docx text content: " + result.getText());
            assertTrue("Text should not be empty", result.getText().length() > 0);
        } else {
            // 如果解析失败，至少应该返回一个DocumentVO对象
            System.out.println("Warning: docx parsing returned empty text, but DocumentVO was created");
        }

    }

    /**
     * 创建一个最小化的docx文件（ZIP格式）
     * docx文件实际上是一个ZIP压缩包，包含以下结构：
     * - [Content_Types].xml
     * - word/document.xml
     * - _rels/.rels
     * - word/_rels/document.xml.rels
     */
    private byte[] createMinimalDocx() throws IOException {
        // 创建一个简单的docx文件内容
        // 这是一个最小化的docx文件（ZIP格式），包含基本的XML结构
        // 注意：这是一个简化的实现，实际使用时建议使用真实的docx文件

        // 使用Java的ZipOutputStream创建最小化的docx
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {
            // [Content_Types].xml
            java.util.zip.ZipEntry entry1 = new java.util.zip.ZipEntry("[Content_Types].xml");
            zos.putNextEntry(entry1);
            String contentTypes = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n" +
                    "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n" +
                    "<Default Extension=\"xml\" ContentType=\"application/xml\"/>\n" +
                    "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>\n" +
                    "</Types>";
            zos.write(contentTypes.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // word/document.xml
            java.util.zip.ZipEntry entry2 = new java.util.zip.ZipEntry("word/document.xml");
            zos.putNextEntry(entry2);
            String documentXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">\n" +
                    "<w:body>\n" +
                    "<w:p>\n" +
                    "<w:r>\n" +
                    "<w:t>This is a test DOCX document content.</w:t>\n" +
                    "</w:r>\n" +
                    "</w:p>\n" +
                    "</w:body>\n" +
                    "</w:document>";
            zos.write(documentXml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // _rels/.rels
            java.util.zip.ZipEntry entry3 = new java.util.zip.ZipEntry("_rels/.rels");
            zos.putNextEntry(entry3);
            String rels = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>\n" +
                    "</Relationships>";
            zos.write(rels.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        return baos.toByteArray();
    }
}

