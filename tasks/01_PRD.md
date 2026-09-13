**FocusFlow AI**

**Product Requirements Document**

Microservices Architecture Edition

FocusFlow AI • Focus Timer + AI Coach + Study Coach • Java / Spring Boot
/ JavaFX

  ------------------- ---------------------------------------------------
  **Version**         2.0 --- Microservices Edition

  **Status**          In Development

  **Date**            February 2026

  **Author**          Product & Engineering Team

  **Original**        FocusFlow v1 (Monolith JavaFX + SQLite)

  **This Doc**        Full redesign as distributed microservices with
                      JavaFX desktop frontend
  ------------------- ---------------------------------------------------

**1. Executive Summary**

FocusFlow AI is a personal productivity platform that combines deep work
session tracking, AI-powered coaching, and an intelligent flashcard
study system. Version 2.0 re-architects the original monolithic JavaFX +
SQLite application into a microservices-based system, enabling
independent scaling, deployment, and development of each functional
domain.

The platform is composed of four backend microservices --- Session
Service, AI Coach Service, Study Coach Service, and API Gateway --- all
communicating over REST. The JavaFX desktop client acts as a rich
frontend consuming these services. Each microservice owns its own
database, is independently deployable via Docker, and exposes a
versioned REST API.

**2. Problem Statement**

**2.1 Limitations of the Monolith**

The v1 monolith (single JavaFX process + SQLite file) has clear ceiling
limitations as the product grows:

-   All features are tightly coupled --- a bug in the Study Coach can
    crash the Timer

-   AI API calls block the UI thread unless carefully offloaded

-   SQLite cannot scale to multi-user or multi-device scenarios

-   No separation of concerns --- database schema changes affect all
    features simultaneously

-   Impossible to deploy features independently or run A/B experiments

**2.2 Why Microservices**

-   Session tracking, AI coaching, and study management are distinct
    bounded contexts

-   AI services have very different latency and scaling profiles from
    timer logic

-   Enables future multi-platform clients (web, mobile) against the same
    backend

-   Each team member can own and deploy a service independently

**3. Goals & Non-Goals**

**3.1 Goals**

-   Decompose FocusFlow AI into independently deployable microservices

-   Each service owns its own database (no shared schema)

-   API Gateway as single entry point for the JavaFX desktop client

-   All inter-service communication via REST over HTTP

-   Docker Compose for local development and deployment

-   Retain all existing features: Timer, Stats, History, AI Coach, Study
    Coach

-   Preserve existing JavaFX frontend --- connect it to the new backend
    APIs

**3.2 Non-Goals**

-   No Kubernetes or cloud deployment in v2.0 (Docker Compose only)

-   No event-driven / message queue architecture (REST only for now)

-   No authentication or multi-user support in v2.0

-   No mobile or web frontend in v2.0

**4. Microservices Architecture**

**4.1 Services Overview**

  -----------------------------------------------------------------------------
  **Service**   **Port**   **Responsibility**     **Database**   **Language**
  ------------- ---------- ---------------------- -------------- --------------
  API Gateway   8080       Single entry point,    None           Spring Boot
                           routing, CORS                         

  Session       8081       Timer sessions, tags,  PostgreSQL     Spring Boot
  Service                  stats, streaks                        

  AI Coach      8082       Productivity insights  PostgreSQL     Spring Boot
  Service                  via Claude API                        

  Study Coach   8083       Flashcard decks,       PostgreSQL     Spring Boot
  Service                  generation, review                    

  JavaFX Client N/A        Desktop UI, consumes   None           Java 21 +
                           Gateway REST API                      JavaFX
  -----------------------------------------------------------------------------

**4.2 Architecture Diagram (Text)**

  -----------------------------------------------------------------------
  ┌─────────────────────────────────────────────────────────────┐

  │ JavaFX Desktop Client │

  │ (TimerView / StatsView / AiCoachView / StudyView) │

  └──────────────────────────┬──────────────────────────────────┘

  │ HTTP REST

  ▼

  ┌─────────────────────────────────────────────────────────────┐

  │ API Gateway :8080 │

  │ Spring Cloud Gateway --- routing + CORS │

  └───────────┬───────────────────┬───────────────┬────────────┘

  │ │ │

  ▼ ▼ ▼

  ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐

  │ Session Service │ │ AI Coach Service │ │ Study Service │

  │ :8081 │ │ :8082 │ │ :8083 │

  │ PostgreSQL:5432 │ │ PostgreSQL:5433 │ │ PostgreSQL:5434 │

  └──────────────────┘ └────────┬──────────┘ └──────────────────┘

  │

  ▼

  ┌──────────────────┐

  │ Anthropic API │

  │ (Claude Sonnet) │

  └──────────────────┘
  -----------------------------------------------------------------------

