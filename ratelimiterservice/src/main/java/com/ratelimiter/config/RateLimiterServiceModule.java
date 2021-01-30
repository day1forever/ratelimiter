package com.ratelimiter.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.ratelimiter.RateLimiterService;
import com.ratelimiter.dao.LimitDao;
import com.ratelimiter.dao.RedisDao;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;


public class RateLimiterServiceModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterServiceModule.class);

    private final RateLimiterServiceConfiguration config;
    private final Environment environment;

    public RateLimiterServiceModule(RateLimiterServiceConfiguration config, Environment environment) {
        this.config = config;
        this.environment = environment;
    }

    @Override
    protected void configure() {
        log.info("Binding");
        bind(RateLimiterServiceConfiguration.class).toInstance(config);
        for (Class<?> c : RateLimiterService.RESOURCE_CLASSES) {
            bind(c).in(Singleton.class);
        }
        this.binder().requireExplicitBindings();
    }


    @Singleton
    @Provides
    public JedisPool provideJedisPool() {
        final JedisPoolConfig poolConfig = buildPoolConfig();
        return new JedisPool(poolConfig, config.getJedisHost());
    }

    private JedisPoolConfig buildPoolConfig() {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
        poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        return poolConfig;
    }

    @Singleton
    @Provides
    public LimitDao provideRedisDao(JedisPool jedisPool) {
        return new RedisDao(jedisPool);
    }

    public Environment getEnvironment() {
        return environment;
    }
}
