package org.springframework.samples.petclinic.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.repository.PetTypeRepository;
import org.springframework.samples.petclinic.rest.dto.PetDto;
import org.springframework.samples.petclinic.rest.dto.PetTypeDto;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = { "petclinic.security.enable=false" })
public class PetWeightApiIntegrationTest {

  @Autowired
  TestRestTemplate testRestTemplate;

  @Autowired
  PetTypeRepository petTypeRepository;

  @LocalServerPort
  private int port;

  private String url(String uri) {
    return "http://localhost:" + port + "/petclinic" + uri;
  }

  private HttpHeaders headers;

  @BeforeEach
  void setupTypes() {
    headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
  }

  private PetDto buildPetDto(String name, LocalDate date, BigDecimal weight) {
    PetTypeDto petTypeDto = new PetTypeDto();
    petTypeDto.setId(2);
    petTypeDto.setName("dog");

    PetDto petDto = new PetDto();
    petDto.setName(name);
    petDto.setBirthDate(date);
    petDto.setWeight(weight);
    petDto.setType(petTypeDto);

    return petDto;
  }

  @Test
  void testGetAllPets() throws JsonMappingException, JsonProcessingException {
    // Test GET /api/pets - return weight for all pets in response array
    HttpEntity<Void> request = new HttpEntity<>(headers);
    ResponseEntity<String> response = testRestTemplate.exchange(
        url("/api/pets"),
        HttpMethod.GET,
        request,
        String.class);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(response.getBody());
    assertTrue(root.isArray());
    assertTrue(root.size() > 0);
    JsonNode pet = root.get(0);
    assertTrue(pet.has("weight"));
    pet = root.get(root.size() - 1);
    assertTrue(pet.has("weight"));
  }

  @Test
  void testPetWeightApis() {
    // 0. Create a new pet and add to an owner for before testing
    PetDto petToAdd = buildPetDto("Harvey", LocalDate.of(2018, 4, 25), new BigDecimal("21.54"));
    HttpEntity<PetDto> petCreateRequest = new HttpEntity<>(petToAdd, headers);
    ResponseEntity<PetDto> petCreateResponse = testRestTemplate.exchange(
        url("/api/owners/1/pets"),
        HttpMethod.POST,
        petCreateRequest,
        PetDto.class);
    assertEquals(HttpStatus.CREATED, petCreateResponse.getStatusCode());
    PetDto petCreated = petCreateResponse.getBody();
    assertEquals("Harvey", petCreated.getName());
    assertEquals(new BigDecimal("21.54"), petCreated.getWeight());

    // 1. Test GET /api/pets/{petId} - return weight as decimal number in JSON
    // response
    HttpEntity<Void> petFindRequest = new HttpEntity<>(headers);
    ResponseEntity<PetDto> petFindResponse = testRestTemplate.exchange(
        url("/api/pets/" + petCreated.getId()),
        HttpMethod.GET,
        petFindRequest,
        PetDto.class);
    assertEquals(HttpStatus.OK, petFindResponse.getStatusCode());
    PetDto petFound = petFindResponse.getBody();
    assertNotNull(petFound);
    assertEquals("Harvey", petFound.getName());
    assertEquals(new BigDecimal("21.54"), petFound.getWeight());

    // 2. Test PUT /api/pets/{petId} - accept weight as decimal number in JSON
    // request body
    petCreated.setWeight(new BigDecimal("19.11"));
    HttpEntity<PetDto> petUpdateRequest = new HttpEntity<>(petCreated, headers);
    ResponseEntity<Void> updateResponse = testRestTemplate.exchange(
        url("/api/pets/" + petCreated.getId()),
        HttpMethod.PUT,
        petUpdateRequest,
        Void.class);
    assertEquals(HttpStatus.NO_CONTENT, updateResponse.getStatusCode());

    // 2-1. Check if the update was successfully made
    petFindRequest = new HttpEntity<>(headers);
    petFindResponse = testRestTemplate.exchange(
        url("/api/owners/1/pets/" + petCreated.getId()),
        HttpMethod.GET,
        petFindRequest,
        PetDto.class);
    assertEquals(HttpStatus.OK, petFindResponse.getStatusCode());
    petFound = petFindResponse.getBody();
    assertNotNull(petFound);
    assertEquals("Harvey", petFound.getName());
    assertEquals(new BigDecimal("19.11"), petFound.getWeight());
  }

