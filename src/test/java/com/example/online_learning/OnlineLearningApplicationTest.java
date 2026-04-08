package com.example.online_learning;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

class OnlineLearningApplicationTest {

    @Test
    void mainShouldStartApplicationWithoutThrowing() {
        assertNotNull(OnlineLearningApplication.class.getAnnotation(SpringBootApplication.class));
        assertDoesNotThrow(() -> OnlineLearningApplication.main(new String[] {
            "--spring.main.web-application-type=none",
            "--spring.main.lazy-initialization=true",
            "--spring.main.register-shutdown-hook=false",
            "--spring.profiles.active=test",
            "--springdoc.api-docs.enabled=false",
            "--springdoc.swagger-ui.enabled=false"
        }));
    }
}
