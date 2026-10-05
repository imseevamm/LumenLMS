# PROJECT DOCUMENTATION
## LumenLMS — Online Learning Management System

---

## 1. Abstract

LumenLMS is a desktop-based Online Learning Management System designed to bring together
administrators, instructors, and students on a single platform for course delivery, assessment,
and progress tracking. The system is built using Java 17, JavaFX for the presentation layer, and
MySQL for persistent storage, connected through JDBC. It follows a layered architecture
(Model–DAO–Service–UI) to keep the codebase maintainable, testable, and free of duplicated
database logic. Beyond core CRUD functionality, the project places heavy emphasis on user
experience: a centralized JavaFX CSS design system, reusable animation utilities, and toast-based
feedback create an interface comparable to modern commercial SaaS education products, rather than
a bare-bones academic prototype.

---

## 2. Problem Statement

Traditional classroom-only instruction struggles to give learners flexible access to course
material, structured assessment, and visibility into their own progress. Many student-built LMS
projects solve this only partially — they implement basic CRUD screens without addressing role
separation, security of credentials, real progress tracking, or a usable interface. The problem
this project addresses is: *how can a single desktop application coherently serve three distinct
user roles — each with different permissions and workflows — while keeping course content,
assessments, and grades consistent and secure across all of them?*

---

## 3. Objectives

1. Provide **secure, role-based authentication** where only administrators can provision admin
   accounts, and passwords are never stored in plain text.
2. Give **instructors** full control over course creation, structured content (modules → lessons →
   materials), quizzes, and assignments, plus a grading workflow.
3. Give **students** a genuine learning experience: browsing, enrolling, consuming lesson content,
   tracking completion, attempting auto-graded quizzes, and submitting assignments.
4. Give **administrators** full oversight: user management, course oversight, and platform
   analytics.
5. Track **progress quantitatively** (per-lesson completion → per-course percentage → automatic
   completion status) rather than just recording enrollment.
6. Deliver a **premium, animated, consistent UI** rather than default JavaFX controls.
7. Keep the codebase **layered and maintainable** so each concern (data access, business rules,
   presentation) can change independently.

---

## 4. Existing System (Baseline)

Most comparable student LMS projects are either:
- Web-based CRUD apps with no meaningful progress tracking or grading workflow, or
- Desktop Swing/JavaFX apps using default, unstyled controls with minimal validation and no
  distinction between "enrolled" and "actually progressing."

Common shortcomings: passwords stored as plain text or reversible encoding; no protection against
self-registering as an administrator; quizzes with no automatic scoring; assignments with no
deadline/grading loop; and interfaces that look identical to a default Swing/JavaFX form.

---

## 5. Proposed System

LumenLMS addresses each shortcoming directly:

| Shortcoming in baseline systems | LumenLMS approach |
|---|---|
| Plain-text/weak password storage | BCrypt adaptive hashing (work factor 12), verified via `PasswordUtil` |
| Self-registration as Admin | `AuthService.signup()` only accepts `STUDENT`/`INSTRUCTOR`; admin accounts are created exclusively via `UserService.createUser()`, reachable only from the Admin-only User Management screen |
| No real progress tracking | Per-lesson `progress` table, aggregated into a completion percentage per enrollment, auto-flips enrollment status to `COMPLETED` at 100% |
| Manual/no quiz grading | `QuizService.submitAttempt()` grades automatically against the stored correct option and persists a full answer breakdown |
| No grading loop for assignments | Submission → Instructor grading screen → grade + feedback persisted → student notified automatically |
| Generic default UI | Centralized `theme.css` design system + `AnimationUtil` used on every screen |

---

## 6. User Roles & Permissions

| Capability | Admin | Instructor | Student |
|---|---|---|---|
| Create/deactivate/delete any user | ✅ | ❌ | ❌ |
| Create Admin accounts | ✅ | ❌ | ❌ |
| Create/edit/delete any course | ✅ | Own courses only | ❌ |
| Manage modules/lessons/materials | View only | Own courses | ❌ (consume only) |
| Create/grade quizzes & assignments | ❌ | Own courses | Attempt/submit only |
| View platform-wide analytics | ✅ | Own courses only | ❌ |
| Enroll in courses | ❌ | ❌ | ✅ |
| Track personal progress | ❌ | ❌ | ✅ |

---

## 7. Functional Requirements

1. The system shall allow a user to register as a Student or Instructor with full validation.
2. The system shall allow only administrators to create Administrator accounts.
3. The system shall authenticate users and restrict every screen/action to their role.
4. The system shall allow instructors to create courses with title, description, syllabus,
   category, difficulty, and duration.
