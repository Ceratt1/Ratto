package com.learnia.validation.impl;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.learnia.validation.ValidPdfFiles;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

@Component
public class PdfFilesValidator implements ConstraintValidator<ValidPdfFiles, List<FilePart>> {
    private long maxSizeBytes;
    private int maxFiles;

    @Override
    public void initialize(ValidPdfFiles constraintAnnotation) {
        this.maxSizeBytes = constraintAnnotation.maxSizeMb() * 1024L * 1024L;
        this.maxFiles = constraintAnnotation.maxFiles();
    }

    @Override
    public boolean isValid(List<FilePart> files, ConstraintValidatorContext context) {
        if (files == null || files.isEmpty()) {
            return violation(context, "At least one PDF is required");
        }
        if (files.size() > maxFiles) {
            return violation(context, "Send at most " + maxFiles + " PDF");
        }

        for (FilePart file : files) {
            String filename = file.filename();
            MediaType contentType = file.headers().getContentType();
            long contentLength = file.headers().getContentLength();

            boolean validName = StringUtils.hasText(filename) && filename.toLowerCase().endsWith(".pdf");
            boolean validContentType = MediaType.APPLICATION_PDF.equals(contentType);

            if (!validName || !validContentType) {
                return violation(context, "Only .pdf files are accepted");
            }

            if (contentLength > maxSizeBytes) {
                return violation(context, "Each PDF must have at most " + (maxSizeBytes / (1024L * 1024L)) + " MB");
            }
        }

        return true;
    }

    private boolean violation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
