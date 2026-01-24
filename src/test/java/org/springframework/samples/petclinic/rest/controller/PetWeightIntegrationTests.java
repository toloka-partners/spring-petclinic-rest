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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Pet weight field functionality across all REST endpoints.
 * Tests verify that weight field is properly handled in JSON request/response payloads.
 *
 * @author Nwatu Ernest I.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"spring-data-jpa", "hsqldb"})
@Transactional
class PetWeightIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testGetPetByIdWithWeight() throws Exception {
        String createJson = """
            {
                "name": "Weere",
                "birthDate": "2022-01-15",
                "weight": 12.75,
                "type": {"id": 1, "name": "cat"}
            }
            """;

        String response = mockMvc.perform(post("/api/owners/1/pets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        int petId = objectMapper.readTree(response).get("id").asInt();

        mockMvc.perform(get("/api/pets/" + petId)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.weight").value(12.75));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testGetPetByIdWithoutWeight() throws Exception {
        String createJson = """
            {
                "name": "Bolle",
                "birthDate": "2023-01-15",
                "weight": null,
                "type": {"id": 1, "name": "cat"}
            }
            """;

        String response = mockMvc.perform(post("/api/owners/1/pets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        int petId = objectMapper.readTree(response).get("id").asInt();

        mockMvc.perform(get("/api/pets/" + petId)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.weight").isEmpty());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testGetAllPetsWithWeight() throws Exception {
        mockMvc.perform(get("/api/pets")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testUpdatePetWithWeight() throws Exception {
        String updateJson = """
            {
                "id": 1,
                "name": "Venna",
                "birthDate": "2020-01-15",
                "weight": 18.95,
                "type": {"id": 1, "name": "cat"},
                "ownerId": 1
            }
            """;

        mockMvc.perform(put("/api/pets/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/pets/1")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.weight").value(18.95));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testUpdatePetWithNullWeight() throws Exception {
        String updateJson = """
            {
                "id": 1,
                "name": "Biggie",
                "birthDate": "2019-05-20",
                "weight": null,
                "type": {"id": 1, "name": "cat"},
                "ownerId": 1
            }
            """;

        mockMvc.perform(put("/api/pets/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/pets/1")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.weight").isEmpty());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testCreatePetWithWeight() throws Exception {
        String json = """
            {
                "name": "Darey",
                "birthDate": "2023-01-01",
                "weight": 4.25,
                "type": {"id": 1, "name": "cat"}
            }
            """;

        mockMvc.perform(post("/api/owners/1/pets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.weight").value(4.25));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testCreatePetWithNullWeight() throws Exception {
        String json = """
            {
                "name": "Miles",
                "birthDate": "2023-02-01",
                "weight": null,
                "type": {"id": 1, "name": "cat"}
            }
            """;

        mockMvc.perform(post("/api/owners/1/pets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.weight").isEmpty());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testCreatePetWithoutWeightField() throws Exception {
        String json = """
            {
                "name": "Samil",
                "birthDate": "2023-03-01",
                "type": {"id": 1, "name": "cat"}
            }
            """;

        mockMvc.perform(post("/api/owners/1/pets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.weight").isEmpty());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testUpdatePetViaOwnerEndpointWithWeight() throws Exception {
        String json = """
            {
                "name": "Rex",
                "birthDate": "2018-06-15",
                "weight": 22.5,
                "type": {"id": 1, "name": "cat"}
            }
            """;

        mockMvc.perform(put("/api/owners/1/pets/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/owners/1/pets/1")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.weight").value(22.5));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testGetPetViaOwnerEndpointWithWeight() throws Exception {
        String createJson = """
            {
                "name": "Fluffy",
                "birthDate": "2022-01-01",
                "weight": 12.75,
                "type": {"id": 1, "name": "cat"}
            }
            """;

        String response = mockMvc.perform(post("/api/owners/1/pets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        int petId = objectMapper.readTree(response).get("id").asInt();

        mockMvc.perform(get("/api/owners/1/pets/" + petId)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.weight").value(12.75));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testWeightZero() throws Exception {
        String json = """
            {
                "name": "Slim",
                "birthDate": "2023-01-01",
                "weight": 0.0,
                "type": {"id": 1, "name": "cat"}
            }
            """;

        mockMvc.perform(post("/api/owners/1/pets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.weight").value(0.0));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testWeightDecimalPrecision() throws Exception {
        String json = """
            {
                "name": "Zare",
                "birthDate": "2023-01-01",
                "weight": 12.34567,
                "type": {"id": 1, "name": "cat"}
            }
            """;

        mockMvc.perform(post("/api/owners/1/pets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.weight").exists())
            .andExpect(jsonPath("$.weight").isNumber());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testWeightFieldDoesNotAffectOtherFields() throws Exception {
        String json = """
            {
                "name": "Filone",
                "birthDate": "2020-01-15",
                "weight": 25.5,
                "type": {"id": 1, "name": "cat"}
            }
            """;

        mockMvc.perform(post("/api/owners/1/pets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Filone"))
            .andExpect(jsonPath("$.birthDate").exists())
            .andExpect(jsonPath("$.type.id").value(1))
            .andExpect(jsonPath("$.type.name").value("cat"))
            .andExpect(jsonPath("$.weight").value(25.5));
    }
}
