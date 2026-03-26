package com.example.online_learning.service;

import com.example.online_learning.cache.CourseSearchCache;
import com.example.online_learning.cache.CourseSearchCacheInvalidator;
import com.example.online_learning.cache.CourseSearchCacheKey;
import com.example.online_learning.dto.CourseRequestDto;
import com.example.online_learning.dto.CourseResponseDto;
import com.example.online_learning.dto.CourseSearchQueryType;
import com.example.online_learning.dto.LessonRequestDto;
import com.example.online_learning.entity.Category;
import com.example.online_learning.entity.Course;
import com.example.online_learning.entity.Instructor;
import com.example.online_learning.entity.Lesson;
import com.example.online_learning.entity.Student;
import com.example.online_learning.exception.ResourceNotFoundException;
import com.example.online_learning.mapper.CourseMapper;
import com.example.online_learning.repository.CategoryRepository;
import com.example.online_learning.repository.CourseRepository;
import com.example.online_learning.repository.InstructorRepository;
import com.example.online_learning.repository.StudentRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseServiceImpl implements CourseService {

    private static final String COURSE_ENTITY = "Course";
    private static final String STUDENT_ENTITY = "Student";
    private static final String COURSE_SEARCH_LOG_TEMPLATE =
            "Course search request: hasCategoryFilter={}, hasInstructorFilter={}, "
                    + "queryType={}, page={}, size={}, cache={}";
    private static final Logger log = LoggerFactory.getLogger(CourseServiceImpl.class);

    private final CourseRepository courseRepository;
    private final InstructorRepository instructorRepository;
    private final StudentRepository studentRepository;
    private final CategoryRepository categoryRepository;
    private final CourseMapper courseMapper;
    private final CourseSearchCache courseSearchCache;
    private final CourseSearchCacheInvalidator courseSearchCacheInvalidator;

    public CourseServiceImpl(
            CourseRepository courseRepository,
            InstructorRepository instructorRepository,
            StudentRepository studentRepository,
            CategoryRepository categoryRepository,
            CourseMapper courseMapper,
            CourseSearchCache courseSearchCache,
            CourseSearchCacheInvalidator courseSearchCacheInvalidator) {
        this.courseRepository = courseRepository;
        this.instructorRepository = instructorRepository;
        this.studentRepository = studentRepository;
        this.categoryRepository = categoryRepository;
        this.courseMapper = courseMapper;
        this.courseSearchCache = courseSearchCache;
        this.courseSearchCacheInvalidator = courseSearchCacheInvalidator;
    }

    @Override
    @Transactional
    public CourseResponseDto createCourse(CourseRequestDto requestDto) {
        Course course = new Course(requestDto.title(), requestDto.level());
        applyRequest(course, requestDto);
        CourseResponseDto responseDto = courseMapper.toDto(courseRepository.save(course));
        log.info("Course created: id={}", responseDto.id());
        invalidateSearchIndex();
        return responseDto;
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponseDto getCourseById(Long id) {
        Course course = courseRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException(COURSE_ENTITY, id));
        return courseMapper.toDto(course);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseResponseDto> getCourses(String level, Pageable pageable) {
        return toPage(getCoursesInternal(level, true), pageable);
    }

    @Override
    @Transactional
    public CourseResponseDto updateCourse(Long id, CourseRequestDto requestDto) {
        Course course = courseRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException(COURSE_ENTITY, id));
        course.setTitle(requestDto.title());
        course.setLevel(requestDto.level());
        applyRequest(course, requestDto);
        CourseResponseDto responseDto = courseMapper.toDto(course);
        log.info("Course updated: id={}", responseDto.id());
        invalidateSearchIndex();
        return responseDto;
    }

    @Override
    @Transactional
    public void deleteCourse(Long id) {
        Course course = courseRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException(COURSE_ENTITY, id));
        course.clearStudents();
        course.clearCategories();
        course.getInstructor().getCourses().remove(course);
        courseRepository.delete(course);
        log.info("Course deleted: id={}", id);
        invalidateSearchIndex();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponseDto> getCoursesWithNPlusOne(String level) {
        return getCoursesInternal(level, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponseDto> getCoursesWithEntityGraph(String level) {
        return getCoursesInternal(level, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponseDto> searchCourses(
            String categoryName,
            String instructorSpecialization,
            CourseSearchQueryType queryType) {
        return findSearchCourses(
                categoryName,
                instructorSpecialization,
                queryType,
                PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseResponseDto> searchCourses(
            String categoryName,
            String instructorSpecialization,
            CourseSearchQueryType queryType,
            Pageable pageable) {
        return findSearchCourses(categoryName, instructorSpecialization, queryType, pageable);
    }

    private Page<CourseResponseDto> findSearchCourses(
            String categoryName,
            String instructorSpecialization,
            CourseSearchQueryType queryType,
            Pageable pageable) {
        String normalizedCategoryName = normalizeFilter(categoryName);
        String normalizedSpecialization = normalizeFilter(instructorSpecialization);
        CourseSearchCacheKey cacheKey = new CourseSearchCacheKey(
                normalizedCategoryName,
                normalizedSpecialization,
                queryType,
                toRequestedPageNumber(pageable),
                pageable.getPageSize());
        Optional<Page<CourseResponseDto>> cachedCourses = courseSearchCache.get(cacheKey);
        if (cachedCourses.isPresent()) {
            log.info(
                    COURSE_SEARCH_LOG_TEMPLATE,
                    normalizedCategoryName != null,
                    normalizedSpecialization != null,
                    queryType,
                    toRequestedPageNumber(pageable),
                    pageable.getPageSize(),
                    "hit");
            return cachedCourses.get();
        }

        log.info(
                COURSE_SEARCH_LOG_TEMPLATE,
                normalizedCategoryName != null,
                normalizedSpecialization != null,
                queryType,
                toRequestedPageNumber(pageable),
                pageable.getPageSize(),
                "miss");

        Page<Long> courseIds = switch (queryType) {
            case JPQL -> courseRepository.findPagedCourseIdsByCategoryAndInstructorJpql(
                    normalizedCategoryName,
                    normalizedSpecialization,
                    pageable);
            case NATIVE -> courseRepository.findPagedCourseIdsByCategoryAndInstructorNative(
                    normalizedCategoryName,
                    normalizedSpecialization,
                    pageable);
        };
        Page<CourseResponseDto> result = mapPagedCourses(courseIds);

        courseSearchCache.put(cacheKey, result);
        return result;
    }

    private List<CourseResponseDto> getCoursesInternal(String level, boolean optimized) {
        List<Course> courses;
        if (level == null || level.isBlank()) {
            courses = optimized ? courseRepository.findAllWithDetails() : courseRepository.findAll();
        } else {
            String normalizedLevel = normalizeFilter(level);
            courses = optimized
                    ? courseRepository.findAllWithDetailsByLevel(normalizedLevel)
                    : courseRepository.findByLevelIgnoreCase(level);
        }
        return courses.stream()
                .map(courseMapper::toDto)
                .toList();
    }

    private void applyRequest(Course course, CourseRequestDto requestDto) {
        course.setInstructor(resolveInstructor(
                requestDto.instructorFirstName(),
                requestDto.instructorLastName(),
                requestDto.instructorSpecialization()));
        course.replaceLessons(mapLessons(requestDto.lessons()));
        syncStudents(course, requestDto.studentIds());
        syncCategories(course, requestDto.categoryNames());
    }

    private Instructor resolveInstructor(String firstName, String lastName, String specialization) {
        Optional<Instructor> existingInstructor = instructorRepository
                .findByFirstNameIgnoreCaseAndLastNameIgnoreCase(firstName, lastName);
        if (existingInstructor.isPresent()) {
            Instructor instructor = existingInstructor.get();
            instructor.setSpecialization(specialization);
            return instructor;
        }
        return instructorRepository.save(new Instructor(firstName, lastName, specialization));
    }

    private List<Lesson> mapLessons(List<LessonRequestDto> lessonRequestDtos) {
        List<Lesson> lessons = new ArrayList<>();
        for (LessonRequestDto lessonRequestDto : lessonRequestDtos) {
            lessons.add(new Lesson(
                    lessonRequestDto.title(),
                    lessonRequestDto.durationMinutes(),
                    lessonRequestDto.lessonOrder()));
        }
        return lessons;
    }

    private void syncStudents(Course course, List<Long> studentIds) {
        course.clearStudents();
        if (studentIds == null) {
            return;
        }
        for (Long studentId : studentIds) {
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new ResourceNotFoundException(STUDENT_ENTITY, studentId));
            course.addStudent(student);
        }
    }

    private void syncCategories(Course course, List<String> categoryNames) {
        course.clearCategories();
        if (categoryNames == null) {
            return;
        }
        for (String categoryName : categoryNames) {
            Category category = categoryRepository.findByNameIgnoreCase(categoryName)
                    .orElseGet(() -> categoryRepository.save(new Category(categoryName)));
            course.addCategory(category);
        }
    }

    private Page<CourseResponseDto> mapPagedCourses(Page<Long> courseIdsPage) {
        if (courseIdsPage.isEmpty()) {
            return Page.empty(courseIdsPage.getPageable());
        }
        Map<Long, Course> coursesById = new LinkedHashMap<>();
        for (Course course : courseRepository.findAllDetailedByIdIn(courseIdsPage.getContent())) {
            coursesById.put(course.getId(), course);
        }
        List<CourseResponseDto> content = courseIdsPage.getContent().stream()
                .map(coursesById::get)
                .filter(course -> course != null)
                .map(courseMapper::toDto)
                .toList();
        return new PageImpl<>(content, courseIdsPage.getPageable(), courseIdsPage.getTotalElements());
    }

    private Page<CourseResponseDto> toPage(List<CourseResponseDto> courses, Pageable pageable) {
        List<CourseResponseDto> sortedCourses = sortCourses(courses, pageable);
        int fromIndex = (int) Math.min(pageable.getOffset(), sortedCourses.size());
        int toIndex = Math.min(fromIndex + pageable.getPageSize(), sortedCourses.size());
        List<CourseResponseDto> content = sortedCourses.subList(fromIndex, toIndex);
        return new PageImpl<>(content, pageable, sortedCourses.size());
    }

    private List<CourseResponseDto> sortCourses(List<CourseResponseDto> courses, Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return courses;
        }

        Comparator<CourseResponseDto> comparator = null;
        for (org.springframework.data.domain.Sort.Order order : pageable.getSort()) {
            Comparator<CourseResponseDto> nextComparator = buildCourseComparator(order);
            comparator = comparator == null ? nextComparator : comparator.thenComparing(nextComparator);
        }

        return courses.stream()
                .sorted(comparator)
                .toList();
    }

    private Comparator<CourseResponseDto> buildCourseComparator(org.springframework.data.domain.Sort.Order order) {
        Comparator<CourseResponseDto> comparator = switch (normalizeFilter(order.getProperty())) {
            case "id" -> Comparator.comparing(CourseResponseDto::id);
            case "title" -> Comparator.comparing(CourseResponseDto::title, String.CASE_INSENSITIVE_ORDER);
            case "level" -> Comparator.comparing(CourseResponseDto::level, String.CASE_INSENSITIVE_ORDER);
            case "instructorfirstname" ->
                Comparator.comparing(CourseResponseDto::instructorFirstName, String.CASE_INSENSITIVE_ORDER);
            case "instructorlastname" ->
                Comparator.comparing(CourseResponseDto::instructorLastName, String.CASE_INSENSITIVE_ORDER);
            default -> throw new IllegalArgumentException("Unsupported sort field: " + order.getProperty());
        };
        return order.isAscending() ? comparator : comparator.reversed();
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue.toLowerCase(Locale.ROOT);
    }

    private int toRequestedPageNumber(Pageable pageable) {
        return pageable.getPageNumber() + 1;
    }

    private void invalidateSearchIndex() {
        courseSearchCacheInvalidator.invalidate();
    }
}
