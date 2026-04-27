# D&D Manager API Overview

Spring Boot 3 / Java 17 backend for managing worlds, campaigns, sessions, player characters, NPCs, locations, items, session events, chat and uploaded session resources.

## Security

- Auth model: stateless JWT Bearer.
- Public endpoints:
  - `POST /api/auth/register`
  - `POST /api/auth/login`
  - `GET /api/dice/**`
- All other `/api/**` endpoints require `Authorization: Bearer <token>`.
- Download endpoints also accept `?token=<jwt>` when the browser cannot send the header.
- Passwords are stored with BCrypt.

## Roles

- `ROLE_ADMIN`: global access.
- `ROLE_GM`: game management endpoints and owned content.
- `ROLE_PLAYER`: can create and manage own characters, join campaigns and access participant features.
- `ROLE_VIEWER`: authenticated read-only user.

## Main feature areas

- Auth and current user profile
- Worlds and public world discovery
- Campaigns and sessions
- Campaign join requests and approved players
- Player characters with language support
- Session chat, including private whispers
- NPCs, locations and items with GM-only fields
- Session timeline/events
- Session file uploads and downloads
- Dashboard aggregation endpoint

## Core endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Register user |
| `POST` | `/api/auth/login` | Login and receive JWT |
| `GET` | `/api/auth/me` | Current user |
| `PUT` | `/api/auth/me` | Update current user |
| `GET` | `/api/dashboard` | Aggregated dashboard data |
| `GET` | `/api/worlds` | List visible worlds |
| `GET` | `/api/worlds/public` | Public worlds discovery |
| `POST` | `/api/worlds` | Create world (`ADMIN`/`GM`) |
| `GET` | `/api/campaigns` | List campaigns |
| `POST` | `/api/campaigns` | Create campaign (`ADMIN`/`GM`) |
| `GET` | `/api/campaigns/{campaignId}/sessions` | List sessions for campaign |
| `POST` | `/api/campaigns/{campaignId}/sessions` | Create session (`ADMIN`/`GM`) |
| `POST` | `/api/campaigns/{campaignId}/join-requests` | Request join with a character |
| `GET` | `/api/campaigns/{campaignId}/players` | Approved players for a campaign |
| `POST` | `/api/characters` | Create player character |
| `PATCH` | `/api/characters/{id}/hp` | Update current hit points |
| `PATCH` | `/api/characters/{id}/death-saves` | Update death save counters |
| `PATCH` | `/api/characters/{id}/inventory` | Update equipment and treasure |
| `PATCH` | `/api/characters/{id}/spell-slots` | Update spell slot tracking |
| `POST` | `/api/characters/{id}/long-rest` | Restore HP, death saves and spell slots |
| `GET` | `/api/sessions/{sessionId}/chat/messages` | Read session chat |
| `POST` | `/api/sessions/{sessionId}/chat/messages` | Send session chat message |
| `POST` | `/api/sessions/{sessionId}/upload` | Upload session file |
| `GET` | `/api/sessions/{sessionId}/resources` | List uploaded resources |
| `GET` | `/api/sessions/{sessionId}/events` | Session timeline |

## Chat and language notes

- Session chat supports public messages and private whispers through `recipientUserId`.
- Players can speak only through their approved campaign character.
- GM/Admin can speak as narrator without a character, or with a character they own.
- If a language string is unknown, the backend currently falls back to `COMMON`.
- Players see obfuscated content for languages their character does not know.

## File uploads

- Upload endpoint: `POST /api/sessions/{sessionId}/upload`
- Multipart field name: `file`
- Max upload size: `10MB`
- Stored file types are categorized as `IMAGE`, `PDF` or `OTHER`

## Development defaults

- API base URL: `http://localhost:8080`
- Database: PostgreSQL
- CORS is currently permissive for development (`allowedOriginPatterns("*")`)

## Docs

- Technical overview: [docs/project-documentation.md](docs/project-documentation.md)
- Frontend-oriented API reference: [docs/frontend-api-reference.md](docs/frontend-api-reference.md)
- Current flows and endpoint map: [docs/flussi-e-api-attuali.md](docs/flussi-e-api-attuali.md)
- Dev log: [docs/devlog.md](docs/devlog.md)
