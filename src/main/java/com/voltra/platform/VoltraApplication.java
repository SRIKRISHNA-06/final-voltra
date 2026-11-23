package com.voltra.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VoltraApplication {
    public static void main(String[] args) {
        SpringApplication.run(VoltraApplication.class, args);
    }
}
