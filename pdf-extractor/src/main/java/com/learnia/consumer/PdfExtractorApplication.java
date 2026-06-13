package com.learnia.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.learnia")
public class PdfExtractorApplication {

	public static void main(String[] args) {
		SpringApplication.run(PdfExtractorApplication.class, args);
	}

}
