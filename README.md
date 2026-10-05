# LumenLMS — Premium Learning Management System

A full-stack desktop Learning Management System built with **JavaFX**, **JDBC**, and **MySQL**, featuring role-based dashboards for Administrators, Instructors, and Students, a premium light SaaS-inspired interface, and a clean layered architecture (Model → DAO → Service → UI).

---

## 1. Features

### Authentication

- Animated split-panel login screen with show/hide password, "Remember Me" securely stores the email, password, and role using encrypted local storage, and a secure administrator-assisted password reset flow
- Sign-up flow restricted to **Student** and **Instructor** roles (Admin accounts cannot self-register)
- Live password-strength meter, duplicate-email detection, and full input validation
- Passwords hashed with **BCrypt** (adaptive, salted) — never stored or compared in plain text

### Administrator

- Platform-wide dashboard with animated stat counters (users, courses, enrollments, completion rate)
- Full user management: search, filter by role, create (including provisioning new admins), edit, activate/deactivate, delete
- Course oversight: create/edit/delete any course, assign instructors, view enrollment counts
- Analytics: course performance, instructor performance, average quiz scores, completion rates
- System settings: platform name, support email, registration toggle, with the original light theme

### Instructor

- Dashboard with per-course stats (students, quizzes, assignments, average score)
- Course CRUD with category, difficulty, duration, and color-coded thumbnails
- Course content editor: modules → lessons → resource links, all persisted to MySQL
- Quiz builder: multiple-choice questions (4 options), per-question marks, publish/unpublish
- Assignment builder: deadlines, instructions, max marks
- Student roster per course with live progress bars
- Grading workspace: view submissions, assign marks, leave feedback (auto-notifies the student)

### Student

- Personalized dashboard: continue-learning list, overall progress, upcoming assignments
- Course catalog with search, category/difficulty filters, and enroll-with-one-click
- Full learning page: module/lesson navigator, lesson content viewer, resource links, mark-complete, and a live progress sidebar
- Quiz-taking flow: countdown timer, multiple choice, instant auto-scored results with a correct/incorrect breakdown
- Assignment submission with deadline/overdue detection, grade and feedback display
- Quiz results history

### Platform-wide

- Toast notifications for every meaningful action (success/error/warning/info)
- In-app notification center (bell icon + panel) with unread badge and "mark all read"
- Collapsible sidebar, breadcrumb, global search field, with the original light theme
- Centralized JavaFX CSS design system (`theme.css`) with glassmorphism cards, gradients, and consistent component classes (`.card`, `.primary-button`, `.badge`, `.progress-bar`, etc.)
- Reusable `AnimationUtil` (fade, slide, scale, hover-lift, button-press, sidebar collapse, animated counters and progress bars) used consistently across every screen
- Graceful error handling everywhere: invalid input, duplicate enrollment, DB connection failure, etc. surface as friendly toasts/dialogs — the app never crashes on bad input

---

## 2. Technology Stack

| **Layer**   | **Technology**                                         |
| ----------- | ------------------------------------------------------ |
| Language    | Java 17+                                               |
| UI          | JavaFX 21 (programmatic scene graph + centralized CSS) |
| Build       | Maven (`javafx-maven-plugin`, `maven-shade-plugin`)    |
| Database    | MySQL 8+                                               |
| Data Access | JDBC (`mysql-connector-j`)                             |
| Security    | BCrypt (`jbcrypt`) password hashing                    |

> **Note on UI construction:** screens are built programmatically (Java scene-graph builders in `com.lms.ui`) rather than FXML. This keeps the large number of dynamic, data-driven screens (tables, dialogs, dashboards) simpler to maintain correctly, while every visual style still comes from the centralized `theme.css` stylesheet as required.

---

## 3. Architecture

```
com.lms
├── Main.java                 # Application entry point
├── model/                    # Plain data classes (User, Course, Quiz, Assignment, ...)
├── dao/                      # JDBC data-access objects — one per aggregate/table group
├── service/                  # Business logic, validation, and orchestration
├── controller/                (business logic lives in service/; UI event wiring lives in ui/)
├── ui/                        # Screen builders, shared components, dialogs, navigation shell
├── util/                      # DBConnection, SessionManager, ValidationUtil, AnimationUtil
└── security/                  # PasswordUtil (BCrypt hashing/verification)

```

**Flow:** `ui` calls `service` → `service` validates and calls `dao` → `dao` runs JDBC against MySQL. No screen talks to the database directly, and no DAO contains business rules.

### OOP concepts demonstrated

