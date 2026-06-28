package com.example.rate.limiting.component;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class PropsReader {

    @Value("${settings.use.sliding.window.rate.limiting}")
    private Boolean useSlidingWindowRateLimiting;

    @Value("${settings.rate.limit}")
    private int rateLimit;

    @Value("${settings.rate.limit.window.seconds}")
    private int rateLimitWindowSeconds;
}
