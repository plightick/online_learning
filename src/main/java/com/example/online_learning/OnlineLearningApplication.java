package com.example.online_learning;

import com.example.online_learning.entity.Course;
import com.example.online_learning.repository.CourseRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class OnlineLearningApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnlineLearningApplication.class, args);
    }

    @Bean
    public CommandLineRunner seedCourses(CourseRepository courseRepository) {
        return args -> {
            if (courseRepository.count() == 0) {
                courseRepository.save(new Course("Java Fundamentals", "Petrov Ivan", "BEGINNER"));
                courseRepository.save(new Course("Spring Boot API", "Sidorova Anna", "INTERMEDIATE"));
                courseRepository.save(new Course("Algorithms for Interviews", "Kuznetsov Oleg", "ADVANCED"));
            }
        };
    }

}
