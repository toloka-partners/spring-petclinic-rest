/*
 * Copyright 2002-2024 the original author or authors.
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
package org.springframework.samples.petclinic.rest.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.util.SqlQueryCountInterceptor;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests to verify N+1 query problems have been fixed.
 * These tests use real database connections and measure actual SQL query counts.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "petclinic.security.enable=false"
})
public class NPlusOneFixedQueryTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/petclinic/api";
        SqlQueryCountInterceptor.reset();
    }

    @Test
    void testGetAllOwnersWithPetsOptimized() {
        // Start counting SQL queries
        SqlQueryCountInterceptor.startCounting();

        // Make REST API call to get all owners (should now use JOIN FETCH)
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
            baseUrl + "/owners", JsonNode.class);

        // Get the query count
        int queryCount = SqlQueryCountInterceptor.getQueryCount();

        // Verify the response is successful
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        JsonNode owners = response.getBody();
        assertThat(owners.isArray()).isTrue();
        assertThat(owners.size()).isGreaterThan(0);

        // Log the results for documentation
        System.out.println("=== FIXED N+1 Problem Test Results ===");
        System.out.println("Number of owners: " + owners.size());
        System.out.println("Total SQL queries executed: " + queryCount);
        System.out.println("Expected queries with JOIN FETCH: 1-2 queries");
        System.out.println("Previous N+1 queries: " + (owners.size() + 1) + " queries");

        // Verify that we have significantly fewer queries than the N+1 pattern
        // With JOIN FETCH, we should have 1-2 queries instead of 11
        assertThat(queryCount).isLessThanOrEqualTo(2);
        assertThat(queryCount).isGreaterThan(0);

        SqlQueryCountInterceptor.reset();
    }

    @Test
    void testGetAllPetsWithVisitsOptimized() {
        // Start counting SQL queries
        SqlQueryCountInterceptor.startCounting();

        // Make REST API call to get all pets (should now use JOIN FETCH)
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
            baseUrl + "/pets", JsonNode.class);

        // Get the query count
        int queryCount = SqlQueryCountInterceptor.getQueryCount();

        // Verify the response is successful
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        JsonNode pets = response.getBody();
        assertThat(pets.isArray()).isTrue();
        assertThat(pets.size()).isGreaterThan(0);

        // Log the results for documentation
        System.out.println("=== FIXED Pet-Visit N+1 Problem Test Results ===");
        System.out.println("Number of pets: " + pets.size());
        System.out.println("Total SQL queries executed: " + queryCount);
        System.out.println("Expected queries with JOIN FETCH: 1-2 queries");
        System.out.println("Previous N+1 queries: " + (pets.size() + 1) + " queries");

        // Verify that we have significantly fewer queries than the N+1 pattern
        assertThat(queryCount).isLessThanOrEqualTo(2);
        assertThat(queryCount).isGreaterThan(0);

        SqlQueryCountInterceptor.reset();
    }

    @Test
    void testGetSingleOwnerWithPetsOptimized() {
        // Start counting SQL queries
        SqlQueryCountInterceptor.startCounting();

        // Make REST API call to get a single owner (should now use JOIN FETCH)
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
            baseUrl + "/owners/1", JsonNode.class);

        // Get the query count
        int queryCount = SqlQueryCountInterceptor.getQueryCount();

        // Verify the response is successful
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        JsonNode owner = response.getBody();

        // Log the results for documentation
        System.out.println("=== FIXED Single Owner N+1 Problem Test Results ===");
        System.out.println("Owner ID: " + owner.get("id").asInt());
        JsonNode pets = owner.get("pets");
        System.out.println("Number of pets: " + (pets != null ? pets.size() : 0));
        System.out.println("Total SQL queries executed: " + queryCount);
        System.out.println("Expected queries with JOIN FETCH: 1 query");

        // For a single owner, we should have exactly 1 query with JOIN FETCH
        assertThat(queryCount).isEqualTo(1);

        SqlQueryCountInterceptor.reset();
    }
}