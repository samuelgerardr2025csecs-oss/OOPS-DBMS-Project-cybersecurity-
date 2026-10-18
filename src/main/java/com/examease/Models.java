package com.examease;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

enum Role { TEACHER, STUDENT }

@Entity @Table(name="users")
class Account {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(nullable=false,length=100) String name;
    @Column(nullable=false,unique=true,length=150) String username;
    @Column(nullable=false,name="password_hash") String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable=false) Role role;
}

@Entity @Table(name="subjects")
class Subject {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(nullable=false,unique=true,length=100) String name;
    @Column(length=500) String description;
}

@Entity @Table(name="exams")
class Exam {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(nullable=false,length=150) String title;
    @ManyToOne(optional=false) @JoinColumn(name="subject_id") Subject subject;
    @ManyToOne(optional=false) @JoinColumn(name="created_by") Account teacher;
    @Column(nullable=false,name="duration_minutes") int durationMinutes;
    @Column(nullable=false,name="starts_at") LocalDateTime startsAt;
    @Column(nullable=false,name="ends_at") LocalDateTime endsAt;
    @Column(nullable=false) boolean published;
    @OneToMany(mappedBy="exam",cascade=CascadeType.ALL,orphanRemoval=true) List<Question> questions = new ArrayList<>();
}

@Entity @Table(name="questions")
class Question {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @ManyToOne(optional=false) @JoinColumn(name="exam_id") Exam exam;
    @Column(nullable=false,length=2000) String text;
    @Column(nullable=false) int marks;
    @OneToMany(mappedBy="question",cascade=CascadeType.ALL,orphanRemoval=true) List<Choice> choices = new ArrayList<>();
}

@Entity @Table(name="question_options")
class Choice {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @ManyToOne(optional=false) @JoinColumn(name="question_id") Question question;
    @Column(nullable=false,length=1000) String text;
    @Column(nullable=false,name="is_correct") boolean correct;
}

@Entity @Table(name="exam_attempts",uniqueConstraints=@UniqueConstraint(columnNames={"exam_id","student_id"}))
class Attempt {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @ManyToOne(optional=false) @JoinColumn(name="exam_id") Exam exam;
    @ManyToOne(optional=false) @JoinColumn(name="student_id") Account student;
    @Column(nullable=false,name="started_at") LocalDateTime startedAt;
    @Column(name="submitted_at") LocalDateTime submittedAt;
    @Column(nullable=false) String status="IN_PROGRESS";
    Integer score;
    @Column(name="total_marks") Integer totalMarks;
    @OneToMany(mappedBy="attempt",cascade=CascadeType.ALL,orphanRemoval=true) List<StudentAnswer> answers = new ArrayList<>();
}

@Entity @Table(name="student_answers",uniqueConstraints=@UniqueConstraint(columnNames={"attempt_id","question_id"}))
class StudentAnswer {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @ManyToOne(optional=false) @JoinColumn(name="attempt_id") Attempt attempt;
    @ManyToOne(optional=false) @JoinColumn(name="question_id") Question question;
    @ManyToOne @JoinColumn(name="selected_option_id") Choice selected;
    @Column(nullable=false,name="marks_awarded") int marksAwarded;
}
