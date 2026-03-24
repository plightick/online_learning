package com.example.online_learning;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.online_learning.dto.CourseRequestDto;
import com.example.online_learning.dto.CourseResponseDto;
import com.example.online_learning.dto.CourseSearchQueryType;
import com.example.online_learning.dto.LessonRequestDto;
import com.example.online_learning.entity.Category;
import com.example.online_learning.entity.Course;
import com.example.online_learning.entity.Instructor;
import com.example.online_learning.entity.Lesson;
import com.example.online_learning.repository.CategoryRepository;
import com.example.online_learning.repository.CourseRepository;
import com.example.online_learning.repository.InstructorRepository;
import com.example.online_learning.service.CourseService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CourseSearchIntegrationTest {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        courseRepository.deleteAll();
        instructorRepository.deleteAll();
        categoryRepository.deleteAll();

        Category backend = categoryRepository.save(new Category("Backend"));
        Category frontend = categoryRepository.save(new Category("Frontend"));

        Instructor javaInstructor = instructorRepository.save(
                new Instructor("Ivan", "Petrov", "Java Architecture"));
        Instructor uiInstructor = instructorRepository.save(
                new Instructor("Elena", "Smirnova", "UI Design"));

        courseRepository.save(createCourse(
                "Spring Boot Intensive",
                "INTERMEDIATE",
                javaInstructor,
                List.of(backend),
                new Lesson("Spring Context", 45, 1)));
        courseRepository.save(createCourse(
                "Frontend Basics",
                "BEGINNER",
                uiInstructor,
                List.of(frontend),
                new Lesson("HTML", 30, 1)));
    }

    @Test
    void searchCoursesSupportsJpqlNativeAndPagination() {
        PageRequest pageable = PageRequest.of(0, 1, Sort.by("id"));

        Page<CourseResponseDto> jpqlPage = courseService.searchCourses(
                "Backend",
                "Java Architecture",
                CourseSearchQueryType.JPQL,
                pageable);
        Page<CourseResponseDto> nativePage = courseService.searchCourses(
                "Backend",
                "Java Architecture",
                CourseSearchQueryType.NATIVE,
                pageable);

        assertThat(jpqlPage.getTotalElements()).isEqualTo(1);
        assertThat(jpqlPage.getContent()).hasSize(1);
        assertThat(jpqlPage.getContent().getFirst().title()).isEqualTo("Spring Boot Intensive");

        assertThat(nativePage.getTotalElements()).isEqualTo(1);
        assertThat(nativePage.getContent()).hasSize(1);
        assertThat(nativePage.getContent().getFirst().title()).isEqualTo("Spring Boot Intensive");
    }

    @Test
    void searchCacheIsInvalidatedAfterCourseChanges() {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("id"));

        Page<CourseResponseDto> initialPage = courseService.searchCourses(
                "Backend",
                "Java Architecture",
                CourseSearchQueryType.JPQL,
                pageable);

        CourseRequestDto requestDto = new CourseRequestDto(
                "Advanced Spring",
                "ADVANCED",
                "Ivan",
                "Petrov",
                "Java Architecture",
                List.of(new LessonRequestDto("Spring Security", 50, 1)),
                List.of(),
                List.of("Backend"));

        courseService.createCourse(requestDto);

        Page<CourseResponseDto> refreshedPage = courseService.searchCourses(
                "Backend",
                "Java Architecture",
                CourseSearchQueryType.JPQL,
                pageable);

        assertThat(initialPage.getTotalElements()).isEqualTo(1);
        assertThat(refreshedPage.getTotalElements()).isEqualTo(2);
    }

    private Course createCourse(
            String title,
            String level,
            Instructor instructor,
            List<Category> categories,
            Lesson lesson) {
        Course course = new Course(title, level);
        course.setInstructor(instructor);
        course.addLesson(lesson);
        for (Category category : categories) {
            course.addCategory(category);
        }
        return course;
    }
}
