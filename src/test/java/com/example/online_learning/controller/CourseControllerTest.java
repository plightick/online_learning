package com.example.online_learning.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.online_learning.dto.CourseRequestDto;
import com.example.online_learning.dto.CourseResponseDto;
import com.example.online_learning.dto.CourseSearchQueryType;
import com.example.online_learning.dto.LessonRequestDto;
import com.example.online_learning.service.CourseService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

    @Mock
    private CourseService courseService;

    private CourseController controller;

    @BeforeEach
    void setUp() {
        controller = new CourseController(courseService);
    }

    @Test
    void createCourseShouldReturnCreatedResponse() {
        CourseRequestDto requestDto = requestDto("Spring Security");
        CourseResponseDto responseDto = responseDto(1L, "Spring Security");
        when(courseService.createCourse(requestDto)).thenReturn(responseDto);

        ResponseEntity<CourseResponseDto> response = controller.createCourse(requestDto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(responseDto, response.getBody());
        verify(courseService).createCourse(requestDto);
    }

    @Test
    void createCoursesBulkShouldUseTransactionalServiceWhenRequested() {
        List<CourseRequestDto> requestDtos = List.of(requestDto("Bulk Tx"));
        List<CourseResponseDto> responseDtos = List.of(responseDto(2L, "Bulk Tx"));
        when(courseService.createCoursesBulkTx(requestDtos)).thenReturn(responseDtos);

        ResponseEntity<List<CourseResponseDto>> response = controller.createCoursesBulk(requestDtos, true);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(responseDtos, response.getBody());
        verify(courseService).createCoursesBulkTx(requestDtos);
        verify(courseService, never()).createCoursesBulkNoTx(requestDtos);
    }

    @Test
    void createCoursesBulkShouldUseNonTransactionalServiceWhenRequested() {
        List<CourseRequestDto> requestDtos = List.of(requestDto("Bulk No Tx"));
        List<CourseResponseDto> responseDtos = List.of(responseDto(3L, "Bulk No Tx"));
        when(courseService.createCoursesBulkNoTx(requestDtos)).thenReturn(responseDtos);

        ResponseEntity<List<CourseResponseDto>> response = controller.createCoursesBulk(requestDtos, false);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(responseDtos, response.getBody());
        verify(courseService).createCoursesBulkNoTx(requestDtos);
        verify(courseService, never()).createCoursesBulkTx(requestDtos);
    }

    @Test
    void getCoursesShouldBuildDescendingPageableWithMinimumValues() {
        PageRequest pageable = PageRequest.of(0, 1, Sort.by("title").descending());
        Page<CourseResponseDto> page = new PageImpl<>(List.of(responseDto(4L, "Cloud")));
        when(courseService.getCourses("ADVANCED", pageable)).thenReturn(page);

        ResponseEntity<Page<CourseResponseDto>> response = controller.getCourses(
                "ADVANCED",
                0,
                0,
                "title",
                false);

        assertSame(page, response.getBody());
        verify(courseService).getCourses("ADVANCED", pageable);
    }

    @Test
    void searchCoursesShouldBuildPageableWithMinimumValues() {
        PageRequest pageable = PageRequest.of(0, 1);
        Page<CourseResponseDto> page = new PageImpl<>(List.of(responseDto(5L, "Search")));
        when(courseService.searchCourses("Backend", "Security", CourseSearchQueryType.NATIVE, pageable))
                .thenReturn(page);

        ResponseEntity<Page<CourseResponseDto>> response = controller.searchCourses(
                "Backend",
                "Security",
                CourseSearchQueryType.NATIVE,
                0,
                0);

        assertSame(page, response.getBody());
        verify(courseService).searchCourses("Backend", "Security", CourseSearchQueryType.NATIVE, pageable);
    }

    @Test
    void getCourseByIdShouldReturnOkResponse() {
        CourseResponseDto responseDto = responseDto(6L, "Algorithms");
        when(courseService.getCourseById(6L)).thenReturn(responseDto);

        ResponseEntity<CourseResponseDto> response = controller.getCourseById(6L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(responseDto, response.getBody());
    }

    @Test
    void updateCourseShouldReturnUpdatedBody() {
        CourseRequestDto requestDto = requestDto("Updated Course");
        CourseResponseDto responseDto = responseDto(7L, "Updated Course");
        when(courseService.updateCourse(7L, requestDto)).thenReturn(responseDto);

        ResponseEntity<CourseResponseDto> response = controller.updateCourse(7L, requestDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(responseDto, response.getBody());
        verify(courseService).updateCourse(7L, requestDto);
    }

    @Test
    void deleteCourseShouldReturnNoContent() {
        ResponseEntity<Void> response = controller.deleteCourse(8L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(courseService).deleteCourse(8L);
    }

    @Test
    void demonstrateNPlusOneShouldReturnCourses() {
        List<CourseResponseDto> responseDtos = List.of(responseDto(9L, "N+1 Demo"));
        when(courseService.getCoursesWithNPlusOne("INTERMEDIATE")).thenReturn(responseDtos);

        ResponseEntity<List<CourseResponseDto>> response = controller.demonstrateNPlusOne("INTERMEDIATE");

        assertSame(responseDtos, response.getBody());
        verify(courseService).getCoursesWithNPlusOne("INTERMEDIATE");
    }

    @Test
    void getCoursesWithEntityGraphShouldReturnCourses() {
        List<CourseResponseDto> responseDtos = List.of(responseDto(10L, "Optimized"));
        when(courseService.getCoursesWithEntityGraph("ADVANCED")).thenReturn(responseDtos);

        ResponseEntity<List<CourseResponseDto>> response = controller.getCoursesWithEntityGraph("ADVANCED");

        assertSame(responseDtos, response.getBody());
        verify(courseService).getCoursesWithEntityGraph("ADVANCED");
    }

    private static CourseRequestDto requestDto(String title) {
        return new CourseRequestDto(
                title,
                "ADVANCED",
                "Jane",
                "Doe",
                "Backend",
                List.of(new LessonRequestDto("Intro", 30, 1)),
                List.of(),
                List.of("Backend"));
    }

    private static CourseResponseDto responseDto(Long id, String title) {
        return new CourseResponseDto(
                id,
                title,
                "ADVANCED",
                "Jane",
                "Doe",
                List.of(),
                List.of(),
                List.of());
    }
}
