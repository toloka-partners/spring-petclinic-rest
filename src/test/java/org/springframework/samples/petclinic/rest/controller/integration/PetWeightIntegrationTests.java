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
 * End-to-end integration tests for Pet weight field functionality via PetRestController.
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
class PetWeightIntegrationTests {

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

        // Create a baseline pet via API to obtain petId for subsequent tests
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
    void testGetPetWithWeight() {
        // GET /api/pets/{petId} - 200 OK with weight
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<PetDto> response = restTemplate.exchange(
            baseUrl + "/pets/" + petId,
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
    void testGetPetWithNullWeight() throws Exception {
        // Create a pet with null weight via API, then GET
        PetFieldsDto petFields = new PetFieldsDto();
        petFields.setName("Fluffy");
        petFields.setBirthDate(LocalDate.of(2021, 5, 10));
        PetTypeDto typeDto = new PetTypeDto();
        typeDto.setId(petTypeId);
        typeDto.setName(petTypeName);
        petFields.setType(typeDto);
        petFields.setWeight(null);

        HttpHeaders createHeaders = new HttpHeaders();
        createHeaders.setContentType(MediaType.APPLICATION_JSON);
        createHeaders.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<PetFieldsDto> createEntity = new HttpEntity<>(petFields, createHeaders);

        ResponseEntity<PetDto> createResponse = restTemplate.exchange(
            baseUrl + "/owners/" + ownerId + "/pets",
            HttpMethod.POST,
            createEntity,
            PetDto.class
        );

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        Integer createdPetId = createResponse.getBody().getId();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<PetDto> response = restTemplate.exchange(
            baseUrl + "/pets/" + createdPetId,
            HttpMethod.GET,
            entity,
            PetDto.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(createdPetId, response.getBody().getId());
        assertEquals("Fluffy", response.getBody().getName());
        assertNull(response.getBody().getWeight());
    }

    @Test
    void testGetAllPetsWithWeight() throws Exception {
        // Create an additional pet via API, then GET all
        PetFieldsDto petFields = new PetFieldsDto();
        petFields.setName("Max");
        petFields.setBirthDate(LocalDate.of(2019, 3, 20));
        PetTypeDto typeDto = new PetTypeDto();
        typeDto.setId(petTypeId);
        typeDto.setName(petTypeName);
        petFields.setType(typeDto);
        petFields.setWeight(new BigDecimal("30.50"));

        HttpHeaders createHeaders = new HttpHeaders();
        createHeaders.setContentType(MediaType.APPLICATION_JSON);
        createHeaders.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<PetFieldsDto> createEntity = new HttpEntity<>(petFields, createHeaders);

        ResponseEntity<PetDto> createResponse = restTemplate.exchange(
            baseUrl + "/owners/" + ownerId + "/pets",
            HttpMethod.POST,
            createEntity,
            PetDto.class
        );

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        Integer pet2Id = createResponse.getBody().getId();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<PetDto[]> response = restTemplate.exchange(
            baseUrl + "/pets",
            HttpMethod.GET,
            entity,
            PetDto[].class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        PetDto pet1 = java.util.Arrays.stream(response.getBody())
            .filter(p -> p.getId().equals(petId))
            .findFirst()
            .orElse(null);
        assertNotNull(pet1);
        assertEquals(new BigDecimal("25.75"), pet1.getWeight());
        
        PetDto pet2Dto = java.util.Arrays.stream(response.getBody())
            .filter(p -> p.getId().equals(pet2Id))
            .findFirst()
            .orElse(null);
        assertNotNull(pet2Dto);
        assertEquals(new BigDecimal("30.50"), pet2Dto.getWeight());
    }

    @Test
    void testUpdatePetWithWeight() throws Exception {
        // PUT /api/pets/{petId} - 204 NO_CONTENT with weight
        PetDto petUpdate = new PetDto();
        petUpdate.setId(petId);
        petUpdate.setName("Buddy Updated");
        petUpdate.setBirthDate(LocalDate.of(2020, 1, 15));
        PetTypeDto typeDto = new PetTypeDto();
        typeDto.setId(petTypeId);
        typeDto.setName(petTypeName);
        petUpdate.setType(typeDto);
        petUpdate.setWeight(new BigDecimal("28.25"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<PetDto> entity = new HttpEntity<>(petUpdate, headers);

        ResponseEntity<Void> response = restTemplate.exchange(
            baseUrl + "/pets/" + petId,
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
        assertEquals(new BigDecimal("28.25"), getResponse.getBody().getWeight());
        assertEquals("Buddy Updated", getResponse.getBody().getName());
    }

    @Test
    void testUpdatePetWithNullWeight() throws Exception {
        // PUT /api/pets/{petId} - 204 NO_CONTENT with null weight
        PetDto petUpdate = new PetDto();
        petUpdate.setId(petId);
        petUpdate.setName("Buddy");
        petUpdate.setBirthDate(LocalDate.of(2020, 1, 15));
        PetTypeDto typeDto = new PetTypeDto();
        typeDto.setId(petTypeId);
        typeDto.setName(petTypeName);
        petUpdate.setType(typeDto);
        petUpdate.setWeight(null);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<PetDto> entity = new HttpEntity<>(petUpdate, headers);

        ResponseEntity<Void> response = restTemplate.exchange(
            baseUrl + "/pets/" + petId,
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

}