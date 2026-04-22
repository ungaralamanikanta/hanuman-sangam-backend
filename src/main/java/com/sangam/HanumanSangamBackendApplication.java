package com.sangam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // ← Add this
public class HanumanSangamApplication {
    public static void main(String[] args) {
        SpringApplication.run(HanumanSangamApplication.class, args);
    }
}
