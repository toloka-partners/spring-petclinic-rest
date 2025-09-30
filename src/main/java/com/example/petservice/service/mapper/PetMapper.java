package main.java.com.example.petservice.service.mapper;


import com.example.petservice.domain.Pet;
import com.example.petservice.web.dto.PetDTO;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface PetMapper {
PetDTO toDto(Pet pet);
Pet toEntity(PetDTO dto);
}