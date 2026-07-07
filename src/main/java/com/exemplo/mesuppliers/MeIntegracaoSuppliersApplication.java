package com.exemplo.mesuppliers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MeIntegracaoSuppliersApplication {
    public static void main(String[] args) {
        SpringApplication.run(MeIntegracaoSuppliersApplication.class, args);
    }
}
