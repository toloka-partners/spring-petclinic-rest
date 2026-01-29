/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.samples.petclinic.utils;

import org.springframework.samples.petclinic.rest.dto.PetDto;
import org.springframework.samples.petclinic.rest.dto.PetFieldsDto;

/**
 * Utility class for setting weight on DTOs in tests.
 */
public class TestDataUtils {

    /**
     * Sets the weight on a PetDto or PetFieldsDto.
     * Since both have setWeight method, this utility handles it generically.
     */
    public static void setWeightUniversal(Object dto, Double weight) {
        if (dto instanceof PetDto) {
            ((PetDto) dto).setWeight(weight);
        } else if (dto instanceof PetFieldsDto) {
            ((PetFieldsDto) dto).setWeight(weight);
        } else {
            throw new IllegalArgumentException("Unsupported DTO type: " + dto.getClass());
        }
    }
}