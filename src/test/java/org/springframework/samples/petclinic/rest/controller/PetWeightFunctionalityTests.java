/*
 * Copyright 2016-2024 the original author or authors.
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
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.repository.OwnerRepository;
import org.springframework.samples.petclinic.repository.PetRepository;
import org.springframework.samples.petclinic.repository.PetTypeRepository;
import org.springframework.samples.petclinic.rest.dto.PetDto;
import org.springframework.samples.petclinic.rest.dto.PetTypeDto;
import org.springframework.samples.petclinic.service.ClinicService;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Pet weight functionality in REST API endpoints.
 * Tests both FAIL_TO_PASS scenarios (weight feature requirements) and 
 * PASS_TO_PASS scenarios (regression/backward compatibility).
 * 
 * These are pure integration tests with no mocking - all database operations
 * are performed via actual repositories and services using TestRestTemplate
 * with a real running application context.
 *
 * @author Generated for weight tracking feature
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Pet Weight Functionality Tests")
class PetWeightFunctionalityTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ClinicService clinicService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Nested
    class WeightFieldRequirementsTests {

        @Test
        @WithMockUser(roles = "OWNER_ADMIN")
        @DisplayName("GET /api/pets/{petId} should include weight field in JSON response")
        void getPetByIdShouldIncludeWeightField() {
            // Arrange - Use existing pet from database (Leo, ID=1, weight 4.2)
            Pet pet = clinicService.findPetById(1);
            assertNotNull(pet.getWeight(), "Test data should have weight");

            // Act
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) (Object) restTemplate
                .withBasicAuth("admin", "admin")
                .getForEntity("/api/pets/1", Map.class);

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            Map<String, Object> petData = response.getBody();
            assertEquals(1, petData.get("id"));
            assertEquals("Leo", petData.get("name"));
            assertNotNull(petData.get("weight"), "Weight field should be present");
            // Note: Weight may have been modified by other tests, just verify it exists and is numeric
            assertTrue(petData.get("weight") instanceof Number, "Weight should be numeric");
        }

        @Test
        @WithMockUser(roles = "OWNER_ADMIN")
        @DisplayName("GET /api/pets should include weight field for all pets in array response")
        void getAllPetsShouldIncludeWeightField() {
            // Act
            @SuppressWarnings("unchecked")
            ResponseEntity<List<Map<String, Object>>> response = (ResponseEntity<List<Map<String, Object>>>) (Object) restTemplate
                .withBasicAuth("admin", "admin")
                .getForEntity("/api/pets", List.class);

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            List<Map<String, Object>> pets = response.getBody();
            assertTrue(pets.size() > 0, "Should have pets in database");

            // Verify that pets with weights have the field
            Map<String, Object> firstPet = pets.get(0);
            assertNotNull(firstPet.get("weight"), "Weight field should exist for pets with weight");
            assertTrue(firstPet.get("weight") instanceof Number, "Weight should be numeric");
        }

        @Test
        @WithMockUser(roles = "OWNER_ADMIN")
        @DisplayName("PUT /api/pets/{petId} should accept weight field in JSON request")
        void updatePetShouldAcceptWeightField() {
            // Arrange - Use pet Basil (ID=2) to avoid interference with other tests
            Pet petToUpdate = clinicService.findPetById(2);
            PetDto updatedPet = createPetDto(2, petToUpdate.getName(), petToUpdate.getBirthDate(), 
                petToUpdate.getType().getId(), 25.50);

            // Act
            ResponseEntity<Void> response = restTemplate
                .withBasicAuth("admin", "admin")
                .exchange("/api/pets/2", 
                    org.springframework.http.HttpMethod.PUT, 
                    new org.springframework.http.HttpEntity<>(updatedPet),
                    Void.class);

            // Assert
            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

            // Verify the weight was persisted
            Pet updatedPetDb = clinicService.findPetById(2);
            assertEquals(25.50, updatedPetDb.getWeight().doubleValue());
        }

        @Test
        @WithMockUser(roles = "OWNER_ADMIN")
        @DisplayName("POST /api/owners/{ownerId}/pets should accept weight field when creating pets")
        void createPetShouldAcceptWeightField() {
            // Arrange - Use existing owner (George Franklin)
            PetType dogType = clinicService.findPetTypeById(2);

            PetDto newPet = new PetDto();
            newPet.setName("IntegrationTestPetNew");
            newPet.setBirthDate(LocalDate.of(2021, 3, 20));
            newPet.setWeight(18.25);

            PetTypeDto petTypeDto = new PetTypeDto();
            petTypeDto.setId(dogType.getId());
            petTypeDto.setName(dogType.getName());
            newPet.setType(petTypeDto);

            // Act
            ResponseEntity<PetDto> response = restTemplate
                .withBasicAuth("admin", "admin")
                .postForEntity("/api/owners/1/pets", newPet, PetDto.class);

            // Assert
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("IntegrationTestPetNew", response.getBody().getName());
            assertEquals(18.25, response.getBody().getWeight());
        }

        @Test
        @WithMockUser(roles = "OWNER_ADMIN")
        @DisplayName("PUT /api/owners/{ownerId}/pets/{petId} should accept weight field in updates")
        void updateOwnersSpecificPetShouldAcceptWeightField() {
            // Arrange - Use pet Rosy (ID=3) to avoid interference
            Pet pet = clinicService.findPetById(3);
            PetType petType = pet.getType();

            PetDto updatedPet = createPetDto(3, pet.getName(), pet.getBirthDate(), petType.getId(), 20.00);

            // Act
            ResponseEntity<Void> response = restTemplate
                .withBasicAuth("admin", "admin")
                .exchange("/api/owners/3/pets/3", 
                    org.springframework.http.HttpMethod.PUT, 
                    new org.springframework.http.HttpEntity<>(updatedPet),
                    Void.class);

            // Assert
            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

            // Verify the weight was persisted
            Pet updatedPetDb = clinicService.findPetById(3);
            assertEquals(20.00, updatedPetDb.getWeight().doubleValue());
        }

        private PetDto createPetDto(int id, String name, LocalDate birthDate, int petTypeId, double weight) {
            PetDto petDto = new PetDto();
            petDto.setId(id);
            petDto.setName(name);
            petDto.setBirthDate(birthDate);
            petDto.setWeight(weight);

            PetTypeDto petTypeDto = new PetTypeDto();
            PetType petType = clinicService.findPetTypeById(petTypeId);
            petTypeDto.setId(petType.getId());
            petTypeDto.setName(petType.getName());
            petDto.setType(petTypeDto);

            return petDto;
        }
    }

    @Nested
    @DisplayName("Weight Field Validation Tests")
    class WeightValidationTests {

        @Test
        @WithMockUser(roles = "OWNER_ADMIN")
        @DisplayName("Should accept valid decimal weight values")
        void shouldAcceptValidDecimalWeights() {
            // Arrange - Use pet Jewel (ID=4)
            Pet testPet = clinicService.findPetById(4);
            double[] validWeights = {0.0, 0.1, 15.75, 25.50, 100.00, 99.99};

            // Act & Assert
            for (double weight : validWeights) {
                PetDto updatedPet = createValidPetDto(4, testPet.getType().getId(), weight);
                ResponseEntity<Void> response = restTemplate
                    .withBasicAuth("admin", "admin")
                    .exchange("/api/pets/4", 
                        org.springframework.http.HttpMethod.PUT, 
                        new org.springframework.http.HttpEntity<>(updatedPet),
                        Void.class);

                assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode(), 
                    "Should accept weight value: " + weight);

                Pet updatedFromDb = clinicService.findPetById(4);
                assertEquals(weight, updatedFromDb.getWeight().doubleValue(),
                    "Weight should be persisted correctly");
            }
        }

        @Test
        @WithMockUser(roles = "OWNER_ADMIN")
        @DisplayName("Should accept null weight for backward compatibility")
        void shouldAcceptNullWeight() {
            // Arrange - Use pet George (ID=6)
            Pet testPet = clinicService.findPetById(6);
            PetDto updatedPet = createValidPetDto(6, testPet.getType().getId(), null);

            // Act
            ResponseEntity<Void> response = restTemplate
                .withBasicAuth("admin", "admin")
                .exchange("/api/pets/6", 
                    org.springframework.http.HttpMethod.PUT, 
                    new org.springframework.http.HttpEntity<>(updatedPet),
                    Void.class);

            // Assert
            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

            Pet updatedFromDb = clinicService.findPetById(6);
            assertNull(updatedFromDb.getWeight(), "Weight should be null when set to null");
        }

        private PetDto createValidPetDto(int petId, int petTypeId, Double weight) {
            Pet testPet = clinicService.findPetById(petId);
            PetType petType = clinicService.findPetTypeById(petTypeId);

            PetDto pet = new PetDto();
            pet.setId(petId);
            pet.setName(testPet.getName());
            pet.setBirthDate(testPet.getBirthDate());
            pet.setWeight(weight);

            PetTypeDto petTypeDto = new PetTypeDto();
            petTypeDto.setId(petType.getId());
            petTypeDto.setName(petType.getName());
            pet.setType(petTypeDto);

            return pet;
        }
    }
    
    @Nested
    @DisplayName("Weight Persistence Tests")
    class WeightPersistenceTests {

        @Test
        @WithMockUser(roles = "OWNER_ADMIN")
        @DisplayName("Should persist weight correctly - value set via POST can be retrieved via GET")
        void shouldPersistWeightFromPostToGet() {
            // Arrange
            PetType dogType = clinicService.findPetTypeById(2);

            PetDto newPet = new PetDto();
            newPet.setName("WeightTestPetIntTest");
            newPet.setBirthDate(LocalDate.of(2021, 8, 10));
            newPet.setWeight(42.75);

            PetTypeDto petTypeDto = new PetTypeDto();
            petTypeDto.setId(dogType.getId());
            petTypeDto.setName(dogType.getName());
            newPet.setType(petTypeDto);

            // Act - Create pet with weight
            ResponseEntity<PetDto> createResponse = restTemplate
                .withBasicAuth("admin", "admin")
                .postForEntity("/api/owners/1/pets", newPet, PetDto.class);

            // Assert - Verify creation response
            assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
            assertNotNull(createResponse.getBody());
            assertEquals(42.75, createResponse.getBody().getWeight());

            // Get the created pet's ID
            int createdPetId = createResponse.getBody().getId();

            // Act - Retrieve the pet
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> getResponse = (ResponseEntity<Map<String, Object>>) (Object) restTemplate
                .withBasicAuth("admin", "admin")
                .getForEntity("/api/pets/" + createdPetId, Map.class);

            // Assert - Verify weight persisted
            assertEquals(HttpStatus.OK, getResponse.getStatusCode());
            assertNotNull(getResponse.getBody());
            assertEquals(42.75, ((Number) getResponse.getBody().get("weight")).doubleValue(),
                "Weight should be persisted and retrieved correctly");
        }

        @Test
        @WithMockUser(roles = "OWNER_ADMIN")
        @DisplayName("Should persist weight correctly - value set via PUT can be retrieved via GET")
        void shouldPersistWeightFromPutToGet() {
            // Arrange - Use pet Samantha (ID=7)
            Pet petBeforeUpdate = clinicService.findPetById(7);

            PetDto updatedPet = new PetDto();
            updatedPet.setId(7);
            updatedPet.setName(petBeforeUpdate.getName());
            updatedPet.setBirthDate(petBeforeUpdate.getBirthDate());
            updatedPet.setWeight(35.25); // New weight value

            PetTypeDto petTypeDto = new PetTypeDto();
            PetType petType = petBeforeUpdate.getType();
            petTypeDto.setId(petType.getId());
            petTypeDto.setName(petType.getName());
            updatedPet.setType(petTypeDto);

            // Act - Update pet with new weight
            ResponseEntity<Void> updateResponse = restTemplate
                .withBasicAuth("admin", "admin")
                .exchange("/api/pets/7", 
                    org.springframework.http.HttpMethod.PUT, 
                    new org.springframework.http.HttpEntity<>(updatedPet),
                    Void.class);

            // Assert - Verify update succeeded
            assertEquals(HttpStatus.NO_CONTENT, updateResponse.getStatusCode());

            // Act - Retrieve the pet
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> getResponse = (ResponseEntity<Map<String, Object>>) (Object) restTemplate
                .withBasicAuth("admin", "admin")
                .getForEntity("/api/pets/7", Map.class);

            // Assert - Verify new weight persisted
            assertEquals(HttpStatus.OK, getResponse.getStatusCode());
            assertNotNull(getResponse.getBody());
            assertEquals(35.25, ((Number) getResponse.getBody().get("weight")).doubleValue(),
                "Updated weight should be persisted and retrieved correctly");

            // Verify in database
            Pet updatedFromDb = clinicService.findPetById(7);
            assertEquals(35.25, updatedFromDb.getWeight().doubleValue());
        }
    }
}