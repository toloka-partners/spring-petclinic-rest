# Cache Management Documentation

## Overview

The Spring PetClinic REST API now includes comprehensive caching functionality to improve performance and reduce database load. The caching implementation includes automatic cache eviction on entity updates and scheduled cache invalidation.

## Caching Strategy

### Cache Configuration

The application uses Spring Cache abstraction with a `ConcurrentMapCacheManager` for simplicity. The cache configuration is defined in `CacheConfig.java`:

- **Cache Names**:
  - `vets` - Veterinarian data
  - `pets` - Pet data  
  - `petTypes` - Pet type data
  - `owners` - Owner data
  - `visits` - Visit data
  - `specialties` - Specialty data

### Cached Methods

The following service methods are cached in `ClinicServiceImpl`:

#### Read Operations (Cacheable)
- `findAllVets()` - Caches all veterinarians
- `findVetById(int id)` - Caches individual veterinarians by ID
- `findVets()` - Caches all veterinarians (alternative method)
- `findAllOwners()` - Caches all owners
- `findOwnerById(int id)` - Caches individual owners by ID
- `findOwnerByLastName(String lastName)` - Caches owners by last name
- `findAllPets()` - Caches all pets
- `findPetById(int id)` - Caches individual pets by ID
- `findAllPetTypes()` - Caches all pet types
- `findPetTypeById(int petTypeId)` - Caches individual pet types by ID
- `findPetTypes()` - Caches all pet types (alternative method)
- `findAllVisits()` - Caches all visits
- `findVisitById(int visitId)` - Caches individual visits by ID
- `findVisitsByPetId(int petId)` - Caches visits by pet ID
- `findAllSpecialties()` - Caches all specialties
- `findSpecialtyById(int specialtyId)` - Caches individual specialties by ID
- `findSpecialtiesByNameIn(Set<String> names)` - Caches specialties by names

#### Write Operations (Cache Eviction)
All save/delete operations automatically evict related cache entries:

- `saveVet(Vet vet)` - Evicts `vets` cache
- `deleteVet(Vet vet)` - Evicts `vets` cache
- `saveOwner(Owner owner)` - Evicts `owners` cache
- `deleteOwner(Owner owner)` - Evicts `owners` cache
- `savePet(Pet pet)` - Evicts `pets` cache
- `deletePet(Pet pet)` - Evicts `pets` cache
- `savePetType(PetType petType)` - Evicts `petTypes` cache
- `deletePetType(PetType petType)` - Evicts `petTypes` cache
- `saveVisit(Visit visit)` - Evicts `visits` cache
- `deleteVisit(Visit visit)` - Evicts `visits` cache
- `saveSpecialty(Specialty specialty)` - Evicts `specialties` cache
- `deleteSpecialty(Specialty specialty)` - Evicts `specialties` cache

### Cache Keys

Cache keys are strategically designed for optimal cache utilization:

- Simple method calls use default key generation
- ID-based queries use `key = "#id"`
- Complex queries use descriptive keys like `'by-lastname-' + #lastName`
- Collection-based methods use fixed keys like `'all-vets'`

## Cache Invalidation

### Automatic Eviction

Cache entries are automatically evicted when relevant entities are updated through the `@CacheEvict` annotations on write operations. This ensures data consistency by removing potentially stale cached data whenever the underlying data changes.

### Scheduled Invalidation

A scheduled task runs every hour (3600000ms) to clear all caches, providing additional cache management flexibility. This is configured in `CacheConfig.evictAllCaches()`.

## Admin Cache Management

### Manual Cache Clearing Endpoint

**Endpoint**: `DELETE /api/cache`

**Description**: Clears all application caches manually

**Response**: 
```json
{
  "message": "All caches cleared successfully"
}
```

**Error Response**:
```json
{
  "error": "Failed to clear caches: [error message]"
}
```

### Usage Examples

#### Clear all caches using curl:
```bash
curl -X DELETE http://localhost:8080/api/cache
```

#### Clear all caches using REST client:
```http
DELETE /api/cache HTTP/1.1
Host: localhost:8080
```

## Testing

### Integration Tests

Comprehensive integration tests verify cache behavior:

- **CacheIntegrationTests**: Tests cache functionality for all entity types
- **CacheAdminRestControllerTests**: Tests the admin cache clearing endpoint

### Test Coverage

Tests verify:
- Cache population on read operations
- Cache eviction on write operations
- Manual cache clearing via admin endpoint
- Cache manager configuration

## Performance Benefits

- **Reduced Database Load**: Frequently accessed data is served from memory
- **Improved Response Times**: Cached data eliminates database query overhead
- **Scalability**: Better resource utilization under high load

## Cache Monitoring

To monitor cache usage and performance:

1. Enable Spring Boot Actuator metrics for caches
2. Use cache statistics to track hit/miss ratios
3. Monitor memory usage for cache storage
4. Consider implementing custom cache metrics

## Configuration Options

### Environment-Specific Cache Configuration

For production environments, consider:

- Using Redis or Hazelcast for distributed caching
- Configuring TTL (Time To Live) for cache entries
- Setting maximum cache sizes to prevent memory issues
- Implementing cache statistics and monitoring

### Development and Testing

- Use simple `ConcurrentMapCache` (current configuration)
- Clear caches between tests
- Enable cache debug logging if needed

## Best Practices

1. **Cache Key Strategy**: Use meaningful, unique cache keys
2. **Eviction Policy**: Always evict related caches on data updates
3. **Testing**: Include cache behavior in integration tests
4. **Monitoring**: Track cache performance and hit ratios
5. **Documentation**: Keep cache configuration documented and up-to-date

## Troubleshooting

### Common Issues

1. **Stale Data**: Ensure cache eviction on all update operations
2. **Memory Usage**: Monitor cache sizes and implement eviction policies
3. **Testing**: Clear caches between test runs to avoid interference
4. **Performance**: Monitor cache hit ratios and adjust strategy accordingly

### Cache Debugging

Enable cache debug logging:
```properties
logging.level.org.springframework.cache=DEBUG
```