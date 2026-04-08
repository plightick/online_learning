package com.example.online_learning.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EntityModelTest {

    @Test
    void categoryShouldSupportProtectedConstructorAndSetter() {
        Category category = new Category();
        category.setName("Backend");

        assertNull(category.getId());
        assertEquals("Backend", category.getName());
        assertTrue(category.getCourses().isEmpty());
    }

    @Test
    void instructorShouldSupportProtectedConstructorAndSetters() {
        Instructor instructor = new Instructor();
        instructor.setFirstName("Jane");
        instructor.setLastName("Doe");
        instructor.setSpecialization("Architecture");

        assertEquals("Jane", instructor.getFirstName());
        assertEquals("Doe", instructor.getLastName());
        assertEquals("Architecture", instructor.getSpecialization());
        assertTrue(instructor.getCourses().isEmpty());
    }

    @Test
    void lessonShouldSupportProtectedConstructorAndSetters() {
        Lesson lesson = new Lesson();
        Course course = new Course("Java", "ADVANCED");
        lesson.setTitle("Intro");
        lesson.setDurationMinutes(45);
        lesson.setLessonOrder(1);
        lesson.setCourse(course);

        assertEquals("Intro", lesson.getTitle());
        assertEquals(45, lesson.getDurationMinutes());
        assertEquals(1, lesson.getLessonOrder());
        assertSame(course, lesson.getCourse());
    }

    @Test
    void courseSetInstructorShouldHandleSameInstructorExistingRelationAndNull() {
        Course course = new Course("Spring", "ADVANCED");
        Instructor firstInstructor = new Instructor("Jane", "Doe", "Backend");
        Instructor secondInstructor = new Instructor("John", "Smith", "DevOps");

        course.setInstructor(firstInstructor);
        course.setInstructor(firstInstructor);

        assertSame(firstInstructor, course.getInstructor());
        assertEquals(1, firstInstructor.getCourses().size());

        secondInstructor.getCourses().add(course);
        course.setInstructor(secondInstructor);

        assertSame(secondInstructor, course.getInstructor());
        assertTrue(firstInstructor.getCourses().isEmpty());
        assertEquals(1, secondInstructor.getCourses().size());

        course.setInstructor(null);

        assertNull(course.getInstructor());
        assertTrue(secondInstructor.getCourses().isEmpty());
    }
}
