package com.examease;

import jakarta.validation.Valid;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/auth")
class AuthController {
    private final Users users; private final PasswordEncoder encoder; private final AuthenticationManager manager; private final JwtTokens jwt;
    AuthController(Users users,PasswordEncoder encoder,AuthenticationManager manager,JwtTokens jwt){
        this.users=users;this.encoder=encoder;this.manager=manager;this.jwt=jwt;
    }
    @PostMapping("/register") Map<String,Object> register(@Valid @RequestBody Dtos.Register req){
        String username=req.username().trim().toLowerCase();
        if(users.existsByUsername(username)) throw new IllegalStateException("Username already exists");
        Account a=new Account();a.name=req.name().trim();a.username=username;a.passwordHash=encoder.encode(req.password());a.role=Role.STUDENT;
        users.save(a);return response(a);
    }
    @PostMapping("/login") Map<String,Object> login(@Valid @RequestBody Dtos.Login req){
        String username=req.username().trim().toLowerCase();
        manager.authenticate(new UsernamePasswordAuthenticationToken(username,req.password()));
        return response(users.findByUsername(username).orElseThrow());
    }
    @GetMapping("/me") Map<String,Object> me(Authentication auth){
        Account a=users.findByUsername(auth.getName()).orElseThrow();
        return Map.of("id",a.id,"name",a.name,"username",a.username,"role",a.role.name());
    }
    private Map<String,Object> response(Account a){
        return Map.of("token",jwt.create(a.username),"user",Map.of("id",a.id,"name",a.name,"username",a.username,"role",a.role.name()));
    }
}
