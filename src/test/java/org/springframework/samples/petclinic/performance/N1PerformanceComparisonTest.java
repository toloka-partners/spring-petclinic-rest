package org.springframework.samples.petclinic.performance;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.repository.OwnerRepository;
import org.springframework.samples.petclinic.repository.PetRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance comparison test demonstrating the improvement achieved by 
 * fixing the N+1 select problem using JOIN FETCH queries.
 */
@SpringBootTest
@ActiveProfiles({"spring-data-jpa", "hsqldb"})
public class N1PerformanceComparisonTest {

    @Autowired
    private OwnerRepository ownerRepository;
    
    @Autowired
    private PetRepository petRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    public void compareOwnerFetchPerformance() {
        System.out.println("\n========== PERFORMANCE COMPARISON: OWNER FETCHING ==========\n");
        
        SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        
        // Test 1: Optimized query with JOIN FETCH
        entityManager.clear();
        stats.setStatisticsEnabled(true);
        stats.clear();
        
        long startOptimized = System.currentTimeMillis();
        Collection<Owner> ownersOptimized = ownerRepository.findAll();
        
        // Access pets to ensure they're loaded
        int totalPets = 0;
        for (Owner owner : ownersOptimized) {
            totalPets += owner.getPets().size();
        }
        long endOptimized = System.currentTimeMillis();
        
        long optimizedQueries = stats.getQueryExecutionCount();
        long optimizedTime = endOptimized - startOptimized;
        
        System.out.println("=== Optimized Query Results ===");
        System.out.println("Time taken: " + optimizedTime + " ms");
        System.out.println("Total queries executed: " + optimizedQueries);
        System.out.println("Total owners: " + ownersOptimized.size());
        System.out.println("Total pets loaded: " + totalPets);
        
        // Test 2: Simulate N+1 problem (using individual queries)
        entityManager.clear();
        stats.clear();
        
        long startN1 = System.currentTimeMillis();
        // First query: get all owners
        Collection<Owner> ownersN1 = entityManager.createQuery("SELECT o FROM Owner o", Owner.class)
            .getResultList();
        
        // N additional queries: get pets for each owner
        int totalPetsN1 = 0;
        for (Owner owner : ownersN1) {
            // This simulates what would happen with lazy loading
            List<Pet> pets = entityManager.createQuery("SELECT p FROM Pet p WHERE p.owner.id = :ownerId", Pet.class)
                .setParameter("ownerId", owner.getId())
                .getResultList();
            totalPetsN1 += pets.size();
        }
        long endN1 = System.currentTimeMillis();
        
        long n1Queries = stats.getQueryExecutionCount();
        long n1Time = endN1 - startN1;
        
        System.out.println("\n=== N+1 Problem Results ===");
        System.out.println("Time taken: " + n1Time + " ms");
        System.out.println("Total queries executed: " + n1Queries);
        System.out.println("Total owners: " + ownersN1.size());
        System.out.println("Total pets loaded: " + totalPetsN1);
        
        // Calculate improvements
        double performanceImprovement = ((double)(n1Time - optimizedTime) / n1Time) * 100;
        double queryReduction = ((double)(n1Queries - optimizedQueries) / n1Queries) * 100;
        
        System.out.println("\n=== Performance Improvements ===");
        System.out.println("Performance improvement: " + String.format("%.2f", performanceImprovement) + "%");
        System.out.println("Query reduction: " + String.format("%.2f", queryReduction) + "%");
        System.out.println("Queries saved: " + (n1Queries - optimizedQueries));
        
        // Assertions
        assertThat(optimizedQueries).isEqualTo(1);
        assertThat(n1Queries).isGreaterThan(optimizedQueries);
        assertThat(totalPets).isEqualTo(totalPetsN1);
        
        stats.setStatisticsEnabled(false);
    }
    
    @Test
    @Transactional
    public void comparePetFetchPerformance() {
        System.out.println("\n========== PERFORMANCE COMPARISON: PET AND VISIT FETCHING ==========\n");
        
        SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        
        // Test 1: Optimized query with JOIN FETCH
        entityManager.clear();
        stats.setStatisticsEnabled(true);
        stats.clear();
        
        long startOptimized = System.currentTimeMillis();
        Collection<Pet> petsOptimized = petRepository.findAll();
        
        // Access visits to ensure they're loaded
        int totalVisits = 0;
        for (Pet pet : petsOptimized) {
            totalVisits += pet.getVisits().size();
        }
        long endOptimized = System.currentTimeMillis();
        
        long optimizedQueries = stats.getQueryExecutionCount();
        long optimizedTime = endOptimized - startOptimized;
        
        System.out.println("=== Optimized Query Results ===");
        System.out.println("Time taken: " + optimizedTime + " ms");
        System.out.println("Total queries executed: " + optimizedQueries);
        System.out.println("Total pets: " + petsOptimized.size());
        System.out.println("Total visits loaded: " + totalVisits);
        
        // Verify the optimization worked
        assertThat(optimizedQueries).isEqualTo(1);
        assertThat(petsOptimized).isNotEmpty();
        
        stats.setStatisticsEnabled(false);
    }
}