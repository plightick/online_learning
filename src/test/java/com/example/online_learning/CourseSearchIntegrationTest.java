package com.example.online_learning;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.online_learning.controller.CourseController;
import com.example.online_learning.dto.CourseRequestDto;
import com.example.online_learning.dto.CourseResponseDto;
import com.example.online_learning.dto.CourseSearchQueryType;
import com.example.online_learning.dto.LessonRequestDto;
import com.example.online_learning.dto.StudentRequestDto;
import com.example.online_learning.cache.CourseSearchCache;
import com.example.online_learning.cache.CourseSearchCacheKey;
import com.example.online_learning.entity.Category;
import com.example.online_learning.entity.Course;
import com.example.online_learning.entity.Instructor;
import com.example.online_learning.entity.Lesson;
import com.example.online_learning.repository.CategoryRepository;
import com.example.online_learning.repository.CourseRepository;
import com.example.online_learning.repository.InstructorRepository;
import com.example.online_learning.service.CourseService;
import com.example.online_learning.service.StudentService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class CourseSearchIntegrationTest {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseController courseController;

    @Autowired
    private StudentService studentService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CourseSearchCache courseSearchCache;

    @BeforeEach
    void setUp() {
        courseSearchCache.clear();
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
    void searchCoursesSupportsJpqlAndNativeQueries() {
        List<CourseResponseDto> jpqlCourses = courseService.searchCourses(
                "Backend",
                "Java Architecture",
                CourseSearchQueryType.JPQL);
        List<CourseResponseDto> nativeCourses = courseService.searchCourses(
                "Backend",
                "Java Architecture",
                CourseSearchQueryType.NATIVE);

        assertThat(jpqlCourses).hasSize(1);
        assertThat(jpqlCourses.getFirst().title()).isEqualTo("Spring Boot Intensive");

        assertThat(nativeCourses).hasSize(1);
        assertThat(nativeCourses.getFirst().title()).isEqualTo("Spring Boot Intensive");
    }

    @Test
    void searchCoursesIsCaseInsensitiveForJpqlAndNativeQueries() {
        List<CourseResponseDto> jpqlCourses = courseService.searchCourses(
                "bAcKeNd",
                "jAvA aRcHiTeCtUrE",
                CourseSearchQueryType.JPQL);
        List<CourseResponseDto> nativeCourses = courseService.searchCourses(
                "bAcKeNd",
                "jAvA aRcHiTeCtUrE",
                CourseSearchQueryType.NATIVE);

        assertThat(jpqlCourses).hasSize(1);
        assertThat(jpqlCourses.getFirst().title()).isEqualTo("Spring Boot Intensive");

        assertThat(nativeCourses).hasSize(1);
        assertThat(nativeCourses.getFirst().title()).isEqualTo("Spring Boot Intensive");
    }

    @Test
    void searchCacheIsInvalidatedAfterCourseChanges() {
        Page<CourseResponseDto> initialCourses = courseService.searchCourses(
                "Backend",
                "Java Architecture",
                CourseSearchQueryType.JPQL,
                PageRequest.of(0, 10));

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

        Page<CourseResponseDto> refreshedCourses = courseService.searchCourses(
                "Backend",
                "Java Architecture",
                CourseSearchQueryType.JPQL,
                PageRequest.of(0, 10));

        assertThat(initialCourses.getContent()).hasSize(1);
        assertThat(refreshedCourses.getContent()).hasSize(2);
    }

    @Test
    void searchCacheKeepsOnlyLastThreeRequests() {
        PageRequest firstPage = PageRequest.of(0, 10);
        PageRequest secondPage = PageRequest.of(1, 10);

        courseService.searchCourses("Backend", "Java Architecture", CourseSearchQueryType.JPQL, firstPage);
        courseService.searchCourses("Frontend", "UI Design", CourseSearchQueryType.JPQL, firstPage);
        courseService.searchCourses("Backend", null, CourseSearchQueryType.JPQL, firstPage);

        CourseSearchCacheKey firstKey = createCacheKey("Backend", "Java Architecture", 1, 10);
        CourseSearchCacheKey secondKey = createCacheKey("Frontend", "UI Design", 1, 10);
        CourseSearchCacheKey thirdKey = createCacheKey("Backend", null, 1, 10);

        assertThat(courseSearchCache.size()).isEqualTo(3);
        assertThat(courseSearchCache.contains(firstKey)).isTrue();
        assertThat(courseSearchCache.contains(secondKey)).isTrue();
        assertThat(courseSearchCache.contains(thirdKey)).isTrue();

        courseService.searchCourses(null, "UI Design", CourseSearchQueryType.JPQL, secondPage);

        CourseSearchCacheKey fourthKey = createCacheKey(null, "UI Design", 2, 10);

        assertThat(courseSearchCache.size()).isEqualTo(3);
        assertThat(courseSearchCache.contains(firstKey)).isFalse();
        assertThat(courseSearchCache.contains(secondKey)).isTrue();
        assertThat(courseSearchCache.contains(thirdKey)).isTrue();
        assertThat(courseSearchCache.contains(fourthKey)).isTrue();
    }

    @Test
    void searchCoursesReturnsCachedResultForRepeatedRequest() {
        Page<CourseResponseDto> firstResult = courseService.searchCourses(
                "Backend",
                "Java Architecture",
                CourseSearchQueryType.JPQL,
                PageRequest.of(0, 10));
        Page<CourseResponseDto> cachedResult = courseService.searchCourses(
                "Backend",
                "Java Architecture",
                CourseSearchQueryType.JPQL,
                PageRequest.of(0, 10));

        assertThat(courseSearchCache.size()).isEqualTo(1);
        assertThat(cachedResult).isSameAs(firstResult);
    }

    @Test
    void searchCoursesLogsCacheMissAndHitWithRequestData(CapturedOutput output) {
        courseService.searchCourses(
                "Backend",
                "Java Architecture",
                CourseSearchQueryType.JPQL,
                PageRequest.of(0, 10));
        courseService.searchCourses(
                "Backend",
                "Java Architecture",
                CourseSearchQueryType.JPQL,
                PageRequest.of(0, 10));

        assertThat(output.getOut()).contains("cache=miss");
        assertThat(output.getOut()).contains("cache=hit");
        assertThat(output.getOut()).contains("categoryName='backend'");
        assertThat(output.getOut()).contains("instructorSpecialization='java architecture'");
        assertThat(output.getOut()).contains("queryType=JPQL");
        assertThat(output.getOut()).contains("page=1");
        assertThat(output.getOut()).contains("size=10");
    }

    @Test
    void searchCacheIsInvalidatedAfterStudentChanges() {
        Long studentId = studentService.createStudent(new StudentRequestDto(
                "Alex",
                "Brown",
                "alex@learn.io")).id();

        courseService.createCourse(new CourseRequestDto(
                "Backend Mentoring",
                "ADVANCED",
                "Ivan",
                "Petrov",
                "Java Architecture",
                List.of(new LessonRequestDto("Coaching", 35, 1)),
                List.of(studentId),
                List.of("Backend")));

        Page<CourseResponseDto> initialCourses = courseService.searchCourses(
                "Backend",
                "Java Architecture",
                CourseSearchQueryType.JPQL,
                PageRequest.of(0, 10));

        studentService.updateStudent(studentId, new StudentRequestDto(
                "Max",
                "Brown",
                "alex@learn.io"));

        Page<CourseResponseDto> refreshedCourses = courseService.searchCourses(
                "Backend",
                "Java Architecture",
                CourseSearchQueryType.JPQL,
                PageRequest.of(0, 10));

        assertThat(initialCourses.getContent())
                .flatExtracting(CourseResponseDto::studentNames)
                .contains("Alex Brown");
        assertThat(refreshedCourses.getContent())
                .flatExtracting(CourseResponseDto::studentNames)
                .contains("Max Brown")
                .doesNotContain("Alex Brown");
    }

    @Test
    void getCoursesSupportsPaginationAndSort() {
        courseService.createCourse(new CourseRequestDto(
                "API Design",
                "ADVANCED",
                "Ivan",
                "Petrov",
                "Java Architecture",
                List.of(new LessonRequestDto("REST", 40, 1)),
                List.of(),
                List.of("Backend")));
        courseService.createCourse(new CourseRequestDto(
                "Zero Downtime",
                "ADVANCED",
                "Ivan",
                "Petrov",
                "Java Architecture",
                List.of(new LessonRequestDto("Deployments", 55, 1)),
                List.of(),
                List.of("Backend")));

        PageRequest firstPage = PageRequest.of(0, 1, Sort.by("title").ascending());
        PageRequest secondPage = PageRequest.of(1, 1, Sort.by("title").ascending());
        PageRequest thirdPage = PageRequest.of(2, 1, Sort.by("title").ascending());

        Page<CourseResponseDto> firstCoursesPage = courseService.getCourses(null, firstPage);
        Page<CourseResponseDto> secondCoursesPage = courseService.getCourses(null, secondPage);
        Page<CourseResponseDto> thirdCoursesPage = courseService.getCourses(null, thirdPage);

        assertThat(firstCoursesPage.getContent()).extracting(CourseResponseDto::title)
                .containsExactly("API Design");
        assertThat(secondCoursesPage.getContent()).extracting(CourseResponseDto::title)
                .containsExactly("Frontend Basics");
        assertThat(thirdCoursesPage.getContent()).extracting(CourseResponseDto::title)
                .containsExactly("Spring Boot Intensive");
    }

    @Test
    void getCoursesSupportsPaginationWithLevelFilter() {
        courseService.createCourse(new CourseRequestDto(
                "Zero Downtime",
                "ADVANCED",
                "Ivan",
                "Petrov",
                "Java Architecture",
                List.of(new LessonRequestDto("Deployments", 55, 1)),
                List.of(),
                List.of("Backend")));
        courseService.createCourse(new CourseRequestDto(
                "Architecture Review",
                "ADVANCED",
                "Ivan",
                "Petrov",
                "Java Architecture",
                List.of(new LessonRequestDto("Patterns", 35, 1)),
                List.of(),
                List.of("Backend")));

        Page<CourseResponseDto> advancedCourses = courseService.getCourses(
                "ADVANCED",
                PageRequest.of(0, 2, Sort.by("title").ascending()));

        assertThat(advancedCourses.getTotalElements()).isEqualTo(2);
        assertThat(advancedCourses.getContent()).extracting(CourseResponseDto::title)
                .containsExactly("Architecture Review", "Zero Downtime");
    }

    private CourseSearchCacheKey createCacheKey(
            String categoryName,
            String instructorSpecialization,
            int pageNumber,
            int pageSize) {
        return new CourseSearchCacheKey(
                normalizeFilter(categoryName),
                normalizeFilter(instructorSpecialization),
                CourseSearchQueryType.JPQL,
                pageNumber,
                pageSize);
    }

    @Test
    void getCoursesSupportsLaterPage() {
        courseService.createCourse(new CourseRequestDto(
                "Zero Downtime",
                "ADVANCED",
                "Ivan",
                "Petrov",
                "Java Architecture",
                List.of(new LessonRequestDto("Deployments", 55, 1)),
                List.of(),
                List.of("Backend")));
        courseService.createCourse(new CourseRequestDto(
                "API Design",
                "ADVANCED",
                "Ivan",
                "Petrov",
                "Java Architecture",
                List.of(new LessonRequestDto("REST", 40, 1)),
                List.of(),
                List.of("Backend")));

        Page<CourseResponseDto> laterPage = courseService.getCourses(
                null,
                PageRequest.of(3, 1, Sort.by("title").ascending()));

        assertThat(laterPage.getContent()).extracting(CourseResponseDto::title)
                .containsExactly("Zero Downtime");
    }

    @Test
    void courseControllerTreatsFirstPageAsOneBased() {
        Page<CourseResponseDto> firstPage = courseController.getCourses(
                null,
                1,
                1,
                "title",
                true);
        Page<CourseResponseDto> secondPage = courseController.getCourses(
                null,
                2,
                1,
                "title",
                true);

        assertThat(firstPage.getContent()).extracting(CourseResponseDto::title)
                .containsExactly("Frontend Basics");
        assertThat(secondPage.getContent()).extracting(CourseResponseDto::title)
                .containsExactly("Spring Boot Intensive");
    }

    @Test
    void searchEndpointSupportsPagination() {
        courseService.createCourse(new CourseRequestDto(
                "API Design",
                "ADVANCED",
                "Ivan",
                "Petrov",
                "Java Architecture",
                List.of(new LessonRequestDto("REST", 40, 1)),
                List.of(),
                List.of("Backend")));
        courseService.createCourse(new CourseRequestDto(
                "Zero Downtime",
                "ADVANCED",
                "Ivan",
                "Petrov",
                "Java Architecture",
                List.of(new LessonRequestDto("Deployments", 55, 1)),
                List.of(),
                List.of("Backend")));

        Page<CourseResponseDto> firstPage = courseController.searchCourses(
                "Backend",
                "Java Architecture",
                CourseSearchQueryType.JPQL,
                1,
                1);
        Page<CourseResponseDto> secondPage = courseController.searchCourses(
                "Backend",
                "Java Architecture",
                CourseSearchQueryType.JPQL,
                2,
                1);

        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getContent()).extracting(CourseResponseDto::title)
                .containsExactly("Spring Boot Intensive");
        assertThat(secondPage.getContent()).extracting(CourseResponseDto::title)
                .containsExactly("API Design");
    }

    @Test
    void getCoursesReturnsEmptyContentForPageOutsideAvailableRange() {
        Page<CourseResponseDto> farPage = courseService.getCourses(
                null,
                PageRequest.of(20, 10, Sort.by("title").ascending()));

        assertThat(farPage.getContent()).isEmpty();
        assertThat(farPage.getNumber()).isEqualTo(20);
        assertThat(farPage.getTotalElements()).isEqualTo(2);
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue.toLowerCase(java.util.Locale.ROOT);
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
