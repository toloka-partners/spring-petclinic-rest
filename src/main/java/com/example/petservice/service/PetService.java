package com.example.petservice.service;

import com.example.petservice.domain.Pet;
import com.example.petservice.repository.PetRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PetService {

    private final PetRepository repo;

    public PetService(PetRepository repo) {
        this.repo = repo;
    }

    public Pet save(Pet pet) {
        return repo.save(pet);
    }

    public List<Pet> findAll() {
        return repo.findAll();
    }

    public Pet findOne(Long id) {
        return repo.findById(id).orElse(null);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}
