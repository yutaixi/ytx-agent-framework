package com.ytx.ai.parser.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "document.parser")
@Configuration
public class DocumentParserProperties {

    private Map<String,String> classNames=new HashMap<>();
}
