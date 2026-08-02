package com.example.rate.limiting.component;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "settings.use.scheduled.refiller",
        havingValue = "true"
)
public class TokenBucketRefiller {
    private final RedisTemplate<String, String> redisTemplate;
    @Qualifier("refillTokenBucketScript")
    private final RedisScript<Long> refillerScript;
    private final PropsReader propsReader;

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.SECONDS)
    public void refillTokenBuckets(){
        Set<String> keys = redisTemplate.opsForSet().members("keys");
        if (keys == null || keys.isEmpty()) return;
        keys.parallelStream().forEach(key -> redisTemplate.execute(
            refillerScript,
            Collections.singletonList(key),
            String.valueOf(propsReader.getRateLimit()),
            String.valueOf(propsReader.getTokenRefillPerSecond() * 20)
        ));
    }
}
