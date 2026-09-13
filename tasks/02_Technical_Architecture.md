**FocusFlow AI**

**Technical Architecture Document**

Microservices Design, APIs & Code Reference

FocusFlow AI • Spring Boot • JavaFX • PostgreSQL • Docker

  ------------------- ---------------------------------------------------
  **Version**         2.0

  **Date**            February 2026

  **Stack**           Java 21, Spring Boot 3.2, JavaFX 21, PostgreSQL 16,
                      Docker Compose

  **Architecture**    Microservices --- 3 domain services + API Gateway +
                      JavaFX desktop client

  **AI Provider**     Anthropic Claude (claude-sonnet-4-6) via REST API

  **Build Tool**      Apache Maven (per service)
  ------------------- ---------------------------------------------------

**1. System Overview**

FocusFlow AI is composed of five deployable units. Three Spring Boot
microservices handle domain logic, a Spring Cloud Gateway routes client
traffic, and a JavaFX desktop application serves as the rich UI. All
backend services run in Docker containers and are orchestrated locally
via Docker Compose.

**1.1 Technology Stack**

  -----------------------------------------------------------------------------
  **Layer**          **Technology**     **Version**   **Purpose**
  ------------------ ------------------ ------------- -------------------------
  Backend Framework  Spring Boot        3.2.x         REST microservices

  API Gateway        Spring Cloud       4.x           Routing, CORS, load
                     Gateway                          balancing

  Frontend           JavaFX             21.0.2        Desktop client UI

  Database           PostgreSQL         16            Per-service relational
                                                      storage

  Containerisation   Docker + Compose   24+           Local orchestration

  AI Provider        Anthropic Claude   Sonnet 4.6    Coaching & flashcard
                                                      generation

  HTTP Client (FE)   Java HttpClient    Java 21       REST calls from JavaFX

  JSON               Gson / Jackson     2.10+         Serialisation /
                                                      deserialisation

  Build              Maven              3.9+          Dependency management
  -----------------------------------------------------------------------------

**2. Microservice Breakdown**

**2.1 API Gateway (:8080)**

The gateway is a thin Spring Cloud Gateway instance. It does not contain
any business logic --- it routes requests to the correct downstream
service and handles CORS so the JavaFX client can make cross-origin
calls.

**application.yml**

  -----------------------------------------------------------------------
  server:

  port: 8080

  spring:

  cloud:

  gateway:

  routes:

  \- id: session-service

  uri: http://session-service:8081

  predicates:

  \- Path=/api/sessions/\*\*

  \- id: coach-service

  uri: http://coach-service:8082

  predicates:

  \- Path=/api/coach/\*\*

  \- id: study-service

  uri: http://study-service:8083

  predicates:

  \- Path=/api/study/\*\*

  globalcors:

  corsConfigurations:

  \'\[/\*\*\]\':

  allowedOrigins: \"\*\"

  allowedMethods: \[GET, POST, PUT, DELETE, OPTIONS\]

  allowedHeaders: \"\*\"
  -----------------------------------------------------------------------

**2.2 Session Service (:8081)**

Owns all focus session data. Provides REST endpoints for creating,
updating, and querying sessions and tags. Also calculates derived stats
(daily minutes, streaks, completion rates) that the AI Coach uses for
context.

