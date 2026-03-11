package com.example.online_learning.service;

import com.example.online_learning.dto.CourseRequestDto;
import com.example.online_learning.dto.CourseResponseDto;
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
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseServiceImpl implements CourseService {

    private static final String COURSE_ENTITY = "Course";
    private static final String STUDENT_ENTITY = "Student";

    private final CourseRepository courseRepository;
    private final InstructorRepository instructorRepository;
    private final StudentRepository studentRepository;
    private final CategoryRepository categoryRepository;
    private final CourseMapper courseMapper;

    public CourseServiceImpl(
            CourseRepository courseRepository,
            InstructorRepository instructorRepository,
            StudentRepository studentRepository,
            CategoryRepository categoryRepository,
            CourseMapper courseMapper) {
        this.courseRepository = courseRepository;
        this.instructorRepository = instructorRepository;
        this.studentRepository = studentRepository;
        this.categoryRepository = categoryRepository;
        this.courseMapper = courseMapper;
    }

    @Override
    @Transactional
    public CourseResponseDto createCourse(CourseRequestDto requestDto) {
        Course course = new Course(requestDto.title(), requestDto.level());
        applyRequest(course, requestDto);
        return courseMapper.toDto(courseRepository.save(course));
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
        return getCoursesInternal(level, false);
    }

    @Override
    @Transactional
    public CourseResponseDto updateCourse(Long id, CourseRequestDto requestDto) {
        Course course = courseRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException(COURSE_ENTITY, id));
        course.setTitle(requestDto.title());
        course.setLevel(requestDto.level());
        applyRequest(course, requestDto);
        return courseMapper.toDto(course);
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

    private List<CourseResponseDto> getCoursesInternal(String level, boolean optimized) {
        List<Course> courses;
        if (level == null || level.isBlank()) {
            courses = optimized ? courseRepository.findAllWithDetails() : courseRepository.findAll();
        } else {
            courses = optimized
                    ? courseRepository.findAllWithDetailsByLevel(level)
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
}
