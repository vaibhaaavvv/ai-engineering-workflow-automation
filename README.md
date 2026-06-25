# Relay — AI-Powered Engineering Ticket Automation

> **Relay watches your team's Slack conversations and automatically turns bugs, features, and tasks into tracked tickets — without anyone lifting a finger.**

---

## Demo

[![Relay Demo](https://img.youtube.com/vi/9B9p3FcI9L0/maxresdefault.jpg)](https://youtu.be/9B9p3FcI9L0?si=P6WS7UyelBs40bf2)

▶ [Watch the 2-minute demo on YouTube](https://youtu.be/9B9p3FcI9L0?si=P6WS7UyelBs40bf2)

---

## What is Relay?

Engineering teams lose bugs, features, and tasks in Slack every single day. Someone mentions a critical issue, someone else says "I'll handle it" — and it disappears into the thread forever.

Relay fixes this. It connects to your Slack workspace, reads every message in real time, uses AI to detect actionable items (bugs, feature requests, tasks), and proposes tickets directly in the conversation thread. One click to create. Everything lands on a Kanban board automatically.

You can also control it directly from Slack using natural language commands — no syntax to memorize, no tool switching.

---

## Features

- **Automatic triage** — AI reads every Slack message and detects bugs, features, and tasks without any @mentions needed
- **In-thread proposals** — ticket proposals appear as thread replies with title, type, priority, and assignee pre-filled
- **Duplicate detection** — Relay checks your open board before proposing anything new
- **@Relay commands** — create, update, and delete tickets from Slack using plain English
- **Kanban board** — drag-and-drop status management across 5 stages
- **Ticket detail modal** — full ticket view with description, git branch name, assignee, and timestamps
- **Git branch names** — every ticket ships with an auto-generated branch name ready to copy
- **Test simulation** — `@Relay test-run` replays a 28-message engineering conversation to demo the full AI triage pipeline

---

## Slack Commands

```
@Relay fix the login bug, assign to Vaibhav          → creates a ticket
@Relay move T-042 to in progress                     → updates ticket status
@Relay mark T-009 as done                            → natural language understood
@Relay update T-042 assign to Raj                   → updates assignee
@Relay delete T-007                                  → deletes a ticket
@Relay test-run                                      → runs a live demo simulation
```

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Slack Workspace                       │
│   Team chats → Relay bot listens → @Relay commands          │
└────────────────────────┬────────────────────────────────────┘
                         │ Events API / Slash Commands
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    Backend (Spring Boot)                     │
│                                                             │
│  SlackController → verifies signature → routes events       │
│       │                                                     │
│       ├── app_mention → SlackService.processCommand()       │
│       │        ├── create ticket (AI extracts fields)       │
│       │        ├── update ticket (AI maps natural language) │
│       │        ├── delete ticket                            │
│       │        └── test-run (async, 1s delay per message)   │
│       │                                                     │
│       └── message → SlackService.processMessage()          │
│                └── AiService.analyzeMessage()               │
│                     ├── PROPOSE → post ticket proposal      │
│                     ├── DUPLICATE → ignore                  │
│                     └── NO_ACTION → ignore                  │
│                                                             │
│  TicketService → CRUD + branch name generation              │
│  JwtAuthFilter → stateless JWT authentication               │
│  SecurityConfig → CORS + route permissions                  │
└────────────┬───────────────────────┬────────────────────────┘
             │                       │
             ▼                       ▼
┌─────────────────┐       ┌──────────────────────┐
│  PostgreSQL DB  │       │   OpenAI GPT-4o-mini  │
│  (Neon)         │       │   AI triage + extract │
└─────────────────┘       └──────────────────────┘
             
┌─────────────────────────────────────────────────────────────┐
│                    Frontend (React + Vite)                   │
│                                                             │
│  Landing → Login/Signup → Connect Slack → Kanban Board      │
│                                                             │
│  Pages: Landing, Login, Signup, Connect, Dashboard          │
│  Components: AppNavbar, Logo, TicketModal, KanbanColumn     │
│  API: axios calls to backend with JWT auth header           │
└─────────────────────────────────────────────────────────────┘
```

---

## Tech Stack

### Backend
| Technology | Purpose |
|---|---|
| Java 17 + Spring Boot 3.2 | Core application framework |
| Spring Security | JWT-based stateless authentication |
| Spring Data JPA / Hibernate | ORM, schema auto-migration |
| PostgreSQL (Neon) | Production database |
| Slack Events API | Receives real-time channel messages |
| Slack Web API | Posts messages, thread replies |
| OpenAI GPT-4o-mini | AI triage, ticket extraction, update parsing |
| Maven | Build and dependency management |

### Frontend
| Technology | Purpose |
|---|---|
| React 19 | UI framework |
| Vite | Build tool and dev server |
| Tailwind CSS | Utility-first styling |
| React Router 7 | Client-side routing |
| Axios | HTTP client |
| Lucide React | Icon library |

### Infrastructure
| Service | Purpose |
|---|---|
| Render | Backend hosting |
| Vercel | Frontend hosting |
| Neon | Serverless PostgreSQL |
| UptimeRobot | Keep-alive pinger (prevents cold starts) |

---

## Project Structure

```
relay-ai-full-stack/
├── backend/
│   ├── src/main/java/com/knowledge/assistant/
│   │   ├── controller/          # REST endpoints (Auth, Tickets, Slack, Integrations)
│   │   ├── service/             # Business logic (SlackService, AiService, TicketService)
│   │   ├── model/               # JPA entities (User, Ticket, Integration)
│   │   ├── dto/                 # Request/response objects
│   │   ├── repository/          # Spring Data JPA repositories
│   │   ├── security/            # JWT filter and utilities
│   │   └── config/              # Security and CORS configuration
│   └── src/main/resources/
│       └── application.properties
│
└── frontend/
    ├── src/
    │   ├── pages/               # Landing, Login, Signup, Connect, Dashboard
    │   ├── components/          # AppNavbar, Logo
    │   ├── api/                 # auth.js, tickets.js, integrations.js
    │   └── index.css            # Global styles and animations
    ├── public/
    │   └── favicon.svg
    └── vercel.json              # SPA routing for Vercel
```

---

## Environment Variables

### Backend (Render)
```
DATABASE_URL          PostgreSQL connection string (Neon)
DATABASE_USERNAME     Database username
DATABASE_PASSWORD     Database password
JWT_SECRET            Secret key for JWT signing
OPENAI_API_KEY        OpenAI API key
SLACK_CLIENT_ID       Slack app client ID
SLACK_CLIENT_SECRET   Slack app client secret
SLACK_SIGNING_SECRET  Slack request signing secret
SLACK_REDIRECT_URI    OAuth callback URL
FRONTEND_URL          Deployed frontend URL (for OAuth redirects)
```

### Frontend (Vercel)
```
VITE_API_URL          Backend API base URL
```

---

## Slack App Setup

1. Create a new app at [api.slack.com/apps](https://api.slack.com/apps)
2. Enable **Event Subscriptions** → set request URL to `https://your-backend/slack/events`
3. Subscribe to bot events: `message.channels`, `app_mention`
4. Enable **OAuth & Permissions** → add scopes:
   - `chat:write`, `channels:history`, `app_mentions:read`, `channels:read`
5. Add redirect URL: `https://your-backend/integrations/slack/callback`
6. Install to workspace and copy credentials to environment variables

---

## Local Development

### Backend
```bash
cd backend
# Set environment variables or create application-local.properties
./mvnw spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm run dev
```

---

## How the AI Triage Works

Every Slack message passes through `AiService.analyzeMessage()` which calls GPT-4o-mini with a structured prompt. The AI returns one of three actions:

- **PROPOSE** — a new actionable item was detected. Returns `type` (BUG/FEATURE/TASK/CHORE), `priority` (LOW/MEDIUM/HIGH/CRITICAL), `title`, `description`, and `assignee` extracted from context
- **DUPLICATE** — the issue already exists on the open board
- **NO_ACTION** — casual conversation, no ticket needed

For `@Relay` commands, a separate prompt (`extractUpdateCommand`) maps natural language like *"mark it as done"* or *"move to in progress"* to structured status/field updates.

---

## Built By

**Vaibhav Sharma** — built solo as a full-stack AI product.

[![YouTube](https://img.shields.io/badge/Demo-YouTube-red?style=flat&logo=youtube)](https://youtu.be/9B9p3FcI9L0?si=P6WS7UyelBs40bf2)
