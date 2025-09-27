package test.java.com.example.petservice;

import com.example.petservice.web.dto.PetDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.boot.test.web.client.TestRestTemplate;

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
        // create DTO
        PetDTO dto = new PetDTO();
        dto.setName("Fido");
        dto.setWeight(12.5);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<PetDTO> request = new HttpEntity<>(dto, headers);

        // POST /api/pets
        ResponseEntity<PetDTO> resp = restTemplate.postForEntity(baseUrl(), request, PetDTO.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        PetDTO created = resp.getBody();
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getWeight()).isEqualTo(12.5);

        // GET /api/pets and verify item present with weight
        ResponseEntity<PetDTO[]> listResp = restTemplate.getForEntity(baseUrl(), PetDTO[].class);
        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        PetDTO[] pets = listResp.getBody();
        assertThat(pets).isNotNull();
        assertThat(pets.length).isGreaterThanOrEqualTo(1);

        boolean found = false;
        for (PetDTO p : pets) {
            if (p.getId().equals(created.getId())) {
                assertThat(p.getWeight()).isEqualTo(12.5);
                found = true;
                break;
            }
        }
        assertThat(found).isTrue();
    }
}
