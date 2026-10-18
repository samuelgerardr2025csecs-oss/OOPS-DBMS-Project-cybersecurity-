package com.examease;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.util.Map;

@RestControllerAdvice
class ApiErrors {
    @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<?> invalid(IllegalArgumentException ex){
        return ResponseEntity.badRequest().body(Map.of("error",ex.getMessage()));
    }
    @ExceptionHandler(IllegalStateException.class) ResponseEntity<?> conflict(IllegalStateException ex){
        return ResponseEntity.status(409).body(Map.of("error",ex.getMessage()));
    }
    @ExceptionHandler(BadCredentialsException.class) ResponseEntity<?> login(BadCredentialsException ex){
        return ResponseEntity.status(401).body(Map.of("error","Invalid username or password"));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation(MethodArgumentNotValidException ex){
        String error=ex.getBindingResult().getFieldErrors().stream().findFirst()
            .map(f->f.getField()+": "+f.getDefaultMessage()).orElse("Invalid input");
        return ResponseEntity.badRequest().body(Map.of("error",error));
    }
}
