package com.example.online_learning.repository;

import java.util.List;

public record PagedCourseIds(List<Long> courseIds, long totalElements) {
}