- **Encapsulation** — every model class exposes only getters/setters; internals are private
- **Inheritance/Polymorphism** — JavaFX `Region`/`Node` subclassing, `TableCell` overrides
- **Abstraction** — services hide DAO/SQL details behind clear method contracts
- **Interfaces** — `Runnable` callbacks for decoupled UI refresh, JavaFX `Callback`/`EventHandler`
- **Enums** — `Role`, `Course.Difficulty`, `Enrollment.Status`, `Submission.Status`, `Notification.Type`
- **Collections** — `List`, `Map`, `Set` used throughout DAOs and services
- **Exception handling** — custom checked business exceptions (`AuthException`, `CourseServiceException`, etc.) caught at the UI boundary and shown as friendly messages
- **Generics** — DAO return types, `TableView<User>`, `ComboBox<Course.Difficulty>`, etc.

---

## 4. Database Setup

1. Install and start **MySQL 8+**.
2. Run the provided script to create the schema and seed demo data:
   ```
   mysql -u root -p < database/lms.sql
   ```
   This creates the `lms_db` database, all 15 tables (with primary/foreign keys, unique constraints, and indexes), and inserts demo accounts, courses, lessons, a quiz, an assignment, and notifications.
3. Configure your MySQL credentials **without editing source files**. Copy `db.local.properties.example` to `db.local.properties` (git-ignored) in the project folder and set your values:
   ```
   db.url=jdbc:mysql://localhost:3306/lms_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
   db.user=root
   db.password=YOUR_PASSWORD
   ```
   Alternatively set the environment variables `LMS_DB_URL`, `LMS_DB_USER`, `LMS_DB_PASSWORD` (they take priority). `src/main/resources/db.properties` only holds non-secret defaults; never put a real password in it.

   `database/lms.sql` is safe to re-run (it never drops data). To wipe the database completely, run `database/reset_database.sql` and then `database/lms.sql`. For an **existing** database, run `database/migrate_quiz_attempt_unique.sql` once to add the one-attempt-per-quiz constraint.

---

## 5. Running in IntelliJ IDEA

1. **Open** the project folder in IntelliJ (`File → Open`, select the folder containing `pom.xml`).
2. Let IntelliJ import the Maven project (it will download JavaFX, MySQL Connector/J, and jBCrypt).
3. Make sure **Project SDK** is set to **Java 17 or newer** (`File → Project Structure → SDK`).
4. Import the database (`database/lms.sql`) into your local MySQL server (see above).
5. Run the app either:
   - Via the Maven tool window: `lms-platform → Plugins → javafx → javafx:run`, or
   - From a terminal in the project root:
     ```
     mvn clean javafx:run
     ```
6. The login screen should appear. Use one of the demo accounts below.

> If MySQL isn't reachable, the app shows a friendly warning dialog on startup but still opens — individual actions will surface a clear error instead of crashing.

---

## 6. Demo Credentials

The built-in administrator account is created once, on first run, with the demo password **`ogadmin980`**. Change it after first sign-in (Profile → Change Password). It is never reset on restart, and the login screen does not pre-fill it.

| **Role** | **Email**                                      |
| -------- | ---------------------------------------------- |
| Admin    | [ogadmin@gmail.com](mailto\:ogadmin@gmail.com) |

The clean database schema creates only the administrator account. Create instructor/student accounts from the application; this avoids filling a fresh database with unwanted demo courses and users.

For an existing database where `ogadmin@gmail.com` is missing, run `database/migrate_admin_credentials.sql` once.

---

## 7. Project Structure

```
lms-project/
├── pom.xml
├── database/
│   └── lms.sql
├── README.md
├── PROJECT_DOCUMENTATION.md
└── src/main/
    ├── java/com/lms/
    │   ├── Main.java
    │   ├── model/        (14 entity classes + Role enum)
    │   ├── dao/           (9 DAO classes)
    │   ├── service/       (7 service classes)
    │   ├── ui/            (screen builders, dialogs, shared components)
    │   ├── util/          (DBConnection, SessionManager, ValidationUtil, AnimationUtil, AppSettings)
    │   └── security/      (PasswordUtil)
    └── resources/
        ├── db.properties
        └── com/lms/css/theme.css

```

---

## 8. Future Improvements

- Richer assignment/profile file attachments and avatar uploads
- Server-sent/websocket-based live notifications instead of on-demand refresh
- Pagination for very large user/course tables
- Expand the automated JUnit test suite with additional service-layer and integration tests
- Certificate generation (PDF) on course completion
- Calendar view for deadlines across all enrolled courses

---

## 9. Troubleshooting

| **Problem**                                   | **Fix**                                                                                                                                   |
| --------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| "Database Connection Failed" dialog on launch | Confirm MySQL is running, `lms_db` exists (run `lms.sql`), and your `db.local.properties` / `LMS_DB_*` credentials are correct            |
| `mvn javafx:run` can't find JavaFX modules    | Ensure you're on Java 17+ and let Maven re-download dependencies (`mvn clean install`)                                                    |
| Login fails with correct demo password        | The admin was probably created earlier and its password changed. To start clean run `database/reset_database.sql` then `database/lms.sql` |
