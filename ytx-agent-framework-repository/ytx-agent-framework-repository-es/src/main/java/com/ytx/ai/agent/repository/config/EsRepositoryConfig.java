package com.ytx.ai.agent.repository.config;

import com.ytx.ai.agent.repository.EsRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import com.ytx.ai.base.constants.RepositoryType;
import javax.net.ssl.SSLContext;

@Configuration
@ConditionalOnProperty(name = "ai.knowledge.repository.type", havingValue = RepositoryType.TYPE_ES, matchIfMissing = true)
public class EsRepositoryConfig extends ElasticsearchConfiguration {


    @Override
    public ClientConfiguration clientConfiguration() {
        try {
            // 构建一个信任所有证书的 SSLContext（仅限测试环境）
            SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
                    .loadTrustMaterial(null, (certificate, authType) -> true)
                    .build();

            return ClientConfiguration.builder()
                    .connectedTo("192.168.88.129:9200") // 配置 Elasticsearch 地址
//                    .usingSsl(sslContext, NoopHostnameVerifier.INSTANCE) // 启用 HTTPS 并跳过主机名验证
                    .withBasicAuth("elastic", "123456") // 用户名密码
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("配置 Elasticsearch HTTPS 连接失败", e);
        }
    }

    @Bean
    public EsRepository esRepository(){
        return new EsRepository();
    }
}
