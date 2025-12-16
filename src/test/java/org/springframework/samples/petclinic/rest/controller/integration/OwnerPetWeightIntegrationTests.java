/*
 * Copyright 2024 the original author or authors.
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

package org.springframework.samples.petclinic.rest.controller.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.samples.petclinic.rest.dto.PetDto;
import org.springframework.samples.petclinic.rest.dto.PetFieldsDto;
import org.springframework.samples.petclinic.rest.dto.PetTypeDto;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests for Pet weight field functionality via OwnerRestController.
 * These tests use TestRestTemplate to make actual HTTP requests to a real embedded server,
 * validating JSON request/response handling through the full Spring MVC stack with a real database.
 *
 * @author Spring PetClinic Team
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"h2", "spring-data-jpa"})
@TestPropertySource(properties = {
    "spring.sql.init.schema-locations=classpath*:db/h2/schema.sql",
    "spring.sql.init.data-locations=classpath*:db/h2/data.sql",
    "petclinic.security.enable=false",
    "server.servlet.context-path="
})
class OwnerPetWeightIntegrationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;

    private Integer ownerId;
    private Integer petTypeId;
    private String petTypeName;
    private Integer petId;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api";
        // Use seeded data.sql owner/type to avoid model/repository usage
        ownerId = 1;
        petTypeId = 1;
        petTypeName = "dog";

        // Seed a baseline pet via API to reuse in tests
        PetFieldsDto petFields = new PetFieldsDto();
        petFields.setName("Buddy");
        petFields.setBirthDate(LocalDate.of(2020, 1, 15));
        PetTypeDto typeDto = new PetTypeDto();
        typeDto.setId(petTypeId);
        typeDto.setName(petTypeName);
        petFields.setType(typeDto);
        petFields.setWeight(new BigDecimal("25.75"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<PetFieldsDto> entity = new HttpEntity<>(petFields, headers);

        ResponseEntity<PetDto> response = restTemplate.exchange(
            baseUrl + "/owners/" + ownerId + "/pets",
            HttpMethod.POST,
            entity,
            PetDto.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        petId = response.getBody().getId();
        assertNotNull(petId, "Pet ID should be assigned");
    }

    @Test
    void testCreatePetWithWeight() throws Exception {
        // POST /api/owners/{ownerId}/pets - 201 CREATED with weight
        PetFieldsDto petFields = new PetFieldsDto();
        petFields.setName("Rex");
        petFields.setBirthDate(LocalDate.of(2022, 6, 1));
        PetTypeDto typeDto = new PetTypeDto();
        typeDto.setId(petTypeId);
        typeDto.setName(petTypeName);
        petFields.setType(typeDto);
        petFields.setWeight(new BigDecimal("15.50"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<PetFieldsDto> entity = new HttpEntity<>(petFields, headers);

        ResponseEntity<PetDto> response = restTemplate.exchange(
            baseUrl + "/owners/" + ownerId + "/pets",
            HttpMethod.POST,
            entity,
            PetDto.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Rex", response.getBody().getName());
        assertEquals(new BigDecimal("15.50"), response.getBody().getWeight());
    }

    @Test
    void testCreatePetWithNullWeight() throws Exception {
        // POST /api/owners/{ownerId}/pets - 201 CREATED with null weight
        PetFieldsDto petFields = new PetFieldsDto();
        petFields.setName("Luna");
        petFields.setBirthDate(LocalDate.of(2023, 1, 1));
        PetTypeDto typeDto = new PetTypeDto();
        typeDto.setId(petTypeId);
        typeDto.setName(petTypeName);
        petFields.setType(typeDto);
        petFields.setWeight(null);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<PetFieldsDto> entity = new HttpEntity<>(petFields, headers);

        ResponseEntity<PetDto> response = restTemplate.exchange(
            baseUrl + "/owners/" + ownerId + "/pets",
            HttpMethod.POST,
            entity,
            PetDto.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Luna", response.getBody().getName());
        assertNull(response.getBody().getWeight());
    }

    @Test
    void testUpdateOwnersPetWithWeight() throws Exception {
        // PUT /api/owners/{ownerId}/pets/{petId} - 204 NO_CONTENT with weight
        PetFieldsDto petFields = new PetFieldsDto();
        petFields.setName("Buddy");
        petFields.setBirthDate(LocalDate.of(2020, 1, 15));
        PetTypeDto typeDto = new PetTypeDto();
        typeDto.setId(petTypeId);
        typeDto.setName(petTypeName);
        petFields.setType(typeDto);
        petFields.setWeight(new BigDecimal("27.00"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<PetFieldsDto> entity = new HttpEntity<>(petFields, headers);

        ResponseEntity<Void> response = restTemplate.exchange(
            baseUrl + "/owners/" + ownerId + "/pets/" + petId,
            HttpMethod.PUT,
            entity,
            Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        // Verify weight was updated
        HttpHeaders getHeaders = new HttpHeaders();
        getHeaders.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<String> getEntity = new HttpEntity<>(getHeaders);

        ResponseEntity<PetDto> getResponse = restTemplate.exchange(
            baseUrl + "/pets/" + petId,
            HttpMethod.GET,
            getEntity,
            PetDto.class
        );

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertNotNull(getResponse.getBody());
        assertEquals(new BigDecimal("27.00"), getResponse.getBody().getWeight());
    }

    @Test
    void testUpdateOwnersPetWithNullWeight() throws Exception {
        // PUT /api/owners/{ownerId}/pets/{petId} - 204 NO_CONTENT with null weight
        PetFieldsDto petFields = new PetFieldsDto();
        petFields.setName("Buddy");
        petFields.setBirthDate(LocalDate.of(2020, 1, 15));
        PetTypeDto typeDto = new PetTypeDto();
        typeDto.setId(petTypeId);
        typeDto.setName(petTypeName);
        petFields.setType(typeDto);
        petFields.setWeight(null);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<PetFieldsDto> entity = new HttpEntity<>(petFields, headers);

        ResponseEntity<Void> response = restTemplate.exchange(
            baseUrl + "/owners/" + ownerId + "/pets/" + petId,
            HttpMethod.PUT,
            entity,
            Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        // Verify weight was set to null
        HttpHeaders getHeaders = new HttpHeaders();
        getHeaders.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<String> getEntity = new HttpEntity<>(getHeaders);

        ResponseEntity<PetDto> getResponse = restTemplate.exchange(
            baseUrl + "/pets/" + petId,
            HttpMethod.GET,
            getEntity,
            PetDto.class
        );

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertNotNull(getResponse.getBody());
        assertNull(getResponse.getBody().getWeight());
    }

    @Test
    void testGetOwnersPetWithWeight() throws Exception {
        // GET /api/owners/{ownerId}/pets/{petId} - 200 OK with weight
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<PetDto> response = restTemplate.exchange(
            baseUrl + "/owners/" + ownerId + "/pets/" + petId,
            HttpMethod.GET,
            entity,
            PetDto.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(petId, response.getBody().getId());
        assertEquals("Buddy", response.getBody().getName());
        assertEquals(new BigDecimal("25.75"), response.getBody().getWeight());
    }

    @Test
    void testWeightPersistence() throws Exception {
        // POST /api/owners/{ownerId}/pets - Test that weight value set via POST can be retrieved via GET
        PetFieldsDto petFields = new PetFieldsDto();
        petFields.setName("Charlie");
        petFields.setBirthDate(LocalDate.of(2021, 3, 15));
        PetTypeDto typeDto = new PetTypeDto();
        typeDto.setId(petTypeId);
        typeDto.setName(petTypeName);
        petFields.setType(typeDto);
        petFields.setWeight(new BigDecimal("22.75"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<PetFieldsDto> entity = new HttpEntity<>(petFields, headers);

        ResponseEntity<PetDto> createResponse = restTemplate.exchange(
            baseUrl + "/owners/" + ownerId + "/pets",
            HttpMethod.POST,
            entity,
            PetDto.class
        );

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        Integer createdPetId = createResponse.getBody().getId();

        // Retrieve the pet and verify weight persists
        HttpHeaders getHeaders = new HttpHeaders();
        getHeaders.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<String> getEntity = new HttpEntity<>(getHeaders);

        ResponseEntity<PetDto> getResponse = restTemplate.exchange(
            baseUrl + "/pets/" + createdPetId,
            HttpMethod.GET,
            getEntity,
            PetDto.class
        );

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertNotNull(getResponse.getBody());
        assertEquals(createdPetId, getResponse.getBody().getId());
        assertEquals("Charlie", getResponse.getBody().getName());
        assertEquals(new BigDecimal("22.75"), getResponse.getBody().getWeight());
    }

    @Test
    void testCreatePetWithInvalidJson() {
        // POST /api/owners/{ownerId}/pets - 400/500 when JSON is malformed (500 is acceptable for parsing errors)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        String invalidJson = "{ \"name\": \"Test\", \"birthDate\": \"2022-06-01\", \"weight\": \"invalid\" }";
        HttpEntity<String> entity = new HttpEntity<>(invalidJson, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/owners/" + ownerId + "/pets",
            HttpMethod.POST,
            entity,
            String.class
        );

        // Invalid JSON can result in either 400 or 500 depending on when parsing fails
        assertTrue(response.getStatusCode() == HttpStatus.BAD_REQUEST || 
                   response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

