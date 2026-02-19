package br.com.gabrielvogado.desafiouds.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

/**
 * Configuração de cache distribuído com Redis
 * Define o time-to-live para os dados em cache
 * TTL padrão: 10 minutos
 */
@Configuration
public class CacheConfig {

    /**
     * Gerenciador de cache Redis
     * Configuração padrão com TTL de 10 minutos
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10));

        return RedisCacheManager.create(connectionFactory);
    }
}

