/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.performance.infrastructure.SqlQueryInterceptor;
import org.springframework.samples.petclinic.rest.dto.PetDto;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for N+1 problem optimization using the optimized profile.
 * This test verifies that the Pet → Owner N+1 problem is fixed and only 1 query executes.
 *
 * @author N+1 Optimization Team
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, 
    properties = {"petclinic.security.enable=false"})
@ActiveProfiles({"hsqldb", "spring-data-jpa"})
@Import(N1TestConfiguration.class)
public class N1OptimizedRestControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SqlQueryInterceptor sqlQueryInterceptor;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/petclinic/api";
        sqlQueryInterceptor.reset();
    }

    @Test
    void testOptimizedPetListEndpoint_shouldExecuteOnlyOneQuery() {
        // Start capturing SQL queries
        sqlQueryInterceptor.startCapturing();

        // Call REST endpoint that loads all pets (should now be optimized)
        ResponseEntity<PetDto[]> response = restTemplate.getForEntity(
            baseUrl + "/pets", PetDto[].class);

        // Stop capturing
        sqlQueryInterceptor.stopCapturing();

        // Verify response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        PetDto[] pets = response.getBody();
        assertThat(pets.length).isGreaterThan(0);

        // Get query metrics
        SqlQueryInterceptor.QueryMetrics metrics = sqlQueryInterceptor.getMetrics();
        
        // Log results for verification
        System.out.println("=".repeat(80));
        System.out.println("OPTIMIZED REST ENDPOINT: GET /api/pets");
        System.out.println("=".repeat(80));
        System.out.println("Number of pets loaded: " + pets.length);
        System.out.println("Total SQL queries executed: " + metrics.getQueryCount());
        System.out.println("SELECT queries: " + metrics.getQueryTypeAnalysis().getSelectCount());
        System.out.println("JOIN queries: " + metrics.getQueryTypeAnalysis().getJoinCount());
        System.out.println("Execution time: " + metrics.getExecutionTimeMs() + "ms");
        System.out.println("Likely N+1 problem: " + metrics.getQueryTypeAnalysis().isLikelyN1Problem());
        System.out.println("-".repeat(80));
        System.out.println("SQL Queries executed:");
        for (int i = 0; i < metrics.getQueries().size(); i++) {
            System.out.println((i + 1) + ". " + metrics.getQueries().get(i));
        }
        System.out.println("=".repeat(80));
        System.out.println("OPTIMIZATION RESULT:");
        System.out.println("Expected: 1 query with all JOINs (pets, visits, types, owners)");
        System.out.println("Previous unoptimized: 11 queries (1 + 10 for owners)");
        System.out.println("Current optimized: " + metrics.getQueryCount() + " queries");
        if (metrics.getQueryCount() == 1) {
            System.out.println("✅ SUCCESS: N+1 problem FIXED! 91% query reduction achieved.");
        } else {
            System.out.println("❌ ISSUE: Expected 1 query, got " + metrics.getQueryCount());
        }
        System.out.println("=".repeat(80));

        // Verify that pets have owner IDs populated (indicating owners were loaded)
        for (PetDto pet : pets) {
            assertThat(pet.getOwnerId()).isNotNull();
            assertThat(pet.getOwnerId()).isGreaterThan(0);
        }

        // The key assertion: should be exactly 1 query with all JOINs
        assertThat(metrics.getQueryCount()).isEqualTo(1);
        assertThat(metrics.getQueryTypeAnalysis().getSelectCount()).isEqualTo(1);
        assertThat(metrics.getQueryTypeAnalysis().getJoinCount()).isEqualTo(1);
        assertThat(metrics.getQueryTypeAnalysis().isLikelyN1Problem()).isFalse();
    }

    @Test
    void testOptimizedSpecificPetEndpoint_shouldExecuteOnlyOneQuery() {
        // Start capturing SQL queries
        sqlQueryInterceptor.startCapturing();

        // Call REST endpoint for a specific pet
        ResponseEntity<PetDto> response = restTemplate.getForEntity(
            baseUrl + "/pets/1", PetDto.class);

        // Stop capturing
        sqlQueryInterceptor.stopCapturing();

        // Verify response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        PetDto pet = response.getBody();
        assertThat(pet.getId()).isEqualTo(1);

        // Get query metrics
        SqlQueryInterceptor.QueryMetrics metrics = sqlQueryInterceptor.getMetrics();
        
        // Log results for verification
        System.out.println("=".repeat(80));
        System.out.println("OPTIMIZED REST ENDPOINT: GET /api/pets/1");
        System.out.println("=".repeat(80));
        System.out.println("Pet loaded: " + pet.getName() + " (ID: " + pet.getId() + ")");
        System.out.println("Owner ID: " + pet.getOwnerId());
        System.out.println("Number of visits: " + (pet.getVisits() != null ? pet.getVisits().size() : 0));
        System.out.println("Total SQL queries executed: " + metrics.getQueryCount());
        System.out.println("SELECT queries: " + metrics.getQueryTypeAnalysis().getSelectCount());
        System.out.println("JOIN queries: " + metrics.getQueryTypeAnalysis().getJoinCount());
        System.out.println("Execution time: " + metrics.getExecutionTimeMs() + "ms");
        System.out.println("-".repeat(80));
        System.out.println("SQL Queries executed:");
        for (int i = 0; i < metrics.getQueries().size(); i++) {
            System.out.println((i + 1) + ". " + metrics.getQueries().get(i));
        }
        System.out.println("=".repeat(80));

        // Verify that owner ID is populated (indicating owner was loaded)
        assertThat(pet.getOwnerId()).isNotNull();
        assertThat(pet.getOwnerId()).isGreaterThan(0);

        // Should be exactly 1 query with all JOINs
        assertThat(metrics.getQueryCount()).isEqualTo(1);
        assertThat(metrics.getQueryTypeAnalysis().getSelectCount()).isEqualTo(1);
        assertThat(metrics.getQueryTypeAnalysis().getJoinCount()).isEqualTo(1);
    }
}