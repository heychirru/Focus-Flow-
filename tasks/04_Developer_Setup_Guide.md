**FocusFlow AI**

**Developer Setup Guide**

Local Development Environment --- Microservices Edition

FocusFlow AI • Spring Boot • JavaFX • Docker Compose

  ------------------- ---------------------------------------------------
  **Audience**        Backend and frontend developers joining the project

  **Prerequisites**   Java 21, Maven 3.9+, Docker Desktop 24+, Git

  **Est. Setup**      \~20 minutes on a clean machine

  **OS Support**      macOS, Windows 11, Ubuntu 22+

  **Date**            February 2026
  ------------------- ---------------------------------------------------

**1. Prerequisites**

  ------------------------------------------------------------------------------
  **Tool**        **Version**   **Install**
  --------------- ------------- ------------------------------------------------
  Java JDK        21 (LTS)      https://adoptium.net or sdk install java 21

  Maven           3.9+          https://maven.apache.org or brew install maven

  Docker Desktop  24+           https://www.docker.com/products/docker-desktop

  Git             2.40+         https://git-scm.com or brew install git

  IntelliJ IDEA   2023.3+       https://www.jetbrains.com/idea (Community is
                  (rec.)        fine)
  ------------------------------------------------------------------------------

  -----------------------------------------------------------------------
  **ℹ️ NOTE:** The JavaFX client requires JavaFX 21 SDK. IntelliJ IDEA
  bundles it when you use the javafx-maven-plugin. No separate download
  needed.

  -----------------------------------------------------------------------

**2. Clone the Repository**

  -----------------------------------------------------------------------
  \# Clone the monorepo

  git clone https://github.com/your-org/focusflow-ai.git

  cd focusflow-ai

  \# Verify structure

  ls

  \# api-gateway/ session-service/ coach-service/

  \# study-service/ javafx-client/ docker-compose.yml
  -----------------------------------------------------------------------

**3. Environment Variables**

Create a .env file in the project root. Docker Compose will pick this up
automatically:

  -----------------------------------------------------------------------
  \# .env (copy from .env.example --- never commit this file)

  ANTHROPIC_API_KEY=sk-ant-your-key-here

  \# Optional overrides (defaults shown)

  SESSION_DB_PASSWORD=postgres

  STUDY_DB_PASSWORD=postgres

  SESSION_SERVICE_URL=http://session-service:8081
  -----------------------------------------------------------------------

  -----------------------------------------------------------------------
  **⚠️ WARNING:** Never commit your .env file or ANTHROPIC_API_KEY to
  version control. The .gitignore already excludes .env --- do not
  override this.

  -----------------------------------------------------------------------

To get an Anthropic API key:

1.  Go to https://console.anthropic.com

2.  Sign in and navigate to API Keys

3.  Click Create Key, copy it, and paste it into your .env file

**4. Start Backend Services (Docker Compose)**

All three microservices and their PostgreSQL databases can be started
with a single command:

  -----------------------------------------------------------------------
  \# From the project root

  docker compose up \--build

  \# First run will pull postgres:16 and build all 4 Spring Boot images

  \# Expect 3-5 minutes on first run

  \# Verify all services are healthy

  docker compose ps

  \# Expected output:

  NAME STATUS PORTS

  api-gateway running 0.0.0.0:8080-\>8080/tcp

  session-service running 0.0.0.0:8081-\>8081/tcp

  coach-service running 0.0.0.0:8082-\>8082/tcp

  study-service running 0.0.0.0:8083-\>8083/tcp

  session-db running 5432/tcp

  study-db running 5432/tcp
  -----------------------------------------------------------------------

  -----------------------------------------------------------------------
  **💡 TIP:** Use \"docker compose up -d\" to run in detached
  (background) mode. View logs with \"docker compose logs -f
  coach-service\".

  -----------------------------------------------------------------------

**4.1 Verify Services Are Running**

  -----------------------------------------------------------------------
  \# Health check --- all should return HTTP 200

  curl http://localhost:8080/api/sessions/stats

  curl http://localhost:8080/api/coach/insights/latest

  curl http://localhost:8080/api/study/decks

  \# Expected (empty DB)

  \# /stats → { \"totalSessions\": 0, \"totalMinutes\": 0, \... }

  \# /latest → HTTP 404 (no insights yet --- that\'s fine)

  \# /decks → \[\]
  -----------------------------------------------------------------------

**5. Run the JavaFX Desktop Client**

The JavaFX client is a standard Maven project. Run it directly from
IntelliJ or from the terminal:

  -----------------------------------------------------------------------
  cd javafx-client

  \# Run via Maven JavaFX plugin

  mvn javafx:run

  \# Or package as a fat JAR and run

  mvn clean package -DskipTests

  java -jar target/focusflow-ai-1.0-SNAPSHOT.jar
  -----------------------------------------------------------------------

  -----------------------------------------------------------------------
  **ℹ️ NOTE:** The JavaFX client connects to the API Gateway at
  http://localhost:8080 by default. To change this, edit ApiClient.java
  and update the BASE_URL constant.

  -----------------------------------------------------------------------

**5.1 Opening in IntelliJ IDEA**

4.  File → Open → select the javafx-client/ folder

5.  IntelliJ will auto-detect the pom.xml and configure the project

