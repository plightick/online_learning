package com.example.online_learning.service;

import com.example.online_learning.cache.CourseSearchCacheInvalidator;
import com.example.online_learning.dto.LessonRequestDto;
import com.example.online_learning.dto.RelatedSaveRequestDto;
import com.example.online_learning.entity.Category;
import com.example.online_learning.entity.Course;
import com.example.online_learning.entity.Instructor;
import com.example.online_learning.entity.Lesson;
import com.example.online_learning.entity.Student;
import com.example.online_learning.exception.ResourceNotFoundException;
import com.example.online_learning.repository.CategoryRepository;
import com.example.online_learning.repository.CourseRepository;
import com.example.online_learning.repository.InstructorRepository;
import com.example.online_learning.repository.LessonRepository;
import com.example.online_learning.repository.StudentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RelatedPersistenceTransactionalWorker {

    private final InstructorRepository instructorRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final CategoryRepository categoryRepository;
    private final StudentRepository studentRepository;
    private final CourseSearchCacheInvalidator courseSearchCacheInvalidator;

    public RelatedPersistenceTransactionalWorker(
            InstructorRepository instructorRepository,
            CourseRepository courseRepository,
            LessonRepository lessonRepository,
            CategoryRepository categoryRepository,
            StudentRepository studentRepository,
            CourseSearchCacheInvalidator courseSearchCacheInvalidator) {
        this.instructorRepository = instructorRepository;
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.categoryRepository = categoryRepository;
        this.studentRepository = studentRepository;
        this.courseSearchCacheInvalidator = courseSearchCacheInvalidator;
    }

    @Transactional
    public void saveWithRollback(RelatedSaveRequestDto requestDto) {
        persistScenario(requestDto);
    }

    public void persistScenario(RelatedSaveRequestDto requestDto) {
        try {
            Instructor instructor = instructorRepository.save(new Instructor(
                    requestDto.instructorFirstName(),
                    requestDto.instructorLastName(),
                    requestDto.instructorSpecialization()));

            Course course = new Course(requestDto.courseTitle(), requestDto.courseLevel());
            course.setInstructor(instructor);
            courseRepository.save(course);

            attachSharedRelations(course, requestDto.categoryNames(), requestDto.studentIds());
            courseRepository.save(course);

            LessonRequestDto firstLesson = requestDto.lessons().getFirst();
            Lesson lesson = new Lesson(
                    firstLesson.title(),
                    firstLesson.durationMinutes(),
                    firstLesson.lessonOrder());
            lesson.setCourse(course);
            lessonRepository.save(lesson);

            throw new IllegalStateException("Simulated failure after partial persistence");
        } finally {
            courseSearchCacheInvalidator.invalidate("related persistence scenario");
        }
    }

    private void attachSharedRelations(Course course, List<String> categoryNames, List<Long> studentIds) {
        if (categoryNames != null) {
            for (String categoryName : categoryNames) {
                Category category = categoryRepository.findByNameIgnoreCase(categoryName)
                        .orElseGet(() -> categoryRepository.save(new Category(categoryName)));
                course.addCategory(category);
            }
        }
        if (studentIds != null) {
            for (Long studentId : studentIds) {
                Student student = studentRepository.findById(studentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
                course.addStudent(student);
            }
        }
    }

}
