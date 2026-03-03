package com.example.online_learning.repository;

import com.example.online_learning.entity.Course;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByLevelIgnoreCase(String level);
}
