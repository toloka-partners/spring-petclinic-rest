package org.springframework.samples.petclinic.model;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PetWeightTest {

    @Test
    public void testPetWeightField() {
        // Test that weight field exists and works
        Pet pet = new Pet();
        
        // Test setting weight
        BigDecimal weight = new BigDecimal("15.75");
        pet.setWeight(weight);
        
        // Test getting weight
        assertEquals(weight, pet.getWeight());
        
        // Test null weight
        pet.setWeight(null);
        assertNull(pet.getWeight());
    }
}
