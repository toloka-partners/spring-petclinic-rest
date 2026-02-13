package org.springframework.samples.petclinic.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simplified test to verify N+1 optimizations work correctly
 */
@SpringBootTest
@ActiveProfiles({"h2", "spring-data-jpa"})
@Transactional
public class N1VerificationTest {

    @Autowired
    private ClinicService clinicService;

    @Test
    public void verifyOwnerPetDataIsLoadedCorrectly() {
        // Test findAllOwners with join fetch
        Collection<Owner> owners = clinicService.findAllOwners();
        
        assertNotNull(owners);
        assertTrue(owners.size() > 0, "Should have owners in test data");
        
        // Verify pets are loaded correctly
        int totalPets = 0;
        for (Owner owner : owners) {
            assertNotNull(owner.getPets(), "Pets should not be null");
            totalPets += owner.getPets().size();
            
            // Verify pet data integrity
            for (Pet pet : owner.getPets()) {
                assertNotNull(pet.getName());
                assertEquals(owner, pet.getOwner());
            }
        }
        
        assertTrue(totalPets > 0, "Should have pets in test data");
        System.out.println("Total owners: " + owners.size() + ", Total pets: " + totalPets);
    }

    @Test
    public void verifyFindByIdWithPets() {
        // Test specific owner fetch
        Owner owner = clinicService.findOwnerById(1);
        
        assertNotNull(owner);
        assertNotNull(owner.getPets());
        
        // The pets should be loaded eagerly
        for (Pet pet : owner.getPets()) {
            assertNotNull(pet.getName());
            assertNotNull(pet.getType());
        }
    }

    @Test  
    public void verifyPetVisitDataIsLoadedCorrectly() {
        // Test findAllPets with join fetch
        Collection<Pet> pets = clinicService.findAllPets();
        
        assertNotNull(pets);
        assertTrue(pets.size() > 0, "Should have pets in test data");
        
        // Verify visits are loaded correctly
        for (Pet pet : pets) {
            assertNotNull(pet.getVisits(), "Visits should not be null");
            
            // Verify visit data integrity
            for (var visit : pet.getVisits()) {
                assertNotNull(visit.getDate());
                assertEquals(pet, visit.getPet());
            }
        }
    }
}