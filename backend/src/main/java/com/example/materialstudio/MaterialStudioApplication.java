package com.example.materialstudio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MaterialStudioApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaterialStudioApplication.class, args);
    }
}
