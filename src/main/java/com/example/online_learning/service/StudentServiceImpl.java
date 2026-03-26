package com.example.online_learning.service;

import com.example.online_learning.cache.CourseSearchCacheInvalidator;
import com.example.online_learning.dto.StudentRequestDto;
import com.example.online_learning.dto.StudentResponseDto;
import com.example.online_learning.entity.Course;
import com.example.online_learning.entity.Student;
import com.example.online_learning.exception.ResourceNotFoundException;
import com.example.online_learning.mapper.StudentMapper;
import com.example.online_learning.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentServiceImpl implements StudentService {

    private static final String STUDENT_ENTITY = "Student";

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;
    private final CourseSearchCacheInvalidator courseSearchCacheInvalidator;

    public StudentServiceImpl(
            StudentRepository studentRepository,
            StudentMapper studentMapper,
            CourseSearchCacheInvalidator courseSearchCacheInvalidator) {
        this.studentRepository = studentRepository;
        this.studentMapper = studentMapper;
        this.courseSearchCacheInvalidator = courseSearchCacheInvalidator;
    }

    @Override
    @Transactional
    public StudentResponseDto createStudent(StudentRequestDto requestDto) {
        Student student = new Student(requestDto.firstName(), requestDto.lastName(), requestDto.email());
        StudentResponseDto responseDto = studentMapper.toDto(studentRepository.save(student));
        invalidateSearchIndex();
        return responseDto;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<StudentResponseDto> getStudents() {
        return studentRepository.findAll().stream()
                .map(studentMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StudentResponseDto getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_ENTITY, id));
        return studentMapper.toDto(student);
    }

    @Override
    @Transactional
    public StudentResponseDto updateStudent(Long id, StudentRequestDto requestDto) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_ENTITY, id));
        student.setFirstName(requestDto.firstName());
        student.setLastName(requestDto.lastName());
        student.setEmail(requestDto.email());
        StudentResponseDto responseDto = studentMapper.toDto(student);
        invalidateSearchIndex();
        return responseDto;
    }

    @Override
    @Transactional
    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_ENTITY, id));
        for (Course course : java.util.List.copyOf(student.getCourses())) {
            course.removeStudent(student);
        }
        studentRepository.delete(student);
        invalidateSearchIndex();
    }

    private void invalidateSearchIndex() {
        courseSearchCacheInvalidator.invalidate();
    }
}
