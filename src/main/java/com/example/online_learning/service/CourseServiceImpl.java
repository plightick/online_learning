package com.example.online_learning.service;

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
import com.example.online_learning.hash.CourseSearchCacheKey;
import com.example.online_learning.hash.CourseSearchIndex;
import com.example.online_learning.mapper.CourseMapper;
import com.example.online_learning.repository.CategoryRepository;
import com.example.online_learning.repository.CourseRepository;
import com.example.online_learning.repository.InstructorRepository;
import com.example.online_learning.repository.StudentRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseServiceImpl implements CourseService {

    private static final String COURSE_ENTITY = "Course";
    private static final String STUDENT_ENTITY = "Student";
    private static final Logger log = LoggerFactory.getLogger(CourseServiceImpl.class);

    private final CourseRepository courseRepository;
    private final InstructorRepository instructorRepository;
    private final StudentRepository studentRepository;
    private final CategoryRepository categoryRepository;
    private final CourseMapper courseMapper;
    private final CourseSearchIndex courseSearchIndex;

    public CourseServiceImpl(
            CourseRepository courseRepository,
            InstructorRepository instructorRepository,
            StudentRepository studentRepository,
            CategoryRepository categoryRepository,
            CourseMapper courseMapper,
            CourseSearchIndex courseSearchIndex) {
        this.courseRepository = courseRepository;
        this.instructorRepository = instructorRepository;
        this.studentRepository = studentRepository;
        this.categoryRepository = categoryRepository;
        this.courseMapper = courseMapper;
        this.courseSearchIndex = courseSearchIndex;
    }

    @Override
    @Transactional
    public CourseResponseDto createCourse(CourseRequestDto requestDto) {
        Course course = new Course(requestDto.title(), requestDto.level());
        applyRequest(course, requestDto);
        CourseResponseDto responseDto = courseMapper.toDto(courseRepository.save(course));
        log.info("Course created: id={}, title='{}'", responseDto.id(), responseDto.title());
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
    public List<CourseResponseDto> getCourses(String level) {
        return getCoursesInternal(level, true);
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
        log.info("Course updated: id={}, title='{}'", responseDto.id(), responseDto.title());
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
    public Page<CourseResponseDto> searchCourses(
            String categoryName,
            String instructorSpecialization,
            CourseSearchQueryType queryType,
            Pageable pageable) {
        String normalizedCategoryName = normalizeFilter(categoryName);
        String normalizedSpecialization = normalizeFilter(instructorSpecialization);
        CourseSearchCacheKey cacheKey = new CourseSearchCacheKey(
                normalizedCategoryName,
                normalizedSpecialization,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().toString(),
                queryType);
        int hash = cacheKey.hashCode();

        Optional<Page<CourseResponseDto>> cachedPage = courseSearchIndex.get(cacheKey);
        if (cachedPage.isPresent()) {
            log.info("Cache hit: hash={}, key={}", hash, cacheKey);
            return cachedPage.get();
        }
        log.info("Cache miss: hash={}, key={}", hash, cacheKey);

        Page<CourseResponseDto> resultPage = switch (queryType) {
            case JPQL -> {
                Page<Long> pagedCourseIds = courseRepository.findPagedCourseIdsByCategoryAndInstructorJpql(
                        normalizedCategoryName,
                        normalizedSpecialization,
                        pageable);
                List<CourseResponseDto> content = mapPagedCourses(pagedCourseIds.getContent());
                yield new PageImpl<>(content, pageable, pagedCourseIds.getTotalElements());
            }
            case NATIVE -> {
                Page<Long> pagedCourseIds = courseRepository.findPagedCourseIdsByCategoryAndInstructorNative(
                        normalizedCategoryName,
                        normalizedSpecialization,
                        pageable);
                List<CourseResponseDto> content = mapPagedCourses(pagedCourseIds.getContent());
                yield new PageImpl<>(content, pageable, pagedCourseIds.getTotalElements());
            }
        };

        courseSearchIndex.put(cacheKey, resultPage);
        return resultPage;
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

    private List<CourseResponseDto> mapPagedCourses(List<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Course> coursesById = new LinkedHashMap<>();
        for (Course course : courseRepository.findAllDetailedByIdIn(courseIds)) {
            coursesById.put(course.getId(), course);
        }
        return courseIds.stream()
                .map(coursesById::get)
                .filter(course -> course != null)
                .map(courseMapper::toDto)
                .toList();
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue.toLowerCase(Locale.ROOT);
    }

    private void invalidateSearchIndex() {
        int cachedEntries = courseSearchIndex.size();
        courseSearchIndex.clear();
        log.info("Search cache invalidated after course data change: clearedEntries={}", cachedEntries);
    }
}
