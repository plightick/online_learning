package com.example.online_learning.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String level;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instructor_id", nullable = false)
    private Instructor instructor;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<Lesson> lessons = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "course_students",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "student_id"))
    private final Set<Student> students = new LinkedHashSet<>();

    @ManyToMany
    @JoinTable(
            name = "course_categories",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private final Set<Category> categories = new LinkedHashSet<>();

    protected Course() {
    }

    public Course(String title, String level) {
        this.title = title;
        this.level = level;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getLevel() {
        return level;
    }

    public Instructor getInstructor() {
        return instructor;
    }

    public List<Lesson> getLessons() {
        return lessons;
    }

    public Set<Student> getStudents() {
        return students;
    }

    public Set<Category> getCategories() {
        return categories;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public void setInstructor(Instructor instructor) {
        if (this.instructor == instructor) {
            return;
        }
        if (this.instructor != null) {
            this.instructor.getCourses().remove(this);
        }
        this.instructor = instructor;
        if (instructor != null && !instructor.getCourses().contains(this)) {
            instructor.getCourses().add(this);
        }
    }

    public void replaceLessons(List<Lesson> updatedLessons) {
        lessons.clear();
        for (Lesson lesson : updatedLessons) {
            addLesson(lesson);
        }
    }

    public void addLesson(Lesson lesson) {
        lessons.add(lesson);
        lesson.setCourse(this);
    }

    public void addStudent(Student student) {
        students.add(student);
        student.getCourses().add(this);
    }

    public void removeStudent(Student student) {
        students.remove(student);
        student.getCourses().remove(this);
    }

    public void clearStudents() {
        List<Student> existingStudents = new ArrayList<>(students);
        for (Student student : existingStudents) {
            removeStudent(student);
        }
    }

    public void addCategory(Category category) {
        categories.add(category);
        category.getCourses().add(this);
    }

    public void removeCategory(Category category) {
        categories.remove(category);
        category.getCourses().remove(this);
    }

    public void clearCategories() {
        List<Category> existingCategories = new ArrayList<>(categories);
        for (Category category : existingCategories) {
            removeCategory(category);
        }
    }
}
