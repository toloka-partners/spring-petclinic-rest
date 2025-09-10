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
package org.springframework.samples.petclinic.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.config.CacheConfig;
import org.springframework.samples.petclinic.service.ClinicService;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for cache admin REST controller
 *
 * @author Vitaliy Fedoriv
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, 
                properties = {"petclinic.security.enable=false"})
@ActiveProfiles({"spring-data-jpa", "hsqldb"})
class CacheAdminRestControllerTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ClinicService clinicService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    @Test
    void shouldClearAllCaches() throws Exception {
        Cache vetCache = cacheManager.getCache(CacheConfig.CACHE_VETS);
        Cache ownerCache = cacheManager.getCache(CacheConfig.CACHE_OWNERS);
        
        clinicService.findVets();
        clinicService.findOwnerById(1);
        
        assertThat(vetCache.get("all-vets")).isNotNull();
        assertThat(ownerCache.get(1)).isNotNull();

        String url = "http://localhost:" + port + "/petclinic/api/cache";
        ResponseEntity<String> response = restTemplate.exchange(
            url, 
            HttpMethod.DELETE, 
            HttpEntity.EMPTY, 
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("All caches cleared successfully");
        
        assertThat(vetCache.get("all-vets")).isNull();
        assertThat(ownerCache.get(1)).isNull();
    }

    @Test
    void shouldHandleEmptyCache() throws Exception {
        String url = "http://localhost:" + port + "/petclinic/api/cache";
        ResponseEntity<String> response = restTemplate.exchange(
            url, 
            HttpMethod.DELETE, 
            HttpEntity.EMPTY, 
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("All caches cleared successfully");
    }
}