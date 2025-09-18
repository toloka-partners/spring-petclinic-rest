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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.samples.petclinic.util.SqlQueryCountInterceptor;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests to demonstrate and fix N+1 query problems.
 * These tests use real database connections and measure actual SQL query counts.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "petclinic.security.enable=false"
})
public class NPlusOneQueryTests {

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
    void testGetAllOwnersWithPetsNPlusOneProblem() {
        // Start counting SQL queries
        SqlQueryCountInterceptor.startCounting();

        // Make REST API call to get all owners (which will load pets due to EAGER fetching)
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
        System.out.println("=== N+1 Problem Test Results ===");
        System.out.println("Number of owners: " + owners.size());
        System.out.println("Total SQL queries executed: " + queryCount);
        System.out.println("Expected queries without N+1: 1 (single query to fetch all owners with pets)");
        System.out.println("Actual queries with N+1: " + queryCount + " (1 for owners + N for each owner's pets)");

        // Document the N+1 problem - we expect more queries than optimal
        // With N+1 problem: 1 query for owners + 1 query per owner for pets
        // Without N+1 problem: 1 query with JOIN to fetch owners and pets together
        assertThat(queryCount).isGreaterThan(1);

        SqlQueryCountInterceptor.reset();
    }

    @Test
    void testGetAllPetsWithVisitsNPlusOneProblem() {
        // Start counting SQL queries
        SqlQueryCountInterceptor.startCounting();

        // Make REST API call to get all pets (which will load visits due to EAGER fetching)
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
        System.out.println("=== Pet-Visit N+1 Problem Test Results ===");
        System.out.println("Number of pets: " + pets.size());
        System.out.println("Total SQL queries executed: " + queryCount);
        System.out.println("Expected queries without N+1: 1 (single query to fetch all pets with visits)");
        System.out.println("Actual queries with N+1: " + queryCount + " (1 for pets + N for each pet's visits)");

        // Document the N+1 problem - we expect more queries than optimal
        assertThat(queryCount).isGreaterThan(1);

        SqlQueryCountInterceptor.reset();
    }

    @Test
    void testGetSingleOwnerWithPets() {
        // Start counting SQL queries
        SqlQueryCountInterceptor.startCounting();

        // Make REST API call to get a single owner (which will load pets due to EAGER fetching)
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
            baseUrl + "/owners/1", JsonNode.class);

        // Get the query count
        int queryCount = SqlQueryCountInterceptor.getQueryCount();

        // Verify the response is successful
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        JsonNode owner = response.getBody();

        // Log the results for documentation
        System.out.println("=== Single Owner N+1 Problem Test Results ===");
        System.out.println("Owner ID: " + owner.get("id").asInt());
        JsonNode pets = owner.get("pets");
        System.out.println("Number of pets: " + (pets != null ? pets.size() : 0));
        System.out.println("Total SQL queries executed: " + queryCount);

        SqlQueryCountInterceptor.reset();
    }
}