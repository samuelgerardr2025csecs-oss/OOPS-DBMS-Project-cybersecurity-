package com.examease;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.*;

class Dtos {
    record Register(@NotBlank String name,@NotBlank String username,@NotBlank @Size(min=8) String password) {}
    record Login(@NotBlank String username,@NotBlank String password) {}
    record SubjectInput(@NotBlank String name,String description) {}
    record OptionInput(@NotBlank String text,boolean correct) {}
    record QuestionInput(@NotBlank String text,@Min(1) int marks,@NotNull @Size(min=2,max=6) List<@Valid OptionInput> options) {}
    record ExamInput(@NotBlank String title,@NotNull Long subjectId,@Min(1) int durationMinutes,
        @NotNull LocalDateTime startsAt,@NotNull LocalDateTime endsAt,
        @NotNull @Size(min=1) List<@Valid QuestionInput> questions) {}
    record OptionView(Long id,String text) {}
    record QuestionView(Long id,String text,int marks,List<OptionView> options) {}
    record ExamView(Long id,String title,String subject,int durationMinutes,LocalDateTime startsAt,
        LocalDateTime endsAt,boolean published,int totalMarks,List<QuestionView> questions) {}
    record AttemptView(Long attemptId,LocalDateTime deadline,long remainingSeconds,List<QuestionView> questions) {}
    record Submission(@NotNull Map<Long,Long> answers) {}
    record Result(Long attemptId,Long examId,String examTitle,String studentName,int score,int totalMarks,
        double percentage,String status,LocalDateTime submittedAt) {}
}
