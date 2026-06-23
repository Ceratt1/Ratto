package com.learnia.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.learnia.validation.impl.PdfFilesValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PdfFilesValidator.class)
public @interface ValidPdfFiles {
    String message() default "Invalid PDF files";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    long maxSizeMb() default 30;

    int maxFiles() default 1;
}
