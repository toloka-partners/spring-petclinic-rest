package com.example.petservice;

import com.example.petservice.web.dto.PetDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PetResourceE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:" + port + "/api/pets";
    }

    @Test
    public void createPet_and_getAll_verifyWeight() {
        // 1️⃣ Create PetDTO
        PetDTO petDto = new PetDTO();
        petDto.setName("Fido");
        petDto.setWeight(12.5);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<PetDTO> request = new HttpEntity<>(petDto, headers);

        // 2️⃣ POST /api/pets
        ResponseEntity<PetDTO> postResponse = restTemplate.postForEntity(baseUrl(), request, PetDTO.class);
        assertThat(postResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        PetDTO createdPet = postResponse.getBody();
        assertThat(createdPet).isNotNull();
        assertThat(createdPet.getId()).isNotNull();
        assertThat(createdPet.getWeight()).isEqualTo(12.5);

        // 3️⃣ GET /api/pets
        ResponseEntity<PetDTO[]> getResponse = restTemplate.getForEntity(baseUrl(), PetDTO[].class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        PetDTO[] pets = getResponse.getBody();
        assertThat(pets).isNotNull();
        assertThat(pets.length).isGreaterThanOrEqualTo(1);

        // 4️⃣ Verify the created pet is in the list with correct weight
        boolean found = false;
        for (PetDTO p : pets) {
            if (p.getId().equals(createdPet.getId())) {
                assertThat(p.getWeight()).isEqualTo(12.5);
                found = true;
                break;
            }
        }
        assertThat(found).isTrue();
    }
}
