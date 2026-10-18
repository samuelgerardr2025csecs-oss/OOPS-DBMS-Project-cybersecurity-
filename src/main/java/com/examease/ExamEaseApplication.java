package com.examease;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class ExamEaseApplication {
    public static void main(String[] args) { SpringApplication.run(ExamEaseApplication.class, args); }
    @Bean CommandLineRunner seed(Users users, PasswordEncoder encoder,
        @Value("${app.teacher.name}") String name,
        @Value("${app.teacher.username}") String username,
        @Value("${app.teacher.password}") String password) {
        String normalizedUsername = username.trim().toLowerCase();
        return args -> {
            if (!users.existsByUsername(normalizedUsername)) {
                Account a = new Account(); a.name = name; a.username = normalizedUsername;
                a.passwordHash = encoder.encode(password); a.role = Role.TEACHER; users.save(a);
                System.out.println("Teacher created: " + normalizedUsername);
            }
        };
    }
}
