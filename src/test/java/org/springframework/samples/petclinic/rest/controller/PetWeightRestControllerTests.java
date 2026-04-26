package org.springframework.samples.petclinic.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.mapper.PetMapper;
import org.springframework.samples.petclinic.rest.advice.ExceptionControllerAdvice;
import org.springframework.samples.petclinic.rest.dto.PetDto;
import org.springframework.samples.petclinic.rest.dto.PetTypeDto;
import org.springframework.samples.petclinic.service.ClinicService;
import org.springframework.samples.petclinic.service.clinicService.ApplicationTestConfig;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ContextConfiguration(classes = ApplicationTestConfig.class)
@WebAppConfiguration
class PetWeightRestControllerTests {

    @MockitoBean
    protected ClinicService clinicService;

    @Autowired
    private PetRestController petRestController;

    @Autowired
    private PetMapper petMapper;

    private MockMvc mockMvc;

    private ObjectMapper mapper;

    private PetDto petWithWeight;
    private PetDto petWithNullWeight;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(petRestController)
            .setControllerAdvice(new ExceptionControllerAdvice())
            .build();

        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        PetTypeDto petType = new PetTypeDto();
        petType.id(2).name("dog");

        petWithWeight = new PetDto();
        petWithWeight.id(3)
            .name("Rosy")
            .birthDate(LocalDate.now())
            .type(petType)
            .weight(new BigDecimal("15.75"));

        petWithNullWeight = new PetDto();
        petWithNullWeight.id(4)
            .name("Jewel")
            .birthDate(LocalDate.now())
            .type(petType);
        // weight intentionally left null
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testGetPetReturnsWeight() throws Exception {
        given(this.clinicService.findPetById(3))
            .willReturn(petMapper.toPet(petWithWeight));

        this.mockMvc.perform(get("/api/pets/3")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.id").value(3))
            .andExpect(jsonPath("$.weight").value(15.75));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testGetPetReturnsNullWeight() throws Exception {
        given(this.clinicService.findPetById(4))
            .willReturn(petMapper.toPet(petWithNullWeight));

        this.mockMvc.perform(get("/api/pets/4")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.weight").isEmpty());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testGetAllPetsReturnsWeight() throws Exception {
        List<PetDto> pets = new ArrayList<>();
        pets.add(petWithWeight);
        pets.add(petWithNullWeight);

        Collection<org.springframework.samples.petclinic.model.Pet> petEntities =
            petMapper.toPets(pets);
        given(this.clinicService.findAllPets()).willReturn(petEntities);

        this.mockMvc.perform(get("/api/pets")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.[0].weight").value(15.75))
            .andExpect(jsonPath("$.[1].weight").isEmpty());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testUpdatePetWeightSuccess() throws Exception {
        given(this.clinicService.findPetById(3))
            .willReturn(petMapper.toPet(petWithWeight));

        PetDto updatedPet = petWithWeight;
        updatedPet.setWeight(new BigDecimal("20.00"));

        String updatedPetJson = mapper.writeValueAsString(updatedPet);

        this.mockMvc.perform(put("/api/pets/3")
                .content(updatedPetJson)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        this.mockMvc.perform(get("/api/pets/3")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.weight").value(20.00));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testUpdatePetWeightToNull() throws Exception {
        given(this.clinicService.findPetById(3))
            .willReturn(petMapper.toPet(petWithWeight));

        PetDto updatedPet = petWithWeight;
        updatedPet.setWeight(null);

        String updatedPetJson = mapper.writeValueAsString(updatedPet);

        this.mockMvc.perform(put("/api/pets/3")
                .content(updatedPetJson)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        this.mockMvc.perform(get("/api/pets/3")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.weight").isEmpty());
    }
}
