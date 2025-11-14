package com.ytx.ai.agent.repository.config;

import com.ytx.ai.base.constants.RepositoryType;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "ai.knowledge.repository.type", havingValue = RepositoryType.TYPE_POSTGRESQL)
public class PostgresConfig {

    @Value("${spring.datasource.postgresql.url}")
    private String url;

    @Value("${spring.datasource.postgresql.username}")
    private String username;

    @Value("${spring.datasource.postgresql.password}")
    private String password;

    @Value("${spring.datasource.postgresql.driver-class-name:org.postgresql.Driver}")
    private String driver;

    @Bean(name = "pgDataSource")
    public DataSource pgDataSource() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(username);
        cfg.setPassword(password);
        cfg.setDriverClassName(driver);
        return new HikariDataSource(cfg);
    }

    @Bean(name = "pgNamedParameterJdbcTemplate")
    public NamedParameterJdbcTemplate pgNamedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(pgDataSource());
    }

}
