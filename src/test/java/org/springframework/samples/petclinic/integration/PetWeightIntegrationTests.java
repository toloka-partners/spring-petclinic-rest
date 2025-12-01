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

package org.springframework.samples.petclinic.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.samples.petclinic.PetClinicApplication;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.rest.dto.PetDto;
import org.springframework.samples.petclinic.rest.dto.PetTypeDto;
import org.springframework.samples.petclinic.service.ClinicService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = PetClinicApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "petclinic.security.enable=false"
})
@Transactional
class PetWeightIntegrationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ClinicService clinicService;

    private ObjectMapper objectMapper;
    private HttpHeaders headers;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/petclinic/api";

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Set up headers without authentication since security is disabled
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void testCompleteWeightWorkflowThroughRestApi() throws Exception {
        // 1. Create a new pet with weight through REST API
        PetDto newPet = createTestPetDto();
        newPet.setWeight(22.25);

        HttpEntity<PetDto> createRequest = new HttpEntity<>(newPet, headers);

        // Create pet via REST API
        ResponseEntity<PetDto> createResponse = restTemplate.exchange(
            baseUrl + "/owners/1/pets",
            HttpMethod.POST,
            createRequest,
            PetDto.class
        );

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        PetDto createdPet = createResponse.getBody();
        assertNotNull(createdPet);
        Integer petId = createdPet.getId();
        assertNotNull(petId);

        // 2. Verify pet was created with correct weight via REST API
        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<String> getResponse = restTemplate.exchange(
            baseUrl + "/pets/" + petId,
            HttpMethod.GET,
            getRequest,
            String.class
        );

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        JsonNode petJson = objectMapper.readTree(getResponse.getBody());
        assertEquals(22.25, petJson.get("weight").asDouble());
        assertEquals("TestPet", petJson.get("name").asText());
        assertEquals("dog", petJson.get("type").get("name").asText());

        // 3. Update pet weight via REST API
        createdPet.setWeight(35.75);
        HttpEntity<PetDto> updateRequest = new HttpEntity<>(createdPet, headers);

        ResponseEntity<Void> updateResponse = restTemplate.exchange(
            baseUrl + "/pets/" + petId,
            HttpMethod.PUT,
            updateRequest,
            Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, updateResponse.getStatusCode());

        // 4. Verify weight was updated via REST API
        ResponseEntity<String> getUpdatedResponse = restTemplate.exchange(
            baseUrl + "/pets/" + petId,
            HttpMethod.GET,
            getRequest,
            String.class
        );

        assertEquals(HttpStatus.OK, getUpdatedResponse.getStatusCode());
        JsonNode updatedPetJson = objectMapper.readTree(getUpdatedResponse.getBody());
        assertEquals(35.75, updatedPetJson.get("weight").asDouble());

        // 5. Set weight to null via REST API
        createdPet.setWeight(null);
        HttpEntity<PetDto> nullWeightRequest = new HttpEntity<>(createdPet, headers);

        ResponseEntity<Void> nullWeightResponse = restTemplate.exchange(
            baseUrl + "/pets/" + petId,
            HttpMethod.PUT,
            nullWeightRequest,
            Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, nullWeightResponse.getStatusCode());

        // 6. Verify weight is null via REST API
        ResponseEntity<String> getNullWeightResponse = restTemplate.exchange(
            baseUrl + "/pets/" + petId,
            HttpMethod.GET,
            getRequest,
            String.class
        );

        assertEquals(HttpStatus.OK, getNullWeightResponse.getStatusCode());
        JsonNode nullWeightPetJson = objectMapper.readTree(getNullWeightResponse.getBody());
        assertTrue(nullWeightPetJson.get("weight").isNull());

        // 7. Verify data persistence through service layer
        Pet persistedPet = clinicService.findPetById(petId);
        assertNotNull(persistedPet);
        assertNull(persistedPet.getWeight());
        assertEquals("TestPet", persistedPet.getName());
    }

    @Test
    void testWeightValidationThroughRestApi() throws Exception {
        PetDto newPet = createTestPetDto();

        // Test invalid negative weight
        newPet.setWeight(-3.00);
        HttpEntity<PetDto> invalidRequest = new HttpEntity<>(newPet, headers);

        ResponseEntity<String> invalidResponse = restTemplate.exchange(
            baseUrl + "/owners/1/pets",
            HttpMethod.POST,
            invalidRequest,
            String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, invalidResponse.getStatusCode());

        // Test weight too large
        newPet.setWeight(1000.00);
        HttpEntity<PetDto> largeRequest = new HttpEntity<>(newPet, headers);

        ResponseEntity<String> largeResponse = restTemplate.exchange(
            baseUrl + "/owners/1/pets",
            HttpMethod.POST,
            largeRequest,
            String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, largeResponse.getStatusCode());

        // Test valid weight
        newPet.setWeight(12.55);
        HttpEntity<PetDto> validRequest = new HttpEntity<>(newPet, headers);

        ResponseEntity<PetDto> validResponse = restTemplate.exchange(
            baseUrl + "/owners/1/pets",
            HttpMethod.POST,
            validRequest,
            PetDto.class
        );

        assertEquals(HttpStatus.CREATED, validResponse.getStatusCode());
        assertNotNull(validResponse.getBody());
    }

    @Test
    void testWeightInOwnerPetOperations() throws Exception {
        // Test creating pet through owner endpoint with weight
        PetDto newPet = createTestPetDto();
        newPet.setWeight(16.25);

        HttpEntity<PetDto> createRequest = new HttpEntity<>(newPet, headers);

        ResponseEntity<PetDto> createResponse = restTemplate.exchange(
            baseUrl + "/owners/1/pets",
            HttpMethod.POST,
            createRequest,
            PetDto.class
        );

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        PetDto createdPet = createResponse.getBody();
        assertNotNull(createdPet);
        Integer petId = createdPet.getId();

        // Test getting pet through owner endpoint includes weight
        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<String> getResponse = restTemplate.exchange(
            baseUrl + "/owners/1/pets/" + petId,
            HttpMethod.GET,
            getRequest,
            String.class
        );

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        JsonNode petJson = objectMapper.readTree(getResponse.getBody());
        assertEquals(16.25, petJson.get("weight").asDouble());

        // Test updating pet through owner endpoint with new weight
        createdPet.setWeight(26.00);
        HttpEntity<PetDto> updateRequest = new HttpEntity<>(createdPet, headers);

        ResponseEntity<Void> updateResponse = restTemplate.exchange(
            baseUrl + "/owners/1/pets/" + petId,
            HttpMethod.PUT,
            updateRequest,
            Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, updateResponse.getStatusCode());

        // Verify weight was updated
        ResponseEntity<String> getUpdatedResponse = restTemplate.exchange(
            baseUrl + "/owners/1/pets/" + petId,
            HttpMethod.GET,
            getRequest,
            String.class
        );

        assertEquals(HttpStatus.OK, getUpdatedResponse.getStatusCode());
        JsonNode updatedPetJson = objectMapper.readTree(getUpdatedResponse.getBody());
        assertEquals(26.0, updatedPetJson.get("weight").asDouble());
    }

    @Test
    void testWeightInAllPetsEndpoint() throws Exception {
        // Get all pets and verify weight field is included
        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/pets",
            HttpMethod.GET,
            getRequest,
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode petsArray = objectMapper.readTree(response.getBody());
        assertTrue(petsArray.isArray());
        assertTrue(petsArray.size() > 0);

        // Check that weight field exists in response (may be null or have value)
        JsonNode firstPet = petsArray.get(0);
        assertTrue(firstPet.has("weight"));
    }

    @Test
    void testSpecificEndpointWeightHandling() throws Exception {
        // Create a test pet first
        PetDto newPet = createTestPetDto();
        newPet.setWeight(11.33);

        HttpEntity<PetDto> createRequest = new HttpEntity<>(newPet, headers);
        ResponseEntity<PetDto> createResponse = restTemplate.exchange(
            baseUrl + "/owners/1/pets",
            HttpMethod.POST,
            createRequest,
            PetDto.class
        );

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        PetDto createdPet = createResponse.getBody();
        assertNotNull(createdPet);
        Integer petId = createdPet.getId();

        // 1. Test GET /api/pets/{petId} - return weight as decimal number in JSON response
        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<String> getPetResponse = restTemplate.exchange(
            baseUrl + "/pets/" + petId,
            HttpMethod.GET,
            getRequest,
            String.class
        );

        assertEquals(HttpStatus.OK, getPetResponse.getStatusCode());
        JsonNode petJson = objectMapper.readTree(getPetResponse.getBody());
        assertTrue(petJson.has("weight"));
        assertEquals(11.33, petJson.get("weight").asDouble());

        // 2. Test PUT /api/pets/{petId} - accept weight as decimal number in JSON request body
        createdPet.setWeight(46.68);
        HttpEntity<PetDto> updatePetRequest = new HttpEntity<>(createdPet, headers);
        ResponseEntity<Void> updatePetResponse = restTemplate.exchange(
            baseUrl + "/pets/" + petId,
            HttpMethod.PUT,
            updatePetRequest,
            Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, updatePetResponse.getStatusCode());

        // Verify the update worked
        ResponseEntity<String> getUpdatedPetResponse = restTemplate.exchange(
            baseUrl + "/pets/" + petId,
            HttpMethod.GET,
            getRequest,
            String.class
        );
        JsonNode updatedPetJson = objectMapper.readTree(getUpdatedPetResponse.getBody());
        assertEquals(46.68, updatedPetJson.get("weight").asDouble());

        // 3. Test GET /api/pets - return weight for all pets in response array
        ResponseEntity<String> getAllPetsResponse = restTemplate.exchange(
            baseUrl + "/pets",
            HttpMethod.GET,
            getRequest,
            String.class
        );

        assertEquals(HttpStatus.OK, getAllPetsResponse.getStatusCode());
        JsonNode petsArray = objectMapper.readTree(getAllPetsResponse.getBody());
        assertTrue(petsArray.isArray());
        assertTrue(petsArray.size() > 0);

        // Find our created pet in the array and verify weight
        boolean foundOurPet = false;
        for (JsonNode pet : petsArray) {
            if (pet.get("id").asInt() == petId) {
                assertTrue(pet.has("weight"));
                assertEquals(46.68, pet.get("weight").asDouble());
                foundOurPet = true;
                break;
            }
        }
        assertTrue(foundOurPet, "Created pet should be found in GET /api/pets response");

        // 4. Test POST /api/owners/{ownerId}/pets - accept weight when creating pets
        PetDto anotherPet = createTestPetDto();
        anotherPet.setName("AnotherTestPet");
        anotherPet.setWeight(99.99);

        HttpEntity<PetDto> createAnotherRequest = new HttpEntity<>(anotherPet, headers);
        ResponseEntity<PetDto> createAnotherResponse = restTemplate.exchange(
            baseUrl + "/owners/1/pets",
            HttpMethod.POST,
            createAnotherRequest,
            PetDto.class
        );

        assertEquals(HttpStatus.CREATED, createAnotherResponse.getStatusCode());
        PetDto createdAnotherPet = createAnotherResponse.getBody();
        assertNotNull(createdAnotherPet);
        assertEquals(99.99, createdAnotherPet.getWeight());

        // 5. Test PUT /api/owners/{ownerId}/pets/{petId} - accept weight in updates
        Integer anotherPetId = createdAnotherPet.getId();
        createdAnotherPet.setWeight(88.88);

        HttpEntity<PetDto> updateOwnerPetRequest = new HttpEntity<>(createdAnotherPet, headers);
        ResponseEntity<Void> updateOwnerPetResponse = restTemplate.exchange(
            baseUrl + "/owners/1/pets/" + anotherPetId,
            HttpMethod.PUT,
            updateOwnerPetRequest,
            Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, updateOwnerPetResponse.getStatusCode());

        // Verify the owner pet update worked
        ResponseEntity<String> getUpdatedOwnerPetResponse = restTemplate.exchange(
            baseUrl + "/owners/1/pets/" + anotherPetId,
            HttpMethod.GET,
            getRequest,
            String.class
        );
        JsonNode updatedOwnerPetJson = objectMapper.readTree(getUpdatedOwnerPetResponse.getBody());
        assertEquals(88.88, updatedOwnerPetJson.get("weight").asDouble());
    }

    private PetDto createTestPetDto() {
        PetTypeDto petType = new PetTypeDto();
        petType.setId(2);
        petType.setName("dog");

        PetDto pet = new PetDto();
        pet.setName("TestPet");
        pet.setBirthDate(LocalDate.now().minusYears(1));
        pet.setType(petType);

        return pet;
    }
}
