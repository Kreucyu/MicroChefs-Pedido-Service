package com.service.pedidos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

@Configuration
public class RedisConfig {

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .computePrefixWith(cacheName -> "pedido-service:v1:" + cacheName + "::")
                .serializeValuesWith(SerializationPair.fromSerializer(
                        GenericJacksonJsonRedisSerializer.create(builder -> builder
                                .enableDefaultTyping(BasicPolymorphicTypeValidator.builder()
                                        .allowIfSubType("java.util.")
                                        .allowIfSubType("com.service.pedidos.")
                                        .build()))))
                .disableCachingNullValues();

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
