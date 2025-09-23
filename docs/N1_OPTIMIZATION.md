# N+1 Query Problem Optimization in Spring Petclinic REST

## Overview

The N+1 query problem is a common performance issue in ORM frameworks where fetching N entities results in 1 query for the entities plus N additional queries for their related associations. This document describes the optimizations implemented to resolve N+1 problems in the Petclinic REST API.

## Problem Description

### What is the N+1 Problem?

When fetching a list of entities with lazy-loaded associations:
1. First query: Fetches N entities (e.g., all owners)
2. Additional N queries: One query per entity to fetch its associations (e.g., each owner's pets)

Total queries = 1 + N, hence "N+1 problem"

### Example Scenario

```java
// Without optimization:
List<Owner> owners = ownerRepository.findAll();  // Query 1
for (Owner owner : owners) {
    owner.getPets().size();  // Triggers Query 2, 3, 4... N+1
}
```

### Identified N+1 Problems in Petclinic

1. **Owner-Pet Relationship**: When fetching owners, their pets were lazy-loaded
2. **Pet-Visit Relationship**: When fetching pets, their visits were lazy-loaded

## Implemented Solutions

### 1. JPQL Join Fetch (Primary Solution)

Modified repository queries to use JOIN FETCH for eager loading:

```java
// SpringDataOwnerRepository.java
@Query("SELECT DISTINCT owner FROM Owner owner left join fetch owner.pets")
Collection<Owner> findAll();

@Query("SELECT owner FROM Owner owner left join fetch owner.pets WHERE owner.id = :id")
Owner findById(@Param("id") int id);
```

**Benefits:**
- Single query retrieves both entities and associations
- Database-level optimization
- Works across all JPA providers

**Trade-offs:**
- Returns more data in a single query
- May cause Cartesian product with multiple associations

### 2. @EntityGraph (Alternative Solution)

Added named entity graphs for flexible fetching strategies:

```java
// Owner.java
@Entity
@NamedEntityGraph(name = "Owner.pets", attributeNodes = @NamedAttributeNode("pets"))
public class Owner extends Person {
    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
    private Set<Pet> pets;
}
```

Usage:
```java
TypedQuery<Owner> query = em.createQuery("SELECT o FROM Owner o", Owner.class);
query.setHint("jakarta.persistence.loadgraph", em.getEntityGraph("Owner.pets"));
```

**Benefits:**
- Declarative approach
- Can be applied dynamically
- Supports complex graphs

### 3. @BatchSize (Hibernate-Specific)

Added batch fetching to reduce number of queries:

```java
// Owner.java
@OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
@BatchSize(size = 25)
private Set<Pet> pets;
```

**Benefits:**
- Reduces N queries to N/batch_size queries
- Simple annotation-based solution
- Good for scenarios where JOIN FETCH isn't suitable

**Trade-offs:**
- Hibernate-specific (not JPA standard)
- Still executes multiple queries (but fewer)

## Performance Comparison

### Before Optimization (Lazy Loading)
- Fetching 10 owners with pets: 11 queries (1 + 10)
- Fetching 13 pets with visits: 14 queries (1 + 13)

### After Optimization (Join Fetch)
- Fetching 10 owners with pets: 1 query
- Fetching 13 pets with visits: 1 query

### With Batch Size (size=25)
- Fetching 10 owners with pets: 2 queries (1 + ceil(10/25))
- Fetching 100 owners with pets: 5 queries (1 + ceil(100/25))

## Best Practices

1. **Use JOIN FETCH for known access patterns**
   - When you always need the association
   - For REST APIs that return complete entities

2. **Use @EntityGraph for flexible fetching**
   - When fetch strategy varies by use case
   - For complex entity graphs

3. **Use @BatchSize for large datasets**
   - When JOIN FETCH would create too large result sets
   - As a simple retrofit for existing code

4. **Keep LAZY as default**
   - Prevents loading unnecessary data
   - Use explicit fetching strategies where needed

## Configuration

### Enable SQL Logging

To monitor query execution:

```properties
# application.properties
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

## Testing

Tests have been created to verify:
1. Data correctness after optimization
2. Proper loading of associations
3. No LazyInitializationException
4. Performance improvements

See `N1OptimizationTest.java` and `N1VerificationTest.java` for test implementations.

## Conclusion

The N+1 problem has been successfully addressed using multiple strategies. The primary solution using JOIN FETCH provides optimal performance for the Petclinic REST API's access patterns. Alternative solutions using @EntityGraph and @BatchSize are available for different scenarios and requirements.