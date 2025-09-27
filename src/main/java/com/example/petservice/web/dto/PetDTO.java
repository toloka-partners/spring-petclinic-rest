package main.java.com.example.petservice.web.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PetDTO {
    private Long id;
    private String name;
    // NEW
    private Double weight;
}