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
public class N1SelectProblemDemoTest {

    @Autowired
    private ClinicService clinicService;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    public void demonstrateN1ProblemWithOwnersAndPets() {
        System.out.println("\n========== DEMONSTRATING N+1 PROBLEM WITH OWNERS AND PETS ==========\n");
        
        // Clear cache to ensure fresh queries
        entityManager.clear();
        
        // Enable Hibernate statistics
        SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        
        // Fetch all owners - this will trigger N+1 queries
        System.out.println("Fetching all owners...\n");
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
        
        // Assert that we have the N+1 problem
        // 1 query for owners + N queries for pets (where N is the number of owners)
        assertThat(stats.getQueryExecutionCount()).isGreaterThan(1);
        
        stats.setStatisticsEnabled(false);
    }
    
    @Test
    @Transactional
    public void demonstrateN1ProblemWithPetsAndVisits() {
        System.out.println("\n========== DEMONSTRATING N+1 PROBLEM WITH PETS AND VISITS ==========\n");
        
        // Clear cache to ensure fresh queries
        entityManager.clear();
        
        // Enable Hibernate statistics
        SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        
        // Fetch all pets - this will trigger N+1 queries for visits
        System.out.println("Fetching all pets...\n");
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
        
        // Assert that we have the N+1 problem
        assertThat(stats.getQueryExecutionCount()).isGreaterThan(1);
        
        stats.setStatisticsEnabled(false);
    }
    
    @Test
    @Transactional
    public void demonstrateOptimizedOwnerQuery() {
        System.out.println("\n========== DEMONSTRATING OPTIMIZED QUERY FOR SPECIFIC OWNER ==========\n");
        
        // Clear cache to ensure fresh queries
        entityManager.clear();
        
        // Enable Hibernate statistics
        SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        
        // Find owner by ID - this uses optimized query with JOIN FETCH
        System.out.println("Fetching owner by ID (optimized)...\n");
        Owner owner = clinicService.findOwnerById(1);
        
        System.out.println("Owner: " + owner.getFirstName() + " " + owner.getLastName() + 
                         " has " + owner.getPets().size() + " pets");
        
        // Print statistics
        System.out.println("\n--- Query Statistics ---");
        System.out.println("Total queries executed: " + stats.getQueryExecutionCount());
        System.out.println("Entity fetch count: " + stats.getEntityFetchCount());
        System.out.println("Collection fetch count: " + stats.getCollectionFetchCount());
        
        // Should be only 1 query due to JOIN FETCH
        assertThat(stats.getQueryExecutionCount()).isEqualTo(1);
        
        stats.setStatisticsEnabled(false);
    }
}