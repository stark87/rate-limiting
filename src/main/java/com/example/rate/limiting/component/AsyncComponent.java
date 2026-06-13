package com.example.rate.limiting.component;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
public class AsyncComponent {
    private final PropsReader propsReader;

    private final int rateLimitWindowSeconds = propsReader.getRateLimitWindowSeconds();

    @Async
    public void evictCache(ConcurrentMap<String, List<Long>> map, String key){
        map.computeIfPresent(key, (k, v) -> {
            long cutoff = Instant.now().minusSeconds(rateLimitWindowSeconds).getEpochSecond();
            return v.stream().filter(l -> l > cutoff)
                    .collect(Collectors.toCollection(ArrayList::new));
        });
    }
}
