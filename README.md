# Task Manager API

## Overview
A REST API for task management built with **Spring Boot 3**. The backend is designed to handle core operations securely while integrating **JWT Authentication** for stateless user sessions. Data is persisted in **PostgreSQL**, with **Redis** used to cache frequently accessed dashboard metrics. File attachments are stored securely in **Amazon S3**. The application is containerized using **Docker** to ensure consistent deployments across AWS environments.

## Live Demo / API Documentation

Interact with the live, deployed API via Swagger UI:

- **AWS Deployment:** [http://13.126.55.172:8080/swagger-ui/index.html#/](http://13.126.55.172:8080/swagger-ui/index.html#/)
- **Render Deployment:** [https://task-manager-api-live.onrender.com/swagger-ui.html](https://task-manager-api-live.onrender.com/swagger-ui.html)

## Key Features
- **JWT Authentication:** Secure, stateless endpoint protection using JSON Web Tokens.
- **Role-Based Access Control (RBAC):** Granular authorization mechanisms supporting `USER` and `ADMIN` roles.
- **Task CRUD Operations:** Complete lifecycle management for task entities.
- **Filtering & Pagination:** Dynamic query execution using Spring Data JPA Specifications.
- **Soft Delete:** Logical record deletion to preserve data integrity and analytics.
- **Redis Caching:** Memory-based caching layer to optimize the dashboard analytics endpoint.
- **AWS S3 Integration:** Secure multipart file uploads for task attachments.
- **Automated Scheduler:** Spring `@Scheduled` cron jobs to identify and isolate overdue tasks asynchronously.
- **Global Exception Handling:** Centralized `@RestControllerAdvice` to format error responses system-wide.

## System Architecture

The application strictly adheres to a layered architecture pattern. This design enforces strong separation of concerns, which makes the codebase highly maintainable, testable, and naturally scalable:
- **Controller Layer:** Intercepts HTTP requests, validates incoming DTO payloads, and routes them to the business logic layer.
- **Service Layer:** Houses the core business logic, transaction management, and coordinates external service calls (e.g., AWS S3, Redis).
- **Repository Layer:** Interfaces with PostgreSQL using Spring Data JPA for persistence and querying.
- **Database Layer:** The underlying persistent data store hosted on Amazon RDS.

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
├── controller/     # REST API controllers
├── service/        # Business logic and transactions
├── repository/     # Data access layer (Spring Data JPA)
├── entity/         # JPA entities and enums
├── dto/            # Request and Response mapping objects
├── security/       # JWT filters and authorization logic
├── config/         # Security, Redis, and Swagger configurations
├── exception/      # Global exception handlers
├── scheduler/      # Scheduled CRON jobs
└── util/           # Helper classes and mappers
```

## Quick Start & Build

### 1. Environment Configuration
Copy the sample environment file and insert your PostgreSQL credentials:
```bash
cp .env.example .env
```

### 2. Build & Run Locally
Ensure you have a PostgreSQL instance running locally with a database named `taskmanager`.
```bash
mvn clean install
mvn spring-boot:run
```

### 3. Build & Run via Docker
To easily spin up the API and the PostgreSQL database simultaneously using containers:
```bash
docker-compose up --build
```