5. The system shall allow instructors to structure content into modules, then lessons, then
   optional resource materials.
6. The system shall allow students to search/filter/browse published courses and enroll, while
   preventing duplicate enrollment.
7. The system shall track lesson completion per student and compute a live progress percentage.
8. The system shall allow instructors to build multiple-choice quizzes with configurable marks per
   question and a duration; the system shall auto-grade student attempts.
9. The system shall allow instructors to create assignments with a deadline and maximum marks, and
   allow students to submit work exactly once per assignment, including PDF attachments.
10. The system shall allow instructors to grade submissions and leave feedback, and shall notify
    the student.
11. The system shall provide administrators with user management (search/filter/CRUD,
    activate/deactivate), course oversight, and platform analytics.
12. The system shall never crash on invalid input; all errors surface as readable messages.

## 8. Non-Functional Requirements

- **Security:** BCrypt password hashing; role-based access enforced at the UI/service boundary.
- **Usability:** consistent design system, animated transitions, empty/error states, toast
  feedback for every action.
- **Maintainability:** strict Model–DAO–Service–UI separation; no SQL outside DAOs; no business
  rules inside DAOs or UI classes.
- **Portability:** runs on any platform with Java 17+ and MySQL 8+; configuration is externalized
  to `db.local.properties` (git-ignored) or the `LMS_DB_*` environment variables.
- **Performance:** JDBC connections are short-lived and closed via try-with-resources; queries use
  indexes on foreign keys and frequently filtered columns (role, category, status).
- **Reliability:** every DAO method wraps `SQLException` into an unchecked `RuntimeException` with
  context, so failures are visible in logs rather than silently swallowed, while the UI layer
  still catches business exceptions gracefully.

---

## 9. System Architecture

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│     UI      │ --> │   Service   │ --> │     DAO     │ --> │   MySQL     │
│ (JavaFX)    │     │ (business   │     │  (JDBC)     │     │  Database   │
│             │ <-- │  rules +    │ <-- │             │ <-- │             │
└─────────────┘     │ validation) │     └─────────────┘     └─────────────┘
                     └─────────────┘
