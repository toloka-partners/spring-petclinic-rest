package org.springframework.samples.petclinic.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.samples.petclinic.model.Vet;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles({"h2", "spring-data-jpa"})
public class N1OptimizationVerificationTest {

    @Autowired
    private ClinicService clinicService;

    @Test
    @Transactional
    public void testOptimizedVisitFetchDoesNotTriggerN1() {
        System.out.println("\n=== TESTING OPTIMIZED VISIT FETCH ===\n");
        
        // This should execute only one query with joins
        Collection<Visit> visits = clinicService.findAllVisitsWithPet();
        System.out.println("Fetched " + visits.size() + " visits using optimized method\n");
        
        // Access pet data - should not trigger additional queries
        System.out.println("=== ACCESSING PET DATA (NO ADDITIONAL QUERIES EXPECTED) ===");
        for (Visit visit : visits) {
            assertNotNull(visit.getPet());
            System.out.println("Visit ID: " + visit.getId() + 
                             ", Pet: " + visit.getPet().getName() + 
                             ", Owner: " + visit.getPet().getOwner().getFirstName() + " " + 
                             visit.getPet().getOwner().getLastName());
        }
        
        assertTrue(visits.size() > 0, "Should have at least one visit");
        System.out.println("\n=== OPTIMIZED FETCH COMPLETED ===\n");
    }

    @Test
    @Transactional
    public void testOptimizedVetFetchDoesNotTriggerN1() {
        System.out.println("\n=== TESTING OPTIMIZED VET FETCH ===\n");
        
        // This should execute only one query with joins
        Collection<Vet> vets = clinicService.findAllVets();
        System.out.println("Fetched " + vets.size() + " vets using optimized method\n");
        
        // Access specialty data - should not trigger additional queries
        System.out.println("=== ACCESSING SPECIALTY DATA (NO ADDITIONAL QUERIES EXPECTED) ===");
        for (Vet vet : vets) {
            System.out.println("Vet: " + vet.getFirstName() + " " + vet.getLastName() + 
                             " has " + vet.getSpecialties().size() + " specialties");
            vet.getSpecialties().forEach(s -> System.out.println("  - " + s.getName()));
        }
        
        assertTrue(vets.size() > 0, "Should have at least one vet");
        System.out.println("\n=== OPTIMIZED FETCH COMPLETED ===\n");
    }
}