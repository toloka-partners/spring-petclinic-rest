package org.springframework.samples.petclinic.service.clinicService;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Pet weight functionality.
 * These tests verify weight field behavior through the service layer.
 */
@SpringBootTest
@ActiveProfiles({"spring-data-jpa", "hsqldb"})
class PetWeightServiceTests extends AbstractClinicServiceTests {

    @Autowired
    EntityManager entityManager;

    @Override
    void clearCache() {
        entityManager.clear();
    }

    @Test
    @Transactional
    void shouldPersistPetWeight() {
        Owner owner = this.clinicService.findOwnerById(1);

        Pet pet = new Pet();
        pet.setName("Phil");
        pet.setBirthDate(LocalDate.of(2023, 1, 15));
        pet.setWeight(15.75);

        PetType petType = this.clinicService.findPetTypeById(1);
        pet.setType(petType);
        pet.setOwner(owner);

        this.clinicService.savePet(pet);
        Integer petId = pet.getId();

        assertThat(petId).isNotNull();

        Pet retrievedPet = this.clinicService.findPetById(petId);
        assertThat(retrievedPet.getWeight()).isEqualTo(15.75);
    }

    @Test
    @Transactional
    void shouldAllowNullWeight() {
        Owner owner = this.clinicService.findOwnerById(1);

        Pet pet = new Pet();
        pet.setName("Verrea");
        pet.setBirthDate(LocalDate.of(2023, 2, 1));
        pet.setWeight(null);

        PetType petType = this.clinicService.findPetTypeById(1);
        pet.setType(petType);
        pet.setOwner(owner);

        this.clinicService.savePet(pet);
        Integer petId = pet.getId();

        Pet retrievedPet = this.clinicService.findPetById(petId);
        assertThat(retrievedPet.getWeight()).isNull();
    }

    @Test
    @Transactional
    void shouldUpdatePetWeight() {
        Pet pet = this.clinicService.findPetById(1);

        pet.setWeight(20.5);
        this.clinicService.savePet(pet);

        Pet updatedPet = this.clinicService.findPetById(1);
        assertThat(updatedPet.getWeight()).isEqualTo(20.5);
    }

    @Test
    @Transactional
    void shouldRetrieveWeightFromAllPets() {
        Owner owner = this.clinicService.findOwnerById(1);
        Pet pet = new Pet();
        pet.setName("Samil");
        pet.setBirthDate(LocalDate.now());
        pet.setWeight(12.5);
        PetType petType = this.clinicService.findPetTypeById(1);
        pet.setType(petType);
        pet.setOwner(owner);

        this.clinicService.savePet(pet);
        Integer petId = pet.getId();

        var allPets = this.clinicService.findAllPets();
        Pet retrievedPet = allPets.stream()
            .filter(p -> p.getId().equals(petId))
            .findFirst()
            .orElseThrow();

        assertThat(retrievedPet.getWeight()).isEqualTo(12.5);
    }
}
