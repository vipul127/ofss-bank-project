package com.bankdemo.cashmanagementservice.service;

import com.bankdemo.cashmanagementservice.entity.AiInsightCache;
import com.bankdemo.cashmanagementservice.repository.AiInsightCacheRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Shared DB-backed cache for AI responses (branch explanations, admin network briefing).
 * Freshness is a plain LAST_UPDATED-timestamp-vs-now comparison in Java — the AI itself is never
 * asked to reason about "today's date" for this decision, and never sees a cache-related date at
 * all; it only ever sees the actual analysis/forecast data it needs to answer the question.
 * Per-key locking means two near-simultaneous requests for the same stale key trigger one
 * OpenRouter call, not two — the second waits and gets the first's freshly-cached result. */
@Service
public class AiInsightCacheService {
    private static final Logger log = LoggerFactory.getLogger(AiInsightCacheService.class);
    private final AiInsightCacheRepository repository;
    private final ObjectMapper mapper;
    private final Duration ttl;
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    public AiInsightCacheService(AiInsightCacheRepository repository, ObjectMapper mapper,
            @Value("${ai.cache-ttl-minutes:10}") long ttlMinutes) {
        this.repository = repository;
        this.mapper = mapper;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    /** Returns the cached value for {@code key} if it's younger than the TTL; otherwise calls
     * {@code refresher} (which is where the real OpenRouter/forecast/etc. calls happen), caches
     * the result, and returns that instead. */
    public <T> T getOrRefresh(String key, Class<T> type, Supplier<T> refresher) {
        Object lock = locks.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            Optional<AiInsightCache> cached = repository.findById(key);
            if (cached.isPresent() && Duration.between(cached.get().getLastUpdated(), LocalDateTime.now()).compareTo(ttl) < 0) {
                try {
                    return mapper.readValue(cached.get().getPayloadJson(), type);
                } catch (Exception e) {
                    log.warn("[AI cache] Corrupt cache entry for {}, refreshing instead: {}", key, e.toString());
                }
            }
            T fresh = refresher.get();
            save(key, fresh, cached.orElse(null));
            return fresh;
        }
    }

    // repository.save() is already transactional per Spring Data JPA's SimpleJpaRepository — no
    // extra @Transactional needed here (and it wouldn't apply via self-invocation anyway).
    private <T> void save(String key, T value, AiInsightCache existing) {
        try {
            AiInsightCache entity = existing != null ? existing : new AiInsightCache();
            entity.setCacheKey(key);
            entity.setPayloadJson(mapper.writeValueAsString(value));
            entity.setLastUpdated(LocalDateTime.now());
            repository.save(entity);
        } catch (Exception e) {
            log.warn("[AI cache] Failed to persist cache entry for {}: {}", key, e.toString());
        }
    }
}
