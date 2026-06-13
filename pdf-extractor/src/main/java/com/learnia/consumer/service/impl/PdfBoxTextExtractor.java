package com.learnia.consumer.service.impl;

import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import com.learnia.consumer.service.PdfTextExtractor;

@Service
public class PdfBoxTextExtractor implements PdfTextExtractor {

    @Override
    public String extract(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            String text = new PDFTextStripper().getText(document);
            if (text == null || text.isBlank()) {
                throw new IllegalStateException("PDF does not contain extractable text");
            }
            return text;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not extract text from PDF", exception);
        }
    }
}
