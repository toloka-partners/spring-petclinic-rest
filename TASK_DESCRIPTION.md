# Task Description
Analyze and fix the N+1 select problem in JPA entity relationships within the Petclinic REST API, focusing on associations like Owner-Pet and Pet-Visit. Use SQL logging to pinpoint inefficiencies, reproduce the issue, and then resolve it using advanced fetching techniques such as JPQL join fetch, @EntityGraph, or @BatchSize. The objective is to minimize unnecessary database queries while preserving correct business logic and data integrity. Document all changes, performance improvements, and alternative approaches for future optimization.

## Acceptance Criteria

A reproducible N+1 select problem is demonstrated in the application with SQL logging.
Entity fetching is optimized using at least one JPA/Hibernate technique, reducing the number of executed queries.
Correctness of the returned data is verified by tests.
Performance improvement is measured and documented.
Documentation explains the N+1 problem, the optimization approach used, and any alternatives considered.
All changes comply with project standards and pass tests.


# Technical Design

## Implementation

- Manually count in logs how many queries execute when REST controller is accessing entities with collections which have N+1 problem (Owner.pets and Pet.visits)
- Record the count in the md file
- Fix N+1 problem for the affected entities
- Add sql query interceptor to count number of sql queries
- Tests should access the REST controller which accesses fixed entities and count the number of SQL queries
- Compare results in md file with results from running tests

## Testing

- Use real `@SpringBootTest` integration tests using actual database
- Execute identical REST API calls using `TestRestTemplate`
- Record number of SQL queries for each test
