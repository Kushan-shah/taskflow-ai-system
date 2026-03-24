# 🚀 Task Manager API

A fully-featured, production-ready REST API for Task Management. This project bridges the gap between basic CRUD applications and scalable, enterprise architectures by implementing industry-standard security, performance tuning, and cloud integration.

## ✨ Advanced System Features 
- **Security (Zero Trust):** Stateless JWT (JSON Web Token) Authentication with Role-Based Access Control (`USER` & `ADMIN`).
- **Cloud Storage:** Amazon S3 integration using `AWS SDK v2` for dynamic `MultipartFile` uploads.
- **Performance (Caching):** Integrated `@EnableCaching` (ConcurrentMapCache) to aggressively reduce database lookups on the Analytics Dashboard.
- **Advanced Data Queries:** Implementation of `Spring Data JPA Specifications` (Criteria API) to support dynamic filtering, sorting, and keyword searching across thousands of rows.
- **Background Automation:** Cron Job utilizing `@Scheduled` to proactively scan and isolate overdue tasks without blocking the main event loop.
- **Robust Error Handling:** A centralized `@RestControllerAdvice` Global Exception Handler to capture constraint violations (`@Future`) and AWS timeouts into structured HTTP 400/500 JSON payloads.

## Tech Stack

- **Java 17** + **Spring Boot 3.2**
- **Spring Security** (BCrypt & JWT)
- **Database:** **PostgreSQL hosted via Amazon RDS**
- **Cloud Infrastructure:** **AWS EC2** (Application Server) + **AWS S3** (Object Storage)
- **SpringDoc OpenAPI (Swagger)** (API Documentation)
- **Docker** (Containerized API)

## ☁️ Cloud Architecture (Deployment Strategy)
Unlike basic student CRUD projects that run everything locally, this project employs a **highly available, decoupled microservice pattern** on AWS:
1. **Compute Layer:** The Spring Boot API is containerized using Docker and runs on an **AWS EC2** instance, handling all business logic and JWT validation.
2. **Data Layer:** The database is completely decoupled using **Amazon RDS (PostgreSQL)**. EC2 communicates with RDS securely over an internal AWS VPC via tight Security Groups, ensuring the database is not exposed to the public internet.

## Quick Start

### 1. Clone & Configure
```bash
cp .env.example .env
# Edit .env with your database credentials
```

### 2. Run with Docker
```bash
docker-compose up --build
```

### 3. Run Locally (without Docker)
```bash
# Ensure PostgreSQL is running with a 'taskmanager' database
mvn spring-boot:run
```

### 4. Access
- **API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html

## API Endpoints

### Auth (Public)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login & get JWT |

### Tasks (JWT Required)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/tasks` | Create task |
| GET | `/api/tasks` | List tasks (filtered + paginated) |
| GET | `/api/tasks/{id}` | Get task by ID |
| PUT | `/api/tasks/{id}` | Update task |
| DELETE | `/api/tasks/{id}` | Soft delete task |
| GET | `/api/tasks/search?keyword=` | Search tasks |
| GET | `/api/tasks/dashboard` | Task stats |
| POST | `/api/tasks/{id}/upload` | Upload file attachment |

### Query Parameters (GET /api/tasks)
| Param | Type | Default | Example |
|-------|------|---------|---------|
| `status` | Enum | — | `TODO`, `IN_PROGRESS`, `DONE` |
| `priority` | Enum | — | `LOW`, `MEDIUM`, `HIGH` |
| `page` | int | 0 | `0` |
| `size` | int | 10 | `5` |
| `sortBy` | String | `createdAt` | `dueDate` |
| `sortDir` | String | `desc` | `asc` |

## Project Structure
```
src/main/java/com/taskmanager/
├── controller/     # REST endpoints
├── service/        # Business logic
├── repository/     # Database queries
├── entity/         # JPA entities + enums
├── dto/            # Request/Response objects
├── security/       # JWT auth
├── config/         # Security, Swagger config
├── exception/      # Global error handling
├── scheduler/      # Cron jobs
└── util/           # Helpers
```

## Build & Test
```bash
mvn clean compile    # Compile
mvn test             # Run unit tests
mvn clean package    # Build JAR
```
