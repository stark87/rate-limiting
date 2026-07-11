package com.example.rate.limiting.component;

import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisScripts {
    @Bean("tokenBucketScript")
    public RedisScript<Long> tokenBucketScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(
                new ClassPathResource("scripts/token-bucket.lua")
        );
        script.setResultType(Long.class);
        return script;
    }

    @Bean("refillTokenBucketScript")
    public RedisScript<Long> refillTokenBucketScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(
                new ClassPathResource("scripts/refill-token-bucket.lua")
        );
        return script;
    }

    @Bean("lazyRefillScript")
    public RedisScript<Void> lazyRefillScript() {
        DefaultRedisScript<Void> script = new DefaultRedisScript<>();
        script.setLocation(
                new ClassPathResource("scripts/lazy-refill.lua")
        );
        return script;
    }
}
