package com.example.rate.limiting.filter;

import com.example.rate.limiting.component.PropsReader;
import com.example.rate.limiting.exception.MissingHeaderException;
import com.example.rate.limiting.exception.RateLimitExceededException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Collections;

import static com.example.rate.limiting.filter.RateLimitingFilter.logError;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "feature.rate.limiting.filter",
        havingValue = "token-bucket"
)
public class TokenBucketRateLimitFilter extends OncePerRequestFilter {
    private final PropsReader propsReader;
    private final RedisTemplate<String, String> redisTemplate;
    @Qualifier("tokenBucketScript")
    private final RedisScript<Long> tokenBucketScript;
    @Qualifier("lazyRefillScript")
    private final RedisScript<Void> lazyRefillScript;
    @Qualifier("handlerExceptionResolver")
    private final HandlerExceptionResolver resolver;

    private int rateLimit;
    private int refillRate;

    @PostConstruct
    void init (){
        rateLimit = propsReader.getRateLimit();
        refillRate = propsReader.getTokenRefillPerSecond();
    }

    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain filterChain) throws ServletException, IOException {
        try{
            String apiKey = request.getHeader("X-Api-Key");
            if (apiKey == null) {
                throw new MissingHeaderException("X-Api-Key header is missing");
            }

            if (!propsReader.isUseScheduledRefiller()){
                lazyRefill(apiKey);
            } else {
                redisTemplate.opsForSet().add("keys", apiKey);
            }

            long tokenCount = redisTemplate.execute(tokenBucketScript, Collections.singletonList(apiKey), String.valueOf(rateLimit));
            if (tokenCount < 0) {
                logError(apiKey, tokenCount);
                throw new RateLimitExceededException("Rate limit exceeded");
            }

        } catch (Exception e) {
            resolver.resolveException(request, response, null, e);
        }
        filterChain.doFilter(request, response);
    }

    private void lazyRefill(String apiKey){
        redisTemplate.execute(lazyRefillScript, Collections.singletonList(apiKey), String.valueOf(rateLimit), String.valueOf(refillRate));
    }
}