6.  Open the Maven panel → Plugins → javafx → javafx:run

7.  Or create a Run Configuration: Main class = com.focusflowai.App

**6. Running Individual Services (Without Docker)**

During development you may want to run a single service locally while
the rest run in Docker:

  -----------------------------------------------------------------------
  \# Example: run Session Service locally against Docker DB

  cd session-service

  mvn spring-boot:run \\

  -Dspring-boot.run.arguments=\"

  \--spring.datasource.url=jdbc:postgresql://localhost:5432/sessions

  \--spring.datasource.username=postgres

  \--spring.datasource.password=postgres\"

  \# In docker-compose.yml, comment out the session-service block

  \# so Docker doesn\'t also run it on :8081
  -----------------------------------------------------------------------

**6.1 Per-Service pom.xml Dependencies**

  ------------------------------------------------------------------------
  **Service**       **Key Dependencies**
  ----------------- ------------------------------------------------------
  session-service   spring-boot-starter-web, spring-boot-starter-data-jpa,
                    postgresql, lombok

  coach-service     spring-boot-starter-web, gson, (no DB --- stateless)

  study-service     spring-boot-starter-web, spring-boot-starter-data-jpa,
                    postgresql, gson

  api-gateway       spring-cloud-starter-gateway (no business logic)

  javafx-client     javafx-controls, javafx-fxml, gson, slf4j-simple
  ------------------------------------------------------------------------

**7. Seeding Test Data**

Use these curl commands to quickly populate the database for UI testing:

  -----------------------------------------------------------------------
  \# Create a few sessions

  curl -X POST http://localhost:8080/api/sessions \\

  -H \"Content-Type: application/json\" \\

  -d \'{\"tag\":\"Coding\",\"durationMinutes\":25}\'

  \# Complete session 1

  curl -X PUT http://localhost:8080/api/sessions/1/complete

  \# Create another session and cancel it

  curl -X POST http://localhost:8080/api/sessions \\

  -H \"Content-Type: application/json\" \\

  -d \'{\"tag\":\"Writing\",\"durationMinutes\":50}\'

  curl -X PUT http://localhost:8080/api/sessions/2/cancel

  \# Generate a flashcard deck

  curl -X POST http://localhost:8080/api/study/decks/generate \\

  -H \"Content-Type: application/json\" \\

  -d \'{\"title\":\"Test Deck\",\"sourceText\":\"Photosynthesis is the
  process by which plants use sunlight, water, and carbon dioxide to
  produce glucose and oxygen. The light-dependent reactions occur in the
  thylakoid membranes. The Calvin cycle occurs in the stroma.\"}\'

  \# Get AI coaching insights (requires ANTHROPIC_API_KEY)

  curl -X POST http://localhost:8080/api/coach/insights
  -----------------------------------------------------------------------

**8. Common Issues & Fixes**

  ------------------------------------------------------------------------
  **Problem**        **Likely Cause**    **Fix**
  ------------------ ------------------- ---------------------------------
  Port 8080 already  Another process is  lsof -i :8080 \| kill the PID, or
  in use             on 8080             change gateway port in
                                         docker-compose.yml

  JavaFX: \"No       JDK missing JavaFX  Use a JDK with JavaFX bundled, or
  toolkit found\"    modules             add \--add-modules
                                         javafx.controls,javafx.fxml to
                                         JVM args

  Claude API returns Invalid or missing  Check .env file has correct
  401                API key             ANTHROPIC_API_KEY and re-run
                                         docker compose up

  DB connection      DB container not    Spring Boot retries
  refused on startup ready yet           automatically. If stuck, run:
                                         docker compose restart
                                         session-service

  \"No such service: Docker Compose YAML Validate docker-compose.yml at
  coach-service\"    indentation error   https://yaml.lint.com

  Maven: \"Could not javafx.version      Check pom.xml --- javafx.version
  resolve javafx\"   mismatch            must match installed JDK JavaFX
                                         (use 21.0.2)
  ------------------------------------------------------------------------

**9. Useful Commands Cheatsheet**

  -----------------------------------------------------------------------
  \# ── Docker ───────────────────────────────────────────

  docker compose up \--build \# Build & start all services

  docker compose up -d \# Start in background

  docker compose down \# Stop all services

  docker compose down -v \# Stop + delete volumes (reset DB)

  docker compose logs -f \# Stream all logs

  docker compose logs -f coach-service \# Stream a single service

  docker compose restart study-service \# Restart one service

  \# ── Maven ────────────────────────────────────────────

  mvn clean install \# Build all modules

  mvn spring-boot:run \# Run a service locally

  mvn javafx:run \# Run JavaFX client

  mvn test \# Run unit tests

  \# ── Quick health checks ──────────────────────────────

  curl http://localhost:8080/api/sessions/stats

  curl http://localhost:8080/api/study/decks

  curl -X POST http://localhost:8080/api/coach/insights
  -----------------------------------------------------------------------

  -----------------------------------------------------------------------
  **💡 TIP:** For fastest development iteration: run the DB containers
  via Docker (docker compose up session-db study-db), then run your
  target service directly with \"mvn spring-boot:run\". This avoids
  Docker rebuilds on every code change.

  -----------------------------------------------------------------------
