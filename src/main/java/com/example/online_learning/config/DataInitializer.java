package com.example.online_learning.config;

import com.example.online_learning.entity.Category;
import com.example.online_learning.entity.Course;
import com.example.online_learning.entity.Instructor;
import com.example.online_learning.entity.Lesson;
import com.example.online_learning.entity.Student;
import com.example.online_learning.repository.CategoryRepository;
import com.example.online_learning.repository.CourseRepository;
import com.example.online_learning.repository.InstructorRepository;
import com.example.online_learning.repository.StudentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class DataInitializer {

    @Bean
    @Profile("!test")
    CommandLineRunner seedData(
            CourseRepository courseRepository,
            InstructorRepository instructorRepository,
            StudentRepository studentRepository,
            CategoryRepository categoryRepository) {
        return args -> {
            if (courseRepository.count() > 0) {
                return;
            }

            Student alex = studentRepository.save(new Student("Alex", "Novak", "alex@learn.io"));
            Student maria = studentRepository.save(new Student("Maria", "Green", "maria@learn.io"));
            Student nina = studentRepository.save(new Student("Nina", "Fox", "nina@learn.io"));

            Category backend = categoryRepository.save(new Category("Backend"));
            Category database = categoryRepository.save(new Category("Database"));
            Category devops = categoryRepository.save(new Category("DevOps"));

            Instructor instructor = instructorRepository.save(
                    new Instructor("Ivan", "Petrov", "Java Architecture"));
            Instructor secondInstructor = instructorRepository.save(
                    new Instructor("Anna", "Sidorova", "Data Engineering"));

            Course springCourse = new Course("Spring Boot Intensive", "INTERMEDIATE");
            springCourse.setInstructor(instructor);
            springCourse.addLesson(new Lesson("Spring Context", 45, 1));
            springCourse.addLesson(new Lesson("Spring Data JPA", 60, 2));
            springCourse.addStudent(alex);
            springCourse.addStudent(maria);
            springCourse.addCategory(backend);
            springCourse.addCategory(database);

            Course sqlCourse = new Course("PostgreSQL for Developers", "BEGINNER");
            sqlCourse.setInstructor(secondInstructor);
            sqlCourse.addLesson(new Lesson("Indexes", 40, 1));
            sqlCourse.addLesson(new Lesson("Transactions", 50, 2));
            sqlCourse.addStudent(maria);
            sqlCourse.addStudent(nina);
            sqlCourse.addCategory(database);
            sqlCourse.addCategory(devops);

            courseRepository.save(springCourse);
            courseRepository.save(sqlCourse);
        };
    }
}
