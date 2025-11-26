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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.rest.dto.PetDto;
import org.springframework.samples.petclinic.rest.dto.PetFieldsDto;
import org.springframework.samples.petclinic.rest.dto.PetTypeDto;
import org.springframework.samples.petclinic.service.ClinicService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.LocalDate;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration test for Pet REST API with weight field.
 * Uses real database (HSQLDB) to verify weight persistence.
 *
 * @author Spring PetClinic Team
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"spring-data-jpa", "hsqldb"})
@TestPropertySource(properties = "petclinic.security.enable=false")
@Transactional
class PetRestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClinicService clinicService;

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Test
    void testWeightFieldEndToEnd() throws Exception {
        ObjectMapper mapper = createObjectMapper();

        // 1. Create an owner first
        Owner owner = new Owner();
        owner.setFirstName("Test");
        owner.setLastName("Owner");
        owner.setAddress("123 Test St");
        owner.setCity("Test City");
        owner.setTelephone("1234567890");
        clinicService.saveOwner(owner);

        // 2. Get a pet type
        PetType petType = clinicService.findPetTypes().stream()
            .filter(pt -> pt.getName().equals("dog"))
            .findFirst()
            .orElseThrow();

        // 3. Create a pet with weight via POST /api/owners/{ownerId}/pets
        PetFieldsDto petFieldsDto = new PetFieldsDto();
        petFieldsDto.setName("TestPet");
        petFieldsDto.setBirthDate(LocalDate.of(2020, 1, 1));
        petFieldsDto.setWeight(15.75);
        PetTypeDto petTypeDto = new PetTypeDto();
        petTypeDto.setId(petType.getId());
        petTypeDto.setName(petType.getName());
        petFieldsDto.setType(petTypeDto);
        String petJson = mapper.writeValueAsString(petFieldsDto);

        String createResponse = mockMvc.perform(post("/api/owners/" + owner.getId() + "/pets")
                .content(petJson)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.weight").value(15.75))
            .andExpect(jsonPath("$.name").value("TestPet"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        // Extract pet ID from response
        Integer petId = mapper.readTree(createResponse).get("id").asInt();

        // 4. Verify weight persists: GET /api/pets/{petId}
        mockMvc.perform(get("/api/pets/" + petId)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.weight").value(15.75))
            .andExpect(jsonPath("$.name").value("TestPet"));

        // 5. Update weight via PUT /api/pets/{petId}
        PetDto updateDto = new PetDto();
        updateDto.setId(petId);
        updateDto.setName("TestPet");
        updateDto.setBirthDate(LocalDate.of(2020, 1, 1));
        updateDto.setWeight(20.5);
        PetTypeDto updateTypeDto = new PetTypeDto();
        updateTypeDto.setId(petType.getId());
        updateTypeDto.setName(petType.getName());
        updateDto.setType(updateTypeDto);
        String updateJson = mapper.writeValueAsString(updateDto);

        mockMvc.perform(put("/api/pets/" + petId)
                .content(updateJson)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // 6. Verify updated weight persists: GET /api/pets/{petId}
        mockMvc.perform(get("/api/pets/" + petId)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.weight").value(20.5));

        // 7. Update weight to null via PUT /api/pets/{petId}
        PetDto nullWeightDto = new PetDto();
        nullWeightDto.setId(petId);
        nullWeightDto.setName("TestPet");
        nullWeightDto.setBirthDate(LocalDate.of(2020, 1, 1));
        nullWeightDto.setWeight(null);
        PetTypeDto nullTypeDto = new PetTypeDto();
        nullTypeDto.setId(petType.getId());
        nullTypeDto.setName(petType.getName());
        nullWeightDto.setType(nullTypeDto);
        String nullWeightJson = mapper.writeValueAsString(nullWeightDto);

        mockMvc.perform(put("/api/pets/" + petId)
                .content(nullWeightJson)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // 8. Verify null weight persists: GET /api/pets/{petId}
        mockMvc.perform(get("/api/pets/" + petId)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.weight").value(nullValue()));

        // 9. Verify weight in list: GET /api/pets (check that pet exists in the list)
        mockMvc.perform(get("/api/pets")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id == " + petId + ")]").exists())
            .andExpect(jsonPath("$[?(@.id == " + petId + ")].weight").value(hasItem(nullValue())));
    }

    @Test
    void testWeightFieldThroughOwnerEndpoint() throws Exception {
        ObjectMapper mapper = createObjectMapper();

        // 1. Create an owner
        Owner owner = new Owner();
        owner.setFirstName("Test");
        owner.setLastName("Owner2");
        owner.setAddress("456 Test St");
        owner.setCity("Test City");
        owner.setTelephone("0987654321");
        clinicService.saveOwner(owner);

        // 2. Get a pet type
        PetType petType = clinicService.findPetTypes().stream()
            .filter(pt -> pt.getName().equals("cat"))
            .findFirst()
            .orElseThrow();

        // 3. Create a pet with weight via POST /api/owners/{ownerId}/pets
        PetFieldsDto petFieldsDto = new PetFieldsDto();
        petFieldsDto.setName("TestCat");
        petFieldsDto.setBirthDate(LocalDate.of(2021, 5, 15));
        petFieldsDto.setWeight(8.3);
        PetTypeDto petTypeDto = new PetTypeDto();
        petTypeDto.setId(petType.getId());
        petTypeDto.setName(petType.getName());
        petFieldsDto.setType(petTypeDto);
        String petJson = mapper.writeValueAsString(petFieldsDto);

        String createResponse = mockMvc.perform(post("/api/owners/" + owner.getId() + "/pets")
                .content(petJson)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.weight").value(8.3))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Integer petId = mapper.readTree(createResponse).get("id").asInt();

        // 4. Verify weight via GET /api/pets/{petId} (direct endpoint)
        mockMvc.perform(get("/api/pets/" + petId)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.weight").value(8.3));

        // 5. Update weight via PUT /api/owners/{ownerId}/pets/{petId}
        PetFieldsDto updateDto = new PetFieldsDto();
        updateDto.setName("TestCat");
        updateDto.setBirthDate(LocalDate.of(2021, 5, 15));
        updateDto.setWeight(10.0);
        PetTypeDto updateTypeDto = new PetTypeDto();
        updateTypeDto.setId(petType.getId());
        updateTypeDto.setName(petType.getName());
        updateDto.setType(updateTypeDto);
        String updateJson = mapper.writeValueAsString(updateDto);

        mockMvc.perform(put("/api/owners/" + owner.getId() + "/pets/" + petId)
                .content(updateJson)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // 6. Verify updated weight persists via GET /api/pets/{petId}
        mockMvc.perform(get("/api/pets/" + petId)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.weight").value(10.0));
    }

}