**4.3 API Gateway Routing Rules**

  -------------------------------------------------------------------------
  **Path Prefix**      **Routes To**            **Example**
  -------------------- ------------------------ ---------------------------
  /api/sessions/\*\*   Session Service :8081    GET
                                                /api/sessions/today-stats

  /api/coach/\*\*      AI Coach Service :8082   POST /api/coach/insights

  /api/study/\*\*      Study Coach Service      POST
                       :8083                    /api/study/decks/generate
  -------------------------------------------------------------------------

**5. Feature Requirements**

  ---------------------------------------------------------------------------
  **Feature**     **Service**    **Description**               **Priority**
  --------------- -------------- ----------------------------- --------------
  Focus Timer     Session Svc    Start, pause, stop sessions;  **P0**
                                 configurable duration & tag   

  Session         Session Svc    POST/GET sessions via REST;   **P0**
  Persistence                    stored in PostgreSQL          

  Daily Stats     Session Svc    Total focus minutes today,    **P0**
                                 completion rate               

  Streak Tracking Session Svc    Consecutive days with ≥1      **P0**
                                 completed session             

  Session History Session Svc    Paginated list of past        **P0**
                                 sessions with filters         

  AI Coach        AI Coach Svc   Claude analyses session       **P0**
  Insights                       history; returns coaching     
                                 report                        

  Flashcard       Study Coach    Paste text → Claude generates **P0**
  Generation      Svc            Q&A flashcard JSON            

  Deck Management Study Coach    CRUD operations for decks and **P0**
                  Svc            cards                         

  Review Mode     Study Coach    Flip cards, mark              **P1**
                  Svc            known/review, track score per 
                                 session                       

  Stats Dashboard Session Svc    Aggregate view: total hours,  **P1**
                                 top tags, weekly trends       

  Custom Tags     Session Svc    Create, rename, color-code    **P1**
                                 session categories            

  CSV Export      Session Svc    Export session history as     **P2**
                                 downloadable CSV              

  Quiz Mode       Study Coach    Multiple-choice quiz          **P2**
                  Svc            generated from deck; scored   
  ---------------------------------------------------------------------------

**6. Data Models**

**6.1 Session Service --- sessions table**

  -----------------------------------------------------------------------
  CREATE TABLE sessions (

  id SERIAL PRIMARY KEY,

  tag VARCHAR(100) NOT NULL DEFAULT \'General\',

  duration_min INTEGER NOT NULL,

  start_time TIMESTAMP NOT NULL,

  end_time TIMESTAMP,

  completed BOOLEAN NOT NULL DEFAULT false

  );

  CREATE TABLE tags (

  id SERIAL PRIMARY KEY,

  name VARCHAR(100) UNIQUE NOT NULL,

  color VARCHAR(7) DEFAULT \'#4A90D9\'

  );
  -----------------------------------------------------------------------

**6.2 Study Coach Service --- decks & flashcards tables**

  -----------------------------------------------------------------------
  CREATE TABLE decks (

  id SERIAL PRIMARY KEY,

  title VARCHAR(255) NOT NULL,

  created_at TIMESTAMP NOT NULL DEFAULT now()

  );

  CREATE TABLE flashcards (

  id SERIAL PRIMARY KEY,

  deck_id INTEGER REFERENCES decks(id) ON DELETE CASCADE,

  question TEXT NOT NULL,

  answer TEXT NOT NULL,

  known BOOLEAN DEFAULT false

  );
  -----------------------------------------------------------------------

**7. Milestones & Roadmap**

  --------------------------------------------------------------------------
  **Milestone**   **Target**   **Scope**
  --------------- ------------ ---------------------------------------------
  v2.0 Alpha      Week 1--2    Session Service + PostgreSQL + REST API +
                               Docker

  v2.0 Beta       Week 3       AI Coach Service + Study Coach Service + API
                               Gateway routing

  v2.0 RC         Week 4       JavaFX client refactored to call Gateway APIs
                               (drop SQLite)

  v2.0            Week 5       Integration testing, Docker Compose
                               one-command startup, docs

  v2.1            Month 2      CSV export, custom tags UI, quiz mode, weekly
                               chart view

  v3.0            Month 4      Auth layer, multi-device sync, web frontend
                               (React)
  --------------------------------------------------------------------------

**8. Success Metrics**

-   All 3 backend services start successfully with docker compose up

-   JavaFX client connects to Gateway and completes a full session flow
    end-to-end

-   AI Coach returns insights in \< 10 seconds for a typical session
    history

-   Study Coach generates a 5-card deck from 500 words in \< 15 seconds

-   Session data survives service restarts (PostgreSQL persistence
    verified)

-   Zero breaking changes to the JavaFX UI layer when backend services
    redeploy

**9. Open Questions**

-   Should inter-service communication eventually move to async
    messaging (RabbitMQ/Kafka)?

-   What is the preferred auth strategy for v3: JWT, OAuth2, or API key
    per device?

-   Should the AI Coach cache responses to avoid redundant API calls on
    unchanged data?

-   Will the Study Coach eventually support PDF/image input for
    flashcard generation?
