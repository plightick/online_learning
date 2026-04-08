package com.example.online_learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.online_learning.dto.LessonRequestDto;
import com.example.online_learning.dto.RelatedSaveRequestDto;
import com.example.online_learning.entity.Student;
import com.example.online_learning.repository.CategoryRepository;
import com.example.online_learning.repository.CourseRepository;
import com.example.online_learning.repository.InstructorRepository;
import com.example.online_learning.repository.LessonRepository;
import com.example.online_learning.repository.StudentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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
class PersistenceDemoTransactionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private StudentRepository studentRepository;

    private Long existingStudentId;

    @BeforeEach
    void setUp() {
        courseRepository.deleteAll();
        instructorRepository.deleteAll();
        categoryRepository.deleteAll();
        studentRepository.deleteAll();

        Student student = studentRepository.save(new Student("Alex", "Novak", "alex.demo@example.com"));
        existingStudentId = student.getId();
    }

    @Test
    void saveWithoutTransactionEndpointShouldKeepPersistedDataAfterFailure() throws Exception {
        mockMvc.perform(post("/api/demo/persistence/without-transaction")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("WITHOUT_TRANSACTION"))
                .andExpect(jsonPath("$.message").value("Simulated failure after partial persistence"))
                .andExpect(jsonPath("$.instructorId").isNumber())
                .andExpect(jsonPath("$.courseId").isNumber())
                .andExpect(jsonPath("$.persistedLessons").value(1));

        assertThat(courseRepository.count()).isEqualTo(1);
        assertThat(lessonRepository.count()).isEqualTo(1);
        assertThat(instructorRepository.count()).isEqualTo(1);
        assertThat(categoryRepository.count()).isEqualTo(1);
        assertThat(courseRepository.findByTitleIgnoreCase("SQL Essentials")).isPresent();
    }

    @Test
    void saveWithTransactionEndpointShouldRollbackPersistedDataAfterFailure() throws Exception {
        mockMvc.perform(post("/api/demo/persistence/with-transaction")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("WITH_TRANSACTION"))
                .andExpect(jsonPath("$.message").value("Simulated failure after partial persistence"))
                .andExpect(jsonPath("$.instructorId").value(nullValue()))
                .andExpect(jsonPath("$.courseId").value(nullValue()))
                .andExpect(jsonPath("$.persistedLessons").value(0));

        assertThat(courseRepository.count()).isZero();
        assertThat(lessonRepository.count()).isZero();
        assertThat(instructorRepository.count()).isZero();
        assertThat(categoryRepository.count()).isZero();
    }

    private RelatedSaveRequestDto requestDto() {
        return new RelatedSaveRequestDto(
                "Jane",
                "Brown",
                "Databases",
                "SQL Essentials",
                "INTERMEDIATE",
                List.of(new LessonRequestDto("Intro", 30, 1)),
                List.of("Backend"),
                List.of(existingStudentId));
    }
}
