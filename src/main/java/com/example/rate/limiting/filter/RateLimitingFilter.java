package com.example.rate.limiting.filter;

import com.example.rate.limiting.component.AsyncComponent;
import com.example.rate.limiting.component.PropsReader;
import com.example.rate.limiting.exception.MissingHeaderException;
import com.example.rate.limiting.exception.RateLimitExceededException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {
    private final PropsReader propsReader;
    private final HandlerExceptionResolver resolver;
    private final AsyncComponent asyncComponent;

    private final boolean useSlidingWindow = propsReader.getUseSlidingWindowRateLimiting();
    private final int rateLimit = propsReader.getRateLimit();
    private final int rateLimitWindowSeconds = propsReader.getRateLimitWindowSeconds();

    ConcurrentHashMap<String, Integer> rates = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, List<Long>> slidingWindowRates = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) {
        try {
            String apiKey = request.getHeader("X-Api-Key");
            if (apiKey == null) {
                throw new MissingHeaderException("X-Api-Key header is missing");
            }
            if (!useSlidingWindow) {
                //fixed rate limiting
                handleRateLimiting(apiKey);
            } else {
                //sliding window
                handleSlidingWindowRateLimiting(apiKey);
            }
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            resolver.resolveException(request, response, null, e);
        }
    }

    private void handleSlidingWindowRateLimiting(String apiKey) {
        slidingWindowRates.compute(apiKey,(k, v) -> {
            if (v == null) {
                List<Long> values = new ArrayList<>();
                values.add(Instant.now().getEpochSecond());
                return values;
            } else {
                Long limit = Instant.now().minusSeconds(rateLimitWindowSeconds).getEpochSecond();
                long rateCount = v.stream().filter(l -> l > limit).count();
                if (rateCount >= rateLimit) {
                    logError(apiKey, rateCount);
                    throw new RateLimitExceededException();
                }

                v.add(Instant.now().getEpochSecond());
                //Cache eviction
                asyncComponent.evictCache(slidingWindowRates, apiKey);
                return v;
            }
        });
    }

    private void handleRateLimiting(String apiKey) {
        rates.compute(apiKey, (k, v) -> {
            if (v == null) {
                return 1;
            } else {
                int current = v;
                current++;
                if (current > rateLimit) {
                    logError(apiKey, v);
                    throw new RateLimitExceededException();
                }
                return current;
            }
        });
    }

    private static void logError(String apiKey, long rateCount) {
        log.error("RateLimitExceededException, API Key: {}; Rate: {}", apiKey, rateCount);
    }
}
