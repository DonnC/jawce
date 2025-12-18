# jawce - WhatsApp Live Support Dashboard

A real-time live support agent dashboard for handling WhatsApp customer conversations.

![img](/screenshots/)

## Quick Start (Demo Mode)

```bash
npm install
npm run dev
```


## Architecture

```
┌─────────────────┐     WebSocket     ┌──────────────────┐     WhatsApp API    ┌─────────────┐
│  React Frontend │◄──────────────────►│  Spring Boot     │◄───────────────────►│  WhatsApp   │
│  (Agent UI)     │                    │  Backend         │                     │  Business   │
└─────────────────┘                    └──────────────────┘                     └─────────────┘
                                              │
                                              ▼
                                       ┌──────────────┐
                                       │  H2 Database │
                                       └──────────────┘
```

---

## Tech Stack

- **Frontend**: React, TypeScript, Tailwind CSS, shadcn/ui
- **Backend**: Spring Boot 3, WebSocket, Spring Data JPA
- **Database**: H2 (in-memory for demo)

## Connecting to Spring Boot Backend

### Step 1: Enable WebSocket Mode

Edit `src/hooks/useChatStore.ts`:

```typescript
const USE_WEBSOCKET = true;  // Change from false to true
const WS_URL = 'ws://localhost:8080/ws/chat';  // Your Spring Boot WebSocket URL
```

---
