# FocusFlow AI — Modular Monolith

FocusFlow is a personal productivity platform with a deep-work timer, AI coaching, and an AI-powered flashcard study coach. The backend is now a **single Spring Boot application** with modular Session, Study, and Coach features, plus the JavaFX desktop client.

## Architecture

```text
JavaFX Desktop Client
        │ HTTP REST
        ▼
FocusFlow Backend :8080
 ├── Session module
 ├── Study module
 ├── AI Coach module
 ├── Tags & statistics
 └── CORS
        │
        ▼
Spring Data JPA
        │
        ├── PostgreSQL (DB_TYPE=postgres)
        └── MySQL      (DB_TYPE=mysql)
```

Only **one database is active per application startup**. Select it with `DB_TYPE`.

## Prerequisites

- Java 21
- Maven 3.9+
- MySQL 8+ or PostgreSQL 14+
- Git

Docker is not required.

## 1. Configure the database

Copy the environment template:

```bash
cp .env.example .env
```

### PostgreSQL

```text
DB_TYPE=postgres
DB_URL=jdbc:postgresql://localhost:5432/focusflow
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

Create the database first:

```sql
CREATE DATABASE focusflow;
```

### MySQL

```text
DB_TYPE=mysql
DB_URL=jdbc:mysql://localhost:3306/focusflow?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=your_password
```

Create the database first:

```sql
CREATE DATABASE focusflow;
```

The backend contains both JDBC drivers, but Spring Boot connects only to the database selected by `DB_TYPE`.

## 2. Configure AI features

Set your Anthropic key in `.env`:

```text
ANTHROPIC_API_KEY=sk-ant-your-key-here
```

AI features require this key; normal session/tag/statistics features do not.

## 3. Start the backend

From the repository root:

```bash
mvn -pl backend spring-boot:run
```

The API is available at:

```text
http://localhost:8080
```

You can also build everything with:

```bash
mvn clean package
```

## 4. Run the JavaFX client

In another terminal:

```bash
cd javafx-client
mvn javafx:run
```

The client continues to use the same `/api/...` endpoints on port `8080`, so no gateway or service ports are required.

## API Endpoints

### Sessions

```text
POST   /api/sessions
PUT    /api/sessions/{id}/complete
PUT    /api/sessions/{id}/cancel
GET    /api/sessions
GET    /api/sessions/stats
GET    /api/sessions/stats/weekly
GET    /api/sessions/summary
GET    /api/sessions/tags
POST   /api/sessions/tags
PUT    /api/sessions/tags/{id}
GET    /api/sessions/export
```

### AI Coach

```text
POST   /api/coach/insights
GET    /api/coach/insights/latest
```

### Study Coach

```text
GET    /api/study/decks
POST   /api/study/decks/generate
GET    /api/study/decks/{id}/cards
PATCH  /api/study/cards/{id}/known
DELETE /api/study/decks/{id}
```

## Project Structure

```text
Focus-Flow-/
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/focusflow/
│       │   ├── FocusFlowApplication.java
│       │   ├── config/
│       │   ├── session/
│       │   ├── study/
│       │   └── coach/
│       └── resources/
│           └── application.yml
├── javafx-client/
├── tasks/
├── .env.example
├── pom.xml
└── README.md
```

## Database Selection

Change only the environment variables before starting the backend:

```text
DB_TYPE=postgres
```

or:

```text
DB_TYPE=mysql
```

The schema is managed by Hibernate with `ddl-auto=update` by default.

## Migration Notes

- Removed the API Gateway from the runtime architecture.
- Combined Session, Study, and AI Coach into one Spring Boot application.
- Preserved the existing `/api/...` REST contract for the JavaFX client.
- AI Coach now calls the Session module directly instead of making an internal HTTP request.
- Added startup selection between MySQL and PostgreSQL.
- Removed Docker Compose and Docker-specific startup instructions.
