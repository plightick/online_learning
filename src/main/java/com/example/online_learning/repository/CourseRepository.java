package com.example.online_learning.repository;

import com.example.online_learning.entity.Course;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByLevelIgnoreCase(String level);

    java.util.Optional<Course> findByTitleIgnoreCase(String title);

    @EntityGraph(attributePaths = {"instructor", "lessons", "students", "categories"})
    @Query("select c from Course c")
    List<Course> findAllWithDetails();

    @EntityGraph(attributePaths = {"instructor", "lessons", "students", "categories"})
    @Query("select c from Course c where lower(c.level) = lower(:level)")
    List<Course> findAllWithDetailsByLevel(String level);

    @Query("""
            select distinct c
            from Course c
            left join fetch c.instructor
            left join fetch c.lessons
            left join fetch c.students
            left join fetch c.categories
            where c.id = :id
            """)
    java.util.Optional<Course> findDetailedById(Long id);
}
