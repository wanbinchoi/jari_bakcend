package com.project.jari.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CacheManager cacheManager;

    /**
     * 캐시 통계 조회
     *
     * 면접 포인트: "캐싱 효과를 정량적으로 측정했습니다"
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        org.springframework.cache.Cache springCache = cacheManager.getCache("coordinates");

        if (springCache == null) {
            return ResponseEntity.ok(Map.of("error", "캐시를 찾을 수 없습니다"));
        }

        // spring cache를 caffeinecache로 캐스팅
        CaffeineCache caffeineCache = (CaffeineCache) springCache;
        // cache 원본으로 캐스팅
        Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
        CacheStats stats = nativeCache.stats();

        Map<String, Object> result = new HashMap<>();
        result.put("캐시_이름", "coordinates");
        result.put("저장된_항목_수", nativeCache.estimatedSize());
        result.put("총_요청_수", stats.requestCount());
        result.put("캐시_히트_수", stats.hitCount());
        result.put("캐시_미스_수", stats.missCount());
        result.put("캐시_히트율", String.format("%.2f%%", stats.hitRate() * 100));
        result.put("평균_로드_시간", String.format("%.2f ms", stats.averageLoadPenalty() / 1_000_000));
        result.put("삭제된_항목_수", stats.evictionCount());

        log.info("캐시 통계 조회: 히트율={}, 저장 항목={}",
                stats.hitRate(), nativeCache.estimatedSize());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/showcache")
    public ResponseEntity<Map<Object, Object>> printCacheContents(
            @RequestParam String cacheName) {

        CaffeineCache caffeineCache = (CaffeineCache) cacheManager.getCache(cacheName);

        if (caffeineCache != null) {
            Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
            System.out.println("현재 캐시 내용: " + nativeCache.asMap());
            return ResponseEntity.ok(nativeCache.asMap());
        } else {
            System.out.println("캐시 이름이 존재하지 않아요: " + cacheName);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "캐시 이름이 존재하지 않습니다: " + cacheName
            ));
        }
    }

    /**
     * 특정 주소의 캐시 삭제
     */
    @DeleteMapping("/evict")
    public ResponseEntity<Map<String, String>> evictCache(@RequestParam String address) {
        org.springframework.cache.Cache cache = cacheManager.getCache("coordinates");

        if (cache != null) {
            cache.evict(address);
            log.info("캐시 삭제: {}", address);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "캐시가 삭제되었습니다: " + address
            ));
        }

        return ResponseEntity.ok(Map.of(
                "status", "error",
                "message", "캐시를 찾을 수 없습니다"
        ));
    }

    /**
     * 전체 캐시 삭제
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, String>> clearCache() {
        org.springframework.cache.Cache cache = cacheManager.getCache("coordinates");

        if (cache != null) {
            cache.clear();
            log.info("전체 캐시 삭제");
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "전체 캐시가 삭제되었습니다"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "status", "error",
                "message", "캐시를 찾을 수 없습니다"
        ));
    }
}