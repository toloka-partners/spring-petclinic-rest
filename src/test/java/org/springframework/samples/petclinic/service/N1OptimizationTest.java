package org.springframework.samples.petclinic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify the correctness and performance of N+1 problem optimizations
 */
@SpringBootTest
@ActiveProfiles({"h2", "spring-data-jpa"})
public class N1OptimizationTest {

    @Autowired
    private ClinicService clinicService;

    @PersistenceContext
    private EntityManager entityManager;

    private static final int EXPECTED_OWNER_COUNT = 10; // Based on data.sql
    private static final int EXPECTED_PET_COUNT = 13; // Based on data.sql

    @BeforeEach
    public void clearEntityManagerCache() {
        entityManager.clear();
    }

    @Test
    @Transactional
    public void testJoinFetchReturnsCorrectData() {
        System.out.println("\n=== Testing JPQL Join Fetch Solution ===");
        
        Collection<Owner> owners = clinicService.findAllOwners();
        
        assertNotNull(owners);
        assertEquals(EXPECTED_OWNER_COUNT, owners.size());
        
        // Verify pets are loaded
        for (Owner owner : owners) {
            assertNotNull(owner.getPets());
            System.out.println("Owner: " + owner.getFirstName() + " " + owner.getLastName() + 
                             " has " + owner.getPets().size() + " pets");
        }
        
        // Verify total pet count
        long totalPets = owners.stream()
            .mapToLong(owner -> owner.getPets().size())
            .sum();
        assertEquals(EXPECTED_PET_COUNT, totalPets);
    }

    @Test
    @Transactional
    public void testEntityGraphReturnsCorrectData() {
        System.out.println("\n=== Testing @EntityGraph Solution ===");
        
        // Use EntityManager directly to test EntityGraph
        TypedQuery<Owner> query = entityManager.createQuery("SELECT DISTINCT o FROM Owner o", Owner.class);
        query.setHint("jakarta.persistence.loadgraph", entityManager.getEntityGraph("Owner.pets"));
        Collection<Owner> owners = query.getResultList();
        
        assertNotNull(owners);
        assertEquals(EXPECTED_OWNER_COUNT, owners.size());
        
        // Verify pets are loaded
        for (Owner owner : owners) {
            assertNotNull(owner.getPets());
            System.out.println("Owner: " + owner.getFirstName() + " " + owner.getLastName() + 
                             " has " + owner.getPets().size() + " pets");
        }
    }

    @Test
    @Transactional
    public void testBatchSizeOptimization() {
        System.out.println("\n=== Testing @BatchSize Solution ===");
        
        // First, we need to use a query that doesn't use join fetch
        // to see the batch size optimization in action
        List<Owner> owners = entityManager
            .createQuery("SELECT o FROM Owner o", Owner.class)
            .getResultList();
        
        assertNotNull(owners);
        assertEquals(EXPECTED_OWNER_COUNT, owners.size());
        
        // Access pets - this should trigger batch loading
        System.out.println("Accessing pets with @BatchSize optimization:");
        for (Owner owner : owners) {
            assertNotNull(owner.getPets());
            System.out.println("Owner: " + owner.getFirstName() + " " + owner.getLastName() + 
                             " has " + owner.getPets().size() + " pets");
        }
    }

    @Test
    @Transactional
    public void testPetVisitRelationshipOptimization() {
        System.out.println("\n=== Testing Pet-Visit Relationship Optimization ===");
        
        Collection<Pet> pets = clinicService.findAllPets();
        
        assertNotNull(pets);
        assertEquals(EXPECTED_PET_COUNT, pets.size());
        
        // Verify visits are loaded
        for (Pet pet : pets) {
            assertNotNull(pet.getVisits());
            System.out.println("Pet: " + pet.getName() + " has " + pet.getVisits().size() + " visits");
        }
    }

    @Test
    @Transactional
    public void testSingleOwnerFetchOptimization() {
        System.out.println("\n=== Testing Single Owner Fetch Optimization ===");
        
        // Test with known owner ID
        Owner owner = clinicService.findOwnerById(1);
        
        assertNotNull(owner);
        assertEquals("George", owner.getFirstName());
        assertEquals("Franklin", owner.getLastName());
        
        // Verify pets are eagerly loaded
        assertNotNull(owner.getPets());
        assertTrue(owner.getPets().size() > 0);
        
        System.out.println("Owner: " + owner.getFirstName() + " " + owner.getLastName() + 
                         " has " + owner.getPets().size() + " pets");
    }

    @Test
    @Transactional
    public void testDataIntegrityAfterOptimization() {
        System.out.println("\n=== Testing Data Integrity ===");
        
        // Test specific owner-pet relationships
        Owner george = clinicService.findOwnerById(1);
        assertEquals(1, george.getPets().size());
        Pet georgesPet = george.getPets().iterator().next();
        assertEquals("Leo", georgesPet.getName());
        
        Owner betty = clinicService.findOwnerById(2);
        assertEquals(1, betty.getPets().size());
        Pet bettysPet = betty.getPets().iterator().next();
        assertEquals("Basil", bettysPet.getName());
        
        // Test owner with multiple pets
        Owner eduardo = clinicService.findOwnerById(3);
        assertEquals(2, eduardo.getPets().size());
        assertTrue(eduardo.getPets().stream()
            .anyMatch(pet -> "Rosy".equals(pet.getName())));
        assertTrue(eduardo.getPets().stream()
            .anyMatch(pet -> "Jewel".equals(pet.getName())));
    }
}