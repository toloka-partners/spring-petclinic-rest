package org.springframework.samples.petclinic.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * @author Michael Isvy
 *         Simple test to make sure that Bean Validation is working
 *         (useful when upgrading to a new version of Hibernate Validator/ Bean Validation)
 */
class ValidatorTests {

    private Validator createValidator() {
        LocalValidatorFactoryBean localValidatorFactoryBean = new LocalValidatorFactoryBean();
        localValidatorFactoryBean.afterPropertiesSet();
        return localValidatorFactoryBean;
    }

    @Test
    void shouldNotValidateWhenFirstNameEmpty() {

        LocaleContextHolder.setLocale(Locale.ENGLISH);
        Person person = new Person();
        person.setFirstName("");
        person.setLastName("smith");

        Validator validator = createValidator();
        Set<ConstraintViolation<Person>> constraintViolations = validator.validate(person);

        assertThat(constraintViolations.size()).isEqualTo(1);
        ConstraintViolation<Person> violation = constraintViolations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("firstName");
        assertThat(violation.getMessage()).isEqualTo("must not be empty");
    }

    @Test
    void shouldValidatePetWithValidWeight() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        Pet pet = new Pet();
        pet.setName("Fluffy");
        pet.setWeight(15.5);

        Validator validator = createValidator();
        Set<ConstraintViolation<Pet>> constraintViolations = validator.validate(pet);

        // Should have violations for missing fields but not for weight
        boolean hasWeightViolation = constraintViolations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("weight"));
        assertThat(hasWeightViolation).isFalse();
    }

    @Test
    void shouldValidatePetWithNullWeight() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        Pet pet = new Pet();
        pet.setName("Fluffy");
        pet.setWeight(null);

        Validator validator = createValidator();
        Set<ConstraintViolation<Pet>> constraintViolations = validator.validate(pet);

        // Should not have violations for null weight (it's optional)
        boolean hasWeightViolation = constraintViolations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("weight"));
        assertThat(hasWeightViolation).isFalse();
    }

}
