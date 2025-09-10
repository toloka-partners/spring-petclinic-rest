/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Cache configuration for the PetClinic application
 *
 * @author Vitaliy Fedoriv
 */
@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig {

    public static final String CACHE_VETS = "vets";
    public static final String CACHE_PETS = "pets";
    public static final String CACHE_PET_TYPES = "petTypes";
    public static final String CACHE_OWNERS = "owners";
    public static final String CACHE_VISITS = "visits";
    public static final String CACHE_SPECIALTIES = "specialties";

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
            CACHE_VETS,
            CACHE_PETS,
            CACHE_PET_TYPES,
            CACHE_OWNERS,
            CACHE_VISITS,
            CACHE_SPECIALTIES
        );
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }

    @Scheduled(fixedDelay = 3600000) // 1 hour in milliseconds
    @CacheEvict(allEntries = true, value = {CACHE_VETS, CACHE_PETS, CACHE_PET_TYPES, CACHE_OWNERS, CACHE_VISITS, CACHE_SPECIALTIES})
    public void evictAllCaches() {
        // Scheduled cache eviction every hour
    }
}