**SessionController.java --- key endpoints**

  -----------------------------------------------------------------------
  \@RestController

  \@RequestMapping(\"/api/sessions\")

  public class SessionController {

  \@PostMapping

  public ResponseEntity\<Session\> create(@RequestBody
  CreateSessionRequest req) {

  Session s = sessionService.start(req.tag(), req.durationMinutes());

  return ResponseEntity.status(201).body(s);

  }

  \@PutMapping(\"/{id}/complete\")

  public ResponseEntity\<Session\> complete(@PathVariable Long id) {

  return ResponseEntity.ok(sessionService.complete(id));

  }

  \@PutMapping(\"/{id}/cancel\")

  public ResponseEntity\<Session\> cancel(@PathVariable Long id) {

  return ResponseEntity.ok(sessionService.cancel(id));

  }

  \@GetMapping

  public List\<Session\> getAll() {

  return sessionService.findAll();

  }

  \@GetMapping(\"/stats\")

  public StatsResponse getStats() {

  return sessionService.buildStats();

  }

  \@GetMapping(\"/summary\")

  public String getSummary() {

  // Plain-text stats summary consumed by AI Coach Service

  return sessionService.buildTextSummary();

  }

  }
  -----------------------------------------------------------------------

**Session.java --- JPA entity**

  -----------------------------------------------------------------------
  \@Entity

  \@Table(name = \"sessions\")

  public class Session {

  \@Id \@GeneratedValue(strategy = GenerationType.IDENTITY)

  private Long id;

  \@Column(nullable = false)

  private String tag = \"General\";

  \@Column(nullable = false)

  private int durationMinutes;

  \@Column(nullable = false)

  private LocalDateTime startTime;

  private LocalDateTime endTime;

  private boolean completed = false;

  // getters / setters / constructors omitted for brevity

  }
  -----------------------------------------------------------------------

**2.3 AI Coach Service (:8082)**

Calls the Session Service to get a text summary of the user\'s history,
then sends it to the Anthropic API with a coaching system prompt. Caches
the last response to avoid redundant API calls when data has not
changed.

**AiCoachService.java**

  -----------------------------------------------------------------------
  \@Service

  public class AiCoachService {

  private static final String SYSTEM_PROMPT = \"\"\"

  You are a personal productivity coach embedded in FocusFlow AI.

  Analyse the session history and provide:

  1\. A warm, encouraging 1-2 sentence opening

  2\. 3-5 specific, data-driven insights about focus habits

  3\. 2-3 actionable recommendations tailored to the patterns you see

  4\. A motivating closing line

  Be specific --- reference actual numbers. 200-300 words.

  \"\"\";

  public String getInsights() {

  // 1. Fetch stats summary from Session Service

  String stats = sessionClient.getSummary();

  // 2. Build Claude request

  ClaudeRequest req = new ClaudeRequest(

  \"claude-sonnet-4-6\",

  List.of(new Message(\"user\",

  \"Session history:\\n\" + stats + \"\\nGive me coaching insights.\")),

  SYSTEM_PROMPT, 2048

  );

  // 3. Call Anthropic API and extract text

  ClaudeResponse resp = claudeClient.complete(req);

  return resp.content().get(0).text();

  }

  }
  -----------------------------------------------------------------------

**ClaudeApiClient.java --- HTTP client wrapper**

  -----------------------------------------------------------------------
  \@Component

  public class ClaudeApiClient {

  private static final String URL =
  \"https://api.anthropic.com/v1/messages\";

  private final HttpClient http = HttpClient.newHttpClient();

  public ClaudeResponse complete(ClaudeRequest req) {

  String json = gson.toJson(req);

  HttpRequest request = HttpRequest.newBuilder()

  .uri(URI.create(URL))

  .header(\"x-api-key\", apiKey)

  .header(\"anthropic-version\", \"2023-06-01\")

  .header(\"Content-Type\", \"application/json\")

  .POST(HttpRequest.BodyPublishers.ofString(json))

  .timeout(Duration.ofSeconds(60))

  .build();

  HttpResponse\<String\> resp = http.send(request,

  HttpResponse.BodyHandlers.ofString());

  return gson.fromJson(resp.body(), ClaudeResponse.class);

  }

  }
  -----------------------------------------------------------------------

**2.4 Study Coach Service (:8083)**

Manages flashcard decks. On deck generation it calls Claude with the
user\'s source text and a JSON-structured prompt, parses the response,
and persists the cards. Also handles deck CRUD and per-card
known/unknown tracking.

**StudyCoachService.java --- generateDeck()**

  -----------------------------------------------------------------------
  public Deck generateDeck(String title, String sourceText) {

  String prompt = \"\"\"

  Generate flashcards from this text. Respond ONLY with valid JSON:

  {\"cards\":\[{\"question\":\"\...\",\"answer\":\"\...\"}\]}

  Text: %s

  \"\"\".formatted(sourceText);

  String json = claudeClient.complete(SYSTEM_PROMPT, prompt);

  // Parse response

  JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

  JsonArray cards = obj.getAsJsonArray(\"cards\");

  // Persist

  Deck deck = deckRepository.save(new Deck(title));

  cards.forEach(el -\> {

  JsonObject c = el.getAsJsonObject();

  flashcardRepository.save(new Flashcard(

  deck.getId(),

  c.get(\"question\").getAsString(),

  c.get(\"answer\").getAsString()

  ));

  });

  return deckRepository.findWithCards(deck.getId());

  }
  -----------------------------------------------------------------------

**3. JavaFX Desktop Client**

The JavaFX client replaces all direct SQLite calls with REST calls to
the API Gateway. A shared ApiClient service handles HTTP, JSON parsing,
and error handling. Each controller calls ApiClient asynchronously using
CompletableFuture to keep the UI thread responsive.

**ApiClient.java --- shared REST client**

  -----------------------------------------------------------------------
  public class ApiClient {

  private static final String BASE = \"http://localhost:8080\";

  private final HttpClient http = HttpClient.newHttpClient();

  private final Gson gson = new Gson();

  public \<T\> CompletableFuture\<T\> getAsync(String path, Class\<T\>
  type) {

  return CompletableFuture.supplyAsync(() -\> {

  HttpRequest req = HttpRequest.newBuilder()

  .uri(URI.create(BASE + path))

  .GET().build();

  HttpResponse\<String\> resp =

  http.send(req, HttpResponse.BodyHandlers.ofString());

  return gson.fromJson(resp.body(), type);

  });

  }

  public \<T\> CompletableFuture\<T\> postAsync(String path,

  Object body,

  Class\<T\> type) {

  return CompletableFuture.supplyAsync(() -\> {

  HttpRequest req = HttpRequest.newBuilder()

  .uri(URI.create(BASE + path))

  .header(\"Content-Type\", \"application/json\")

  .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))

  .build();

  HttpResponse\<String\> resp =

  http.send(req, HttpResponse.BodyHandlers.ofString());

  return gson.fromJson(resp.body(), type);

  });

  }

  }
  -----------------------------------------------------------------------

**TimerController.java --- async session start**

  -----------------------------------------------------------------------
  // Start session: POST /api/sessions

  public void onStart() {

  CreateSessionRequest req = new CreateSessionRequest(

  tagCombo.getValue(), durationSpinner.getValue()

  );

  apiClient.postAsync(\"/api/sessions\", req, Session.class)

  .thenAccept(session -\> Platform.runLater(() -\> {

  this.activeSessionId = session.id();

  timer.start();

  setStatus(\"Stay focused! \", \"#4CAF50\");

  }))

  .exceptionally(ex -\> {

  Platform.runLater(() -\>

  setStatus(\"Error: \" + ex.getMessage(), \"#EF5350\"));

  return null;

  });

  }
  -----------------------------------------------------------------------

**4. Docker Compose Setup**

  -----------------------------------------------------------------------
  version: \"3.9\"

  services:

  api-gateway:

  build: ./api-gateway

  ports: \[\"8080:8080\"\]

  depends_on: \[session-service, coach-service, study-service\]

  session-service:

  build: ./session-service

  ports: \[\"8081:8081\"\]

  environment:

  SPRING_DATASOURCE_URL: jdbc:postgresql://session-db:5432/sessions

  SPRING_DATASOURCE_USERNAME: postgres

  SPRING_DATASOURCE_PASSWORD: postgres

  depends_on: \[session-db\]

  coach-service:

  build: ./coach-service

  ports: \[\"8082:8082\"\]

  environment:

  ANTHROPIC_API_KEY: \${ANTHROPIC_API_KEY}

  SESSION_SERVICE_URL: http://session-service:8081

  depends_on: \[coach-db, session-service\]

  study-service:

  build: ./study-service

  ports: \[\"8083:8083\"\]

  environment:

  ANTHROPIC_API_KEY: \${ANTHROPIC_API_KEY}

  SPRING_DATASOURCE_URL: jdbc:postgresql://study-db:5432/study

  depends_on: \[study-db\]

  session-db:

  image: postgres:16

  environment: { POSTGRES_DB: sessions, POSTGRES_PASSWORD: postgres }

  volumes: \[session-data:/var/lib/postgresql/data\]

  coach-db:

  image: postgres:16

  environment: { POSTGRES_DB: coach, POSTGRES_PASSWORD: postgres }

  study-db:

  image: postgres:16

  environment: { POSTGRES_DB: study, POSTGRES_PASSWORD: postgres }

  volumes: \[study-data:/var/lib/postgresql/data\]

  volumes:

  session-data:

  study-data:
  -----------------------------------------------------------------------

**5. Repository Structure**

  -----------------------------------------------------------------------
  focusflow-ai-microservices/

  ├── api-gateway/

  │ ├── src/main/resources/application.yml

  │ └── pom.xml

  ├── session-service/

  │ ├── src/main/java/com/focusflow/session/

  │ │ ├── SessionController.java

  │ │ ├── SessionService.java

  │ │ ├── Session.java (JPA entity)

  │ │ └── SessionRepository.java (Spring Data JPA)

  │ └── pom.xml

  ├── coach-service/

  │ ├── src/main/java/com/focusflow/coach/

  │ │ ├── AiCoachController.java

  │ │ ├── AiCoachService.java

  │ │ └── ClaudeApiClient.java

  │ └── pom.xml

  ├── study-service/

  │ ├── src/main/java/com/focusflow/study/

  │ │ ├── StudyController.java

  │ │ ├── StudyCoachService.java

  │ │ ├── Deck.java

  │ │ ├── Flashcard.java

  │ │ └── DeckRepository.java

  │ └── pom.xml

  ├── javafx-client/

  │ ├── src/main/java/com/focusflowai/

  │ │ ├── App.java

  │ │ ├── service/ApiClient.java

  │ │ ├── controller/\*Controller.java

  │ │ └── model/\*.java

  │ └── pom.xml

  └── docker-compose.yml
  -----------------------------------------------------------------------
