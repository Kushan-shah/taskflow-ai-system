# Task Manager API

## Overview
An **AI-augmented backend system with asynchronous LLM processing** that enhances task management with intelligent summarization, classification, and tagging using LLM APIs. The backend is designed to handle core operations securely while integrating **JWT Authentication** for stateless user sessions. Data is persisted in **PostgreSQL**, with **Redis** used as a distributed cache for frequently accessed dashboard metrics (with auto-TTL of 10 minutes). File attachments are stored securely in **Amazon S3** with a local storage fallback. AI processing is fully asynchronous using a bounded `ThreadPoolTaskExecutor` to ensure zero latency impact on user operations. The application is containerized using **Docker** with Docker Compose orchestrating PostgreSQL, Redis, and the application. A **React** frontend with a glassmorphism dark-mode UI provides an interview-ready demo experience.

## 🌐 Live Deployments & Cross-Repository Links

This project is built as a fully decoupled microservice architecture.

- **Frontend Application (Live):** [https://taskflow-ui-two.vercel.app/](https://taskflow-ui-two.vercel.app/)
- **Frontend Source Code:** [https://github.com/Kushan-shah/TaskFlow-UI](https://github.com/Kushan-shah/TaskFlow-UI)
- **Backend API (Swagger Docs):** [https://task-manager-api-live.onrender.com/swagger-ui/index.html#/](https://task-manager-api-live.onrender.com/swagger-ui/index.html#/)
- **Backend Source Code:** [https://github.com/Kushan-shah/TaskFlow-AI](https://github.com/Kushan-shah/TaskFlow-AI)

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
- Integrated with Google Gemini API (LLM inference via REST)
- Designed prompts to enforce strict JSON output, ensuring deterministic parsing and minimizing hallucination risks
- Implemented defensive JSON parsing with validation to prevent malformed AI responses
- Asynchronous processing using bounded ThreadPoolTaskExecutor
- Fail-fast timeout handling (10s) with graceful degradation
- Structured output parsing (summary, priority, tags)
- Retry mechanism via manual re-trigger endpoint

## ⚠️ AI Reliability & Failure Handling
- AI processing failures do not impact core task creation flow
- Errors captured in `aiErrorMessage` field for observability
- Timeout protection ensures API responsiveness under slow LLM responses
- Manual retry endpoint allows reprocessing failed tasks

## 🤖 Sample AI Output

**Input from User:**
* **Title:** "Fix slow database queries"
* **Description:** "Dashboard API is taking 3 seconds due to unoptimized queries"

**Output from Google Gemini (Structured JSON):**
```json
{
  "summary": "Optimize database queries to improve dashboard performance",
  "priority": "HIGH",
  "tags": ["database", "performance", "optimization"]
}
```

## System Architecture

The application strictly adheres to a layered architecture pattern. This design enforces strong separation of concerns, which makes the codebase highly maintainable, testable, and naturally scalable:
- **Controller Layer:** Intercepts HTTP requests, validates incoming DTO payloads, and routes them to the business logic layer.
- **Service Layer:** Houses the core business logic, transaction management, and coordinates external service calls (e.g., AWS S3, Gemini AI).
- **Repository Layer:** Interfaces with PostgreSQL using Spring Data JPA for persistence and querying.
- **Caching Layer:** Redis-backed distributed cache for hot-path analytics (dashboard). Uses Spring Cache abstraction — swappable with zero code changes.
- **Database Layer:** The underlying persistent data store (PostgreSQL for production, H2 in-memory for development).

## 🏗️ Design Decisions
- Chose async processing over synchronous LLM calls to eliminate user-facing latency.
- Async design reduces unnecessary repeated LLM calls, optimizing API usage cost.
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

## 🔥 Load Testing (k6)

Automated load tests are run using [k6](https://k6.io/) to validate API performance under concurrent user traffic.

### Test Configuration
| Parameter | Value |
|-----------|-------|
| Tool | k6 v1.7.1 |
| Ramp-Up | 10 → 25 → **50 concurrent users** |
| Duration | 70 seconds |
| Endpoints Tested | Register, Login, Create, List, Dashboard, Get, AI Insights, Update, Search, Delete |

### Results Summary (50 Concurrent Users)

| Metric | Value |
|--------|-------|
| **Total HTTP Requests** | 6,132 |
| **Throughput** | 86 req/s |
| **Avg Response Time** | 3.65 ms |
| **p(95) Response Time** | **8.12 ms** ✅ |
| **p(90) Response Time** | 6.83 ms |
| **Error Rate** | **2.57%** ✅ (below 10% threshold) |
| **Tasks Created** | 707 |
| **Data Transferred** | 47 MB received / 2.5 MB sent |

### Per-Endpoint Latency Breakdown

| Endpoint | Avg | p(95) | Max |
|----------|-----|-------|-----|
| Create Task | 3.50 ms | 8.34 ms | 24.87 ms |
| List Tasks (Paginated) | 3.75 ms | 6.34 ms | 36.62 ms |
| Dashboard (Cached) | 5.17 ms | 9.86 ms | 49.84 ms |
| AI Insights | 2.15 ms | 3.47 ms | 14.44 ms |

### Thresholds
```
✅ http_req_duration p(95) < 2000ms → PASSED (actual: 8.12ms)
✅ error_rate < 10%                 → PASSED (actual: 2.57%)
```

### Run Load Tests
```bash
k6 run k6-load-test.js
```

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
