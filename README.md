# FocusFlow AI - Microservices Edition

A personal productivity platform with a deep work timer, AI coaching, and a flashcard study coach. The project is built as Spring Boot microservices plus a JavaFX desktop client.

## Architecture

```text
JavaFX Desktop Client
        | HTTP REST
        v
API Gateway :8080
        |
   +----+-------------------+-------------------+
   v                        v                   v
Session Service :8081   AI Coach :8082   Study Service :8083
(PostgreSQL)            (Claude)         (PostgreSQL + Claude)
```

## Quick Start

### Prerequisites

- Java 21
- Maven 3.9+
- Docker Desktop 24+
- Git

### 1. Configure environment

```bash
cp .env.example .env
# Edit .env and set ANTHROPIC_API_KEY
```

### 2. Start the backend stack

```bash
docker compose up --build
```

### 3. Compile the whole monorepo

```bash
mvn compile
```

### 4. Verify services

```bash
curl http://localhost:8080/api/sessions/stats
curl http://localhost:8080/api/study/decks
curl http://localhost:8080/api/coach/insights/latest
```

### 5. Run the JavaFX client

```bash
cd javafx-client
mvn javafx:run
```

## Services

| Service | Port | Description |
|---|---:|---|
| API Gateway | 8080 | Routes `/api/*` requests and handles CORS |
| Session Service | 8081 | Sessions, tags, stats, weekly trends, CSV export |
| AI Coach Service | 8082 | Claude-powered coaching insights |
| Study Service | 8083 | Flashcard decks and AI generation |

## Key API Endpoints

```text
POST   /api/sessions
PUT    /api/sessions/{id}/complete
PUT    /api/sessions/{id}/cancel
GET    /api/sessions
GET    /api/sessions/stats
GET    /api/sessions/stats/weekly
GET    /api/sessions/tags
POST   /api/sessions/tags
PUT    /api/sessions/tags/{id}
GET    /api/sessions/export

POST   /api/coach/insights
GET    /api/coach/insights/latest

GET    /api/study/decks
POST   /api/study/decks/generate
GET    /api/study/decks/{id}/cards
PATCH  /api/study/cards/{id}/known
DELETE /api/study/decks/{id}
```

## Useful Commands

```bash
docker compose up --build
docker compose up -d
docker compose down
docker compose logs -f

mvn compile
mvn -pl session-service spring-boot:run
mvn -pl coach-service spring-boot:run
mvn -pl study-service spring-boot:run
```

## Recent roadmap progress

- Session history now supports pagination and CSV export.
- The timer screen now includes a custom tag editor with create, rename, and recolor flows.
- The stats dashboard now renders a weekly focus chart.
- Study mode now includes a client-side multiple-choice quiz flow for decks.

## Project Structure

```text
api-gateway/
session-service/
coach-service/
study-service/
javafx-client/
tasks/
docker-compose.yml
pom.xml
```
