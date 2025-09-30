package com.example.petservice.web.rest;

import com.example.petservice.service.PetService;
import com.example.petservice.service.mapper.PetMapper;
import com.example.petservice.web.dto.PetDTO;
import com.example.petservice.domain.Pet;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pets")
public class PetController {

    private final PetService petService;
    private final PetMapper petMapper;

    public PetController(PetService petService, PetMapper petMapper) {
        this.petService = petService;
        this.petMapper = petMapper;
    }

    // Create a new pet
    @PostMapping
    public ResponseEntity<PetDTO> createPet(@RequestBody PetDTO dto) {
        Pet pet = petMapper.toEntity(dto);
        Pet saved = petService.save(pet);
        PetDTO result = petMapper.toDto(saved);
        return ResponseEntity.created(URI.create("/api/pets/" + result.getId()))
                .body(result);
    }

    // Get all pets
    @GetMapping
    public ResponseEntity<List<PetDTO>> getAll() {
        List<PetDTO> pets = petService.findAll()
                .stream()
                .map(petMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(pets);
    }

    // Get a pet by ID
    @GetMapping("/{id}")
    public ResponseEntity<PetDTO> getOne(@PathVariable Long id) {
        Pet pet = petService.findOne(id);
        if (pet == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(petMapper.toDto(pet));
    }
}
