package com.ytx.ai.agent.base.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnMissingBean(RedissonClient.class)
@Import(RedissonCacheConfig.RedissonConfig.class)
public class RedissonCacheConfig {

    private final Config config;

    RedissonCacheConfig(Config config){
        this.config=config;
    }

    @Bean
    public RedissonClient redissonClient()
    {
        return Redisson.create(config);
    }

    @ConditionalOnMissingBean(Config.class)
    @EnableConfigurationProperties({RedisProperties.class})
    static class RedissonConfig {

        @Autowired
        private RedisProperties redisProperties;

        @Bean
        public Config redissonConfig() {
            Config config = new Config();

            //哨兵模式
            if (redisProperties.getSentinel() != null) { //sentinel
                SentinelServersConfig sentinelServersConfig = config.useSentinelServers();
                org.springframework.boot.autoconfigure.data.redis.RedisProperties.Sentinel sentinel = redisProperties.getSentinel();
                sentinelServersConfig.setMasterName(sentinel.getMaster());
                sentinelServersConfig.addSentinelAddress(sentinel.getNodes().toArray(new String[sentinel.getNodes().size()]));
                sentinelServersConfig.setDatabase(redisProperties.getDatabase());
                baseConfig(sentinelServersConfig, redisProperties);

                //集群模式
            } else if (redisProperties.getCluster() != null) { //cluster
                ClusterServersConfig clusterServersConfig = config.useClusterServers();
                org.springframework.boot.autoconfigure.data.redis.RedisProperties.Cluster cluster = redisProperties.getCluster();
                clusterServersConfig.addNodeAddress(cluster.getNodes().toArray(new String[cluster.getNodes().size()]));
                clusterServersConfig.setFailedSlaveReconnectionInterval(cluster.getMaxRedirects());
                baseConfig(clusterServersConfig, redisProperties);

                //普通模式
            } else { //single server
                SingleServerConfig singleServerConfig = config.useSingleServer();
                // format as redis://127.0.0.1:7181 or rediss://127.0.0.1:7181 for SSL
                String schema = redisProperties.isSsl() ? "rediss://" : "redis://";
                singleServerConfig.setAddress(schema + redisProperties.getHost() + ":" + redisProperties.getPort());
                singleServerConfig.setDatabase(redisProperties.getDatabase());
                baseConfig(singleServerConfig, redisProperties);
            }
            config.setCodec(new JsonJacksonCodec());
            return config;
        }

        private void baseConfig(BaseConfig config, RedisProperties properties) {
            if (!StringUtils.hasText(properties.getPassword())) {
                config.setPassword(properties.getPassword());
            }
            if (properties.getTimeout() != null) {
                config.setTimeout(Long.valueOf(properties.getTimeout().getSeconds() * 1000).intValue());
            }
            if (!StringUtils.hasText(properties.getClientName())) {
                config.setClientName(properties.getClientName());
            }
        }
    }
}
