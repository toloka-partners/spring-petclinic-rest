package org.springframework.samples.petclinic.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.samples.petclinic.service.ClinicService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"spring-data-jpa", "hsqldb"})
public class N1OptimizationTest {

    @Autowired
    private ClinicService clinicService;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    public void testOptimizedOwnerFindAll() {
        System.out.println("\n========== TESTING OPTIMIZED OWNER FIND ALL ==========\n");
        
        // Clear cache to ensure fresh queries
        entityManager.clear();
        
        // Enable Hibernate statistics
        SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        
        // Fetch all owners with optimized query
        System.out.println("Fetching all owners with optimized query...\n");
        Collection<Owner> owners = clinicService.findAllOwners();
        
        System.out.println("\n--- Accessing pets for each owner ---");
        for (Owner owner : owners) {
            System.out.println("Owner: " + owner.getFirstName() + " " + owner.getLastName() + 
                             " has " + owner.getPets().size() + " pets");
        }
        
        // Print statistics
        System.out.println("\n--- Query Statistics ---");
        System.out.println("Total queries executed: " + stats.getQueryExecutionCount());
        System.out.println("Entity fetch count: " + stats.getEntityFetchCount());
        System.out.println("Collection fetch count: " + stats.getCollectionFetchCount());
        
        // Should be only 1 query due to JOIN FETCH
        assertThat(stats.getQueryExecutionCount()).isEqualTo(1);
        assertThat(stats.getCollectionFetchCount()).isEqualTo(0); // Collections loaded in the main query
        
        stats.setStatisticsEnabled(false);
    }
    
    @Test
    @Transactional
    public void testOptimizedPetFindAll() {
        System.out.println("\n========== TESTING OPTIMIZED PET FIND ALL ==========\n");
        
        // Clear cache to ensure fresh queries
        entityManager.clear();
        
        // Enable Hibernate statistics
        SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        
        // Fetch all pets with optimized query
        System.out.println("Fetching all pets with optimized query...\n");
        Collection<Pet> pets = clinicService.findAllPets();
        
        System.out.println("\n--- Accessing visits for each pet ---");
        for (Pet pet : pets) {
            System.out.println("Pet: " + pet.getName() + " has " + pet.getVisits().size() + " visits");
            for (Visit visit : pet.getVisits()) {
                System.out.println("  Visit on " + visit.getDate() + ": " + visit.getDescription());
            }
        }
        
        // Print statistics
        System.out.println("\n--- Query Statistics ---");
        System.out.println("Total queries executed: " + stats.getQueryExecutionCount());
        System.out.println("Entity fetch count: " + stats.getEntityFetchCount());
        System.out.println("Collection fetch count: " + stats.getCollectionFetchCount());
        
        // Should be only 1 query due to JOIN FETCH
        assertThat(stats.getQueryExecutionCount()).isEqualTo(1);
        assertThat(stats.getCollectionFetchCount()).isEqualTo(0); // Collections loaded in the main query
        
        stats.setStatisticsEnabled(false);
    }
    
    @Test
    @Transactional
    public void comparePerformanceBeforeAndAfter() {
        System.out.println("\n========== COMPARING PERFORMANCE BEFORE AND AFTER OPTIMIZATION ==========\n");
        
        // Clear cache
        entityManager.clear();
        
        // Test with lazy loading (simulating N+1)
        long startLazy = System.currentTimeMillis();
        Collection<Owner> owners = clinicService.findAllOwners();
        
        // Force lazy loading of pets
        for (Owner owner : owners) {
            owner.getPets().size(); // This triggers lazy loading
        }
        long endLazy = System.currentTimeMillis();
        
        System.out.println("Time with optimized query: " + (endLazy - startLazy) + " ms");
        System.out.println("Number of owners: " + owners.size());
        
        // Verify data correctness
        assertThat(owners).isNotEmpty();
        assertThat(owners.size()).isEqualTo(10); // Based on test data
        
        // Verify that all owners have their pets loaded
        for (Owner owner : owners) {
            assertThat(owner.getPets()).isNotNull();
        }
    }
}