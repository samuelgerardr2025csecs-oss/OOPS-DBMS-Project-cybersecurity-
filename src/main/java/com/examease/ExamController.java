package com.examease;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api")
class ExamController {
    private final ExamService service;private final Subjects subjects;
    ExamController(ExamService service,Subjects subjects){this.service=service;this.subjects=subjects;}
    @GetMapping("/subjects") List<Map<String,Object>> subjects(){
        return subjects.findAll().stream().map(s->{
            Map<String,Object> m=new LinkedHashMap<>();m.put("id",s.id);m.put("name",s.name);m.put("description",s.description);return m;
        }).toList();
    }
    @PostMapping("/subjects") @PreAuthorize("hasRole('TEACHER')") Map<String,Object> createSubject(@Valid @RequestBody Dtos.SubjectInput req){
        if(subjects.existsByNameIgnoreCase(req.name().trim())) throw new IllegalStateException("Subject already exists");
        Subject s=new Subject();s.name=req.name().trim();s.description=req.description();subjects.save(s);
        return Map.of("id",s.id,"name",s.name);
    }
    @PostMapping("/exams") @PreAuthorize("hasRole('TEACHER')") Dtos.ExamView create(@Valid @RequestBody Dtos.ExamInput req,Authentication auth){return service.create(req,auth.getName());}
    @GetMapping("/exams") @PreAuthorize("hasRole('TEACHER')") List<Dtos.ExamView> all(){return service.all();}
    @PatchMapping("/exams/{id}/publish") @PreAuthorize("hasRole('TEACHER')") Dtos.ExamView publish(@PathVariable Long id){return service.publish(id);}
    @GetMapping("/exams/available") @PreAuthorize("hasRole('STUDENT')") List<Dtos.ExamView> available(){return service.available();}
    @PostMapping("/exams/{id}/start") @PreAuthorize("hasRole('STUDENT')") Dtos.AttemptView start(@PathVariable Long id,Authentication auth){return service.start(id,auth.getName());}
    @GetMapping("/attempts/{id}") @PreAuthorize("hasRole('STUDENT')") Dtos.AttemptView attempt(@PathVariable Long id,Authentication auth){return service.getAttempt(id,auth.getName());}
    @PostMapping("/attempts/{id}/submit") @PreAuthorize("hasRole('STUDENT')") Dtos.Result submit(@PathVariable Long id,@Valid @RequestBody Dtos.Submission req,Authentication auth){return service.submit(id,auth.getName(),req.answers());}
    @GetMapping("/results/my") @PreAuthorize("hasRole('STUDENT')") List<Dtos.Result> mine(Authentication auth){return service.myResults(auth.getName());}
    @GetMapping("/exams/{id}/results") @PreAuthorize("hasRole('TEACHER')") List<Dtos.Result> results(@PathVariable Long id){return service.examResults(id);}
}
