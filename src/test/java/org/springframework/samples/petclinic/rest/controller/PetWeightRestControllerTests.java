/*
 * Copyright 2016-2017 the original author or authors.
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.mapper.OwnerMapper;
import org.springframework.samples.petclinic.mapper.PetMapper;
import org.springframework.samples.petclinic.rest.advice.ExceptionControllerAdvice;
import org.springframework.samples.petclinic.rest.dto.OwnerDto;
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

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive test class for Pet weight field functionality through REST API endpoints
 *
 * @author Generated for Weight Feature Implementation
 */
@SpringBootTest
@ContextConfiguration(classes = ApplicationTestConfig.class)
@WebAppConfiguration
class PetWeightRestControllerTests {

    @MockitoBean
    protected ClinicService clinicService;
    
    @Autowired
    private PetRestController petRestController;
    
    @Autowired
    private OwnerRestController ownerRestController;
    
    @Autowired
    private PetMapper petMapper;
    
    @Autowired
    private OwnerMapper ownerMapper;
    
    private MockMvc petMockMvc;
    private MockMvc ownerMockMvc;

    private List<PetDto> pets;
    private List<OwnerDto> owners;

    @BeforeEach
    void initTestData() {
        this.petMockMvc = MockMvcBuilders.standaloneSetup(petRestController)
            .setControllerAdvice(new ExceptionControllerAdvice())
            .build();
            
        this.ownerMockMvc = MockMvcBuilders.standaloneSetup(ownerRestController)
            .setControllerAdvice(new ExceptionControllerAdvice())
            .build();

        // Create test owners
        owners = new ArrayList<>();
        OwnerDto owner = new OwnerDto();
        owners.add(owner.id(1).firstName("John").lastName("Doe")
            .address("123 Main St.").city("Springfield").telephone("555-1234"));

        // Create test pet types
        PetTypeDto dogType = new PetTypeDto();
        dogType.id(1).name("dog");
        
        PetTypeDto catType = new PetTypeDto();
        catType.id(2).name("cat");

        // Create test pets with various weight scenarios
        pets = new ArrayList<>();
        
        // Pet with normal weight
        PetDto petWithWeight = new PetDto();
        pets.add(petWithWeight.id(1).name("Fluffy").birthDate(LocalDate.of(2020, 1, 15))
            .weight(15.75).type(dogType));
            
        // Pet with null weight (backward compatibility)
        PetDto petWithoutWeight = new PetDto();
        pets.add(petWithoutWeight.id(2).name("Whiskers").birthDate(LocalDate.of(2019, 6, 10))
            .weight(null).type(catType));
            
        // Pet with minimum weight
        PetDto petMinWeight = new PetDto();
        pets.add(petMinWeight.id(3).name("Tiny").birthDate(LocalDate.of(2023, 3, 1))
            .weight(0.1).type(catType));
            
        // Pet with maximum weight
        PetDto petMaxWeight = new PetDto();
        pets.add(petMaxWeight.id(4).name("Max").birthDate(LocalDate.of(2018, 8, 20))
            .weight(999.99).type(dogType));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testGetPetWithWeight() throws Exception {
        given(this.clinicService.findPetById(1)).willReturn(petMapper.toPet(pets.get(0)));
        
        this.petMockMvc.perform(get("/api/pets/1")
                .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Fluffy"))
            .andExpect(jsonPath("$.weight").value(15.75));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testGetPetWithNullWeight() throws Exception {
        given(this.clinicService.findPetById(2)).willReturn(petMapper.toPet(pets.get(1)));
        
        this.petMockMvc.perform(get("/api/pets/2")
                .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.id").value(2))
            .andExpect(jsonPath("$.name").value("Whiskers"))
            .andExpect(jsonPath("$.weight").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testGetAllPetsWithWeights() throws Exception {
        final Collection<org.springframework.samples.petclinic.model.Pet> petEntities = petMapper.toPets(this.pets);
        given(this.clinicService.findAllPets()).willReturn(petEntities);
        
        this.petMockMvc.perform(get("/api/pets")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.[0].id").value(1))
            .andExpect(jsonPath("$.[0].weight").value(15.75))
            .andExpect(jsonPath("$.[1].id").value(2))
            .andExpect(jsonPath("$.[1].weight").doesNotExist())
            .andExpect(jsonPath("$.[2].id").value(3))
            .andExpect(jsonPath("$.[2].weight").value(0.1))
            .andExpect(jsonPath("$.[3].id").value(4))
            .andExpect(jsonPath("$.[3].weight").value(999.99));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testUpdatePetWithWeight() throws Exception {
        given(this.clinicService.findPetById(1)).willReturn(petMapper.toPet(pets.get(0)));
        
        PetDto updatedPet = pets.get(0);
        updatedPet.setWeight(18.25);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String updatedPetAsJSON = mapper.writeValueAsString(updatedPet);
        
        this.petMockMvc.perform(put("/api/pets/1")
                .content(updatedPetAsJSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testUpdatePetWeightToNull() throws Exception {
        given(this.clinicService.findPetById(1)).willReturn(petMapper.toPet(pets.get(0)));
        
        PetDto updatedPet = pets.get(0);
        updatedPet.setWeight(null);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String updatedPetAsJSON = mapper.writeValueAsString(updatedPet);
        
        this.petMockMvc.perform(put("/api/pets/1")
                .content(updatedPetAsJSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testCreatePetWithWeight() throws Exception {
        PetDto newPet = new PetDto();
        newPet.setId(999);
        newPet.setName("NewPet");
        newPet.setBirthDate(LocalDate.now());
        newPet.setWeight(12.5);
        PetTypeDto petType = new PetTypeDto();
        petType.id(1).name("dog");
        newPet.setType(petType);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        String newPetAsJSON = mapper.writeValueAsString(newPet);
        
        this.ownerMockMvc.perform(post("/api/owners/1/pets")
                .content(newPetAsJSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testCreatePetWithoutWeight() throws Exception {
        PetDto newPet = new PetDto();
        newPet.setId(998);
        newPet.setName("NewPetNoWeight");
        newPet.setBirthDate(LocalDate.now());
        newPet.setWeight(null);
        PetTypeDto petType = new PetTypeDto();
        petType.id(2).name("cat");
        newPet.setType(petType);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        String newPetAsJSON = mapper.writeValueAsString(newPet);
        
        this.ownerMockMvc.perform(post("/api/owners/1/pets")
                .content(newPetAsJSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testUpdateOwnerPetWithWeight() throws Exception {
        given(this.clinicService.findOwnerById(1)).willReturn(ownerMapper.toOwner(owners.get(0)));
        given(this.clinicService.findPetById(1)).willReturn(petMapper.toPet(pets.get(0)));
        
        PetDto updatedPet = pets.get(0);
        updatedPet.setWeight(22.5);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        String updatedPetAsJSON = mapper.writeValueAsString(updatedPet);
        
        this.ownerMockMvc.perform(put("/api/owners/1/pets/1")
                .content(updatedPetAsJSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testWeightValidationMinimumBoundary() throws Exception {
        PetDto newPet = new PetDto();
        newPet.setId(997);
        newPet.setName("BoundaryTestMin");
        newPet.setBirthDate(LocalDate.now());
        newPet.setWeight(0.0); // Minimum boundary
        PetTypeDto petType = new PetTypeDto();
        petType.id(1).name("dog");
        newPet.setType(petType);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        String newPetAsJSON = mapper.writeValueAsString(newPet);
        
        this.ownerMockMvc.perform(post("/api/owners/1/pets")
                .content(newPetAsJSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testWeightValidationMaximumBoundary() throws Exception {
        PetDto newPet = new PetDto();
        newPet.setId(996);
        newPet.setName("BoundaryTestMax");
        newPet.setBirthDate(LocalDate.now());
        newPet.setWeight(999.99); // Maximum boundary
        PetTypeDto petType = new PetTypeDto();
        petType.id(1).name("dog");
        newPet.setType(petType);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        String newPetAsJSON = mapper.writeValueAsString(newPet);
        
        this.ownerMockMvc.perform(post("/api/owners/1/pets")
                .content(newPetAsJSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testWeightValidationExceedsMaximum() throws Exception {
        PetDto newPet = new PetDto();
        newPet.setId(995);
        newPet.setName("ExceedsMax");
        newPet.setBirthDate(LocalDate.now());
        newPet.setWeight(1000.0); // Exceeds maximum
        PetTypeDto petType = new PetTypeDto();
        petType.id(1).name("dog");
        newPet.setType(petType);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        String newPetAsJSON = mapper.writeValueAsString(newPet);
        
        this.ownerMockMvc.perform(post("/api/owners/1/pets")
                .content(newPetAsJSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testWeightValidationNegativeValue() throws Exception {
        PetDto newPet = new PetDto();
        newPet.setId(994);
        newPet.setName("NegativeWeight");
        newPet.setBirthDate(LocalDate.now());
        newPet.setWeight(-1.0); // Negative value
        PetTypeDto petType = new PetTypeDto();
        petType.id(1).name("dog");
        newPet.setType(petType);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        String newPetAsJSON = mapper.writeValueAsString(newPet);
        
        this.ownerMockMvc.perform(post("/api/owners/1/pets")
                .content(newPetAsJSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isBadRequest());
    }
}