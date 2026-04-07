package com.example.online_learning.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.online_learning.cache.CourseSearchCache;
import com.example.online_learning.cache.CourseSearchCacheKey;
import com.example.online_learning.cache.CourseSearchCacheInvalidator;
import com.example.online_learning.dto.CourseRequestDto;
import com.example.online_learning.dto.CourseResponseDto;
import com.example.online_learning.dto.CourseSearchQueryType;
import com.example.online_learning.dto.LessonRequestDto;
import com.example.online_learning.dto.LessonResponseDto;
import com.example.online_learning.entity.Category;
import com.example.online_learning.entity.Course;
import com.example.online_learning.entity.Instructor;
import com.example.online_learning.entity.Lesson;
import com.example.online_learning.entity.Student;
import com.example.online_learning.exception.BadRequestException;
import com.example.online_learning.exception.ResourceNotFoundException;
import com.example.online_learning.mapper.CourseMapper;
import com.example.online_learning.repository.CategoryRepository;
import com.example.online_learning.repository.CourseRepository;
import com.example.online_learning.repository.InstructorRepository;
import com.example.online_learning.repository.StudentRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private InstructorRepository instructorRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CourseSearchCache courseSearchCache;

    @Mock
    private CourseSearchCacheInvalidator courseSearchCacheInvalidator;

    private CourseServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CourseServiceImpl(
                courseRepository,
                instructorRepository,
                studentRepository,
                categoryRepository,
                new CourseMapper(),
                courseSearchCache,
                courseSearchCacheInvalidator);
    }

    @Test
    void createCoursesBulkTxShouldCreateAllCoursesAndInvalidateCacheOnce() {
        Student student = student(1L, "Anna", "Petrova", "anna@example.com");
        Instructor instructor = instructor("Pavel", "Ivanov", "Security");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(instructorRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase("Pavel", "Ivanov"))
                .thenReturn(Optional.of(instructor));
        when(categoryRepository.findByNameIgnoreCase(anyString()))
                .thenAnswer(invocation -> Optional.of(new Category(invocation.getArgument(0))));

        AtomicLong ids = new AtomicLong(10L);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course course = invocation.getArgument(0);
            ReflectionTestUtils.setField(course, "id", ids.getAndIncrement());
            return course;
        });

        List<CourseResponseDto> responseDtos = service.createCoursesBulkTx(List.of(
                courseRequest("Spring Security Deep Dive", List.of(1L)),
                courseRequest("Docker for Java Teams", List.of(1L))));

        assertEquals(2, responseDtos.size());
        assertEquals(10L, responseDtos.getFirst().id());
        assertEquals(11L, responseDtos.get(1).id());
        assertEquals("Spring Security Deep Dive", responseDtos.getFirst().title());
        assertEquals(1, responseDtos.getFirst().studentNames().size());
        assertEquals("Anna Petrova", responseDtos.getFirst().studentNames().getFirst());
        verify(courseRepository, times(2)).save(any(Course.class));
        verify(courseSearchCacheInvalidator).invalidate();
    }

    @Test
    void createCoursesBulkTxShouldRejectEmptyRequest() {
        List<CourseRequestDto> requests = List.of();

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.createCoursesBulkTx(requests));

        assertTrue(exception.getMessage().contains("at least one item"));
        verify(courseRepository, never()).save(any(Course.class));
        verify(courseSearchCacheInvalidator, never()).invalidate();
    }

    @Test
    void createCoursesBulkNoTxShouldStopOnMissingStudentAfterFirstSuccessfulCourse() {
        Student student = student(1L, "Anna", "Petrova", "anna@example.com");
        Instructor instructor = instructor("Pavel", "Ivanov", "Security");
        List<CourseRequestDto> requests = List.of(
                courseRequest("Spring Security Deep Dive", List.of(1L)),
                courseRequest("Broken Bulk Demo", List.of(999L)));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());
        when(instructorRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase("Pavel", "Ivanov"))
                .thenReturn(Optional.of(instructor));
        when(categoryRepository.findByNameIgnoreCase("Backend"))
                .thenReturn(Optional.of(new Category("Backend")));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course course = invocation.getArgument(0);
            ReflectionTestUtils.setField(course, "id", 50L);
            return course;
        });

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.createCoursesBulkNoTx(requests));

        assertTrue(exception.getMessage().contains("999"));
        verify(courseRepository, times(1)).save(any(Course.class));
        verify(instructorRepository, times(1))
                .findByFirstNameIgnoreCaseAndLastNameIgnoreCase("Pavel", "Ivanov");
        verify(categoryRepository, times(1)).findByNameIgnoreCase("Backend");
        verify(courseSearchCacheInvalidator).invalidate();
    }

    @Test
    void createCourseShouldPersistMappedResponseAndInvalidateCache() {
        when(instructorRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase("Jane", "Doe"))
                .thenReturn(Optional.empty());
        when(instructorRepository.save(any(Instructor.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(categoryRepository.findByNameIgnoreCase("Backend")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course course = invocation.getArgument(0);
            ReflectionTestUtils.setField(course, "id", 77L);
            return course;
        });

        CourseResponseDto responseDto = service.createCourse(new CourseRequestDto(
                "Data Pipelines",
                "ADVANCED",
                "Jane",
                "Doe",
                "Data Engineering",
                List.of(
                        new LessonRequestDto("Warehouse", 50, 2),
                        new LessonRequestDto("Streaming", 40, 1)),
                List.of(),
                List.of("Backend")));

        assertEquals(77L, responseDto.id());
        assertEquals(List.of("Streaming", "Warehouse"), lessonTitles(responseDto));
        verify(instructorRepository).save(any(Instructor.class));
        verify(categoryRepository).save(any(Category.class));
        verify(courseSearchCacheInvalidator).invalidate();
    }

    @Test
    void createCoursesBulkNoTxShouldCreateAllCoursesAndInvalidateCacheOnce() {
        Student student = student(1L, "Anna", "Petrova", "anna@example.com");
        Instructor instructor = instructor("Pavel", "Ivanov", "Security");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(instructorRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase("Pavel", "Ivanov"))
                .thenReturn(Optional.of(instructor));
        when(categoryRepository.findByNameIgnoreCase(anyString()))
                .thenAnswer(invocation -> Optional.of(new Category(invocation.getArgument(0))));
        AtomicLong ids = new AtomicLong(30L);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course course = invocation.getArgument(0);
            ReflectionTestUtils.setField(course, "id", ids.getAndIncrement());
            return course;
        });

        List<CourseResponseDto> responseDtos = service.createCoursesBulkNoTx(List.of(
                courseRequest("Observability", List.of(1L)),
                courseRequest("CI Pipelines", List.of(1L))));

        assertEquals(2, responseDtos.size());
        assertEquals(30L, responseDtos.getFirst().id());
        assertEquals(31L, responseDtos.get(1).id());
        verify(courseSearchCacheInvalidator).invalidate();
    }

    @Test
    void createCoursesBulkTxShouldRejectNullRequest() {
        assertThrows(BadRequestException.class, () -> service.createCoursesBulkTx(null));
        verify(courseRepository, never()).save(any(Course.class));
        verify(courseSearchCacheInvalidator, never()).invalidate();
    }

    @Test
    void getCourseByIdShouldReturnMappedCourse() {
        Course course = detailedCourse(5L, "Algorithms", "ADVANCED", "Ira", "Stone");
        when(courseRepository.findDetailedById(5L)).thenReturn(Optional.of(course));

        CourseResponseDto responseDto = service.getCourseById(5L);

        assertEquals(5L, responseDto.id());
        assertEquals("Algorithms", responseDto.title());
        assertEquals(List.of("Anna Petrova"), responseDto.studentNames());
    }

    @Test
    void getCourseByIdShouldThrowWhenMissing() {
        when(courseRepository.findDetailedById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.getCourseById(99L));

        assertTrue(exception.getMessage().contains("99"));
    }

    @Test
    void getCoursesShouldKeepRepositoryOrderWhenPageableIsUnsorted() {
        Course firstCourse = detailedCourse(1L, "Zeta", "BEGINNER", "Ann", "A");
        Course secondCourse = detailedCourse(2L, "Alpha", "ADVANCED", "Ben", "B");
        when(courseRepository.findAllWithDetails()).thenReturn(List.of(firstCourse, secondCourse));

        Page<CourseResponseDto> page = service.getCourses(null, PageRequest.of(0, 10));

        assertIterableEquals(List.of("Zeta", "Alpha"), page.map(CourseResponseDto::title).getContent());
    }

    @Test
    void updateCourseShouldUpdateExistingCourseAndInvalidateCache() {
        Course course = detailedCourse(11L, "Legacy", "BEGINNER", "Old", "Instructor");
        Instructor existingInstructor = instructor("Jane", "Doe", "Backend");
        Student student = student(4L, "Mila", "Stone", "mila@example.com");
        Category category = new Category("Backend");
        when(courseRepository.findDetailedById(11L)).thenReturn(Optional.of(course));
        when(instructorRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase("Jane", "Doe"))
                .thenReturn(Optional.of(existingInstructor));
        when(studentRepository.findById(4L)).thenReturn(Optional.of(student));
        when(categoryRepository.findByNameIgnoreCase("Backend")).thenReturn(Optional.of(category));

        CourseResponseDto responseDto = service.updateCourse(
                11L,
                new CourseRequestDto(
                        "Modern APIs",
                        "ADVANCED",
                        "Jane",
                        "Doe",
                        "Cloud",
                        List.of(
                                new LessonRequestDto("REST", 45, 2),
                                new LessonRequestDto("HTTP", 30, 1)),
                        List.of(4L),
                        List.of("Backend")));

        assertEquals("Modern APIs", responseDto.title());
        assertEquals("ADVANCED", responseDto.level());
        assertEquals("Cloud", existingInstructor.getSpecialization());
        assertEquals(List.of("HTTP", "REST"), lessonTitles(responseDto));
        verify(courseSearchCacheInvalidator).invalidate();
    }

    @Test
    void deleteCourseShouldRemoveRelationsDeleteAndInvalidateCache() {
        Course course = detailedCourse(12L, "Delete Me", "BEGINNER", "Jane", "Doe");
        when(courseRepository.findDetailedById(12L)).thenReturn(Optional.of(course));

        service.deleteCourse(12L);

        assertTrue(course.getStudents().isEmpty());
        assertTrue(course.getCategories().isEmpty());
        assertTrue(course.getInstructor().getCourses().isEmpty());
        verify(courseRepository).delete(course);
        verify(courseSearchCacheInvalidator).invalidate();
    }

    @Test
    void getCoursesWithNPlusOneShouldUseNonOptimizedRepository() {
        Course course = detailedCourse(21L, "Ops", "ADVANCED", "Jane", "Doe");
        when(courseRepository.findByLevelIgnoreCase(" Advanced ")).thenReturn(List.of(course));

        List<CourseResponseDto> responseDtos = service.getCoursesWithNPlusOne(" Advanced ");

        assertEquals(1, responseDtos.size());
        verify(courseRepository).findByLevelIgnoreCase(" Advanced ");
    }

    @Test
    void getCoursesWithEntityGraphShouldUseNormalizedLevel() {
        Course course = detailedCourse(22L, "Cloud", "ADVANCED", "Jane", "Doe");
        when(courseRepository.findAllWithDetailsByLevel("advanced")).thenReturn(List.of(course));

        List<CourseResponseDto> responseDtos = service.getCoursesWithEntityGraph(" Advanced ");

        assertEquals(1, responseDtos.size());
        verify(courseRepository).findAllWithDetailsByLevel("advanced");
    }

    @Test
    void searchCoursesShouldReturnCachedPageOnHit() {
        Page<CourseResponseDto> cachedPage = new PageImpl<>(List.of(responseDto(1L, "Cached")));
        when(courseSearchCache.get(any(CourseSearchCacheKey.class))).thenReturn(Optional.of(cachedPage));

        Page<CourseResponseDto> responsePage = service.searchCourses(
                "Backend",
                "Security",
                CourseSearchQueryType.JPQL,
                PageRequest.of(1, 5));

        assertSame(cachedPage, responsePage);
        verify(courseRepository, never()).findPagedCourseIdsByCategoryAndInstructorJpql(any(), any(), any());
        verify(courseSearchCache, never()).put(any(CourseSearchCacheKey.class), any(Page.class));
    }

    @Test
    void searchCoursesShouldUseNativeQueryAndFilterMissingCourses() {
        Page<Long> idsPage = new PageImpl<>(List.of(1L, 2L), PageRequest.of(1, 2), 2);
        Course course = detailedCourse(2L, "Native", "ADVANCED", "Jane", "Doe");
        when(courseSearchCache.get(any(CourseSearchCacheKey.class))).thenReturn(Optional.empty());
        when(courseRepository.findPagedCourseIdsByCategoryAndInstructorNative(
                eq("backend"),
                eq("security"),
                any(PageRequest.class))).thenReturn(idsPage);
        when(courseRepository.findAllDetailedByIdIn(List.of(1L, 2L))).thenReturn(List.of(course));

        Page<CourseResponseDto> responsePage = service.searchCourses(
                "Backend",
                "Security",
                CourseSearchQueryType.NATIVE,
                PageRequest.of(1, 2));

        assertEquals(1, responsePage.getContent().size());
        assertEquals("Native", responsePage.getContent().getFirst().title());
        verify(courseSearchCache).put(any(CourseSearchCacheKey.class), any(Page.class));
    }

    @Test
    void searchCoursesShouldReturnEmptyPageWhenNoIdsFound() {
        Page<Long> emptyIds = Page.empty(PageRequest.of(0, 10));
        when(courseSearchCache.get(any(CourseSearchCacheKey.class))).thenReturn(Optional.empty());
        when(courseRepository.findPagedCourseIdsByCategoryAndInstructorJpql(
                eq(null),
                eq(null),
                any(PageRequest.class))).thenReturn(emptyIds);

        Page<CourseResponseDto> responsePage = service.searchCourses(
                null,
                null,
                CourseSearchQueryType.JPQL,
                PageRequest.of(0, 10));

        assertTrue(responsePage.isEmpty());
        verify(courseRepository, never()).findAllDetailedByIdIn(any(List.class));
    }

    @Test
    void getCoursesShouldRejectUnsupportedSortField() {
        Course course = detailedCourse(1L, "Alpha", "ADVANCED", "Jane", "Doe");
        when(courseRepository.findAllWithDetails()).thenReturn(List.of(course));

        assertThrows(
                BadRequestException.class,
                () -> service.getCourses(
                        null,
                        PageRequest.of(0, 10, Sort.by("unsupported").ascending())));
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildCourseComparatorShouldSupportConfiguredFieldsAndDescendingOrder() {
        CourseResponseDto first = responseDto(1L, "Beta", "Intermediate", "Bella", "Clark");
        CourseResponseDto second = responseDto(2L, "alpha", "Advanced", "Adam", "Brown");
        CourseResponseDto third = responseDto(3L, "Gamma", "Beginner", "Chris", "Adams");
        List<CourseResponseDto> courses = List.of(first, second, third);

        Comparator<CourseResponseDto> idComparator = (Comparator<CourseResponseDto>)
                ReflectionTestUtils.invokeMethod(service, "buildCourseComparator", Sort.Order.asc("id"));
        Comparator<CourseResponseDto> titleComparator = (Comparator<CourseResponseDto>)
                ReflectionTestUtils.invokeMethod(service, "buildCourseComparator", Sort.Order.asc("title"));
        Comparator<CourseResponseDto> levelComparator = (Comparator<CourseResponseDto>)
                ReflectionTestUtils.invokeMethod(service, "buildCourseComparator", Sort.Order.asc("level"));
        Comparator<CourseResponseDto> firstNameComparator = (Comparator<CourseResponseDto>)
                ReflectionTestUtils.invokeMethod(
                        service,
                        "buildCourseComparator",
                        Sort.Order.asc("instructorFirstName"));
        Comparator<CourseResponseDto> lastNameComparator = (Comparator<CourseResponseDto>)
                ReflectionTestUtils.invokeMethod(
                        service,
                        "buildCourseComparator",
                        Sort.Order.desc("instructorLastName"));

        assertEquals(List.of(1L, 2L, 3L), sortIds(courses, idComparator));
        assertEquals(List.of(2L, 1L, 3L), sortIds(courses, titleComparator));
        assertEquals(List.of(2L, 3L, 1L), sortIds(courses, levelComparator));
        assertEquals(List.of(2L, 1L, 3L), sortIds(courses, firstNameComparator));
        assertEquals(List.of(1L, 2L, 3L), sortIds(courses, lastNameComparator));

        assertThrows(
                BadRequestException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        service,
                        "buildCourseComparator",
                        Sort.Order.asc("unknown")));
    }

    @Test
    void normalizeFilterShouldHandleNullBlankAndTrimmedValues() {
        assertNull(ReflectionTestUtils.invokeMethod(service, "normalizeFilter", (String) null));
        assertNull(ReflectionTestUtils.invokeMethod(service, "normalizeFilter", "   "));
        assertEquals("mixed case", ReflectionTestUtils.invokeMethod(service, "normalizeFilter", " Mixed Case "));
    }

    @Test
    void toRequestedPageNumberShouldReturnOneBasedIndex() {
        Integer requestedPageNumber = ReflectionTestUtils.invokeMethod(
                service,
                "toRequestedPageNumber",
                PageRequest.of(2, 5));

        assertEquals(3, requestedPageNumber);
    }

    private static CourseRequestDto courseRequest(String title, List<Long> studentIds) {
        return new CourseRequestDto(
                title,
                "ADVANCED",
                "Pavel",
                "Ivanov",
                "Security",
                List.of(new LessonRequestDto("Authentication", 40, 1)),
                studentIds,
                List.of("Backend"));
    }

    private static Student student(Long id, String firstName, String lastName, String email) {
        Student student = new Student(firstName, lastName, email);
        ReflectionTestUtils.setField(student, "id", id);
        return student;
    }

    private static Instructor instructor(String firstName, String lastName, String specialization) {
        return new Instructor(firstName, lastName, specialization);
    }

    private static Course detailedCourse(
            Long id,
            String title,
            String level,
            String instructorFirstName,
            String instructorLastName) {
        Course course = new Course(title, level);
        ReflectionTestUtils.setField(course, "id", id);
        Instructor instructor = instructor(instructorFirstName, instructorLastName, "Architecture");
        course.setInstructor(instructor);

        Lesson secondLesson = new Lesson("Second", 40, 2);
        ReflectionTestUtils.setField(secondLesson, "id", id + 200);
        Lesson firstLesson = new Lesson("First", 30, 1);
        ReflectionTestUtils.setField(firstLesson, "id", id + 100);
        course.addLesson(secondLesson);
        course.addLesson(firstLesson);

        Student student = student(900L + id, "Anna", "Petrova", "anna" + id + "@example.com");
        course.addStudent(student);
        Category category = new Category("Backend");
        course.addCategory(category);
        return course;
    }

    private static List<String> lessonTitles(CourseResponseDto responseDto) {
        return responseDto.lessons().stream()
                .map(LessonResponseDto::title)
                .toList();
    }

    private static CourseResponseDto responseDto(Long id, String title) {
        return responseDto(id, title, "ADVANCED", "Jane", "Doe");
    }

    private static CourseResponseDto responseDto(
            Long id,
            String title,
            String level,
            String instructorFirstName,
            String instructorLastName) {
        return new CourseResponseDto(
                id,
                title,
                level,
                instructorFirstName,
                instructorLastName,
                List.of(),
                List.of(),
                List.of());
    }

    private static List<Long> sortIds(List<CourseResponseDto> courses, Comparator<CourseResponseDto> comparator) {
        return courses.stream()
                .sorted(comparator)
                .map(CourseResponseDto::id)
                .toList();
    }
}
