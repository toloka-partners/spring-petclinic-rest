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

    private static final String API_BASE_PATH = "/petclinic/api";
    private static final String PETS_ENDPOINT = "/pets";
    private static final String OWNERS_ENDPOINT = "/owners";
    private static final int DEFAULT_OWNER_ID = 1;
    private static final int DOG_TYPE_ID = 2;
    private static final String DOG_TYPE_NAME = "dog";
    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd";

    @LocalServerPort
    private int serverPort;

    @Autowired
    private TestRestTemplate httpClient;

    @Autowired
    private ClinicService petClinicService;

    private ObjectMapper jsonMapper;
    private HttpHeaders requestHeaders;
    private String apiBaseUrl;

    @BeforeEach
    void initializeTestEnvironment() {
        apiBaseUrl = String.format("http://localhost:%d%s", serverPort, API_BASE_PATH);

        jsonMapper = new ObjectMapper();
        jsonMapper.registerModule(new JavaTimeModule());
        jsonMapper.setDateFormat(new SimpleDateFormat(DATE_FORMAT_PATTERN));
        jsonMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void testCompleteWeightWorkflowThroughRestApi() throws Exception {
        BigDecimal initialWeight = new BigDecimal("25.75");
        BigDecimal updatedWeight = new BigDecimal("30.25");

        PetDto petDto = buildTestPetDto();
        petDto.setWeight(initialWeight);

        HttpEntity<PetDto> creationEntity = new HttpEntity<>(petDto, requestHeaders);

        ResponseEntity<PetDto> creationResult = httpClient.exchange(
            buildOwnerPetsUrl(DEFAULT_OWNER_ID),
            HttpMethod.POST,
            creationEntity,
            PetDto.class
        );

        assertEquals(HttpStatus.CREATED, creationResult.getStatusCode());
        PetDto savedPet = creationResult.getBody();
        assertNotNull(savedPet);
        Integer savedPetId = savedPet.getId();
        assertNotNull(savedPetId);

        HttpEntity<Void> retrievalEntity = new HttpEntity<>(requestHeaders);
        ResponseEntity<String> retrievalResult = httpClient.exchange(
            buildPetUrl(savedPetId),
            HttpMethod.GET,
            retrievalEntity,
            String.class
        );

        assertEquals(HttpStatus.OK, retrievalResult.getStatusCode());
        JsonNode retrievedPetNode = jsonMapper.readTree(retrievalResult.getBody());
        assertEquals(initialWeight.doubleValue(), retrievedPetNode.get("weight").asDouble());
        assertEquals("TestPet", retrievedPetNode.get("name").asText());
        assertEquals(DOG_TYPE_NAME, retrievedPetNode.get("type").get("name").asText());

        savedPet.setWeight(updatedWeight);
        HttpEntity<PetDto> updateEntity = new HttpEntity<>(savedPet, requestHeaders);

        ResponseEntity<Void> updateResult = httpClient.exchange(
            buildPetUrl(savedPetId),
            HttpMethod.PUT,
            updateEntity,
            Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, updateResult.getStatusCode());

        ResponseEntity<String> verificationResult = httpClient.exchange(
            buildPetUrl(savedPetId),
            HttpMethod.GET,
            retrievalEntity,
            String.class
        );

        assertEquals(HttpStatus.OK, verificationResult.getStatusCode());
        JsonNode verifiedPetNode = jsonMapper.readTree(verificationResult.getBody());
        assertEquals(updatedWeight.doubleValue(), verifiedPetNode.get("weight").asDouble());

        savedPet.setWeight(null);
        HttpEntity<PetDto> nullUpdateEntity = new HttpEntity<>(savedPet, requestHeaders);

        ResponseEntity<Void> nullUpdateResult = httpClient.exchange(
            buildPetUrl(savedPetId),
            HttpMethod.PUT,
            nullUpdateEntity,
            Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, nullUpdateResult.getStatusCode());

        ResponseEntity<String> nullVerificationResult = httpClient.exchange(
            buildPetUrl(savedPetId),
            HttpMethod.GET,
            retrievalEntity,
            String.class
        );

        assertEquals(HttpStatus.OK, nullVerificationResult.getStatusCode());
        JsonNode nullWeightNode = jsonMapper.readTree(nullVerificationResult.getBody());
        assertTrue(nullWeightNode.get("weight").isNull());

        Pet persistedEntity = petClinicService.findPetById(savedPetId);
        assertNotNull(persistedEntity);
        assertNull(persistedEntity.getWeight());
        assertEquals("TestPet", persistedEntity.getName());
    }

    @Test
    void testWeightValidationThroughRestApi() throws Exception {
        PetDto petDto = buildTestPetDto();

        petDto.setWeight(new BigDecimal("-5.00"));
        HttpEntity<PetDto> negativeWeightEntity = new HttpEntity<>(petDto, requestHeaders);

        ResponseEntity<String> negativeWeightResult = httpClient.exchange(
            buildOwnerPetsUrl(DEFAULT_OWNER_ID),
            HttpMethod.POST,
            negativeWeightEntity,
            String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, negativeWeightResult.getStatusCode());

        petDto.setWeight(new BigDecimal("1000.00"));
        HttpEntity<PetDto> excessiveWeightEntity = new HttpEntity<>(petDto, requestHeaders);

        ResponseEntity<String> excessiveWeightResult = httpClient.exchange(
            buildOwnerPetsUrl(DEFAULT_OWNER_ID),
            HttpMethod.POST,
            excessiveWeightEntity,
            String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, excessiveWeightResult.getStatusCode());

        petDto.setWeight(new BigDecimal("15.50"));
        HttpEntity<PetDto> validWeightEntity = new HttpEntity<>(petDto, requestHeaders);

        ResponseEntity<PetDto> validWeightResult = httpClient.exchange(
            buildOwnerPetsUrl(DEFAULT_OWNER_ID),
            HttpMethod.POST,
            validWeightEntity,
            PetDto.class
        );

        assertEquals(HttpStatus.CREATED, validWeightResult.getStatusCode());
        assertNotNull(validWeightResult.getBody());
    }

    @Test
    void testWeightInAllPetsEndpoint() throws Exception {
        HttpEntity<Void> retrievalEntity = new HttpEntity<>(requestHeaders);
        ResponseEntity<String> apiResponse = httpClient.exchange(
            apiBaseUrl + PETS_ENDPOINT,
            HttpMethod.GET,
            retrievalEntity,
            String.class
        );

        assertEquals(HttpStatus.OK, apiResponse.getStatusCode());
        JsonNode petsCollection = jsonMapper.readTree(apiResponse.getBody());
        assertTrue(petsCollection.isArray());
        assertTrue(petsCollection.size() > 0);

        JsonNode samplePet = petsCollection.get(0);
        assertTrue(samplePet.has("weight"));
    }

    @Test
    void testSpecificEndpointWeightHandling() throws Exception {
        BigDecimal firstWeightValue = new BigDecimal("12.34");
        BigDecimal secondWeightValue = new BigDecimal("56.78");
        BigDecimal thirdWeightValue = new BigDecimal("99.99");
        BigDecimal fourthWeightValue = new BigDecimal("88.88");

        PetDto primaryPet = buildTestPetDto();
        primaryPet.setWeight(firstWeightValue);

        HttpEntity<PetDto> primaryCreationEntity = new HttpEntity<>(primaryPet, requestHeaders);
        ResponseEntity<PetDto> primaryCreationResult = httpClient.exchange(
            buildOwnerPetsUrl(DEFAULT_OWNER_ID),
            HttpMethod.POST,
            primaryCreationEntity,
            PetDto.class
        );

        assertEquals(HttpStatus.CREATED, primaryCreationResult.getStatusCode());
        PetDto primarySavedPet = primaryCreationResult.getBody();
        assertNotNull(primarySavedPet);
        Integer primaryPetId = primarySavedPet.getId();

        HttpEntity<Void> fetchEntity = new HttpEntity<>(requestHeaders);
        ResponseEntity<String> fetchResult = httpClient.exchange(
            buildPetUrl(primaryPetId),
            HttpMethod.GET,
            fetchEntity,
            String.class
        );

        assertEquals(HttpStatus.OK, fetchResult.getStatusCode());
        JsonNode fetchedNode = jsonMapper.readTree(fetchResult.getBody());
        assertTrue(fetchedNode.has("weight"));
        assertEquals(firstWeightValue.doubleValue(), fetchedNode.get("weight").asDouble());

        primarySavedPet.setWeight(secondWeightValue);
        HttpEntity<PetDto> modificationEntity = new HttpEntity<>(primarySavedPet, requestHeaders);
        ResponseEntity<Void> modificationResult = httpClient.exchange(
            buildPetUrl(primaryPetId),
            HttpMethod.PUT,
            modificationEntity,
            Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, modificationResult.getStatusCode());

        ResponseEntity<String> postUpdateFetch = httpClient.exchange(
            buildPetUrl(primaryPetId),
            HttpMethod.GET,
            fetchEntity,
            String.class
        );
        JsonNode postUpdateNode = jsonMapper.readTree(postUpdateFetch.getBody());
        assertEquals(secondWeightValue.doubleValue(), postUpdateNode.get("weight").asDouble());

        ResponseEntity<String> collectionFetch = httpClient.exchange(
            apiBaseUrl + PETS_ENDPOINT,
            HttpMethod.GET,
            fetchEntity,
            String.class
        );

        assertEquals(HttpStatus.OK, collectionFetch.getStatusCode());
        JsonNode petsArray = jsonMapper.readTree(collectionFetch.getBody());
        assertTrue(petsArray.isArray());
        assertTrue(petsArray.size() > 0);

        boolean petLocated = false;
        for (JsonNode petNode : petsArray) {
            if (petNode.get("id").asInt() == primaryPetId) {
                assertTrue(petNode.has("weight"));
                assertEquals(secondWeightValue.doubleValue(), petNode.get("weight").asDouble());
                petLocated = true;
                break;
            }
        }
        assertTrue(petLocated, "Created pet should be found in GET /api/pets response");

        PetDto secondaryPet = buildTestPetDto();
        secondaryPet.setName("AnotherTestPet");
        secondaryPet.setWeight(thirdWeightValue);

        HttpEntity<PetDto> secondaryCreationEntity = new HttpEntity<>(secondaryPet, requestHeaders);
        ResponseEntity<PetDto> secondaryCreationResult = httpClient.exchange(
            buildOwnerPetsUrl(DEFAULT_OWNER_ID),
            HttpMethod.POST,
            secondaryCreationEntity,
            PetDto.class
        );

        assertEquals(HttpStatus.CREATED, secondaryCreationResult.getStatusCode());
        PetDto secondarySavedPet = secondaryCreationResult.getBody();
        assertNotNull(secondarySavedPet);
        assertEquals(thirdWeightValue, secondarySavedPet.getWeight());

        Integer secondaryPetId = secondarySavedPet.getId();
        secondarySavedPet.setWeight(fourthWeightValue);

        HttpEntity<PetDto> secondaryUpdateEntity = new HttpEntity<>(secondarySavedPet, requestHeaders);
        ResponseEntity<Void> secondaryUpdateResult = httpClient.exchange(
            buildOwnerPetUrl(DEFAULT_OWNER_ID, secondaryPetId),
            HttpMethod.PUT,
            secondaryUpdateEntity,
            Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, secondaryUpdateResult.getStatusCode());

        ResponseEntity<String> secondaryVerification = httpClient.exchange(
            buildOwnerPetUrl(DEFAULT_OWNER_ID, secondaryPetId),
            HttpMethod.GET,
            fetchEntity,
            String.class
        );
        JsonNode secondaryVerifiedNode = jsonMapper.readTree(secondaryVerification.getBody());
        assertEquals(fourthWeightValue.doubleValue(), secondaryVerifiedNode.get("weight").asDouble());
    }

    @Test
    void testWeightInExistingPetsFromDatabase() {
        Pet firstExistingPet = petClinicService.findPetById(1);
        assertNotNull(firstExistingPet);
        assertEquals(new BigDecimal("4.50"), firstExistingPet.getWeight());

        Pet secondExistingPet = petClinicService.findPetById(2);
        assertNotNull(secondExistingPet);
        assertEquals(new BigDecimal("0.80"), secondExistingPet.getWeight());

        Pet thirdExistingPet = petClinicService.findPetById(4);
        assertNotNull(thirdExistingPet);
        assertNull(thirdExistingPet.getWeight());
    }

    private PetDto buildTestPetDto() {
        PetTypeDto animalType = new PetTypeDto();
        animalType.setId(DOG_TYPE_ID);
        animalType.setName(DOG_TYPE_NAME);

        PetDto petDto = new PetDto();
        petDto.setName("TestPet");
        petDto.setBirthDate(LocalDate.now().minusYears(1));
        petDto.setType(animalType);

        return petDto;
    }

    private String buildPetUrl(Integer petId) {
        return String.format("%s%s/%d", apiBaseUrl, PETS_ENDPOINT, petId);
    }

    private String buildOwnerPetsUrl(int ownerId) {
        return String.format("%s%s/%d/pets", apiBaseUrl, OWNERS_ENDPOINT, ownerId);
    }

    private String buildOwnerPetUrl(int ownerId, Integer petId) {
        return String.format("%s%s/%d/pets/%d", apiBaseUrl, OWNERS_ENDPOINT, ownerId, petId);
    }
}
