**FocusFlow AI**

**API Reference**

REST Endpoints for All Microservices

FocusFlow AI • v2.0 • Base URL: http://localhost:8080

  ------------------- ---------------------------------------------------
  **Base URL**        http://localhost:8080 (API Gateway)

  **Protocol**        HTTP/1.1 --- JSON request & response bodies

  **Auth**            None in v2.0 (API key planned for v3.0)

  **Content-Type**    application/json

  **Date**            February 2026
  ------------------- ---------------------------------------------------

All requests flow through the API Gateway at :8080, which routes to the
appropriate downstream service. Clients should never call service ports
directly in production.

**Session Service --- /api/sessions**

Manages focus sessions, tags, and derived productivity statistics.
Routes to session-service:8081.

**POST /api/sessions**

Create and start a new focus session. Returns the created session with
its generated ID.

**Request Body:**

  -----------------------------------------------------------------------
  {

  \"tag\": \"Coding\",

  \"durationMinutes\": 25

  }
  -----------------------------------------------------------------------

**Response:**

  -----------------------------------------------------------------------
  {

  \"id\": 42,

  \"tag\": \"Coding\",

  \"durationMinutes\": 25,

  \"startTime\": \"2026-02-27T09:15:00\",

  \"endTime\": null,

  \"completed\": false

  }
  -----------------------------------------------------------------------

*Note: Call this when the user clicks Start. Store the returned id to
complete or cancel later.*

**PUT /api/sessions/{id}/complete**

Mark a session as successfully completed. Sets endTime to now and
completed to true.

**Parameters:**

  ---------------------------------------------------------------------------------
  **Name**     **In**   **Type**   **Required**   **Description**
  ------------ -------- ---------- -------------- ---------------------------------
  id           path     Long       Yes            Session ID returned from POST
                                                  /api/sessions

  ---------------------------------------------------------------------------------

**Response:**

  -----------------------------------------------------------------------
  {

  \"id\": 42,

  \"tag\": \"Coding\",

  \"durationMinutes\": 25,

  \"startTime\": \"2026-02-27T09:15:00\",

  \"endTime\": \"2026-02-27T09:40:00\",

  \"completed\": true

  }
  -----------------------------------------------------------------------

**PUT /api/sessions/{id}/cancel**

Cancel an in-progress session. Sets endTime to now and completed remains
false.

**Parameters:**

  ---------------------------------------------------------------------------------
  **Name**     **In**   **Type**   **Required**   **Description**
  ------------ -------- ---------- -------------- ---------------------------------
  id           path     Long       Yes            Session ID to cancel

  ---------------------------------------------------------------------------------

