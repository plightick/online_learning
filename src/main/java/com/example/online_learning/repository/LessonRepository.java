package com.example.online_learning.repository;

import com.example.online_learning.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    long countByCourseTitleIgnoreCase(String title);
}