```

- **UI layer** (`com.lms.ui`): builds the JavaFX scene graph, wires event handlers, and shows
  toasts/dialogs. It never issues SQL directly.
- **Service layer** (`com.lms.service`): validates input, enforces business rules (e.g. "cannot
  self-register as Admin", "cannot enroll twice"), and coordinates one or more DAOs.
- **DAO layer** (`com.lms.dao`): one class per aggregate (Users, Courses, Modules/Lessons,
  Enrollments, Progress, Quizzes, Assignments, Submissions, Notifications, Settings). Each method
  opens a connection via `DBConnection`, runs a parameterized `PreparedStatement`, and maps
  `ResultSet` rows to model objects.
- **Model layer** (`com.lms.model`): plain encapsulated data classes with no persistence logic.
- **Util/Security** (`com.lms.util`, `com.lms.security`): cross-cutting concerns — DB connection
  factory, session state, validation helpers, animation helpers, and password hashing.

---

## 10. Database Design

### Entity groups and relationships
- **users** (1) — (M) **courses** (an instructor owns many courses)
- **courses** (1) — (M) **course_modules** (1) — (M) **lessons** (1) — (M) **materials**
- **users** (M) — (M) **courses** through **enrollments** (a student enrolls in many courses; a
  course has many enrolled students)
- **enrollments** (1) — (M) **progress** (1 row per lesson per enrollment, tracking completion)
- **courses** (1) — (M) **quizzes** (1) — (M) **quiz_questions**
- **users** (1) — (M) **quiz_attempts** (1) — (M) **quiz_answers** (each attempt records one
  answer per question, graded against `quiz_questions.correct_option`)
- **courses** (1) — (M) **assignments** (1) — (M) **submissions** (one submission per student per
  assignment, enforced by a unique constraint)
- **users** (1) — (M) **notifications**
- **system_settings** — a simple key/value table for platform configuration

### Keys, constraints, and indexes
- Every table uses an auto-incrementing surrogate primary key.
- Foreign keys cascade on delete where the child record has no meaning without the parent (e.g.
  deleting a course removes its modules, lessons, materials, quizzes, and assignments).
- Unique constraints prevent duplicate enrollment (`student_id`, `course_id`) and duplicate
  submission (`assignment_id`, `student_id`).
- Indexes exist on frequently filtered columns: `users.role`, `users.is_active`,
  `courses.category`, `courses.instructor_id`, `notifications.user_id`.

### ER-style relationship summary
```
users ──< courses ──< course_modules ──< lessons ──< materials
users ──< enrollments >── courses
enrollments ──< progress >── lessons
courses ──< quizzes ──< quiz_questions
users ──< quiz_attempts ──< quiz_answers >── quiz_questions
courses ──< assignments ──< submissions >── users
users ──< notifications
```

---

## 11. Modules

1. **Authentication Module** — login, signup, password change, session management.
2. **User Management Module** (Admin) — CRUD, search/filter, role and status management.
3. **Course Management Module** (Admin/Instructor) — course CRUD, instructor assignment.
4. **Content Module** (Instructor) — modules, lessons, materials.
5. **Enrollment & Progress Module** (Student) — browsing, enrolling, completion tracking.
6. **Assessment Module** — quizzes (creation, attempt, auto-grading) and assignments
   (creation, submission, grading).
7. **Notification Module** — toasts + persistent notification center.
8. **Analytics Module** (Admin/Instructor) — aggregated statistics and performance views.
9. **Profile Module** — shared across all roles: edit profile, change password.

---

## 12. OOP Concepts Used

- **Encapsulation:** all model fields are private with controlled getters/setters (e.g. `User`,
  `Course`, `Assignment`).
- **Inheritance & Polymorphism:** UI components extend and override JavaFX base classes
  (`TableCell`, `Region`-based custom views); overridden `updateItem()` in `TableCell` is a direct
  example of polymorphic behavior driven by the JavaFX framework.
- **Abstraction:** service classes expose intention-revealing methods (`enroll`, `submitAttempt`,
  `grade`) that hide SQL and validation details from the UI.
- **Interfaces:** `Runnable` is used extensively as a lightweight callback interface to let a child
  dialog notify a parent screen to refresh, without coupling the two classes directly.
- **Enums:** `Role`, `Course.Difficulty`, `Enrollment.Status`, `Submission.Status`,
  `Notification.Type`, `Material.Type` — all type-safe, exhaustively `switch`ed over.
- **Collections Framework:** `List`, `Map`, `Set`, and `Optional` are used throughout the DAO and
  service layers instead of arrays or raw types.
- **Exception Handling:** each service defines its own unchecked exception type
  (e.g. `AuthService.AuthException`, `QuizService.QuizException`) so the UI layer can catch a
  specific, meaningful exception and show the right message, while DAOs translate low-level
  `SQLException`s into `RuntimeException`s with context rather than swallowing them.
- **Generics:** DAOs return typed `List<T>`/`Optional<T>`; UI uses generic JavaFX controls
  (`TableView<User>`, `ComboBox<Course.Difficulty>`).

---

## 13. Testing

Manual functional testing was performed against the following scenarios (see the Final Quality
Checklist in the README for the full pass/fail list):

- Valid and invalid login attempts (wrong password, unknown email, disabled account)
- Signup validation (weak password, mismatched confirmation, duplicate email, blocked Admin role)
- Course CRUD as Admin and as Instructor, including instructor re-assignment
- Module → lesson → material creation and deletion, including cascade delete of a course
- Enrollment, duplicate-enrollment prevention, and progress percentage recalculation
- Marking lessons complete and confirming automatic enrollment completion at 100%
- Quiz creation, question authoring, publishing, student attempt, and automatic scoring accuracy
- Assignment creation, on-time vs. late submission flagging, grading, and feedback notification
- Search/filter across users, courses, and role-restricted navigation
- Deliberate bad input (empty fields, invalid emails, SQL-special characters in text fields) to
  confirm the UI shows a friendly message instead of crashing

Because this is a JavaFX desktop application backed by a real MySQL instance, testing was
performed interactively rather than through an automated headless suite; the service layer's
clear separation from the UI (see §12, Abstraction) makes it straightforward to add JUnit tests
against `AuthService`, `CourseService`, `QuizService`, etc. in the future.

---

## 14. Future Scope

- Automated unit/integration test suite (JUnit 5 + an in-memory or containerized MySQL instance)
- REST API layer to allow a companion mobile or web client to reuse the same service layer
- Additional file attachments and profile avatar uploads
- Real-time notifications via WebSockets instead of on-demand refresh
- PDF certificate generation on course completion
- Full accessibility (screen-reader, keyboard navigation) pass
- Role-based analytics export (CSV/PDF reports)

---

## 15. Conclusion

LumenLMS demonstrates that a desktop JavaFX application can deliver the structure, security, and
polish typically associated with commercial web-based learning platforms. By strictly separating
data access, business logic, and presentation, and by centralizing styling and animation, the
project remains both feature-complete for three distinct user roles and maintainable enough to
extend — whether that means adding new assessment types, a REST API, or a richer analytics suite.
