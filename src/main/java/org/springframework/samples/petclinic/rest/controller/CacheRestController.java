package org.springframework.samples.petclinic.rest.controller;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.config.CacheConfig;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cache")
@CrossOrigin(exposedHeaders = "errors, content-type")
public class CacheRestController {

    private final CacheConfig.CacheStatsCollector cacheStatsCollector;

    @Autowired
    public CacheRestController(CacheConfig.CacheStatsCollector cacheStatsCollector) {
        this.cacheStatsCollector = cacheStatsCollector;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, CacheStatsDto>> getCacheStats() {
        Map<String, CacheStats> stats = cacheStatsCollector.getStats();
        Map<String, CacheStatsDto> response = new HashMap<>();
        
        stats.forEach((name, cacheStats) -> {
            CacheStatsDto dto = new CacheStatsDto();
            dto.setHitCount(cacheStats.hitCount());
            dto.setMissCount(cacheStats.missCount());
            response.put(name, dto);
        });
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearAllCaches() {
        cacheStatsCollector.clearAllCaches();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/clear/{cacheName}")
    public ResponseEntity<Void> clearCache(@PathVariable String cacheName) {
        cacheStatsCollector.clearCache(cacheName);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    
    public static class CacheStatsDto {
        private long hitCount;
        private long missCount;

        public long getHitCount() {
            return hitCount;
        }

        public void setHitCount(long hitCount) {
            this.hitCount = hitCount;
        }

        public long getMissCount() {
            return missCount;
        }

        public void setMissCount(long missCount) {
            this.missCount = missCount;
        }
    }
}