**Response:**

  -----------------------------------------------------------------------
  { \"id\": 42, \"completed\": false, \"endTime\":
  \"2026-02-27T09:22:00\" }

  -----------------------------------------------------------------------

**GET /api/sessions**

Retrieve all sessions in reverse chronological order.

**Parameters:**

  ---------------------------------------------------------------------------------
  **Name**     **In**   **Type**   **Required**   **Description**
  ------------ -------- ---------- -------------- ---------------------------------
  limit        query    int        No             Max results to return (default:
                                                  100)

  offset       query    int        No             Pagination offset (default: 0)

  tag          query    string     No             Filter by tag name
  ---------------------------------------------------------------------------------

**Response:**

  -----------------------------------------------------------------------
  \[

  {

  \"id\": 42, \"tag\": \"Coding\", \"durationMinutes\": 25,

  \"startTime\": \"2026-02-27T09:15:00\",

  \"endTime\": \"2026-02-27T09:40:00\", \"completed\": true

  },

  { \... }

  \]
  -----------------------------------------------------------------------

**GET /api/sessions/stats**

Returns aggregated productivity statistics for the dashboard.

**Response:**

  -----------------------------------------------------------------------
  {

  \"totalSessions\": 128,

  \"totalMinutes\": 3200,

  \"todayMinutes\": 75,

  \"completionRate\": 0.84,

  \"currentStreak\": 5,

  \"topTags\": \[

  { \"tag\": \"Coding\", \"sessions\": 62, \"totalMinutes\": 1550 },

  { \"tag\": \"Writing\", \"sessions\": 38, \"totalMinutes\": 950 },

  { \"tag\": \"Reading\", \"sessions\": 28, \"totalMinutes\": 700 }

  \]

  }
  -----------------------------------------------------------------------

**GET /api/sessions/summary**

Returns a plain-text summary of session stats, intended for the AI Coach
Service prompt.

**Response:**

  -----------------------------------------------------------------------
  Total completed sessions: 128

  Total focus minutes logged: 3200

  Today\'s focus minutes: 75

  Session breakdown by category:

  \- Coding: 62 sessions, 1550 min total

  \- Writing: 38 sessions, 950 min total

  Last 7 days activity:

  \- 2026-02-27: 3 sessions, 75 min

  \- 2026-02-26: 4 sessions, 100 min

  Session completion rate: 84%
  -----------------------------------------------------------------------

*Note: This endpoint is called internally by the AI Coach Service, not
typically by the JavaFX client directly.*

**GET /api/sessions/tags**

List all available session tags.

**Response:**

  ----------------------------------------------------------------------------------------------------------------
  \[{\"id\":1,\"name\":\"General\",\"color\":\"#4A90D9\"},{\"id\":2,\"name\":\"Coding\",\"color\":\"#FF6B6B\"}\]

  ----------------------------------------------------------------------------------------------------------------

**POST /api/sessions/tags**

Create a new session tag.

**Request Body:**

  -----------------------------------------------------------------------
  { \"name\": \"Research\", \"color\": \"#9B59B6\" }

  -----------------------------------------------------------------------

**Response:**

  -----------------------------------------------------------------------
  { \"id\": 6, \"name\": \"Research\", \"color\": \"#9B59B6\" }

  -----------------------------------------------------------------------

**AI Coach Service --- /api/coach**

Generates personalised productivity insights by calling Claude with the
user\'s session history. Routes to coach-service:8082. Requires
ANTHROPIC_API_KEY to be set in the service environment.

**POST /api/coach/insights**

Request a fresh AI coaching report. Internally fetches the session
summary from Session Service, then calls Claude.

**Response:**

  -----------------------------------------------------------------------
  {

  \"insights\": \"Great consistency this week! You\'ve completed 128
  sessions\...\",

  \"generatedAt\": \"2026-02-27T09:45:00\",

  \"sessionCount\": 128

  }
  -----------------------------------------------------------------------

*Note: This call may take 5-15 seconds as it waits for Claude. Always
call asynchronously from the UI.*

**GET /api/coach/insights/latest**

Return the most recently generated coaching report without triggering a
new Claude call.

**Response:**

  -----------------------------------------------------------------------
  {

  \"insights\": \"\...\",

  \"generatedAt\": \"2026-02-27T09:45:00\",

  \"sessionCount\": 128

  }
  -----------------------------------------------------------------------

*Note: Returns 404 if no insights have been generated yet for this
session.*

**Study Coach Service --- /api/study**

Manages flashcard decks. Generates cards via Claude on demand, persists
them, and tracks per-card known/unknown state. Routes to
study-service:8083.

**GET /api/study/decks**

List all saved flashcard decks in reverse chronological order.

**Response:**

  -----------------------------------------------------------------------
  \[

  { \"id\": 1, \"title\": \"Chapter 3 Photosynthesis\", \"cardCount\": 8,
  \"knownCount\": 5, \"createdAt\": \"2026-02-26T14:00:00\" },

  { \"id\": 2, \"title\": \"Java Concurrency Notes\", \"cardCount\": 10,
  \"knownCount\": 3, \"createdAt\": \"2026-02-25T10:30:00\" }

  \]
  -----------------------------------------------------------------------

**POST /api/study/decks/generate**

Generate flashcards from source text using Claude. Creates and saves the
deck automatically.

**Request Body:**

  -----------------------------------------------------------------------
  {

  \"title\": \"Chapter 3 --- Photosynthesis\",

  \"sourceText\": \"Photosynthesis is the process by which plants\...\"

  }
  -----------------------------------------------------------------------

**Response:**

  -----------------------------------------------------------------------
  {

  \"id\": 3,

  \"title\": \"Chapter 3 --- Photosynthesis\",

  \"createdAt\": \"2026-02-27T10:00:00\",

  \"cards\": \[

  { \"id\": 21, \"question\": \"What is photosynthesis?\", \"answer\":
  \"The process by which plants convert sunlight into glucose.\",
  \"known\": false },

  { \"id\": 22, \"question\": \"What are the inputs of photosynthesis?\",
  \"answer\": \"CO2, water, and sunlight.\", \"known\": false }

  \]

  }
  -----------------------------------------------------------------------

*Note: sourceText should be at least 100 characters for meaningful
cards. May take 10-20 seconds.*

**GET /api/study/decks/{id}/cards**

Get all flashcards for a given deck.

**Parameters:**

  ---------------------------------------------------------------------------------
  **Name**     **In**   **Type**   **Required**   **Description**
  ------------ -------- ---------- -------------- ---------------------------------
  id           path     Long       Yes            Deck ID

  ---------------------------------------------------------------------------------

**Response:**

  -----------------------------------------------------------------------
  \[

  { \"id\": 21, \"deckId\": 3, \"question\": \"What is photosynthesis?\",
  \"answer\": \"\...\", \"known\": false },

  { \"id\": 22, \"deckId\": 3, \"question\": \"What are the inputs?\",
  \"answer\": \"\...\", \"known\": true }

  \]
  -----------------------------------------------------------------------

**PATCH /api/study/cards/{id}/known**

Update the known status of a single flashcard.

**Parameters:**

  ---------------------------------------------------------------------------------
  **Name**     **In**   **Type**   **Required**   **Description**
  ------------ -------- ---------- -------------- ---------------------------------
  id           path     Long       Yes            Flashcard ID

  ---------------------------------------------------------------------------------

**Request Body:**

  -----------------------------------------------------------------------
  { \"known\": true }

  -----------------------------------------------------------------------

**Response:**

  -----------------------------------------------------------------------
  { \"id\": 21, \"known\": true }

  -----------------------------------------------------------------------

**DELETE /api/study/decks/{id}**

Delete a deck and all its flashcards (cascading delete).

**Parameters:**

  ---------------------------------------------------------------------------------
  **Name**     **In**   **Type**   **Required**   **Description**
  ------------ -------- ---------- -------------- ---------------------------------
  id           path     Long       Yes            Deck ID to delete

  ---------------------------------------------------------------------------------

**Response:**

  -----------------------------------------------------------------------
  HTTP 204 No Content

  -----------------------------------------------------------------------

**Error Responses**

All services return standard error envelopes on failure:

  -----------------------------------------------------------------------
  {

  \"status\": 404,

  \"error\": \"Not Found\",

  \"message\": \"Session with id 99 not found\",

  \"timestamp\": \"2026-02-27T10:05:00\"

  }
  -----------------------------------------------------------------------

  -----------------------------------------------------------------------
  **HTTP        **Meaning**      **Common Cause**
  Status**                       
  ------------- ---------------- ----------------------------------------
  200 OK        Success          Standard successful response

  201 Created   Resource created POST requests that create new entities

  204 No        Success, no body DELETE operations
  Content                        

  400 Bad       Invalid input    Missing required fields, invalid JSON
  Request                        

  404 Not Found Resource missing Unknown session ID, deck ID, etc.

  500 Internal  Server failure   Database down, AI API error, timeout
  Server Error                   

  503 Service   Service          Downstream service not running
  Unavailable   unreachable      
  -----------------------------------------------------------------------
