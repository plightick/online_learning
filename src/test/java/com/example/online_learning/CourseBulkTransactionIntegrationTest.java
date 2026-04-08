package com.example.online_learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.online_learning.dto.CourseRequestDto;
import com.example.online_learning.dto.LessonRequestDto;
import com.example.online_learning.entity.Student;
import com.example.online_learning.exception.ResourceNotFoundException;
import com.example.online_learning.repository.CategoryRepository;
import com.example.online_learning.repository.CourseRepository;
import com.example.online_learning.repository.InstructorRepository;
import com.example.online_learning.repository.LessonRepository;
import com.example.online_learning.repository.StudentRepository;
import com.example.online_learning.service.CourseService;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CourseBulkTransactionIntegrationTest {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Long existingStudentId;

    @BeforeEach
    void setUp() {
        courseRepository.deleteAll();
        instructorRepository.deleteAll();
        categoryRepository.deleteAll();
        studentRepository.deleteAll();

        Student student = studentRepository.save(new Student("Alex", "Novak", "alex.bulk@example.com"));
        existingStudentId = student.getId();
    }

    @Test
    void createCoursesBulkTxShouldRollbackAllChangesWhenLaterItemFails() {
        List<CourseRequestDto> requests = bulkRequests();

        assertThatThrownBy(() -> courseService.createCoursesBulkTx(requests))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        assertThat(courseRepository.count()).isZero();
        assertThat(lessonRepository.count()).isZero();
        assertThat(instructorRepository.count()).isZero();
        assertThat(categoryRepository.count()).isZero();
    }

    @Test
    void createCoursesBulkNoTxShouldKeepFirstCourseWhenLaterItemFails() {
        List<CourseRequestDto> requests = bulkRequests();

        assertThatThrownBy(() -> courseService.createCoursesBulkNoTx(requests))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        assertThat(courseRepository.count()).isEqualTo(1);
        assertThat(lessonRepository.count()).isEqualTo(1);
        assertThat(instructorRepository.count()).isEqualTo(1);
        assertThat(categoryRepository.count()).isEqualTo(2);
        assertThat(courseRepository.findByTitleIgnoreCase("Spring Security Deep Dive")).isPresent();
    }

    @Test
    void bulkEndpointTransactionalTrueShouldRollbackAllChangesWhenLaterItemFails() throws Exception {
        List<CourseRequestDto> requests = bulkRequests();

        mockMvc.perform(post("/api/courses/bulk")
                        .param("transactional", "true")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Student with id 999 was not found"));

        assertThat(courseRepository.count()).isZero();
        assertThat(lessonRepository.count()).isZero();
        assertThat(instructorRepository.count()).isZero();
        assertThat(categoryRepository.count()).isZero();
    }

    @Test
    void bulkEndpointTransactionalFalseShouldKeepFirstCourseWhenLaterItemFails() throws Exception {
        List<CourseRequestDto> requests = bulkRequests();

        mockMvc.perform(post("/api/courses/bulk")
                        .param("transactional", "false")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Student with id 999 was not found"));

        assertThat(courseRepository.count()).isEqualTo(1);
        assertThat(lessonRepository.count()).isEqualTo(1);
        assertThat(instructorRepository.count()).isEqualTo(1);
        assertThat(categoryRepository.count()).isEqualTo(2);
        assertThat(courseRepository.findByTitleIgnoreCase("Spring Security Deep Dive")).isPresent();
    }

    private List<CourseRequestDto> bulkRequests() {
        return List.of(
                new CourseRequestDto(
                        "Spring Security Deep Dive",
                        "ADVANCED",
                        "Pavel",
                        "Ivanov",
                        "Security",
                        List.of(new LessonRequestDto("Authentication", 40, 1)),
                        List.of(existingStudentId),
                        List.of("Backend", "Security")),
                new CourseRequestDto(
                        "Broken Bulk Demo",
                        "ADVANCED",
                        "Pavel",
                        "Ivanov",
                        "Security",
                        List.of(new LessonRequestDto("Authorization", 45, 1)),
                        List.of(999L),
                        List.of("Backend")));
    }
}
