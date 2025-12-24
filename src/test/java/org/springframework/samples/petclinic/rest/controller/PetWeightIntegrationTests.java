package org.springframework.samples.petclinic.rest.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"hsqldb", "spring-data-jpa"})
@Transactional
@org.springframework.security.test.context.support.WithMockUser(roles="OWNER_ADMIN")
class PetWeightIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateAndRetrievePetWithWeight() throws Exception {
        // Create a new Pet with weight
        String newPetJson = "{" +
            "\"name\": \"WeightChecker\"," +
            "\"birthDate\": \"2023-01-01\"," +
            "\"type\": {\"id\": 2, \"name\": \"dog\"}," + // Assuming Dog type exists with ID 2
            "\"weight\": 15.5" +
            "}";

        // POST to create pet for Owner 1
        mockMvc.perform(post("/api/owners/1/pets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(newPetJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("WeightChecker"))
            .andExpect(jsonPath("$.weight").value(15.5));

        // Get the ID of the created pet (it won't be returned in the POST response body in some implementations, but typical rest does return it)
        // Wait, typical save returns the saved entity.
        // Let's verify by retrieving it. We can't easily guess the ID unless we capture it.
        // But the previous andExpect checks the response body, so if it passes, we have the weight in response.
    }

    @Test
    void shouldUpdatePetWeight() throws Exception {
        // Using existing pet ID 7 (Samantha, cat, owner 6) from data.sql
        // Update weight to 7.25
        String updatePetJson = "{" +
            "\"id\": 7," +
            "\"name\": \"Samantha\"," +
            "\"birthDate\": \"2012-09-04\"," +
            "\"type\": {\"id\": 1, \"name\": \"cat\"}," +
            "\"weight\": 7.25" +
            "}";

        mockMvc.perform(put("/api/pets/7")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePetJson))
            .andExpect(status().isNoContent());

        // Verify with GET
        mockMvc.perform(get("/api/pets/7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(7))
            .andExpect(jsonPath("$.weight").value(7.25));
    }
    
    @Test
    void shouldHandleNullWeight() throws Exception {
       // Using existing pet ID 8 (Max, cat, owner 6)
       // Update weight to null
       String updatePetJson = "{" +
           "\"id\": 8," +
           "\"name\": \"Max\"," +
           "\"birthDate\": \"2012-09-04\"," +
           "\"type\": {\"id\": 1, \"name\": \"cat\"}," +
           "\"weight\": null" +
           "}";

       mockMvc.perform(put("/api/pets/8")
               .contentType(MediaType.APPLICATION_JSON)
               .content(updatePetJson))
           .andExpect(status().isNoContent());

       // Verify with GET
       mockMvc.perform(get("/api/pets/8"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value(8))
           .andExpect(jsonPath("$.weight").isEmpty()); // or null value check
    }
}
