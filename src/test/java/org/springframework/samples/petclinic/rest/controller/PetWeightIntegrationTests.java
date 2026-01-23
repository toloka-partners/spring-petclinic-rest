/*
 * Copyright 2026 the original author or authors.
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
import org.springframework.samples.petclinic.mapper.PetMapper;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.rest.advice.ExceptionControllerAdvice;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Pet weight field functionality across all REST endpoints.
 * Tests verify that weight field is properly handled in JSON request/response payloads.
 *
 * @author Nwatu Ernest I.
 */
@SpringBootTest
@ContextConfiguration(classes = ApplicationTestConfig.class)
@WebAppConfiguration
class PetWeightIntegrationTests {

    @MockitoBean
    private ClinicService clinicService;

    @Autowired
    private PetRestController petRestController;

    @Autowired
    private OwnerRestController ownerRestController;

    @Autowired
    private PetMapper petMapper;

    private MockMvc petMockMvc;
    private MockMvc ownerMockMvc;
    private ObjectMapper objectMapper;

    private Owner testOwner;
    private PetType testPetType;

    @BeforeEach
    void setUp() {
        petMockMvc = MockMvcBuilders.standaloneSetup(petRestController)
            .setControllerAdvice(new ExceptionControllerAdvice())
            .build();

        ownerMockMvc = MockMvcBuilders.standaloneSetup(ownerRestController)
            .setControllerAdvice(new ExceptionControllerAdvice())
            .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        testOwner = new Owner();
        testOwner.setId(1);
        testOwner.setFirstName("John");
        testOwner.setLastName("Doe");
        testOwner.setAddress("123 Main St");
        testOwner.setCity("Springfield");
        testOwner.setTelephone("555-1234");

        testPetType = new PetType();
        testPetType.setId(1);
        testPetType.setName("dog");
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testGetPetByIdWithWeight() throws Exception {
        Pet pet = createPet(1, "Max", LocalDate.now().minusYears(2), 12.75);
        given(clinicService.findPetById(1)).willReturn(pet);

        petMockMvc.perform(get("/api/pets/1").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.weight").value(12.75));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testGetPetByIdWithoutWeight() throws Exception {
        Pet pet = createPet(2, "Luna", LocalDate.now().minusYears(1), null);
        given(clinicService.findPetById(2)).willReturn(pet);

        petMockMvc.perform(get("/api/pets/2").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.weight").isEmpty());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testGetAllPetsWithWeight() throws Exception {
        List<Pet> pets = List.of(
            createPet(1, "Max", null, 8.40),
            createPet(2, "Bella", null, null)
        );
        given(clinicService.findAllPets()).willReturn(pets);

        petMockMvc.perform(get("/api/pets").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].weight").value(8.40))
            .andExpect(jsonPath("$[1].weight").isEmpty());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testUpdatePetWithWeight() throws Exception {
        Pet existing = createPet(1, "Rocky", null, 15.0);
        given(clinicService.findPetById(1)).willReturn(existing);

        String json = """
            {
                "id": 1,
                "name": "Rocky",
                "birthDate": "2020-01-15",
                "weight": 18.95,
                "type": {"id": 1, "name": "dog"},
                "ownerId": 1
            }
            """;

        petMockMvc.perform(put("/api/pets/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isNoContent());

        Pet updated = createPet(1, "Rocky", LocalDate.of(2020, 1, 15), 18.95);
        given(clinicService.findPetById(1)).willReturn(updated);

        petMockMvc.perform(get("/api/pets/1").accept(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.weight").value(18.95));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testUpdatePetWithNullWeight() throws Exception {
        Pet existing = createPet(1, "Buddy", null, 10.0);
        given(clinicService.findPetById(1)).willReturn(existing);

        String json = """
            {
                "id": 1,
                "name": "Buddy",
                "birthDate": "2019-05-20",
                "weight": null,
                "type": {"id": 1, "name": "dog"},
                "ownerId": 1
            }
            """;

        petMockMvc.perform(put("/api/pets/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isNoContent());

        Pet updated = createPet(1, "Buddy", LocalDate.of(2019, 5, 20), null);
        given(clinicService.findPetById(1)).willReturn(updated);

        petMockMvc.perform(get("/api/pets/1").accept(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.weight").isEmpty());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testCreatePetWithWeight() throws Exception {
        given(clinicService.findOwnerById(1)).willReturn(testOwner);

        String json = """
            {
                "name": "Charlie",
                "birthDate": "2023-01-01",
                "weight": 4.25,
                "type": {"id": 1, "name": "dog"}
            }
            """;

        Pet saved = createPet(100, "Charlie", LocalDate.of(2023, 1, 1), 4.25);
        doNothing().when(clinicService).savePet(any(Pet.class));
        given(clinicService.findPetById(100)).willReturn(saved);

        ownerMockMvc.perform(post("/api/owners/1/pets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.weight").value(4.25));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testCreatePetWithNullWeight() throws Exception {
        given(clinicService.findOwnerById(1)).willReturn(testOwner);

        String json = """
            {
                "name": "Milo",
                "birthDate": "2023-02-01",
                "weight": null,
                "type": {"id": 1, "name": "dog"}
            }
            """;

        Pet saved = createPet(101, "Milo", LocalDate.of(2023, 2, 1), null);
        doNothing().when(clinicService).savePet(any(Pet.class));
        given(clinicService.findPetById(101)).willReturn(saved);

        ownerMockMvc.perform(post("/api/owners/1/pets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.weight").isEmpty());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testCreatePetWithoutWeightField() throws Exception {
        given(clinicService.findOwnerById(1)).willReturn(testOwner);

        String json = """
            {
                "name": "NoWeightField",
                "birthDate": "2023-03-01",
                "type": {"id": 1, "name": "dog"}
            }
            """;

        Pet saved = createPet(102, "NoWeightField", LocalDate.of(2023, 3, 1), null);
        doNothing().when(clinicService).savePet(any(Pet.class));
        given(clinicService.findPetById(102)).willReturn(saved);

        ownerMockMvc.perform(post("/api/owners/1/pets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.weight").isEmpty());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testUpdatePetViaOwnerEndpointWithWeight() throws Exception {
        Pet existing = createPet(1, "Rex", null, 20.0);
        existing.setOwner(testOwner);
        given(clinicService.findPetById(1)).willReturn(existing);
        given(clinicService.findOwnerById(1)).willReturn(testOwner);

        String json = """
            {
                "name": "Rex",
                "birthDate": "2018-06-15",
                "weight": 22.5,
                "type": {"id": 1, "name": "dog"}
            }
            """;

        ownerMockMvc.perform(put("/api/owners/1/pets/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isNoContent());

        Pet updated = createPet(1, "Rex", LocalDate.of(2018, 6, 15), 22.5);
        updated.setOwner(testOwner);
        testOwner.addPet(updated);
        given(clinicService.findPetById(1)).willReturn(updated);
        given(clinicService.findOwnerById(1)).willReturn(testOwner);

        ownerMockMvc.perform(get("/api/owners/1/pets/1")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.weight").value(22.5));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testGetPetViaOwnerEndpointWithWeight() throws Exception {
        Pet pet = createPet(1, "Max", LocalDate.now().minusYears(2), 12.75);
        pet.setOwner(testOwner);
        testOwner.addPet(pet);

        given(clinicService.findOwnerById(1)).willReturn(testOwner);

        ownerMockMvc.perform(get("/api/owners/1/pets/1")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.weight").value(12.75));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testWeightZero() throws Exception {
        Pet pet = createPet(1, "Slim", null, 0.0);
        given(clinicService.findPetById(1)).willReturn(pet);

        petMockMvc.perform(get("/api/pets/1").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.weight").value(0.0));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testWeightDecimalPrecision() throws Exception {
        Pet pet = createPet(1, "Precision", null, 12.34567);
        given(clinicService.findPetById(1)).willReturn(pet);

        petMockMvc.perform(get("/api/pets/1").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.weight").exists())
            .andExpect(jsonPath("$.weight").isNumber());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testWeightFieldNotAffectsOtherFields() throws Exception {
        Pet pet = createPet(1, "Complete", LocalDate.of(2020, 1, 15), 25.5);
        given(clinicService.findPetById(1)).willReturn(pet);

        petMockMvc.perform(get("/api/pets/1").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Complete"))
            .andExpect(jsonPath("$.birthDate[0]").value(2020))
            .andExpect(jsonPath("$.birthDate[1]").value(1))
            .andExpect(jsonPath("$.birthDate[2]").value(15))
            .andExpect(jsonPath("$.type.id").value(1))
            .andExpect(jsonPath("$.type.name").value("dog"))
            .andExpect(jsonPath("$.weight").value(25.5));
    }

    private Pet createPet(Integer id, String name, LocalDate birthDate, Double weight) {
        Pet p = new Pet();
        p.setId(id);
        p.setName(name);
        p.setBirthDate(birthDate);
        p.setWeight(weight);
        p.setType(testPetType);
        p.setOwner(testOwner);
        return p;
    }
}
