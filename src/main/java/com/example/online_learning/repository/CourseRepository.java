package com.example.online_learning.repository;

import com.example.online_learning.entity.Course;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query(
            value = """
                    select distinct c.id
                    from Course c
                    join c.instructor instructor
                    left join c.categories category
                    where (:categoryName is null or :categoryName = ''
                        or lower(category.name) = lower(:categoryName))
                        and (:instructorSpecialization is null or :instructorSpecialization = ''
                        or lower(instructor.specialization) = lower(:instructorSpecialization))
                    """,
            countQuery = """
                    select count(distinct c.id)
                    from Course c
                    join c.instructor instructor
                    left join c.categories category
                    where (:categoryName is null or :categoryName = ''
                        or lower(category.name) = lower(:categoryName))
                        and (:instructorSpecialization is null or :instructorSpecialization = ''
                        or lower(instructor.specialization) = lower(:instructorSpecialization))
                    """)
    Page<Long> findCourseIdsByCategoryAndInstructorJpql(
            @Param("categoryName") String categoryName,
            @Param("instructorSpecialization") String instructorSpecialization,
            Pageable pageable);

    @Query(
            value = """
                    select distinct c.*
                    from courses c
                    join instructors i on i.id = c.instructor_id
                    left join course_categories cc on cc.course_id = c.id
                    left join categories cat on cat.id = cc.category_id
                    where (:categoryName is null or :categoryName = ''
                        or lower(cat.name) = lower(:categoryName))
                        and (:instructorSpecialization is null or :instructorSpecialization = ''
                        or lower(i.specialization) = lower(:instructorSpecialization))
                    order by c.id
                    """,
            countQuery = """
                    select count(distinct c.id)
                    from courses c
                    join instructors i on i.id = c.instructor_id
                    left join course_categories cc on cc.course_id = c.id
                    left join categories cat on cat.id = cc.category_id
                    where (:categoryName is null or :categoryName = ''
                        or lower(cat.name) = lower(:categoryName))
                        and (:instructorSpecialization is null or :instructorSpecialization = ''
                        or lower(i.specialization) = lower(:instructorSpecialization))
                    """,
            nativeQuery = true)
    Page<Course> findCoursesByCategoryAndInstructorNative(
            @Param("categoryName") String categoryName,
            @Param("instructorSpecialization") String instructorSpecialization,
            Pageable pageable);

    @Query("""
            select distinct c
            from Course c
            left join fetch c.instructor
            left join fetch c.lessons
            left join fetch c.students
            left join fetch c.categories
            where c.id in :ids
            """)
    List<Course> findAllDetailedByIdIn(@Param("ids") List<Long> ids);
}
