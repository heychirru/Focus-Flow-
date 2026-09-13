**FocusFlow**

Product Requirements Document

Deep Work Session Tracker • Java Desktop App

  ------------------ ----------------------------------------------------
  **Version**        1.0 --- Initial Draft

  **Status**         In Development

  **Author**         Product Team

  **Date**           February 27, 2026

  **Stack**          Java 21, JavaFX, SQLite, Maven

  **Platform**       Desktop (Windows / macOS / Linux)
  ------------------ ----------------------------------------------------

**1. Executive Summary**

FocusFlow is a lightweight Java desktop application designed to help
knowledge workers track, manage, and analyze their deep work sessions.
Built on top of a Pomodoro-inspired timer model, FocusFlow captures
session data locally via SQLite and provides actionable insights into
personal productivity habits --- streaks, daily focus totals, and
tag-based breakdowns.

The core problem: professionals have no reliable, frictionless way to
measure how much genuinely focused work they complete each day.
FocusFlow solves this with a minimal, distraction-free interface that
logs every session without requiring cloud accounts or subscriptions.

**2. Problem Statement**

**2.1 The Challenge**

Modern knowledge workers frequently overestimate how much focused work
they accomplish. Interruptions, passive distractions, and the absence of
any personal productivity tracking system make it nearly impossible to
identify patterns, protect deep work time, or build sustainable focus
habits.

**2.2 Pain Points**

-   No visibility into actual vs. perceived deep work hours

-   Lack of accountability tools that don\'t require cloud subscriptions

-   Existing productivity apps are either too complex or too generic

-   No easy way to see which projects or tasks consume the most focus
    time

-   Absence of streak-based motivation for daily consistency

**3. Goals & Non-Goals**

**3.1 Goals**

-   Provide a frictionless timer interface for starting and logging
    focus sessions

-   Persist all session data locally (no cloud, no account required)

-   Surface meaningful stats: daily minutes, streaks, and category
    breakdowns

-   Support customizable session tags so users can track work by project
    type

-   Enable CSV export of historical session data

-   Run as a polished, installable Java desktop app on all major
    platforms

**3.2 Non-Goals**

-   No cloud sync or multi-device support in v1

-   No social or team features --- this is a personal tool

-   No AI-powered recommendations or external integrations in v1

-   Not a task manager --- FocusFlow tracks time, not to-dos

**4. Target Users**

**Primary Persona: The Deep Worker**

A software developer, writer, or knowledge worker aged 22--45 who values
focused work blocks, is aware of the deep work concept, and wants an
honest log of how their workday is spent. They prefer lightweight
desktop tools over bloated web apps and are comfortable running a Java
application.

**Secondary Persona: The Habit Builder**

Someone in the process of building a daily focus habit --- using streaks
and daily goal visualizations as motivation to show up consistently.
They may be a student, freelancer, or anyone working toward a long-term
personal project.

**5. Feature Requirements**

Priority definitions: P0 = must ship in v1, P1 = strongly desired in v1,
P2 = v2 candidate.

  -----------------------------------------------------------------------------
  **Feature**      **Description**                   **Priority**   **Phase**
  ---------------- --------------------------------- -------------- -----------
  **Countdown      Configurable timer (1--120 min)   **P0**         v1.0
  Timer**          with start, pause, resume, and                   
                   stop controls                                    

  **Session        Auto-save each session to local   **P0**         v1.0
  Logging**        SQLite DB with tag, duration,                    
                   start/end time                                   

  **Session Tags** Predefined and user-created       **P0**         v1.0
                   categories (e.g. Coding, Writing,                
                   Reading)                                         

  **Today\'s       Live display of total focus       **P0**         v1.0
  Stats**          minutes completed today                          

  **Streak         Consecutive days with at least    **P0**         v1.0
  Counter**        one completed session                            

  **Session        Paginated table of all past       **P0**         v1.0
  History**        sessions with tag, duration, and                 
                   status                                           

  **Stats          Aggregate view: total sessions,   **P0**         v1.0
  Dashboard**      total hours, top categories                      

  **Completion     In-app dialog on session          **P1**         v1.0
  Alert**          completion with break prompt                     

  **Custom Tags**  UI for creating, renaming, and    **P1**         v1.1
                   color-coding personal tags                       

  **CSV Export**   One-click export of session       **P1**         v1.1
                   history to a .csv file                           

  **Daily Goal**   Set a target focus minutes/day;   **P1**         v1.1
                   progress bar on timer screen                     

  **Bar Chart      Weekly/monthly focus hours bar    **P2**         v2.0
  View**           chart using JavaFX Charts                        

  **System Tray**  Minimize to system tray; show     **P2**         v2.0
                   timer status in tray icon                        

  **Heatmap        GitHub-style contribution heatmap **P2**         v2.0
  Calendar**       of focus activity                                
  -----------------------------------------------------------------------------

