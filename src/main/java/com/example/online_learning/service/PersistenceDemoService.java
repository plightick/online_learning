package com.example.online_learning.service;

import com.example.online_learning.dto.RelatedSaveRequestDto;
import com.example.online_learning.dto.RelatedSaveResponseDto;
import com.example.online_learning.exception.LoggingException;
import com.example.online_learning.repository.CourseRepository;
import com.example.online_learning.repository.InstructorRepository;
import com.example.online_learning.repository.LessonRepository;
import org.springframework.stereotype.Service;

@Service
public class PersistenceDemoService {

    private final RelatedPersistenceTransactionalWorker transactionalWorker;
    private final InstructorRepository instructorRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;

    public PersistenceDemoService(
            RelatedPersistenceTransactionalWorker transactionalWorker,
            InstructorRepository instructorRepository,
            CourseRepository courseRepository,
            LessonRepository lessonRepository) {
        this.transactionalWorker = transactionalWorker;
        this.instructorRepository = instructorRepository;
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
    }

    public RelatedSaveResponseDto saveWithoutTransaction(RelatedSaveRequestDto requestDto) {
        try {
            transactionalWorker.persistScenario(requestDto);
            return buildResponse("WITHOUT_TRANSACTION", "Unexpected success", requestDto);
        } catch (IllegalStateException | LoggingException exception) {
            return buildResponse("WITHOUT_TRANSACTION", extractFailureMessage(exception), requestDto);
        }
    }

    public RelatedSaveResponseDto saveWithTransaction(RelatedSaveRequestDto requestDto) {
        try {
            transactionalWorker.saveWithRollback(requestDto);
            return buildResponse("WITH_TRANSACTION", "Unexpected success", requestDto);
        } catch (IllegalStateException | LoggingException exception) {
            return buildResponse("WITH_TRANSACTION", extractFailureMessage(exception), requestDto);
        }
    }

    private RelatedSaveResponseDto buildResponse(
            String mode,
            String message,
            RelatedSaveRequestDto requestDto) {
        Long instructorId = instructorRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase(
                        requestDto.instructorFirstName(),
                        requestDto.instructorLastName())
                .map(instructor -> instructor.getId())
                .orElse(null);
        Long courseId = courseRepository.findByTitleIgnoreCase(requestDto.courseTitle())
                .map(course -> course.getId())
                .orElse(null);
        return new RelatedSaveResponseDto(
                mode,
                message,
                instructorId,
                courseId,
                lessonRepository.countByCourseTitleIgnoreCase(requestDto.courseTitle()));
    }

    private String extractFailureMessage(Exception exception) {
        if (exception instanceof LoggingException loggingException && loggingException.getCause() != null) {
            return loggingException.getCause().getMessage();
        }
        return exception.getMessage();
    }
}