  @Test
  void testCreatePetWeightValidity() {
    // 1. Test creating pet weight with negative value
    PetDto petToAdd = buildPetDto("Harvey", LocalDate.of(2018, 4, 25), null);
    petToAdd.setWeight(new BigDecimal("-9.99"));
    HttpEntity<PetDto> petCreateRequest = new HttpEntity<>(petToAdd, headers);
    ResponseEntity<String> petCreateResponseFail = testRestTemplate.exchange(
        url("/api/owners/1/pets"),
        HttpMethod.POST,
        petCreateRequest,
        String.class);
    assertEquals(HttpStatus.BAD_REQUEST, petCreateResponseFail.getStatusCode());

    // 2. Test creating pet weight with larger than maximum value
    petToAdd.setWeight(new BigDecimal("1000.00"));
    petCreateRequest = new HttpEntity<>(petToAdd, headers);
    petCreateResponseFail = testRestTemplate.exchange(
        url("/api/owners/1/pets"),
        HttpMethod.POST,
        petCreateRequest,
        String.class);
    assertEquals(HttpStatus.BAD_REQUEST, petCreateResponseFail.getStatusCode());

    // 3. Test creating pet weight with correct value
    petToAdd.setWeight(new BigDecimal("21.54"));
    petCreateRequest = new HttpEntity<>(petToAdd, headers);
    ResponseEntity<PetDto> petCreateResponseSuccess = testRestTemplate.exchange(
        url("/api/owners/1/pets"),
        HttpMethod.POST,
        petCreateRequest,
        PetDto.class);
    assertEquals(HttpStatus.CREATED, petCreateResponseSuccess.getStatusCode());
    PetDto petCreated = petCreateResponseSuccess.getBody();
    assertEquals("Harvey", petCreated.getName());
    assertEquals(new BigDecimal("21.54"), petCreated.getWeight());

    // 3-1. Check if the update was successfully made
    HttpEntity<Void> petFindRequest = new HttpEntity<>(headers);
    ResponseEntity<PetDto> petFindResponse = testRestTemplate.exchange(
        url("/api/owners/1/pets/" + petCreated.getId()),
        HttpMethod.GET,
        petFindRequest,
        PetDto.class);
    assertEquals(HttpStatus.OK, petFindResponse.getStatusCode());
    PetDto petFound = petFindResponse.getBody();
    assertNotNull(petFound);
    assertEquals("Harvey", petFound.getName());
    assertEquals(new BigDecimal("21.54"), petFound.getWeight());
  }

  @Test
  void testUpdatePetWeightValidity() {
    // 0. Create a new pet and add to an owner for before testing
    PetDto petToAdd = buildPetDto("Harvey", LocalDate.of(2018, 4, 25), new BigDecimal("21.54"));
    HttpEntity<PetDto> petCreateRequest = new HttpEntity<>(petToAdd, headers);
    ResponseEntity<PetDto> petCreateResponse = testRestTemplate.exchange(
        url("/api/owners/1/pets"),
        HttpMethod.POST,
        petCreateRequest,
        PetDto.class);
    assertEquals(HttpStatus.CREATED, petCreateResponse.getStatusCode());
    PetDto petCreated = petCreateResponse.getBody();
    assertEquals("Harvey", petCreated.getName());
    assertEquals(new BigDecimal("21.54"), petCreated.getWeight());

    // 1. Test updating pet weight with negative value
    petCreated.setWeight(new BigDecimal("-9.99"));
    HttpEntity<PetDto> petUpdateRequest = new HttpEntity<>(petCreated, headers);
    ResponseEntity<Void> updateResponse = testRestTemplate.exchange(
        url("/api/pets/" + petCreated.getId()),
        HttpMethod.PUT,
        petUpdateRequest,
        Void.class);
    assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatusCode());

