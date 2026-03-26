package com.example.online_learning.repository;

import com.example.online_learning.entity.Course;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByLevelIgnoreCase(String level);

    java.util.Optional<Course> findByTitleIgnoreCase(String title);

    @Query("""
            select distinct c
            from Course c
            left join fetch c.instructor
            left join fetch c.lessons
            left join fetch c.students
            left join fetch c.categories
            """)
    List<Course> findAllWithDetails();

    @Query("""
            select distinct c
            from Course c
            left join fetch c.instructor
            left join fetch c.lessons
            left join fetch c.students
            left join fetch c.categories
            where lower(c.level) = :level
            """)
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

    @Query(
            value = """
                    select c.id
                    from Course c
                    where (:level is null or lower(c.level) = :level)
                    """,
            countQuery = """
                    select count(c.id)
                    from Course c
                    where (:level is null or lower(c.level) = :level)
                    """)
    Page<Long> findPagedCourseIdsByLevel(
            @Param("level") String level,
            Pageable pageable);

    @Query(
            value = """
                    select distinct c.id
                    from Course c
                    join c.instructor i
                    left join c.categories cat
                    where (:categoryName is null or lower(cat.name) = :categoryName)
                      and (:instructorSpecialization is null
                           or lower(i.specialization) = :instructorSpecialization)
                    order by c.id
                    """,
            countQuery = """
                    select count(distinct c.id)
                    from Course c
                    join c.instructor i
                    left join c.categories cat
                    where (:categoryName is null or lower(cat.name) = :categoryName)
                      and (:instructorSpecialization is null
                           or lower(i.specialization) = :instructorSpecialization)
                    """)
    Page<Long> findPagedCourseIdsByCategoryAndInstructorJpql(
            @Param("categoryName") String categoryName,
            @Param("instructorSpecialization") String instructorSpecialization,
            Pageable pageable);

    @Query(
            value = """
                    select distinct c.id
                    from courses c
                    join instructors i on i.id = c.instructor_id
                    left join course_categories cc on cc.course_id = c.id
                    left join categories cat on cat.id = cc.category_id
                    where (:categoryName is null or lower(cat.name) = :categoryName)
                      and (:instructorSpecialization is null
                           or lower(i.specialization) = :instructorSpecialization)
                    order by c.id
                    """,
            countQuery = """
                    select count(distinct c.id)
                    from courses c
                    join instructors i on i.id = c.instructor_id
                    left join course_categories cc on cc.course_id = c.id
                    left join categories cat on cat.id = cc.category_id
                    where (:categoryName is null or lower(cat.name) = :categoryName)
                      and (:instructorSpecialization is null
                           or lower(i.specialization) = :instructorSpecialization)
                    """,
            nativeQuery = true)
    Page<Long> findPagedCourseIdsByCategoryAndInstructorNative(
            @Param("categoryName") String categoryName,
            @Param("instructorSpecialization") String instructorSpecialization,
            Pageable pageable);
}
