package com.example.online_learning.repository;

import com.example.online_learning.dto.CourseResponseDto;
import com.example.online_learning.dto.LessonResponseDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class CourseRepositoryNativeSearchImpl implements CourseRepositoryNativeSearch {

    private static final String ITEM_SEPARATOR = "\u001E";
    private static final String FIELD_SEPARATOR = "\u001F";
    private static final String ITEM_SEPARATOR_SQL = "chr(30)";
    private static final String FIELD_SEPARATOR_SQL = "chr(31)";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public PagedCourseIds findPagedCourseIdsByCategoryAndInstructor(
            String categoryName,
            String instructorSpecialization,
            Pageable pageable) {
        String filterSql = buildFilterSql(categoryName, instructorSpecialization);
        String orderByClause = buildCourseOrderByClause(pageable);
        String idsSql = """
                with filtered_course_ids as (
                    select distinct c.id, c.title, c.level
                    from courses c
                    join instructors i on i.id = c.instructor_id
                    left join course_categories cc on cc.course_id = c.id
                    left join categories cat on cat.id = cc.category_id
                """
                + filterSql
                + """
                ),
                paged_course_ids as (
                    select
                        fci.id,
                        count(*) over() as total_count
                    from filtered_course_ids fci
                    """
                + orderByClause
                + """
                    limit %d offset %d
                )
                select id, total_count
                from paged_course_ids
                order by id
                """.formatted(pageable.getPageSize(), pageable.getOffset());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(idsSql).getResultList();
        if (rows.isEmpty()) {
            return new PagedCourseIds(List.of(), 0L);
        }

        List<Long> courseIds = rows.stream()
                .map(row -> ((Number) row[0]).longValue())
                .toList();
        long totalElements = ((Number) rows.getFirst()[1]).longValue();
        return new PagedCourseIds(courseIds, totalElements);
    }

    @Override
    public Page<CourseResponseDto> searchDetailedCoursesNative(
            String categoryName,
            String instructorSpecialization,
            Pageable pageable) {
        String filterSql = buildFilterSql(categoryName, instructorSpecialization);
        String countSql = """
                select count(distinct c.id)
                from courses c
                join instructors i on i.id = c.instructor_id
                left join course_categories cc on cc.course_id = c.id
                left join categories cat on cat.id = cc.category_id
                """
                + filterSql;
        Number totalElements = (Number) entityManager.createNativeQuery(countSql).getSingleResult();

        if (totalElements.longValue() == 0L) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        String dataSql = """
                with paged_courses as (
                    select distinct c.id, c.title, c.level, c.instructor_id
                    from courses c
                    join instructors i on i.id = c.instructor_id
                    left join course_categories cc on cc.course_id = c.id
                    left join categories cat on cat.id = cc.category_id
                    """
                + filterSql
                + """
                    order by c.id
                    limit %d offset %d
                ),
                lesson_agg as (
                    select
                        l.course_id,
                        string_agg(
                            cast(l.id as varchar) || %s
                                || l.title || %s
                                || cast(l.duration_minutes as varchar) || %s
                                || cast(l.lesson_order as varchar),
                            %s
                            order by l.lesson_order
                        ) as lessons_data
                    from lessons l
                    where l.course_id in (select id from paged_courses)
                    group by l.course_id
                ),
                student_agg as (
                    select
                        cs.course_id,
                        string_agg(
                            distinct s.first_name || ' ' || s.last_name,
                            %s
                            order by s.first_name || ' ' || s.last_name
                        ) as student_names_data
                    from course_students cs
                    join students s on s.id = cs.student_id
                    where cs.course_id in (select id from paged_courses)
                    group by cs.course_id
                ),
                category_agg as (
                    select
                        cc.course_id,
                        string_agg(
                            distinct cat.name,
                            %s
                            order by cat.name
                        ) as category_names_data
                    from course_categories cc
                    join categories cat on cat.id = cc.category_id
                    where cc.course_id in (select id from paged_courses)
                    group by cc.course_id
                )
                select
                    pc.id,
                    pc.title,
                    pc.level,
                    i.first_name,
                    i.last_name,
                    la.lessons_data,
                    sa.student_names_data,
                    ca.category_names_data
                from paged_courses pc
                join instructors i on i.id = pc.instructor_id
                left join lesson_agg la on la.course_id = pc.id
                left join student_agg sa on sa.course_id = pc.id
                left join category_agg ca on ca.course_id = pc.id
                order by pc.id
                """.formatted(
                pageable.getPageSize(),
                pageable.getOffset(),
                FIELD_SEPARATOR_SQL,
                FIELD_SEPARATOR_SQL,
                FIELD_SEPARATOR_SQL,
                ITEM_SEPARATOR_SQL,
                ITEM_SEPARATOR_SQL,
                ITEM_SEPARATOR_SQL);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(dataSql).getResultList();

        List<CourseResponseDto> content = rows.stream()
                .map(this::mapRow)
                .toList();
        return new PageImpl<>(content, pageable, totalElements.longValue());
    }

    private CourseResponseDto mapRow(Object[] row) {
        return new CourseResponseDto(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                (String) row[3],
                (String) row[4],
                parseLessons((String) row[5]),
                parseSimpleList((String) row[6]),
                parseSimpleList((String) row[7]));
    }

    private String buildFilterSql(String categoryName, String instructorSpecialization) {
        List<String> conditions = new ArrayList<>();
        if (categoryName != null) {
            conditions.add("lower(cat.name) = " + toSqlLiteral(categoryName));
        }
        if (instructorSpecialization != null) {
            conditions.add("lower(i.specialization) = " + toSqlLiteral(instructorSpecialization));
        }
        if (conditions.isEmpty()) {
            return "";
        }
        return "\nwhere " + String.join("\n    and ", conditions) + "\n";
    }

    private String buildCourseOrderByClause(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return "\norder by fci.id asc\n";
        }

        List<String> orderExpressions = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            orderExpressions.add(resolveSortableColumn(order.getProperty())
                    + (order.isAscending() ? " asc" : " desc"));
        }
        return "\norder by " + String.join(", ", orderExpressions) + "\n";
    }

    private String resolveSortableColumn(String property) {
        String normalizedProperty = Objects.requireNonNullElse(property, "id")
                .trim()
                .toLowerCase(Locale.ROOT);
        return switch (normalizedProperty) {
            case "id" -> "fci.id";
            case "title" -> "fci.title";
            case "level" -> "fci.level";
            default -> throw new IllegalArgumentException("Unsupported sort field: " + property);
        };
    }

    private String toSqlLiteral(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    private List<LessonResponseDto> parseLessons(String serializedLessons) {
        if (serializedLessons == null || serializedLessons.isBlank()) {
            return List.of();
        }
        List<LessonResponseDto> lessons = new ArrayList<>();
        for (String serializedLesson : serializedLessons.split(ITEM_SEPARATOR, -1)) {
            String[] fields = serializedLesson.split(Pattern.quote(FIELD_SEPARATOR), -1);
            lessons.add(new LessonResponseDto(
                    Long.parseLong(fields[0]),
                    fields[1],
                    Integer.parseInt(fields[2]),
                    Integer.parseInt(fields[3])));
        }
        return lessons;
    }

    private List<String> parseSimpleList(String serializedValues) {
        if (serializedValues == null || serializedValues.isBlank()) {
            return List.of();
        }
        return List.of(serializedValues.split(Pattern.quote(ITEM_SEPARATOR), -1));
    }
}
