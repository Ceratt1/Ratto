package com.learnia.ledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.learnia.tools.aws.config.S3AutoConfiguration;

@SpringBootApplication(exclude = S3AutoConfiguration.class)
public class EventLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventLedgerApplication.class, args);
    }
}
