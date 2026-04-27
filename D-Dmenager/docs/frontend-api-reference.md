# Guida API per frontend Flutter (DD Manager)

Riferimento rapido per integrare il frontend Flutter con il backend corrente.

## Convenzioni generali

- Base URL sviluppo: `http://localhost:8080`
- Auth: JWT Bearer su tutte le chiamate protette
- Header standard: `Authorization: Bearer <token>`
- Download file: possibile anche `?token=<jwt>`
- Errori: JSON `ApiError`

## Auth

### Register

- `POST /api/auth/register`
- pubblico
- body:

```json
{
  "email": "user@example.com",
  "password": "secret",
  "nickname": "Nick",
  "role": "PLAYER"
}
```

### Login

- `POST /api/auth/login`
- pubblico
- response:

```json
{
  "token": "jwt",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "nickname": "Nick",
    "roles": ["ROLE_PLAYER"]
  }
}
```

### Current user

- `GET /api/auth/me`
- `PUT /api/auth/me`

## Worlds

- `GET /api/worlds`
- `GET /api/worlds/my`
- `GET /api/worlds/public`
- `GET /api/worlds/public/{id}`
- `GET /api/worlds/{id}`
- `POST /api/worlds`
- `PUT /api/worlds/{id}`
- `DELETE /api/worlds/{id}`

Request tipico:

```json
{
  "name": "Forgotten Realms",
  "description": "Ambientazione",
  "isPublic": true
}
```

## Campaigns

- `GET /api/campaigns`
- `GET /api/campaigns/{id}`
- `GET /api/campaigns/world/{worldId}`
- `GET /api/campaigns/my`
- `POST /api/campaigns`
- `PUT /api/campaigns/{id}`
- `DELETE /api/campaigns/{id}`

## Sessions

- `GET /api/campaigns/{campaignId}/sessions`
- `POST /api/campaigns/{campaignId}/sessions`
- `GET /api/sessions/my`
- `GET /api/sessions/{sessionId}`
- `PUT /api/sessions/{sessionId}`
- `DELETE /api/sessions/{sessionId}`

## Player characters

- `POST /api/characters`
- `GET /api/characters/my`
- `GET /api/characters/{id}`
- `GET /api/characters/world/{worldId}`
- `PUT /api/characters/{id}`
- `DELETE /api/characters/{id}`

Note frontend:

- `knownLanguages` e un array di enum stringa
- se omesso o vuoto il backend garantisce almeno `COMMON`
- alcuni campi GM-only possono mancare in base al ruolo del caller

## Campaign join flow

- `POST /api/campaigns/{campaignId}/join-requests`
- `GET /api/campaigns/{campaignId}/join-requests`
- `POST /api/campaigns/{campaignId}/join-requests/{campaignPlayerId}/approve`
- `POST /api/campaigns/{campaignId}/join-requests/{campaignPlayerId}/reject`
- `GET /api/campaigns/{campaignId}/my-join-request`
- `GET /api/campaigns/{campaignId}/players`
- `GET /api/campaign-players/my`
- `GET /api/join-requests/my`
- `GET /api/dm/join-requests`

Status possibili:

- `PENDING`
- `APPROVED`
- `REJECTED`
- `LEFT`
- `KICKED`

Request di join:

```json
{
  "characterId": 12,
  "message": "Vorrei unirmi alla campagna"
}
```

## Session chat

- `GET /api/sessions/{sessionId}/chat/messages`
- `POST /api/sessions/{sessionId}/chat/messages`

Parametri GET:

- `from` opzionale, `ISO-8601 instant`
- `recipientUserId` opzionale, per leggere solo un thread privato con quell'utente

Body POST:

```json
{
  "content": "Messaggio",
  "language": "ELVISH",
  "senderCharacterId": 22,
  "messageType": "IC",
  "recipientUserId": 7
}
```

Regole utili per il frontend:

- se `recipientUserId` e valorizzato, il messaggio e un whisper privato
- i player devono usare il proprio personaggio approvato
- il GM puo parlare senza personaggio oppure con un personaggio che possiede
- se il backend non riconosce la lingua passata, oggi la tratta come `COMMON`
- il testo restituito al frontend e in `contentVisible`

## Session resources

- `POST /api/sessions/{sessionId}/upload`
- `GET /api/sessions/{sessionId}/resources`
- `GET /api/resources/file/{fileName}`

Upload:

- multipart
- campo richiesto: `file`
- max: `10MB`

Tipi esposti in response:

- `IMAGE`
- `PDF`
- `OTHER`

## Session events

- `GET /api/sessions/{sessionId}/events`
- `POST /api/session-events`
- `PUT /api/session-events/{id}`
- `DELETE /api/session-events/{id}`

## NPC, locations, items

CRUD completi sotto:

- `/api/npcs`
- `/api/locations`
- `/api/items`

Note frontend:

- GM/Admin vedono anche i campi riservati
- Player/Viewer vedono solo contenuti visibili
- alcune risorse possono restituire `404` se nascoste al caller

## Dashboard

- `GET /api/dashboard`

Campi principali restituiti:

- `view`
- `stats`
- `worlds`
- `campaigns`
- `npcs`
- `locations`
- `items`
- `recentEvents`
- `pendingJoinRequests`
- `myCharacters`

## Utility endpoints

- `GET /api/health`
- `GET /api/debug/me`
- `GET /api/dice/d4`
- `GET /api/dice/d6`
- `GET /api/dice/d8`
- `GET /api/dice/d10`
- `GET /api/dice/d12`
- `GET /api/dice/d20`