**6. Technical Architecture**

**6.1 Technology Stack**

  ------------------ ----------------------------------------------------
  **Language**       Java 21 (LTS)

  **UI Framework**   JavaFX 21.0.2 with FXML + CSS

  **Database**       SQLite via sqlite-jdbc 3.45.1

  **Build Tool**     Apache Maven

  **Architecture**   MVC --- Models, Services, Controllers, FXML Views

  **Data Storage**   Local file at \~/.focusflow/focusflow.db
  ------------------ ----------------------------------------------------

**6.2 Package Structure**

com.focusflow.model --- Session, Tag domain objects

com.focusflow.db --- DatabaseManager (singleton), SessionRepository
(CRUD + queries)

com.focusflow.service --- TimerService (JavaFX Timeline), SessionService
(business logic)

com.focusflow.controller --- MainController, TimerController,
StatsController, HistoryController

com.focusflow.util --- Future: CSV export, date helpers

**6.3 Data Model**

  -------------------------------------------------------------------------------
  **Column**             **Type**        **Description**
  ---------------------- --------------- ----------------------------------------
  **id**                 INTEGER PK      Auto-incremented session identifier

  **tag**                TEXT            Category label (e.g. \'Coding\',
                                         \'Writing\')

  **duration_minutes**   INTEGER         Planned duration in minutes

  **start_time**         TEXT            ISO-8601 datetime string of session
                                         start

  **end_time**           TEXT            ISO-8601 datetime string of session end
                                         (nullable)

  **completed**          INTEGER         1 if session ran to completion, 0 if
                                         cancelled
  -------------------------------------------------------------------------------

**7. UX & Design Principles**

-   Dark-first theme (navy/charcoal base, red accent #E94560) ---
    minimizes eye strain during long sessions

-   Three-view navigation: Timer, Stats, History --- accessible from
    persistent sidebar

-   Zero onboarding --- app is usable immediately on first launch with
    default tags pre-seeded

-   No modals or wizards for the core timer flow --- start a session in
    2 clicks

-   Typography: Inter/Segoe UI for UI labels; Courier New monospace for
    the timer display

-   Session completion triggers a congratulatory alert --- positive
    reinforcement without noise

**8. Milestones & Roadmap**

  -------------------------------------------------------------------------
  **Milestone**   **Target**       **Scope**
  --------------- ---------------- ----------------------------------------
  **v1.0 Alpha**  Week 1--2        Project scaffold, timer UI, SQLite
                                   integration, session logging, history
                                   view

  **v1.0 Beta**   Week 3           Stats dashboard, streak tracker,
                                   today\'s minutes, polished CSS theme

  **v1.0          Week 4           Bug fixes, edge case handling, packaging
  Release**                        for distribution

  **v1.1**        Month 2          Custom tag editor, CSV export, daily
                                   goal + progress bar

  **v2.0**        Month 3--4       Bar chart analytics, system tray,
                                   heatmap calendar view
  -------------------------------------------------------------------------

**9. Success Metrics**

-   User completes at least one session on first launch (onboarding
    success rate)

-   Average session completion rate \> 70% (started vs. fully completed)

-   User returns to the app on at least 5 of 7 days in the first week
    (retention)

-   Zero data loss across app restarts (SQLite persistence reliability)

-   App launch to first session start in under 10 seconds on standard
    hardware

**10. Open Questions**

-   Should break timers (e.g. 5-min short break) be included in v1.0 or
    deferred?

-   What is the preferred packaging format: installer (.exe/.dmg) or
    runnable JAR?

-   Should completed vs. cancelled sessions be weighted differently in
    streak calculation?

-   Is there value in optional notification sounds on session start/end?

-   Should daily goal be configurable in minutes or number of sessions?

FocusFlow PRD v1.0 • Confidential • February 2026
