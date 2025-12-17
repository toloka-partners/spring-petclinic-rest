package org.springframework.samples.petclinic.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.rest.dto.PetDto;
import org.springframework.samples.petclinic.rest.dto.PetFieldsDto;
import org.springframework.samples.petclinic.rest.dto.PetTypeDto;

import java.math.BigDecimal;
import java.util.Collection;

/**
 * Map Pet & PetDto using mapstruct
 */
@Mapper(uses = VisitMapper.class)
public interface PetMapper {

    // ENTITY -> DTO
    @Mapping(source = "owner.id", target = "ownerId")
    PetDto toPetDto(Pet pet);

    Collection<PetDto> toPetsDto(Collection<Pet> pets);

    // DTO -> ENTITY
    @Mapping(source = "ownerId", target = "owner.id")
    Pet toPet(PetDto petDto);

    Collection<Pet> toPets(Collection<PetDto> petsDto);

    // FIELDS DTO -> ENTITY (POST / PUT payloads)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "visits", ignore = true)
    Pet toPet(PetFieldsDto petFieldsDto);

    // PetType mappings (unchanged)
    PetTypeDto toPetTypeDto(PetType petType);

    PetType toPetType(PetTypeDto petTypeDto);

    Collection<PetTypeDto> toPetTypeDtos(Collection<PetType> petTypes);

    // Helper methods for BigDecimal <-> Double conversion
    default Double map(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    default BigDecimal map(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
