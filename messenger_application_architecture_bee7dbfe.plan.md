---
name: Messenger Application Architecture
overview: Design and implement a production-ready messaging application with Phase 1 MVP focusing on authentication, one-to-one and group chats (max 50 members), real-time WebSocket delivery, and comprehensive observability. The architecture will support future Phase 2 Signal-like E2EE protocol.
todos: []
---

# Messenger Application - Architecture & Implementation Plan

## Overview

This plan outlines the architecture and implementation strategy for a production-ready messaging application. The system will support one-to-one and group conversations (max 50 members) with real-time message delivery via WebSockets, JWT-based authentication, and comprehensive observability.

## Architecture Overview

### High-Level Architecture

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Client    │────▶│   REST API  │────▶│   Service   │
│  (Browser)  │     │   (v1)      │     │   Layer     │
└──────┬──────┘     └─────────────┘     └──────┬──────┘
       │                                        │
       │ WebSocket                              │
       │ (Events)                               │
       │                                        ▼
       │                              ┌─────────────────┐
       └─────────────────────────────▶│  PostgreSQL DB  │
                                      └─────────────────┘
```

### Service Boundaries (Future Microservices)

- **Auth Service**: Authentication, JWT token management, refresh tokens
- **Chat Service**: Conversations, messages, group management
- **Fanout Service**: Real-time message distribution via WebSocket
- **Notification Service**: Push notifications (future)

Internal communication: gRPC (optional, for future microservices split)

## Phase 1 MVP Requirements

### Core Features

1. **Authentication & Authorization**

   - Username/email + password (or OTP) registration/login
   - JWT access tokens (15-minute expiry)
   - Rotating refresh tokens
   - WebSocket authentication using access tokens
   - Basic user profile (username, display name, avatar)

2. **Messaging**

   - One-to-one conversations
   - Group chats (max 50 members)
   - Plain text messages only
   - Message history with cursor pagination
   - User-controlled message deletion

3. **Real-Time Delivery**

   - WebSocket connections for event streaming
   - Event IDs for resume capability
   - REST API for resource operations

4. **Data Management**

   - PostgreSQL database
   - Cursor-based pagination for message history
   - Event IDs for real-time resume

### Delivery Guarantees

- **Messages**: At-least-once + idempotency (effectively once)
- **Typing/Presence**: At-most-once (best effort)
- **Receipts**: At-least-once + idempotency
- **Group Membership Changes**: At-least-once + idempotency

### Non-Functional Requirements

- **Scale**: Medium (1,000 - 100,000 users)
- **Performance**: < 100ms for critical operations
- **Testing**: Full coverage (unit, integration, e2e)
- **Observability**: Full (structured logging, metrics, distributed tracing)
- **Error Handling**: Detailed error responses (dev mode)
- **Input Validation**: Comprehensive (length limits, format validation, sanitization)

## Implementation Plan

### Phase 1.1: Foundation & Database Migration

**Files to modify:**

- `build.gradle` - Add PostgreSQL driver, remove H2 for production
- `src/main/resources/application.yml` - PostgreSQL configuration
- Create database migration scripts (Flyway or Liquibase)

**Tasks:**

1. [x] Add PostgreSQL dependency to `build.gradle`
2. [x] Configure PostgreSQL connection in `application.yml`
3. [x] Set up database migration framework
4. [x] Create initial schema migrations for users, conversations, messages
5. [ ] Add connection pooling configuration

### Phase 1.2: Authentication System

**New files to create:**

- `src/main/java/com/example/messenger/domain/RefreshToken.java`
- `src/main/java/com/example/messenger/dto/AuthRequest.java`
- `src/main/java/com/example/messenger/dto/AuthResponse.java`
- `src/main/java/com/example/messenger/dto/TokenRefreshRequest.java`
- `src/main/java/com/example/messenger/service/AuthService.java`
- `src/main/java/com/example/messenger/service/TokenService.java`
- `src/main/java/com/example/messenger/api/AuthController.java`
- `src/main/java/com/example/messenger/config/SecurityConfig.java`
- `src/main/java/com/example/messenger/config/JwtConfig.java`
- `src/main/java/com/example/messenger/security/JwtTokenProvider.java`
- `src/main/java/com/example/messenger/security/JwtAuthenticationFilter.java`

**Files to modify:**

- `AppUser.java` - Add email, password hash, display name, avatar fields
- `AppUserRepository.java` - Add findByEmail method

**Tasks:**

1. [x] Extend `AppUser` entity with authentication fields
2. [x] Implement password hashing (BCrypt)
3. [x] Create JWT token provider (access + refresh tokens)
4. [x] Implement authentication service (register, login, refresh)
5. [x] Create Spring Security configuration
6. [x] Add JWT authentication filter
7. [x] Create authentication REST endpoints
8. [x] Add comprehensive input validation

### Phase 1.3: Enhanced Domain Model

**Files to modify:**

- `Conversation.java` - Support group conversations (change from userA/userB to participants)
- `Message.java` - Add idempotency key, sender info in MessageView
- `AppUser.java` - Already updated in Phase 1.2

**New files:**

- `src/main/java/com/example/messenger/domain/ConversationParticipant.java` - Many-to-many relationship
- `src/main/java/com/example/messenger/domain/MessageIdempotencyKey.java` - For deduplication

**Tasks:**

1. [x] Refactor `Conversation` to support multiple participants
2. [x] Create `ConversationParticipant` join entity
3. [x] Add group conversation support (max 50 members validation)
4. [x] Add idempotency key to messages
5. [x] Update repositories for new relationships

### Phase 1.4: WebSocket Real-Time Delivery

**New files:**

- `src/main/java/com/example/messenger/websocket/WebSocketConfig.java`
- `src/main/java/com/example/messenger/websocket/WebSocketAuthInterceptor.java`
- `src/main/java/com/example/messenger/websocket/MessageWebSocketHandler.java`
- `src/main/java/com/example/messenger/dto/WebSocketMessage.java`
- `src/main/java/com/example/messenger/service/EventService.java`
- `src/main/java/com/example/messenger/domain/Event.java` - For event IDs and resume

**Files to modify:**

- `MessageService.java` - Publish events after message creation
- `MessagingController.java` - Add cursor pagination support

**Tasks:**

1. [x] Configure WebSocket endpoint with authentication
2. [x] Implement JWT authentication for WebSocket connections
3. [x] Create event service for message distribution
4. [x] Add event ID tracking for resume capability
5. [x] Implement WebSocket message handler
6. [x] Update message service to publish events
7. [x] Add cursor pagination to message listing endpoint

### Phase 1.5: API Improvements

**Files to modify:**

- `MessagingController.java` - Add API versioning, improve request/response DTOs
- `MessageService.java` - Add idempotency checks, improve error handling

**New files:**

- `src/main/java/com/example/messenger/dto/MessageRequest.java`
- `src/main/java/com/example/messenger/dto/MessageResponse.java`
- `src/main/java/com/example/messenger/dto/CreateConversationRequest.java`
- `src/main/java/com/example/messenger/dto/PaginatedResponse.java`
- `src/main/java/com/example/messenger/exception/GlobalExceptionHandler.java`
- `src/main/java/com/example/messenger/exception/CustomException.java`

**Tasks:**

1. [x] Add `/api/v1` versioning to all endpoints
2. [x] Create proper DTOs for requests/responses
3. [x] Implement global exception handler
4. [x] Add comprehensive input validation
5. [x] Implement cursor pagination
6. [x] Add idempotency handling for message creation

### Phase 1.6: Observability

**New files:**

- `src/main/java/com/example/messenger/config/LoggingConfig.java`
- `src/main/java/com/example/messenger/config/MetricsConfig.java`
- `src/main/java/com/example/messenger/config/TracingConfig.java`

**Files to modify:**

- `build.gradle` - Add Micrometer, OpenTelemetry dependencies
- `application.yml` - Add logging, metrics, tracing configuration

**Tasks:**

1. [x] Configure structured logging (JSON format)
2. [x] Add Micrometer metrics (message counts, latency, WebSocket connections)
3. [x] Set up distributed tracing (OpenTelemetry)
4. [x] Add correlation IDs for request tracking
5. [x] Create health check endpoints with detailed status

### Phase 1.7: Testing

**New files:**

- Test files for all new services and controllers
- Integration tests for authentication flow
- Integration tests for WebSocket connections
- E2E tests for message delivery

**Tasks:**

1. [x] Unit tests for all services (AuthService, MessageService, TokenService)
2. [x] Integration tests for REST endpoints
3. [x] WebSocket integration tests
4. [x] Database integration tests
5. [ ] E2E tests for complete message flow
6. [x] Test idempotency guarantees
7. [x] Test delivery guarantees

### Phase 1.8: Docker & Deployment

**New files:**

- `Dockerfile`
- `docker-compose.yml` - PostgreSQL + application
- `.dockerignore`

**Files to modify:**

- `application.yml` - Environment-based configuration

**Tasks:**

1. [ ] Create Dockerfile for application
2. [ ] Create docker-compose.yml with PostgreSQL
3. [ ] Add environment variable configuration
4. [ ] Create startup scripts
5. [ ] Document deployment process

## Data Model Changes

### AppUser

- Add: `email` (unique), `passwordHash`, `displayName`, `avatarUrl`
- Keep: `id`, `username`

### Conversation

- Change: From `userA`/`userB` to `participants` (many-to-many)
- Add: `type` enum (ONE_TO_ONE, GROUP), `name` (for groups), `createdAt`

### ConversationParticipant (new)

- `conversationId`, `userId`, `joinedAt`, `role` (OWNER, ADMIN, MEMBER)

### Message

- Add: `idempotencyKey` (unique per conversation), `eventId` (for resume)
- Keep: `id`, `conversation`, `sender`, `body`, `createdAt`

### Event (new)

- `id`, `conversationId`, `type`, `payload`, `createdAt` - For WebSocket resume

### RefreshToken (new)

- `id`, `userId`, `token`, `expiresAt`, `revoked`

## API Endpoints (v1)

### Authentication

- `POST /api/v1/auth/register` - Register new user
- `POST /api/v1/auth/login` - Login (username/email + password)
- `POST /api/v1/auth/refresh` - Refresh access token
- `POST /api/v1/auth/logout` - Revoke refresh token

### Users

- `GET /api/v1/users/me` - Get current user profile
- `PATCH /api/v1/users/me` - Update profile

### Conversations

- `POST /api/v1/conversations` - Create conversation (one-to-one or group)
- `GET /api/v1/conversations` - List user's conversations
- `GET /api/v1/conversations/{id}` - Get conversation details
- `POST /api/v1/conversations/{id}/participants` - Add participant (group)
- `DELETE /api/v1/conversations/{id}/participants/{userId}` - Remove participant

### Messages

- `POST /api/v1/conversations/{id}/messages` - Send message (with idempotency key)
- `GET /api/v1/conversations/{id}/messages` - List messages (cursor pagination)

### WebSocket

- `ws://host/api/v1/events` - Real-time event stream (authenticated)

## Future Phase 2 Considerations

- Signal-like protocol (device identity keys, prekeys, session establishment)
- Sender keys for groups
- Safety numbers / verification UI
- End-to-end encryption implementation

## Key Design Decisions

1. **Idempotency**: Messages include idempotency keys to prevent duplicates
2. **Event IDs**: WebSocket events include IDs for resume capability
3. **Cursor Pagination**: Efficient pagination for large message histories
4. **Service Boundaries**: Code organized to support future microservices split
5. **Security**: JWT with short-lived access tokens and rotating refresh tokens
6. **Observability**: Full stack observability from day one for debugging and monitoring