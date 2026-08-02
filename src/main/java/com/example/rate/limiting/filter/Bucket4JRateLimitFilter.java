package com.example.rate.limiting.filter;

import com.example.rate.limiting.component.PropsReader;
import com.example.rate.limiting.exception.MissingHeaderException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "feature.rate.limiting.filter",
        havingValue = "bucket4j"
)
public class Bucket4JRateLimitFilter extends OncePerRequestFilter {
    private final ProxyManager<String> proxyManager;
    private final PropsReader propsReader;
    @Qualifier("handlerExceptionResolver")
    private final HandlerExceptionResolver resolver;
    private int rateLimit;
    private int burstRateLimit;
    private int refillRate;

    @PostConstruct
    void init() {
        burstRateLimit = propsReader.getBurstRateLimit();
        rateLimit = propsReader.getRateLimit();
        refillRate = propsReader.getTokenRefillPerSecond();
    }

    @Override
    protected void doFilterInternal(@Nonnull  HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain filterChain) {
        try {
            String apiKey = request.getHeader("X-Api-Key");
            if (apiKey == null) {
                throw new MissingHeaderException("X-Api-Key header is missing");
            }

            Bandwidth burst = Bandwidth.builder()
                    .capacity(burstRateLimit)
                    .refillIntervally(refillRate * 5L, Duration.ofSeconds(5))
                    .initialTokens(burstRateLimit)
                    .build();
            Bandwidth sustained = Bandwidth.builder()
                    .capacity(rateLimit)
                    .refillGreedy(refillRate * 50L, Duration.ofMinutes(1))
                    .initialTokens(rateLimit)
                    .build();

            BucketConfiguration bucketConfig = BucketConfiguration.builder()
                    .addLimit(burst)
                    .addLimit(sustained)
                    .build();

            Bucket bucket = proxyManager.builder()
                    .build(apiKey, () -> bucketConfig);

            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            if (probe.isConsumed()) {
                response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
                filterChain.doFilter(request, response);
            } else {
                long waitDuration = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
                log.error("API-key: {}, RateLimit Exceeded, Retry after: {}s", apiKey, waitDuration);
                response.addHeader(
                        "X-Rate-Limit-Retry-After-Seconds",
                        String.valueOf(waitDuration)
                );
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"error\": \"Rate limit exceeded\"}");
            }
        } catch (Exception ex) {
            resolver.resolveException(request, response, null, ex);
        }
    }
}
