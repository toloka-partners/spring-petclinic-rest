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
package org.springframework.samples.petclinic.service.clinicService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.samples.petclinic.config.CacheConfig;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.model.Vet;
import org.springframework.samples.petclinic.service.ClinicService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for cache functionality
 *
 * @author Vitaliy Fedoriv
 */
@SpringBootTest
@ActiveProfiles({"spring-data-jpa", "hsqldb"})
class CacheIntegrationTests {

    @Autowired
    private ClinicService clinicService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearAllCaches() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    @Test
    void shouldCacheVetData() {
        Cache vetCache = cacheManager.getCache(CacheConfig.CACHE_VETS);
        assertThat(vetCache).isNotNull();
        assertThat(vetCache.get("all-vets")).isNull();

        Collection<Vet> vets1 = clinicService.findVets();
        assertThat(vets1).isNotEmpty();
        assertThat(vetCache.get("all-vets")).isNotNull();

        Collection<Vet> vets2 = clinicService.findVets();
        assertThat(vets2).isEqualTo(vets1);
    }

    @Test
    void shouldCacheSingleVetData() {
        Cache vetCache = cacheManager.getCache(CacheConfig.CACHE_VETS);
        assertThat(vetCache).isNotNull();
        assertThat(vetCache.get(1)).isNull();

        Vet vet1 = clinicService.findVetById(1);
        assertThat(vet1).isNotNull();
        assertThat(vetCache.get(1)).isNotNull();

        Vet vet2 = clinicService.findVetById(1);
        assertThat(vet2).isEqualTo(vet1);
    }

    @Test
    @Transactional
    void shouldEvictVetCacheOnSave() {
        Cache vetCache = cacheManager.getCache(CacheConfig.CACHE_VETS);
        
        Collection<Vet> vets = clinicService.findVets();
        assertThat(vetCache.get("all-vets")).isNotNull();

        Vet vet = new Vet();
        vet.setFirstName("Test");
        vet.setLastName("Vet");
        clinicService.saveVet(vet);

        assertThat(vetCache.get("all-vets")).isNull();
    }

    @Test
    void shouldCacheOwnerData() {
        Cache ownerCache = cacheManager.getCache(CacheConfig.CACHE_OWNERS);
        assertThat(ownerCache).isNotNull();

        Owner owner1 = clinicService.findOwnerById(1);
        assertThat(owner1).isNotNull();
        assertThat(ownerCache.get(1)).isNotNull();

        Owner owner2 = clinicService.findOwnerById(1);
        assertThat(owner2).isEqualTo(owner1);
    }

    @Test
    void shouldCacheOwnerByLastNameData() {
        Cache ownerCache = cacheManager.getCache(CacheConfig.CACHE_OWNERS);
        assertThat(ownerCache).isNotNull();
        
        String cacheKey = "by-lastname-Davis";
        assertThat(ownerCache.get(cacheKey)).isNull();

        Collection<Owner> owners1 = clinicService.findOwnerByLastName("Davis");
        assertThat(owners1).isNotEmpty();
        assertThat(ownerCache.get(cacheKey)).isNotNull();

        Collection<Owner> owners2 = clinicService.findOwnerByLastName("Davis");
        assertThat(owners2).isEqualTo(owners1);
    }

    @Test
    @Transactional
    void shouldEvictOwnerCacheOnSave() {
        Cache ownerCache = cacheManager.getCache(CacheConfig.CACHE_OWNERS);
        
        Owner originalOwner = clinicService.findOwnerById(1);
        assertThat(ownerCache.get(1)).isNotNull();

        Owner owner = new Owner();
        owner.setFirstName("Test");
        owner.setLastName("Owner");
        owner.setAddress("123 Test St");
        owner.setCity("Test City");
        owner.setTelephone("5551234567");
        clinicService.saveOwner(owner);

        assertThat(ownerCache.get(1)).isNull();
    }

    @Test
    void shouldCachePetData() {
        Cache petCache = cacheManager.getCache(CacheConfig.CACHE_PETS);
        assertThat(petCache).isNotNull();

        Pet pet1 = clinicService.findPetById(1);
        assertThat(pet1).isNotNull();
        assertThat(petCache.get(1)).isNotNull();

        Pet pet2 = clinicService.findPetById(1);
        assertThat(pet2).isEqualTo(pet1);
    }

    @Test
    @Transactional
    void shouldEvictPetCacheOnSave() {
        Cache petCache = cacheManager.getCache(CacheConfig.CACHE_PETS);
        
        Collection<Pet> pets = clinicService.findAllPets();
        
        // Check if any cache entry exists (the exact key structure may vary)
        boolean hasCacheEntry = false;
        ConcurrentMapCache concurrentMapCache = (ConcurrentMapCache) petCache;
        for (Object key : concurrentMapCache.getNativeCache().keySet()) {
            if (petCache.get(key) != null) {
                hasCacheEntry = true;
                break;
            }
        }
        assertThat(hasCacheEntry).isTrue();

        Pet pet = new Pet();
        pet.setName("Test Pet");
        pet.setBirthDate(LocalDate.now());
        
        PetType petType = clinicService.findPetTypeById(1);
        pet.setType(petType);
        
        Owner owner = clinicService.findOwnerById(1);
        pet.setOwner(owner);
        
        clinicService.savePet(pet);

        // Check if cache is now empty
        boolean hasCacheEntryAfter = false;
        for (Object key : concurrentMapCache.getNativeCache().keySet()) {
            if (petCache.get(key) != null) {
                hasCacheEntryAfter = true;
                break;
            }
        }
        assertThat(hasCacheEntryAfter).isFalse();
    }

    @Test
    void shouldCachePetTypeData() {
        Cache petTypeCache = cacheManager.getCache(CacheConfig.CACHE_PET_TYPES);
        assertThat(petTypeCache).isNotNull();

        PetType petType1 = clinicService.findPetTypeById(1);
        assertThat(petType1).isNotNull();
        assertThat(petTypeCache.get(1)).isNotNull();

        PetType petType2 = clinicService.findPetTypeById(1);
        assertThat(petType2).isEqualTo(petType1);
    }

    @Test
    @Transactional
    void shouldEvictPetTypeCacheOnSave() {
        Cache petTypeCache = cacheManager.getCache(CacheConfig.CACHE_PET_TYPES);
        
        Collection<PetType> petTypes = clinicService.findAllPetTypes();
        
        // Check if any cache entry exists (the exact key structure may vary)
        boolean hasCacheEntry = false;
        ConcurrentMapCache concurrentMapCacheType = (ConcurrentMapCache) petTypeCache;
        for (Object key : concurrentMapCacheType.getNativeCache().keySet()) {
            if (petTypeCache.get(key) != null) {
                hasCacheEntry = true;
                break;
            }
        }
        assertThat(hasCacheEntry).isTrue();

        PetType petType = new PetType();
        petType.setName("Test Pet Type");
        clinicService.savePetType(petType);

        // Check if cache is now empty
        boolean hasCacheEntryAfter = false;
        for (Object key : concurrentMapCacheType.getNativeCache().keySet()) {
            if (petTypeCache.get(key) != null) {
                hasCacheEntryAfter = true;
                break;
            }
        }
        assertThat(hasCacheEntryAfter).isFalse();
    }
}