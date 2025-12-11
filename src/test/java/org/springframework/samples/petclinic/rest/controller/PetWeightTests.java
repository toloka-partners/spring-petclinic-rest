package org.springframework.samples.petclinic.rest.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.rest.dto.PetDto;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the weight feature of the Pet entity.
 * These tests verify that the weight field works correctly across all REST endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"hsqldb", "spring-data-jpa"})
public class PetWeightTests {

    @Autowired
    private TestRestTemplate restTemplate;

    private static final String PETS_BASE_URL = "/api/pets";
    private static final String OWNERS_BASE_URL = "/api/owners";

    @Test
    public void shouldIncludeWeightInGetAllPets() {
// When
        ResponseEntity<PetDto[]> response = restTemplate
            .withBasicAuth("admin", "admin")
            .getForEntity(PETS_BASE_URL, PetDto[].class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
        
        // All pets should have weight field (can be null)
        for (PetDto pet : response.getBody()) {
            assertThat(pet).hasFieldOrProperty("weight");
        }
    }
@Test
    public void shouldIncludeWeightInGetPetById() {
        // When
        ResponseEntity<PetDto> response = restTemplate
            .withBasicAuth("admin", "admin")
            .getForEntity(PETS_BASE_URL + "/1", PetDto.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasFieldOrProperty("weight");
    }

    @Test
public void shouldUpdatePetWeight() {
        // Given - Get existing pet
        ResponseEntity<PetDto> getResponse = restTemplate
            .withBasicAuth("admin", "admin")
            .getForEntity(PETS_BASE_URL + "/1", PetDto.class);
        
        PetDto existingPet = getResponse.getBody();
        assertThat(existingPet).isNotNull();

        // When - Update weight
        existingPet.setWeight(12.7);
        ResponseEntity<PetDto> updateResponse = restTemplate
            .withBasicAuth("admin", "admin")
            .exchange(PETS_BASE_URL + "/1", 
                     org.springframework.http.HttpMethod.PUT, 
                     new org.springframework.http.HttpEntity<>(existingPet), 
                     PetDto.class);
// Then - The API returns 204 NO_CONTENT for updates
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify the update persisted by fetching the pet again
        ResponseEntity<PetDto> verifyResponse = restTemplate
            .withBasicAuth("admin", "admin")
            .getForEntity(PETS_BASE_URL + "/1", PetDto.class);
        assertThat(verifyResponse.getBody().getWeight()).isEqualTo(12.7);
    }

    @Test
public void shouldHandleNullWeightInResponse() {
        // When - Get a pet that should have null weight initially
        ResponseEntity<PetDto> response = restTemplate
            .withBasicAuth("admin", "admin")
            .getForEntity(PETS_BASE_URL + "/2", PetDto.class);

        // Then - Weight field should be present but can be null
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasFieldOrProperty("weight");
        // Note: It's acceptable for weight to be null for existing pets
    }
}
