# ExamEase Backend — Spring Boot + MySQL

## Features

- Teacher (Admin + Teacher combined) and Student roles
- Student registration, login, BCrypt password hashing, JWT bearer-token authentication
- Teacher creates subjects and MCQ exams with answer options, then publishes exams
- Students list available exams, start one attempt, submit selected answers, and view results
- Automatic grading, percentage calculation, stored answers, teacher results view
- Server-side exam deadline: earlier of start-of-attempt + duration or exam end time
- One attempt per student per exam; transaction-protected grading and submissions

## Requirements

- Java JDK 21
- Maven 3.9+
- MySQL 8+
- Any Java IDE (VS Code, IntelliJ IDEA, Eclipse, etc.)

## 1. Create the database

In MySQL Workbench, open and run `database/schema.sql`. Alternatively, with MySQL configured to allow database creation, the Spring datasource URL can create the database and JPA can generate the tables. For a college DBMS demonstration, using the SQL file is recommended.

## 2. Configure the MySQL connection

`src/main/resources/application.properties` uses environment variables. **Never commit real passwords or secrets.**

In Windows PowerShell, before starting the application:

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="YOUR_MYSQL_PASSWORD"
$env:JWT_SECRET="replace-with-a-long-random-secret-32-bytes-minimum"
$env:TEACHER_USERNAME="teacher@examease.local"
$env:TEACHER_PASSWORD="Use-A-Strong-Teacher-Password!"
```

On macOS/Linux:

```bash
export DB_USERNAME=root
export DB_PASSWORD='YOUR_MYSQL_PASSWORD'
export JWT_SECRET='replace-with-a-long-random-secret-32-bytes-minimum'
export TEACHER_USERNAME='teacher@examease.local'
export TEACHER_PASSWORD='Use-A-Strong-Teacher-Password!'
```

The application creates one teacher account on first run if its username is not present. For a local demo only, default username is `teacher@examease.local` and default password is `ChangeMe123!`. Change it before sharing or deploying.

## 3. Run

From the `ExamEase` folder:

```bash
mvn spring-boot:run
```

The REST API runs at `http://localhost:8080`. Open the folder as a Maven project in your IDE if you prefer. Test endpoints with Postman or curl; there is no website frontend yet.

## 4. Test the full flow in Postman

**Teacher login** — `POST http://localhost:8080/api/auth/login`

```json
{"username":"teacher@examease.local","password":"ChangeMe123!"}
```

Use the password you set. Copy `token` from the response. For teacher-only calls, set header `Authorization: Bearer TEACHER_TOKEN`.

**Create subject** — `POST /api/subjects` (teacher token)

```json
{"name":"DBMS","description":"Database Management Systems"}
```

**Create exam** — `POST /api/exams` (teacher token). Replace `subjectId` with the subject ID returned above. Use an availability window that includes your current server-local date/time.

```json
{
  "title":"DBMS Quiz 1",
  "subjectId":1,
  "durationMinutes":15,
  "startsAt":"2026-10-18T09:00:00",
  "endsAt":"2026-10-19T23:00:00",
  "questions":[
    {"text":"What does SQL stand for?","marks":2,"options":[
      {"text":"Structured Query Language","correct":true},
      {"text":"Simple Question List","correct":false},
      {"text":"Standard Queue Language","correct":false}
    ]},
    {"text":"Which key uniquely identifies a row?","marks":3,"options":[
      {"text":"Primary key","correct":true},
      {"text":"Foreign key","correct":false}
    ]}
  ]
}
```

**Publish exam** — `PATCH /api/exams/1/publish` (teacher token). Replace `1` with returned exam ID.

**Register student** — `POST /api/auth/register` (no token)

```json
{"name":"Demo Student","username":"student1@example.com","password":"StudentPass123!"}
```

Registration creates STUDENT accounts only. Use the returned student token or call `POST /api/auth/login` with the student's username and password.

**Student exam flow** — with header `Authorization: Bearer STUDENT_TOKEN`:

1. `GET /api/exams/available` — currently published exams within their availability windows.
2. `POST /api/exams/1/start` — returns attempt ID, question IDs, option IDs, and deadline.
3. `POST /api/attempts/1/submit` — use your actual attempt ID and actual question/option IDs:

```json
{"answers":{"1":1,"2":4}}
```

The `answers` object maps **question ID to selected option ID**. Missing questions receive zero marks.

4. `GET /api/results/my` — the student's completed results.
5. `GET /api/exams/1/results` — teacher-only results for an exam.

## API routes

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/auth/register` | Public, creates Student |
| POST | `/api/auth/login` | Public |
| GET | `/api/auth/me` | Authenticated |
| GET | `/api/subjects` | Authenticated |
| POST | `/api/subjects` | Teacher |
| POST | `/api/exams` | Teacher |
| GET | `/api/exams` | Teacher |
| PATCH | `/api/exams/{id}/publish` | Teacher |
| GET | `/api/exams/available` | Student |
| POST | `/api/exams/{id}/start` | Student |
| GET | `/api/attempts/{id}` | Student (owner only) |
| POST | `/api/attempts/{id}/submit` | Student (owner only) |
| GET | `/api/results/my` | Student |
| GET | `/api/exams/{id}/results` | Teacher |

## Timer behavior and limitations

The server calculates the deadline from the student's attempt start time and the exam's overall end time. If a student submits after the deadline, the attempt is graded as unanswered (zero). A timed-out attempt is also finalized when that student fetches it again. **This starter does not automatically finalize abandoned attempts in the background**, and it does not continuously save draft answers. Therefore, to avoid losing answers, the future frontend should submit them before the deadline and show a countdown based on `remainingSeconds`. A robust production timer should add autosaving and a scheduled expiry job.

## Important scope notes

This is a runnable-source starter, **not a deployed or end-to-end-tested production system**. No frontend, password reset, email verification, question editing, retakes, or live timer interface is included. JPA `ddl-auto=update` is for development; production should use versioned migrations. The teacher sees all exams/results (combined Admin role). Add HTTPS, CORS configuration for your chosen frontend origin, login rate limiting, backups, and security tests before any public deployment. Dates use the backend server's local timezone.