    // 2. Test updating pet weight with larger than maximum value
    petCreated.setWeight(new BigDecimal("1000.00"));
    petUpdateRequest = new HttpEntity<>(petCreated, headers);
    updateResponse = testRestTemplate.exchange(
        url("/api/pets/" + petCreated.getId()),
        HttpMethod.PUT,
        petUpdateRequest,
        Void.class);
    assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatusCode());

    // 3. Test updating pet weight with correct value
    petCreated.setWeight(new BigDecimal("19.11"));
    petUpdateRequest = new HttpEntity<>(petCreated, headers);
    updateResponse = testRestTemplate.exchange(
        url("/api/pets/" + petCreated.getId()),
        HttpMethod.PUT,
        petUpdateRequest,
        Void.class);
    assertEquals(HttpStatus.NO_CONTENT, updateResponse.getStatusCode());

    // 3-1. Check if the update was successfully made
    HttpEntity<Void> petFindRequest = new HttpEntity<>(headers);
    ResponseEntity<PetDto> petFindResponse = testRestTemplate.exchange(
        url("/api/owners/1/pets/" + petCreated.getId()),
        HttpMethod.GET,
        petFindRequest,
        PetDto.class);
    assertEquals(HttpStatus.OK, petFindResponse.getStatusCode());
    PetDto petFound = petFindResponse.getBody();
    assertNotNull(petFound);
    assertEquals("Harvey", petFound.getName());
    assertEquals(new BigDecimal("19.11"), petFound.getWeight());
  }

  @Test
  void testPetWeightWithOwnerApis() {
    // 0. Create a new Pet DTO for testing
    PetDto petToAdd = buildPetDto("Harvey", LocalDate.of(2018, 4, 25), new BigDecimal("21.54"));

    // 1. Test POST /api/owners/{ownerId}/pets - accept weight when creating pets
    HttpEntity<PetDto> petCreateRequest = new HttpEntity<>(petToAdd, headers);
    ResponseEntity<PetDto> petCreateResponse = testRestTemplate.exchange(
        url("/api/owners/1/pets"),
        HttpMethod.POST,
        petCreateRequest,
        PetDto.class);
    assertEquals(HttpStatus.CREATED, petCreateResponse.getStatusCode());
    PetDto petCreated = petCreateResponse.getBody();
    assertEquals("Harvey", petCreated.getName());
    assertEquals(new BigDecimal("21.54"), petCreated.getWeight());

    // 2. Test PUT /api/owners/{ownerId}/pets/{petId} - accept weight in updates
    petCreated.setWeight(new BigDecimal("19.11"));
    HttpEntity<PetDto> petUpdateRequest = new HttpEntity<>(petCreated, headers);
    ResponseEntity<Void> updateResponse = testRestTemplate.exchange(
        url("/api/owners/1/pets/" + petCreated.getId()),
        HttpMethod.PUT,
        petUpdateRequest,
        Void.class);
    assertEquals(HttpStatus.NO_CONTENT, updateResponse.getStatusCode());

    // 2-1. Check if the update was successfully made
    HttpEntity<Void> petFindRequest = new HttpEntity<>(headers);
    ResponseEntity<PetDto> petFindResponse = testRestTemplate.exchange(
        url("/api/owners/1/pets/" + petCreated.getId()),
        HttpMethod.GET,
        petFindRequest,
        PetDto.class);
    assertEquals(HttpStatus.OK, petFindResponse.getStatusCode());
    PetDto petFound = petFindResponse.getBody();
    assertNotNull(petFound);
    assertEquals("Harvey", petFound.getName());
    assertEquals(new BigDecimal("19.11"), petFound.getWeight());
  }
}
