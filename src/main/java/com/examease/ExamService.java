package com.examease;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
class ExamService {
    private final Users users;private final Subjects subjects;private final Exams exams;private final Attempts attempts;
    ExamService(Users users,Subjects subjects,Exams exams,Attempts attempts){
        this.users=users;this.subjects=subjects;this.exams=exams;this.attempts=attempts;
    }
    @Transactional Dtos.ExamView create(Dtos.ExamInput input,String username){
        if(!input.endsAt().isAfter(input.startsAt())) throw new IllegalArgumentException("End must be after start");
        Exam e=new Exam();e.title=input.title().trim();e.subject=subjects.findById(input.subjectId()).orElseThrow(()->new IllegalArgumentException("Subject not found"));
        e.teacher=users.findByUsername(username).orElseThrow();e.durationMinutes=input.durationMinutes();
        e.startsAt=input.startsAt();e.endsAt=input.endsAt();
        for(Dtos.QuestionInput qi:input.questions()){
            if(qi.options().stream().filter(Dtos.OptionInput::correct).count()!=1) throw new IllegalArgumentException("Each question needs exactly one correct option");
            Question q=new Question();q.exam=e;q.text=qi.text().trim();q.marks=qi.marks();
            for(Dtos.OptionInput oi:qi.options()){
                Choice c=new Choice();c.question=q;c.text=oi.text().trim();c.correct=oi.correct();q.choices.add(c);
            }
            e.questions.add(q);
        }
        exams.save(e);return view(e);
    }
    @Transactional Dtos.ExamView publish(Long id){
        Exam e=exams.findById(id).orElseThrow(()->new IllegalArgumentException("Exam not found"));
        if(e.questions.isEmpty()) throw new IllegalStateException("No questions in exam");
        e.published=true;return view(e);
    }
    @Transactional(readOnly=true) List<Dtos.ExamView> all(){return exams.findAll().stream().map(this::view).toList();}
    @Transactional(readOnly=true) List<Dtos.ExamView> available(){
        LocalDateTime now=LocalDateTime.now();
        return exams.findByPublishedTrue().stream().filter(e->!now.isBefore(e.startsAt)&&now.isBefore(e.endsAt))
            .map(e->new Dtos.ExamView(e.id,e.title,e.subject.name,e.durationMinutes,e.startsAt,e.endsAt,true,total(e),List.of())).toList();
    }
    @Transactional Dtos.AttemptView start(Long id,String username){
        Exam e=exams.findById(id).orElseThrow(()->new IllegalArgumentException("Exam not found"));
        LocalDateTime now=LocalDateTime.now();
        if(!e.published||now.isBefore(e.startsAt)||!now.isBefore(e.endsAt)) throw new IllegalStateException("Exam not available");
        Account student=users.findByUsername(username).orElseThrow();
        if(attempts.existsByExamIdAndStudentId(id,student.id)) throw new IllegalStateException("Retakes are disabled");
        Attempt a=new Attempt();a.exam=e;a.student=student;a.startedAt=now;attempts.saveAndFlush(a);
        return attemptView(a);
    }
    @Transactional Dtos.AttemptView getAttempt(Long id,String username){
        Attempt a=ownedLocked(id,username);
        if(!a.status.equals("IN_PROGRESS")) throw new IllegalStateException("Attempt already submitted");
        if(!LocalDateTime.now().isBefore(deadline(a))) grade(a,Map.of());
        if(!a.status.equals("IN_PROGRESS")) throw new IllegalStateException("Exam time expired; view your result");
        return attemptView(a);
    }
    @Transactional Dtos.Result submit(Long id,String username,Map<Long,Long> answers){
        Attempt a=ownedLocked(id,username);
        if(!a.status.equals("IN_PROGRESS")) throw new IllegalStateException("Attempt already submitted");
        boolean expired=!LocalDateTime.now().isBefore(deadline(a));
        grade(a,expired?Map.of():answers);
        return result(a);
    }
    @Transactional(readOnly=true) List<Dtos.Result> myResults(String username){
        Account student=users.findByUsername(username).orElseThrow();
        return attempts.findByStudentId(student.id).stream().filter(a->!a.status.equals("IN_PROGRESS")).map(this::result).toList();
    }
    @Transactional(readOnly=true) List<Dtos.Result> examResults(Long examId){
        if(!exams.existsById(examId)) throw new IllegalArgumentException("Exam not found");
        return attempts.findByExamId(examId).stream().filter(a->!a.status.equals("IN_PROGRESS")).map(this::result).toList();
    }
    private Attempt ownedLocked(Long id,String username){
        Attempt a=attempts.lockById(id).orElseThrow(()->new IllegalArgumentException("Attempt not found"));
        if(!a.student.username.equals(username)) throw new IllegalArgumentException("Attempt not found");
        return a;
    }
    private void grade(Attempt a,Map<Long,Long> selected){
        Set<Long> questionIds=new HashSet<>();
        for(Question q:a.exam.questions) questionIds.add(q.id);
        if(!questionIds.containsAll(selected.keySet())) throw new IllegalArgumentException("Unknown question ID");
        int score=0;
        for(Question q:a.exam.questions){
            Long optionId=selected.get(q.id);
            Choice choice=null;
            if(optionId!=null){
                choice=q.choices.stream().filter(c->c.id.equals(optionId)).findFirst()
                    .orElseThrow(()->new IllegalArgumentException("Option does not belong to question "+q.id));
            }
            StudentAnswer answer=new StudentAnswer();answer.attempt=a;answer.question=q;answer.selected=choice;
            answer.marksAwarded=choice!=null&&choice.correct?q.marks:0;score+=answer.marksAwarded;
            a.answers.add(answer);
        }
        a.score=score;a.totalMarks=total(a.exam);a.submittedAt=LocalDateTime.now();a.status="SUBMITTED";
    }
    private LocalDateTime deadline(Attempt a){
        LocalDateTime byDuration=a.startedAt.plusMinutes(a.exam.durationMinutes);
        return byDuration.isBefore(a.exam.endsAt)?byDuration:a.exam.endsAt;
    }
    private Dtos.AttemptView attemptView(Attempt a){
        LocalDateTime due=deadline(a);
        return new Dtos.AttemptView(a.id,due,Math.max(0,Duration.between(LocalDateTime.now(),due).toSeconds()),
            a.exam.questions.stream().map(this::questionView).toList());
    }
    private int total(Exam e){return e.questions.stream().mapToInt(q->q.marks).sum();}
    private Dtos.QuestionView questionView(Question q){
        return new Dtos.QuestionView(q.id,q.text,q.marks,q.choices.stream().map(c->new Dtos.OptionView(c.id,c.text)).toList());
    }
    private Dtos.ExamView view(Exam e){
        return new Dtos.ExamView(e.id,e.title,e.subject.name,e.durationMinutes,e.startsAt,e.endsAt,e.published,total(e),
            e.questions.stream().map(this::questionView).toList());
    }
    private Dtos.Result result(Attempt a){
        double percent=a.totalMarks==null||a.totalMarks==0?0:Math.round(a.score*10000.0/a.totalMarks)/100.0;
        return new Dtos.Result(a.id,a.exam.id,a.exam.title,a.student.name,a.score,a.totalMarks,percent,a.status,a.submittedAt);
    }
}
