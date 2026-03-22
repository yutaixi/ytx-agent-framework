package com.ytx.ai.parser.config;


import com.ytx.ai.parser.CellsParser;
import com.ytx.ai.parser.HtmlParser;
import com.ytx.ai.parser.ImageParser;
import com.ytx.ai.parser.OutlookMsgParser;
import com.ytx.ai.parser.PdfDocumentParser;
import com.ytx.ai.parser.PlainTextParser;
import com.ytx.ai.parser.WordDocParser;
import com.ytx.ai.parser.service.DocumentHandleService;
import com.ytx.ai.parser.service.DocumentHandleServiceImpl;
import com.ytx.ai.parser.util.NativeLoader;
import com.ytx.ai.parser.util.ParserUtils;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 文档解析相关的自动配置类。
 * <p>
 * 该配置类主要负责：
 * <ul>
 *     <li>注册文档处理服务 {@link DocumentHandleService} 的默认实现。</li>
 *     <li>将各类具体文档解析器（Excel、图片、HTML、纯文本、PDF、Outlook 邮件、Word 文档等）注册为 Spring Bean。</li>
 * </ul>
 * 在引入本模块依赖后，Spring Boot 会通过自动配置机制加载本类中定义的 Bean，
 * 业务侧无需手动声明这些解析器 Bean，提升复用性和可扩展性。
 */
@AutoConfiguration
public class DocumentParserConfig {

    /**
     * 文档处理服务 Bean。
     *
     * @return 文档处理服务的默认实现 {@link DocumentHandleServiceImpl}
     */
    @Bean
    public DocumentHandleService documentHandleService() {
        return new DocumentHandleServiceImpl();
    }

    /**
     * Excel 文档解析器 Bean。
     * <p>
     * 用于将 Excel（基于 Aspose Cells）渲染为图片并封装为 {@code DocumentVO}。
     *
     * @return {@link CellsParser} 实例
     */
    @Bean
    public CellsParser cellsParser() {
        return new CellsParser();
    }

    /**
     * 图片文档解析器 Bean。
     *
     * @return {@link ImageParser} 实例
     */
    @Bean
    public ImageParser imageParser() {
        return new ImageParser();
    }

    /**
     * HTML 文档解析器 Bean。
     * <p>
     * @return {@link HtmlParser} 实例
     */
    @Bean
    public HtmlParser htmlParser() {
        return new HtmlParser();
    }

    /**
     * 纯文本文档解析器 Bean。
     *
     * @return {@link PlainTextParser} 实例
     */
    @Bean
    public PlainTextParser plainTextParser() {
        return new PlainTextParser();
    }

    /**
     * PDF 文档解析器 Bean。
     *
     * @return {@link PdfDocumentParser} 实例
     */
    @Bean
    public PdfDocumentParser pdfDocumentParser() {
        return new PdfDocumentParser();
    }

    /**
     * Outlook 邮件（.msg）文档解析器 Bean。
     *
     * @return {@link OutlookMsgParser} 实例
     */
    @Bean
    public OutlookMsgParser outlookMsgParser() {
        return new OutlookMsgParser();
    }

    /**
     * Word 文档解析器 Bean。
     *
     * @return {@link WordDocParser} 实例
     */
    @Bean
    public WordDocParser wordDocParser() {
        return new WordDocParser();
    }

    @Bean
    public TextDocumentParser textDocumentParser() {
        return new TextDocumentParser();
    }

    @Bean
    public ApplicationRunner applicationStartupRunner(){
        return args -> {
            ParserUtils.removeWaterMark();
            ParserUtils.setCellsLicense();
            NativeLoader.loadNativeLibrary();
        };
    }
}
