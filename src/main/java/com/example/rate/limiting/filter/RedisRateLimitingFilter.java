package com.example.rate.limiting.filter;

import com.example.rate.limiting.component.PropsReader;
import com.example.rate.limiting.exception.MissingHeaderException;
import com.example.rate.limiting.exception.RateLimitExceededException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static com.example.rate.limiting.filter.RateLimitingFilter.logError;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisRateLimitingFilter extends OncePerRequestFilter {
    private final PropsReader propsReader;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisScript<Long> incrementScript;
    @Qualifier("handlerExceptionResolver")
    private final HandlerExceptionResolver resolver;


    private int rateLimit;
    private int rateLimitWindowSeconds;

    @PostConstruct
    void init (){
        rateLimit = propsReader.getRateLimit();
        rateLimitWindowSeconds = propsReader.getRateLimitWindowSeconds();
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) {
        try {
            String apiKey = request.getHeader("X-Api-Key");
            if (apiKey == null) {
                throw new MissingHeaderException("X-Api-Key header is missing");
            }

            Instant instant = Instant.now();
            long now = instant.getEpochSecond();
            long lbound = instant.minusSeconds(rateLimitWindowSeconds).getEpochSecond();

            Long count = increment(formatKey(apiKey), now, lbound);
            if (count != null && count > rateLimit) {
                logError(apiKey, count);
                throw new RateLimitExceededException();
            }
            filterChain.doFilter(request, response);
        }catch (Exception e){
            resolver.resolveException(request, response, null, e);
        }
    }

    private Long increment (String key, long timestamp, long lbound) {
        return redisTemplate.execute(incrementScript, Collections.singletonList(key), UUID.randomUUID().toString(), String.valueOf(timestamp), String.valueOf(lbound));
    }

    private String formatKey(String apiKey) {
        return "rate_limit:" + apiKey;
    }
}
