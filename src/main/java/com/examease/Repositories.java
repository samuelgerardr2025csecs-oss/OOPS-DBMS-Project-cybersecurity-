package com.examease;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

interface Users extends JpaRepository<Account,Long> {
    Optional<Account> findByUsername(String username);
    boolean existsByUsername(String username);
}
interface Subjects extends JpaRepository<Subject,Long> { boolean existsByNameIgnoreCase(String name); }
interface Exams extends JpaRepository<Exam,Long> { List<Exam> findByPublishedTrue(); }
interface Attempts extends JpaRepository<Attempt,Long> {
    boolean existsByExamIdAndStudentId(Long examId,Long studentId);
    List<Attempt> findByStudentId(Long studentId);
    List<Attempt> findByExamId(Long examId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Attempt a where a.id=:id") Optional<Attempt> lockById(@Param("id") Long id);
}
