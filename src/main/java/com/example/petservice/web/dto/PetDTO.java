package com.example.petservice.web.dto;

public class PetDTO {
    private Long id;
    private String name;
    private Double weight;

    public PetDTO() {
    }

    public PetDTO(Long id, String name, Double weight) {
        this.id = id;
        this.name = name;
        this.weight = weight;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }
}
