CREATE DATABASE IF NOT EXISTS examease;
USE examease;

CREATE TABLE IF NOT EXISTS users (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(100) NOT NULL,
 username VARCHAR(150) NOT NULL UNIQUE,
 password_hash VARCHAR(255) NOT NULL,
 role ENUM('TEACHER','STUDENT') NOT NULL
);
CREATE TABLE IF NOT EXISTS subjects (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(100) NOT NULL UNIQUE,
 description VARCHAR(500)
);
CREATE TABLE IF NOT EXISTS exams (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 title VARCHAR(150) NOT NULL,
 subject_id BIGINT NOT NULL,
 created_by BIGINT NOT NULL,
 duration_minutes INT NOT NULL,
 starts_at DATETIME NOT NULL,
 ends_at DATETIME NOT NULL,
 published BOOLEAN NOT NULL DEFAULT FALSE,
 FOREIGN KEY(subject_id) REFERENCES subjects(id),
 FOREIGN KEY(created_by) REFERENCES users(id),
 CHECK(duration_minutes > 0),
 CHECK(ends_at > starts_at)
);
CREATE TABLE IF NOT EXISTS questions (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 exam_id BIGINT NOT NULL,
 text VARCHAR(2000) NOT NULL,
 marks INT NOT NULL,
 FOREIGN KEY(exam_id) REFERENCES exams(id),
 CHECK(marks > 0)
);
CREATE TABLE IF NOT EXISTS question_options (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 question_id BIGINT NOT NULL,
 text VARCHAR(1000) NOT NULL,
 is_correct BOOLEAN NOT NULL DEFAULT FALSE,
 FOREIGN KEY(question_id) REFERENCES questions(id)
);
CREATE TABLE IF NOT EXISTS exam_attempts (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 exam_id BIGINT NOT NULL,
 student_id BIGINT NOT NULL,
 started_at DATETIME NOT NULL,
 submitted_at DATETIME,
 status VARCHAR(255) NOT NULL,
 score INT,
 total_marks INT,
 FOREIGN KEY(exam_id) REFERENCES exams(id),
 FOREIGN KEY(student_id) REFERENCES users(id),
 UNIQUE(exam_id,student_id)
);
CREATE TABLE IF NOT EXISTS student_answers (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 attempt_id BIGINT NOT NULL,
 question_id BIGINT NOT NULL,
 selected_option_id BIGINT,
 marks_awarded INT NOT NULL,
 FOREIGN KEY(attempt_id) REFERENCES exam_attempts(id),
 FOREIGN KEY(question_id) REFERENCES questions(id),
 FOREIGN KEY(selected_option_id) REFERENCES question_options(id),
 UNIQUE(attempt_id,question_id)
);
