package org.springframework.samples.petclinic.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@SpringBootTest
@ActiveProfiles({"h2", "spring-data-jpa"})
public class N1ProblemDemoTest {

    @Autowired
    private ClinicService clinicService;

    @Test
    @Transactional
    public void demonstrateN1ProblemWithOwnerPets() {
        System.out.println("\n=== DEMONSTRATING N+1 PROBLEM WITH OWNER-PET RELATIONSHIP ===\n");
        
        // This will execute 1 query to fetch all owners
        Collection<Owner> owners = clinicService.findAllOwners();
        System.out.println("Fetched " + owners.size() + " owners\n");
        
        // This will trigger N additional queries (one per owner) to fetch pets
        System.out.println("=== NOW ACCESSING PETS FOR EACH OWNER ===");
        for (Owner owner : owners) {
            System.out.println("Owner: " + owner.getFirstName() + " " + owner.getLastName() + 
                             " has " + owner.getPets().size() + " pets");
        }
        
        System.out.println("\n=== END OF N+1 PROBLEM DEMO ===\n");
    }

    @Test
    @Transactional
    public void demonstrateN1ProblemWithPetVisits() {
        System.out.println("\n=== DEMONSTRATING N+1 PROBLEM WITH PET-VISIT RELATIONSHIP ===\n");
        
        // This will execute 1 query to fetch all pets
        Collection<Pet> pets = clinicService.findAllPets();
        System.out.println("Fetched " + pets.size() + " pets\n");
        
        // This will trigger N additional queries (one per pet) to fetch visits
        System.out.println("=== NOW ACCESSING VISITS FOR EACH PET ===");
        for (Pet pet : pets) {
            System.out.println("Pet: " + pet.getName() + " has " + pet.getVisits().size() + " visits");
        }
        
        System.out.println("\n=== END OF N+1 PROBLEM DEMO ===\n");
    }
}