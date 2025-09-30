package com.example.petservice.service.mapper;

import com.example.petservice.domain.Pet;
import com.example.petservice.web.dto.PetDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface PetMapper {

    // Optional: manual mapper instance
    PetMapper INSTANCE = Mappers.getMapper(PetMapper.class);

    PetDTO toDto(Pet pet);

    Pet toEntity(PetDTO dto);
}
