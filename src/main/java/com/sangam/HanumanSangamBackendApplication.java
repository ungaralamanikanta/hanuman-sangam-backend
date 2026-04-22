package com.sangam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // ← Add this
public class HanumanSangamBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(HanumanSangamBackendApplication.class, args);
    }
}
