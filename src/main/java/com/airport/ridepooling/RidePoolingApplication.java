package com.airport.ridepooling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RidePoolingApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(RidePoolingApplication.class, args);
        System.out.println("\n=================================================");
        System.out.println("Ride Pooling Backend Started Successfully!");
        System.out.println("=================================================");
        System.out.println("API Base URL: http://localhost:8080");
        System.out.println("Swagger UI: http://localhost:8080/swagger-ui.html");
        System.out.println("H2 Console: http://localhost:8080/h2-console");
        System.out.println("=================================================\n");
    }
}
