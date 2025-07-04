package com.datn.datn.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;
@Documented
@Constraint(validatedBy = MinAgeValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface MinAge {
    String message() default "Phải đủ ít nhất {value} tuổi";
    int value() default 16;

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}