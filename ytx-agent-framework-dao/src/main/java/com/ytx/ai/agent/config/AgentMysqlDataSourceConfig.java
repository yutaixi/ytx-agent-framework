package com.ytx.ai.agent.config;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.ytx.ai.base.constants.RepositoryType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
@MapperScan(basePackages = "com.ytx.ai.agent.mapper", sqlSessionFactoryRef = "agentSqlSessionFactory")
@ConditionalOnProperty(name = "ai.agent.repository.type", havingValue = RepositoryType.TYPE_MYSQL, matchIfMissing = true)
public class AgentMysqlDataSourceConfig {

    @Autowired
    private MysqlProperty mysqlProperty;

    @Primary
    @Bean(name = "agentDataSource")
    public DataSource agentDataSource() {
        return DataSourceBuilder.create()
                .driverClassName(mysqlProperty.getDriverClassName())
                .url(mysqlProperty.getUrl())
                .username(mysqlProperty.getUsername())
                .password(mysqlProperty.getPassword())
                .build();
    }

    @Primary
    @Bean(name = "agentSqlSessionFactory")
    public SqlSessionFactory agentSqlSessionFactory(@Qualifier("agentDataSource") DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        return factoryBean.getObject();
    }

    @Primary
    @Bean(name = "agentSqlSessionTemplate")
    public SqlSessionTemplate agentSqlSessionTemplate(@Qualifier("agentSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
