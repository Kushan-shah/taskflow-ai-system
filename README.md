# Task Manager API

## Overview
An **AI-powered, asynchronous backend system** that enhances task management with intelligent summarization, classification, and tagging using LLM APIs. The backend is designed to handle core operations securely while integrating **JWT Authentication** for stateless user sessions. Data is persisted in **PostgreSQL**, with **Redis** used as a distributed cache for frequently accessed dashboard metrics (with auto-TTL of 10 minutes). File attachments are stored securely in **Amazon S3** with a local storage fallback. AI processing is fully asynchronous using a bounded `ThreadPoolTaskExecutor` to ensure zero latency impact on user operations. The application is containerized using **Docker** with Docker Compose orchestrating PostgreSQL, Redis, and the application. A **React** frontend with a glassmorphism dark-mode UI provides an interview-ready demo experience.

## Live Demo / API Documentation

Interact with the live, deployed API via Swagger UI:

- **AWS Deployment:** [http://13.126.55.172/swagger-ui/index.html#/](http://13.126.55.172/swagger-ui/index.html#/)
- **Render Deployment:** [https://task-manager-api-live.onrender.com/swagger-ui.html](https://task-manager-api-live.onrender.com/swagger-ui.html)

## Key Features
- **JWT Authentication:** Secure, stateless endpoint protection using JSON Web Tokens.
- **Role-Based Access Control (RBAC):** Granular authorization mechanisms supporting `USER` and `ADMIN` roles via `@PreAuthorize`.
- **Task CRUD Operations:** Complete lifecycle management for task entities with soft deletion.
- **Filtering & Pagination:** Dynamic query execution using Spring Data JPA Specifications.
- **Soft Delete:** Logical record deletion to preserve data integrity and analytics.
- **Redis Caching:** Distributed caching layer (via `RedisCacheManager` with JSON serialization and 10-min TTL) to optimize the dashboard analytics endpoint. Falls back to in-memory cache in dev profile.
- **AWS S3 Integration:** Secure multipart file uploads for task attachments, with local filesystem fallback.
- **Automated Scheduler:** Spring `@Scheduled` cron jobs to identify and log overdue tasks asynchronously.
- **Global Exception Handling:** Centralized `@RestControllerAdvice` to format error responses system-wide.
- **AI Task Summarization:** Automatic intelligent summarization of tasks using Google Gemini LLM API.
- **AI Priority Prediction:** LLM-driven classification of task priority (HIGH / MEDIUM / LOW).
- **AI Tag Extraction:** Auto-detection of relevant tags from task descriptions.
- **Async AI Processing:** Spring `@Async` with bounded `ThreadPoolTaskExecutor` — AI calls never block the API response.
- **React Frontend:** Dark-mode glassmorphism UI with dashboard charts (Recharts), AI insight panels, shimmer loading states, and RBAC-aware sidebar.

## 🧠 AI Engineering Design
- LLM Integration using Google Gemini API
- Zero-shot prompt engineering with strict JSON schema enforcement
- Asynchronous processing using bounded ThreadPoolTaskExecutor
- Fail-fast timeout handling (15s) with graceful degradation
- Structured output parsing (summary, priority, tags)
- Retry mechanism via manual re-trigger endpoint

## ⚠️ AI Reliability & Failure Handling
- AI processing failures do not impact core task creation flow
- Errors captured in `aiErrorMessage` field for observability
- Timeout protection ensures API responsiveness under slow LLM responses
- Manual retry endpoint allows reprocessing failed tasks

## System Architecture

The application strictly adheres to a layered architecture pattern. This design enforces strong separation of concerns, which makes the codebase highly maintainable, testable, and naturally scalable:
- **Controller Layer:** Intercepts HTTP requests, validates incoming DTO payloads, and routes them to the business logic layer.
- **Service Layer:** Houses the core business logic, transaction management, and coordinates external service calls (e.g., AWS S3, Gemini AI).
- **Repository Layer:** Interfaces with PostgreSQL using Spring Data JPA for persistence and querying.
- **Caching Layer:** Redis-backed distributed cache for hot-path analytics (dashboard). Uses Spring Cache abstraction — swappable with zero code changes.
- **Database Layer:** The underlying persistent data store (PostgreSQL for production, H2 in-memory for development).

## 🏗️ Design Decisions
- Chose async processing over synchronous LLM calls to eliminate user-facing latency.
- Used bounded thread pool to prevent resource exhaustion under high load (Queue size capped at 50).
- Leveraged Redis caching for read-heavy dashboard endpoints with tenant-isolated eviction.
- Maintained stateless authentication for seamless horizontal scalability.
- Defaulted to `@Version` JPA Optimistic Locking to prevent "Lost Updates" in high-concurrency environments.
- **Trade-off:** AI insights are eventually consistent (not real-time) due to async processing tradeoffs.

## Authentication Flow

This API utilizes a stateless JWT scheme:
1. **Login:** The client submits credentials to `/api/auth/login`. Upon successful authentication, the server generates and issues a signed JWT.
2. **Token Passing:** Subsequent requests must include the JWT in the `Authorization: Bearer <token>` HTTP header.
3. **Validation:** A custom Spring Security filter intercepts requests to protected endpoints, extracting and validating the token signature and expiration.
4. **Stateless Operation:** No session data is stored on the server. This statelessness significantly improves horizontal scalability, as any server instance can validate requests independently without relying on a shared session store.

## API Endpoints

### Auth (Public)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register a new user account |
| POST | `/api/auth/login` | Authenticate and retrieve JWT |

### Tasks (Protected)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/tasks` | Create a new task |
| GET | `/api/tasks` | List tasks (supports filtering & pagination) |
| GET | `/api/tasks/{id}` | Retrieve task details by ID |
| PUT | `/api/tasks/{id}` | Update an existing task |
| DELETE | `/api/tasks/{id}` | Soft delete a task |
| GET | `/api/tasks/search?keyword=value` | Search tasks by keyword |
| GET | `/api/tasks/dashboard` | Retrieve cached task statistics |
| POST | `/api/tasks/{id}/upload` | Upload a file attachment to S3 |
| GET | `/api/tasks/{id}/ai` | Retrieve AI insights for a task |
| POST | `/api/tasks/{id}/analyze` | Manually trigger AI analysis |

### Admin (Protected — ADMIN role required)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/tasks` | List all tasks across all users |
| GET | `/api/admin/users` | List all registered users |

### Query Parameters (GET `/api/tasks`)
| Parameter | Type | Default | Example |
|-----------|------|---------|---------|
| `status` | Enum | — | `TODO`, `IN_PROGRESS`, `DONE` |
| `priority` | Enum | — | `LOW`, `MEDIUM`, `HIGH` |
| `page` | int | 0 | `0` |
| `size` | int | 10 | `5` |
| `sortBy` | String | `createdAt` | `dueDate` |
| `sortDir` | String | `desc` | `asc` |

## Project Structure
```text
src/main/java/com/taskmanager/
├── controller/     # REST API controllers (Auth, Task, Admin)
├── service/        # Business logic and transactions
├── repository/     # Data access layer (Spring Data JPA)
├── entity/         # JPA entities and enums
├── dto/            # Request and Response mapping objects
├── security/       # JWT filters and authorization logic
├── config/         # Security, Redis Cache, Swagger, CORS, Async configurations
├── exception/      # Global exception handlers
├── scheduler/      # Scheduled CRON jobs (Overdue task detection)
└── util/           # Helper classes and mappers
```

## Quick Start & Build

### 1. Environment Configuration
Copy the sample environment file and insert your credentials:
```bash
cp .env.example .env
```

### 2. Build & Run Locally (Dev Profile — H2 + In-Memory Cache)
No PostgreSQL or Redis needed for development:
```bash
mvn clean install
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 3. Build & Run via Docker (Full Stack — PostgreSQL + Redis)
To easily spin up the API, PostgreSQL database, and Redis simultaneously:
```bash
docker-compose up --build
```

### 4. Production (AWS EC2 with RDS)
```bash
docker-compose -f docker-compose.prod.yml up --build -d
```

### 5. Frontend (React)
```bash
cd ../task-manager-ui
npm install
npm run dev
```
The React app runs on `http://localhost:5173` and connects to the Spring Boot API.
