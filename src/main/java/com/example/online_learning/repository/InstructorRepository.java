package com.example.online_learning.repository;

import com.example.online_learning.entity.Instructor;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstructorRepository extends JpaRepository<Instructor, Long> {

    Optional<Instructor> findByFirstNameIgnoreCaseAndLastNameIgnoreCase(String firstName, String lastName);
}
