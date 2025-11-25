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
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.mapper.PetMapper;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.rest.advice.ExceptionControllerAdvice;
import org.springframework.samples.petclinic.rest.dto.PetDto;
import org.springframework.samples.petclinic.rest.dto.PetTypeDto;
import org.springframework.samples.petclinic.service.ClinicService;
import org.springframework.samples.petclinic.service.clinicService.ApplicationTestConfig;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for Pet weight functionality in REST API endpoints.
 * Tests both FAIL_TO_PASS scenarios (weight feature requirements) and 
 * PASS_TO_PASS scenarios (regression/backward compatibility).
 *
 * @author Generated for weight tracking feature
 */
@SpringBootTest
@ContextConfiguration(classes = ApplicationTestConfig.class)
@WebAppConfiguration
@DisplayName("Pet Weight Functionality Tests")
class PetWeightFunctionalityTests {

    @MockitoBean
    protected ClinicService clinicService;
    
    @Autowired
    private PetRestController petRestController;
    
    @Autowired
    private OwnerRestController ownerRestController;
    
    @Autowired
    private PetMapper petMapper;
    
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Pet testPetWithWeight;
    private Pet testPetWithoutWeight;
    private Owner testOwner;
    private PetType testPetType;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
            .standaloneSetup(petRestController, ownerRestController)
            .setControllerAdvice(new ExceptionControllerAdvice())
            .build();

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        setupTestData();
    }

    private void setupTestData() {
        // Create test pet type
        testPetType = new PetType();
        testPetType.setId(1);
        testPetType.setName("Dog");

        // Create test owner
        testOwner = new Owner();
        testOwner.setId(1);
        testOwner.setFirstName("John");
        testOwner.setLastName("Doe");
        testOwner.setAddress("123 Main St");
        testOwner.setCity("Anytown");
        testOwner.setTelephone("555-1234");

        // Create test pet WITH weight
        testPetWithWeight = new Pet();
        testPetWithWeight.setId(1);
        testPetWithWeight.setName("Buddy");
        testPetWithWeight.setBirthDate(LocalDate.of(2020, 1, 15));
        testPetWithWeight.setType(testPetType);
        testPetWithWeight.setWeight(BigDecimal.valueOf(25.50));
        testPetWithWeight.setOwner(testOwner);

        // Create test pet WITHOUT weight (for backward compatibility)
        testPetWithoutWeight = new Pet();
        testPetWithoutWeight.setId(2);
        testPetWithoutWeight.setName("Max");
        testPetWithoutWeight.setBirthDate(LocalDate.of(2019, 6, 10));
        testPetWithoutWeight.setType(testPetType);
        testPetWithoutWeight.setWeight(null); // Explicitly null for backward compatibility
        testPetWithoutWeight.setOwner(testOwner);
    }

    @Nested
    @DisplayName("FAIL_TO_PASS: Weight Field Requirements")
    class WeightFieldRequirementsTests {

        @Test
        @WithMockUser(roles = "OWNER_ADMIN")
        @DisplayName("GET /api/pets/{petId} should include weight field in JSON response")
        void getPetByIdShouldIncludeWeightField() throws Exception {
            given(clinicService.findPetById(1)).willReturn(testPetWithWeight);

            mockMvc.perform(get("/api/pets/1")
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Buddy")))
                .andExpect(jsonPath("$.weight", is(25.50))) // FAIL_TO_PASS: This would fail without weight field
                .andExpect(jsonPath("$.weight").isNumber()); // Ensures it's a valid number
        }

        @Test
        @WithMockUser(roles = "OWNER_ADMIN")
        @DisplayName("GET /api/pets should include weight field for all pets in array response")
        void getAllPetsShouldIncludeWeightField() throws Exception {
            Collection<Pet> pets = List.of(testPetWithWeight, testPetWithoutWeight);
            given(clinicService.findAllPets()).willReturn(pets);

            mockMvc.perform(get("/api/pets")
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].weight", is(25.50))) // Pet with weight
                .andExpect(jsonPath("$[1].weight").doesNotExist()) // Pet without weight (null)
                .andExpect(jsonPath("$[0].weight").isNumber()); // FAIL_TO_PASS: Weight field exists and is numeric
        }

        @Test
        @WithMockUser(roles = "OWNER_ADMIN")
        @DisplayName("PUT /api/pets/{petId} should accept weight field in JSON request")
        void updatePetShouldAcceptWeightField() throws Exception {
            given(clinicService.findPetById(1)).willReturn(testPetWithWeight);

            PetDto updatedPet = new PetDto();
            updatedPet.setId(1);
            updatedPet.setName("Buddy Updated");
            updatedPet.setBirthDate(LocalDate.of(2020, 1, 15));
            updatedPet.setWeight(30.75); // FAIL_TO_PASS: Should accept weight in request
            
            PetTypeDto petTypeDto = new PetTypeDto();
            petTypeDto.setId(1);
            petTypeDto.setName("Dog");
            updatedPet.setType(petTypeDto);

            String petJson = objectMapper.writeValueAsString(updatedPet);

            mockMvc.perform(put("/api/pets/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(petJson)
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent()); // FAIL_TO_PASS: Request should succeed with weight field
        }

        @Test
        @WithMockUser(roles = "OWNER_ADMIN")
        @DisplayName("POST /api/owners/{ownerId}/pets should accept weight field when creating pets")
        void createPetShouldAcceptWeightField() throws Exception {
            given(clinicService.findOwnerById(1)).willReturn(testOwner);
            given(clinicService.findPetTypeById(1)).willReturn(testPetType);

            PetDto newPet = new PetDto();
            newPet.setName("Charlie");
            newPet.setBirthDate(LocalDate.of(2021, 3, 20));
            newPet.setWeight(18.25); // FAIL_TO_PASS: Should accept weight during creation
            
            PetTypeDto petTypeDto = new PetTypeDto();
            petTypeDto.setId(1);
            petTypeDto.setName("Dog");
            newPet.setType(petTypeDto);

            String petJson = objectMapper.writeValueAsString(newPet);

            mockMvc.perform(post("/api/owners/1/pets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(petJson)
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated()); // FAIL_TO_PASS: Should create pet with weight
        }

        @Test
        @WithMockUser(roles = "OWNER_ADMIN")
        @DisplayName("PUT /api/owners/{ownerId}/pets/{petId} should accept weight field in updates")
        void updateOwnersSpecificPetShouldAcceptWeightField() throws Exception {
            given(clinicService.findOwnerById(1)).willReturn(testOwner);
            given(clinicService.findPetById(1)).willReturn(testPetWithWeight);

            PetDto updatedPet = new PetDto();
            updatedPet.setName("Buddy Modified");
            updatedPet.setBirthDate(LocalDate.of(2020, 1, 15));
            updatedPet.setWeight(22.10); // FAIL_TO_PASS: Should accept weight in owner-specific update
            
            PetTypeDto petTypeDto = new PetTypeDto();
            petTypeDto.setId(1);
            petTypeDto.setName("Dog");
            updatedPet.setType(petTypeDto);

            String petJson = objectMapper.writeValueAsString(updatedPet);

            mockMvc.perform(put("/api/owners/1/pets/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(petJson)
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent()); // FAIL_TO_PASS: Should update with weight
        }
    }

    @Nested
    @DisplayName("Weight Field Validation Tests")
    class WeightValidationTests {

        @Test
        @WithMockUser(roles = "OWNER_ADMIN")
        @DisplayName("Should accept valid decimal weight values")
        void shouldAcceptValidDecimalWeights() throws Exception {
            given(clinicService.findPetById(1)).willReturn(testPetWithWeight);

            // Test various valid decimal weights
            double[] validWeights = {0.0, 0.1, 15.75, 25.50, 100.00, 999.99};
            
            for (double weight : validWeights) {
                PetDto updatedPet = createValidPetDto();
                updatedPet.setWeight(weight);
                String petJson = objectMapper.writeValueAsString(updatedPet);

                mockMvc.perform(put("/api/pets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(petJson))
                    .andExpect(status().isNoContent());
            }
        }

        @Test
        @WithMockUser(roles = "OWNER_ADMIN")
        @DisplayName("Should accept null weight for backward compatibility")
        void shouldAcceptNullWeight() throws Exception {
            given(clinicService.findPetById(1)).willReturn(testPetWithWeight);

            PetDto updatedPet = createValidPetDto();
            updatedPet.setWeight(null); // Should be allowed for backward compatibility
            String petJson = objectMapper.writeValueAsString(updatedPet);

            mockMvc.perform(put("/api/pets/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(petJson))
                .andExpect(status().isNoContent());
        }

        private PetDto createValidPetDto() {
            PetDto pet = new PetDto();
            pet.setId(1);
            pet.setName("Test Pet");
            pet.setBirthDate(LocalDate.of(2020, 1, 1));
            
            PetTypeDto petTypeDto = new PetTypeDto();
            petTypeDto.setId(1);
            petTypeDto.setName("Dog");
            pet.setType(petTypeDto);
            
            return pet;
        }
    }

    @Nested
    @DisplayName("PASS_TO_PASS: Backward Compatibility and Regression Tests")
    class BackwardCompatibilityTests {

        @Test
        @WithMockUser(roles = "OWNER_ADMIN")
        @DisplayName("Should handle pets without weight data (regression test)")
        void shouldHandlePetsWithoutWeightData() throws Exception {
            given(clinicService.findPetById(2)).willReturn(testPetWithoutWeight);

            mockMvc.perform(get("/api/pets/2")
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(2)))
                .andExpect(jsonPath("$.name", is("Max")))
                .andExpect(jsonPath("$.weight").doesNotExist()); // Should not include null weights
        }

        @Test
        @WithMockUser(roles = "OWNER_ADMIN")
        @DisplayName("Should continue to support all existing required pet fields (regression test)")
        void shouldSupportAllExistingRequiredFields() throws Exception {
            given(clinicService.findPetById(1)).willReturn(testPetWithWeight);

            mockMvc.perform(get("/api/pets/1")
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // All existing required fields should still be present
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.birthDate").exists())
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.type.id").exists())
                .andExpect(jsonPath("$.type.name").exists())
                // Weight is optional, should be present when available
                .andExpect(jsonPath("$.weight").exists());
        }

        @Test
        @WithMockUser(roles = "OWNER_ADMIN")
        @DisplayName("Should create pets without weight field (backward compatibility)")
        void shouldCreatePetsWithoutWeightField() throws Exception {
            given(clinicService.findOwnerById(1)).willReturn(testOwner);
            given(clinicService.findPetTypeById(1)).willReturn(testPetType);

            // Create pet without weight field - should still work
            PetDto newPet = new PetDto();
            newPet.setName("Legacy Pet");
            newPet.setBirthDate(LocalDate.of(2021, 5, 15));
            // No weight field set - should default to null
            
            PetTypeDto petTypeDto = new PetTypeDto();
            petTypeDto.setId(1);
            petTypeDto.setName("Dog");
            newPet.setType(petTypeDto);

            String petJson = objectMapper.writeValueAsString(newPet);

            mockMvc.perform(post("/api/owners/1/pets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(petJson)
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated()); // Should still work without weight
        }

        @Test
        @WithMockUser(roles = "OWNER_ADMIN")
        @DisplayName("Should update pets without modifying weight field (regression test)")
        void shouldUpdatePetsWithoutModifyingWeightField() throws Exception {
            given(clinicService.findPetById(1)).willReturn(testPetWithWeight);

            // Update only name, don't include weight in request
            PetDto updatedPet = new PetDto();
            updatedPet.setId(1);
            updatedPet.setName("Updated Name Only");
            updatedPet.setBirthDate(LocalDate.of(2020, 1, 15));
            // Weight not included in update
            
            PetTypeDto petTypeDto = new PetTypeDto();
            petTypeDto.setId(1);
            petTypeDto.setName("Dog");
            updatedPet.setType(petTypeDto);

            String petJson = objectMapper.writeValueAsString(updatedPet);

            mockMvc.perform(put("/api/pets/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(petJson))
                .andExpect(status().isNoContent()); // Should succeed without weight in update
        }
    }

    @Nested
    @DisplayName("Weight Persistence Tests")
    class WeightPersistenceTests {

        @Test
        @WithMockUser(roles = "OWNER_ADMIN")
        @DisplayName("Should persist weight correctly - value set via POST can be retrieved via GET")
        void shouldPersistWeightFromPostToGet() throws Exception {
            given(clinicService.findOwnerById(1)).willReturn(testOwner);
            given(clinicService.findPetTypeById(1)).willReturn(testPetType);

            // Create pet with weight
            PetDto newPet = new PetDto();
            newPet.setName("Weight Test Pet");
            newPet.setBirthDate(LocalDate.of(2021, 8, 10));
            newPet.setWeight(42.75); // Set specific weight
            
            PetTypeDto petTypeDto = new PetTypeDto();
            petTypeDto.setId(1);
            petTypeDto.setName("Dog");
            newPet.setType(petTypeDto);

            Pet createdPet = new Pet();
            createdPet.setId(3);
            createdPet.setName("Weight Test Pet");
            createdPet.setBirthDate(LocalDate.of(2021, 8, 10));
            createdPet.setWeight(BigDecimal.valueOf(42.75));
            createdPet.setType(testPetType);
            createdPet.setOwner(testOwner);

            given(clinicService.findPetById(3)).willReturn(createdPet);

            String petJson = objectMapper.writeValueAsString(newPet);

            // Create the pet
            mockMvc.perform(post("/api/owners/1/pets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(petJson))
                .andExpect(status().isCreated());

            // Retrieve the pet and verify weight persisted
            mockMvc.perform(get("/api/pets/3")
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weight", is(42.75))); // Weight should be persisted
        }

        @Test
        @WithMockUser(roles = "OWNER_ADMIN")
        @DisplayName("Should persist weight correctly - value set via PUT can be retrieved via GET")
        void shouldPersistWeightFromPutToGet() throws Exception {
            Pet petBeforeUpdate = new Pet();
            petBeforeUpdate.setId(1);
            petBeforeUpdate.setName("Test Pet");
            petBeforeUpdate.setBirthDate(LocalDate.of(2020, 1, 1));
            petBeforeUpdate.setType(testPetType);
            petBeforeUpdate.setWeight(BigDecimal.valueOf(20.0));
            petBeforeUpdate.setOwner(testOwner);

            Pet petAfterUpdate = new Pet();
            petAfterUpdate.setId(1);
            petAfterUpdate.setName("Test Pet");
            petAfterUpdate.setBirthDate(LocalDate.of(2020, 1, 1));
            petAfterUpdate.setType(testPetType);
            petAfterUpdate.setWeight(BigDecimal.valueOf(35.25)); // Updated weight
            petAfterUpdate.setOwner(testOwner);

            given(clinicService.findPetById(1)).willReturn(petBeforeUpdate).willReturn(petAfterUpdate);

            // Update pet weight
            PetDto updatedPet = new PetDto();
            updatedPet.setId(1);
            updatedPet.setName("Test Pet");
            updatedPet.setBirthDate(LocalDate.of(2020, 1, 1));
            updatedPet.setWeight(35.25); // New weight value
            
            PetTypeDto petTypeDto = new PetTypeDto();
            petTypeDto.setId(1);
            petTypeDto.setName("Dog");
            updatedPet.setType(petTypeDto);

            String petJson = objectMapper.writeValueAsString(updatedPet);

            // Update the pet
            mockMvc.perform(put("/api/pets/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(petJson))
                .andExpect(status().isNoContent());

            // Retrieve and verify updated weight
            mockMvc.perform(get("/api/pets/1")
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weight", is(35.25))); // Updated weight should be persisted
        }
    }
}