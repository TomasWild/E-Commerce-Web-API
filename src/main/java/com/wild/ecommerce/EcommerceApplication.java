package com.wild.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class EcommerceApplication {

    static void main(String[] args) {
        SpringApplication.run(EcommerceApplication.class, args);
    }

}
