# Flussi e API attuali

Documento di ricognizione del backend `D-Dmenager` aggiornato al codice presente nel repository il 16 aprile 2026.

Nota importante: il progetto non usa piu HTTP Basic come descritto nelle linee guida storiche di `AGENTS.md`. L'implementazione attuale usa autenticazione JWT Bearer stateless.

## Stack e convenzioni operative

- Backend Spring Boot 3 / Java 17.
- Tutte le API applicative sono sotto `/api/**`.
- Autenticazione attuale: JWT Bearer.
- Login pubblico: `POST /api/auth/login`.
- Registrazione pubblica: `POST /api/auth/register`.
- Tutti gli altri endpoint `/api/**` richiedono autenticazione, salvo `/api/dice/**` che e pubblico.
- I download file accettano sia header `Authorization: Bearer <token>` sia query param `?token=<jwt>`.
- La CORS policy attuale e permissiva in sviluppo: `allowedOriginPatterns("*")`.

## Autenticazione e ruoli

### Flusso base

1. Registrazione con `POST /api/auth/register`.
2. Login con `POST /api/auth/login`.
3. Il backend restituisce `AuthResponse` con:
   - `token`
   - `user`
4. Il token va inviato nelle richieste protette con:
   - header `Authorization: Bearer <token>`
5. Per verificare l'utente corrente:
   - `GET /api/auth/me`
   - `GET /api/debug/me`

### Ruoli applicativi

- `ROLE_ADMIN`
- `ROLE_GM`
- `ROLE_PLAYER`
- `ROLE_VIEWER`

### Mappatura ruolo in registrazione

Il campo `role` di `RegisterRequest` accetta:

- `PLAYER` -> `ROLE_PLAYER`
- `DM` -> `ROLE_GM`
- `VIEWER` -> `ROLE_VIEWER`

## Flussi principali

### 1. Onboarding utente

1. L'utente si registra con email, password, nickname e ruolo.
2. Effettua login.
3. Riceve il JWT.
4. Recupera il proprio profilo con `/api/auth/me`.

### 2. Flusso GM

1. Crea un mondo con `POST /api/worlds`.
2. Crea una campagna su quel mondo con `POST /api/campaigns`.
3. Crea le sessioni della campagna con `POST /api/campaigns/{campaignId}/sessions`.
4. Arricchisce il mondo con NPC, location e item.
5. Durante o dopo la sessione:
   - registra eventi in `/api/session-events`
   - carica file in `/api/sessions/{sessionId}/upload`
   - usa la chat di sessione
6. Gestisce le richieste di join dei player:
   - `GET /api/campaigns/{campaignId}/join-requests`
   - `POST /approve`
   - `POST /reject`
   - `GET /api/dm/join-requests`

### 3. Flusso player

1. Crea uno o piu personaggi con `POST /api/characters`.
2. Consulta mondi e campagne disponibili.
3. Invia una richiesta di join alla campagna con uno dei propri personaggi:
   - `POST /api/campaigns/{campaignId}/join-requests`
4. Controlla lo stato della richiesta:
   - `GET /api/campaigns/{campaignId}/my-join-request`
   - `GET /api/join-requests/my`
   - `GET /api/campaign-players/my`
5. Se approvato, puo:
   - vedere le sessioni associate
   - partecipare alla chat
   - consultare eventi e risorse della sessione

### 4. Flusso dashboard

1. L'utente autenticato chiama `GET /api/dashboard`.
2. Il backend costruisce una vista aggregata diversa in base al ruolo e al contesto utente.

### 5. Flusso chat privata

1. Un partecipante di campagna legge la chat pubblica con `GET /api/sessions/{sessionId}/chat/messages`.
2. Per inviare un whisper usa `POST /api/sessions/{sessionId}/chat/messages` valorizzando `recipientUserId`.
3. Per rileggere solo quel thread privato usa `GET /api/sessions/{sessionId}/chat/messages?recipientUserId={userId}`.
4. I player devono usare il proprio personaggio approvato.
5. Il GM puo inviare come narratore oppure con un personaggio che possiede.

## Pattern di autorizzazione

- `ADMIN` vede e gestisce tutto.
- `GM` gestisce i contenuti di gioco e i flussi di approvazione.
- `PLAYER` puo creare e gestire i propri personaggi e richiedere l'accesso alle campagne.
- `VIEWER` e sostanzialmente in sola lettura.
- Per NPC, location, item e session event la risposta cambia in base al ruolo:
  - `GM` e `ADMIN` vedono anche i campi riservati
  - `PLAYER` e `VIEWER` vedono solo contenuti permessi

## Payload principali

### Auth

- `RegisterRequest`
  - `email`
  - `password`
  - `nickname`
  - `role`
- `LoginRequest`
  - `email`
  - `password`
- `UserUpdateRequest`
  - `nickname`
  - `password`

### Join request

- `CampaignPlayerRequest`
  - `characterId`
  - `message`

### Chat sessione

- `SessionChatMessageRequest`
  - `content`
  - `language`
  - `senderCharacterId`
  - `messageType`
  - `recipientUserId`

### Eventi sessione

- `SessionEventRequest`
  - `sessionId`
  - `title`
  - `type`
  - `description`
  - `inGameTime`
  - `isVisibleToPlayers`

### Risorse sessione

- Upload multipart con campo `file`

## Catalogo completo delle API

### Auth e diagnostica

| Metodo | Path | Accesso | Descrizione |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Pubblico | Registra un nuovo utente e restituisce `UserResponse`. |
| `POST` | `/api/auth/login` | Pubblico | Autentica l'utente e restituisce `AuthResponse` con JWT e dati utente. |
| `GET` | `/api/auth/me` | Autenticato | Restituisce il profilo dell'utente corrente. |
| `PUT` | `/api/auth/me` | Autenticato | Aggiorna nickname e/o password dell'utente corrente. |
| `GET` | `/api/debug/me` | Autenticato | Endpoint di debug con username e authorities correnti. |

### Health e utility

| Metodo | Path | Accesso | Descrizione |
| --- | --- | --- | --- |
| `GET` | `/api/health` | Autenticato | Health check con `status` e `timestamp`. |
| `GET` | `/api/dice/d4` | Pubblico | Lancia un d4. |
| `GET` | `/api/dice/d6` | Pubblico | Lancia un d6. |
| `GET` | `/api/dice/d8` | Pubblico | Lancia un d8. |
| `GET` | `/api/dice/d10` | Pubblico | Lancia un d10. |
| `GET` | `/api/dice/d12` | Pubblico | Lancia un d12. |
| `GET` | `/api/dice/d20` | Pubblico | Lancia un d20. |

### Dashboard

| Metodo | Path | Accesso | Descrizione |
| --- | --- | --- | --- |
| `GET` | `/api/dashboard` | Autenticato | Restituisce la dashboard aggregata per l'utente corrente. |

### Worlds

| Metodo | Path | Accesso | Descrizione |
| --- | --- | --- | --- |
| `POST` | `/api/worlds` | `ADMIN` o `GM` | Crea un mondo. |
| `GET` | `/api/worlds` | Autenticato | Elenca i mondi visibili all'utente. |
| `GET` | `/api/worlds/my` | `ADMIN` o `GM` | Elenca i mondi del GM corrente. |
| `GET` | `/api/worlds/public` | Autenticato | Elenca i mondi pubblici. |
| `GET` | `/api/worlds/public/{id}` | Autenticato | Dettaglio di un mondo pubblico. |
| `GET` | `/api/worlds/{id}` | Autenticato | Dettaglio di un mondo visibile all'utente. |
| `PUT` | `/api/worlds/{id}` | `ADMIN` o `GM` | Aggiorna un mondo. |
| `DELETE` | `/api/worlds/{id}` | `ADMIN` o `GM` | Elimina un mondo. |

### Campaigns

| Metodo | Path | Accesso | Descrizione |
| --- | --- | --- | --- |
| `POST` | `/api/campaigns` | `ADMIN` o `GM` | Crea una campagna. |
| `GET` | `/api/campaigns` | Autenticato | Elenca le campagne visibili. |
| `GET` | `/api/campaigns/{id}` | Autenticato | Dettaglio campagna. |
| `GET` | `/api/campaigns/world/{worldId}` | Autenticato | Elenca le campagne di un mondo. |
| `GET` | `/api/campaigns/my` | `ADMIN` o `GM` | Elenca le campagne del GM corrente. |
| `PUT` | `/api/campaigns/{id}` | `ADMIN` o `GM` | Aggiorna una campagna. |
| `DELETE` | `/api/campaigns/{id}` | `ADMIN` o `GM` | Elimina una campagna. |

### Sessions

| Metodo | Path | Accesso | Descrizione |
| --- | --- | --- | --- |
| `POST` | `/api/campaigns/{campaignId}/sessions` | `ADMIN` o `GM` | Crea una sessione per la campagna. |
| `GET` | `/api/campaigns/{campaignId}/sessions` | Autenticato | Elenca le sessioni della campagna. |
| `GET` | `/api/sessions/my` | `ADMIN`, `GM` o `PLAYER` | Elenca le sessioni dell'utente corrente. |
| `GET` | `/api/sessions/{sessionId}` | Autenticato | Dettaglio sessione. |
| `PUT` | `/api/sessions/{sessionId}` | `ADMIN` o `GM` | Aggiorna una sessione. |
| `DELETE` | `/api/sessions/{sessionId}` | `ADMIN` o `GM` | Elimina una sessione. |

### Campaign join requests e membership

| Metodo | Path | Accesso | Descrizione |
| --- | --- | --- | --- |
| `POST` | `/api/campaigns/{campaignId}/join-requests` | `ADMIN`, `GM` o `PLAYER` | Invia una richiesta di join con un personaggio. |
| `GET` | `/api/campaigns/{campaignId}/join-requests` | `ADMIN` o `GM` | Elenca le richieste della campagna. Supporta query param `status`. |
| `POST` | `/api/campaigns/{campaignId}/join-requests/{campaignPlayerId}/approve` | `ADMIN` o `GM` | Approva una richiesta di join. |
| `POST` | `/api/campaigns/{campaignId}/join-requests/{campaignPlayerId}/reject` | `ADMIN` o `GM` | Rifiuta una richiesta di join. |
| `GET` | `/api/campaigns/{campaignId}/my-join-request` | `ADMIN`, `GM` o `PLAYER` | Restituisce la richiesta dell'utente corrente per quella campagna. |
| `GET` | `/api/campaigns/{campaignId}/players` | Autenticato | Elenca i partecipanti approvati della campagna. |
| `GET` | `/api/campaign-players/my` | `ADMIN`, `GM` o `PLAYER` | Elenca i record campaign-player dell'utente corrente. |
| `GET` | `/api/join-requests/my` | Solo `PLAYER` | Elenca tutte le richieste inviate dal player corrente. |
| `GET` | `/api/dm/join-requests` | `ADMIN` o `GM` | Elenca le richieste pendenti rilevanti per il GM corrente. |

### Player characters

| Metodo | Path | Accesso | Descrizione |
| --- | --- | --- | --- |
| `POST` | `/api/characters` | `ADMIN`, `GM` o `PLAYER` | Crea un personaggio giocante. |
| `GET` | `/api/characters/my` | Autenticato | Elenca i personaggi dell'utente corrente. |
| `GET` | `/api/characters/{id}` | Autenticato | Dettaglio personaggio, con filtri di visibilita. |
| `GET` | `/api/characters/world/{worldId}` | Autenticato | Elenca i personaggi di un mondo. |
| `PUT` | `/api/characters/{id}` | `ADMIN`, `GM` o `PLAYER` | Aggiorna un personaggio se autorizzato. |
| `DELETE` | `/api/characters/{id}` | `ADMIN`, `GM` o `PLAYER` | Elimina un personaggio se autorizzato. |

### Chat di sessione

| Metodo | Path | Accesso | Descrizione |
| --- | --- | --- | --- |
| `POST` | `/api/sessions/{sessionId}/chat/messages` | Autenticato | Invia un messaggio pubblico o privato nella chat della sessione. |
| `GET` | `/api/sessions/{sessionId}/chat/messages` | Autenticato | Legge la chat pubblica o un thread privato. Supporta `recipientUserId` e `from`. |

### Risorse di sessione

| Metodo | Path | Accesso | Descrizione |
| --- | --- | --- | --- |
| `POST` | `/api/sessions/{sessionId}/upload` | Autenticato | Carica un file per la sessione. |
| `GET` | `/api/sessions/{sessionId}/resources` | Autenticato | Elenca i file della sessione. |
| `GET` | `/api/resources/file/{fileName}` | Autenticato | Scarica o apre inline il file richiesto. Supporta anche `?token=`. |

### Session events

| Metodo | Path | Accesso | Descrizione |
| --- | --- | --- | --- |
| `GET` | `/api/sessions/{sessionId}/events` | Autenticato | Elenca gli eventi visibili della sessione. |
| `POST` | `/api/session-events` | `ADMIN` o `GM` | Crea un evento di sessione. |
| `PUT` | `/api/session-events/{id}` | `ADMIN` o `GM` | Aggiorna un evento di sessione. |
| `DELETE` | `/api/session-events/{id}` | `ADMIN` o `GM` | Elimina un evento di sessione. |

### NPC

| Metodo | Path | Accesso | Descrizione |
| --- | --- | --- | --- |
| `GET` | `/api/npcs` | Autenticato | Elenca gli NPC visibili all'utente corrente. |
| `GET` | `/api/npcs/{id}` | Autenticato | Dettaglio NPC con filtri di visibilita. |
| `GET` | `/api/npcs/world/{worldId}` | Autenticato | Elenca gli NPC di un mondo. |
| `POST` | `/api/npcs` | `ADMIN` o `GM` | Crea un NPC. |
| `PUT` | `/api/npcs/{id}` | `ADMIN` o `GM` | Aggiorna un NPC. |
| `DELETE` | `/api/npcs/{id}` | `ADMIN` o `GM` | Elimina un NPC. |

### Locations

| Metodo | Path | Accesso | Descrizione |
| --- | --- | --- | --- |
| `GET` | `/api/locations` | Autenticato | Elenca le location visibili all'utente corrente. |
| `GET` | `/api/locations/{id}` | Autenticato | Dettaglio location con filtri di visibilita. |
| `GET` | `/api/locations/world/{worldId}` | Autenticato | Elenca le location di un mondo. |
| `POST` | `/api/locations` | `ADMIN` o `GM` | Crea una location. |
| `PUT` | `/api/locations/{id}` | `ADMIN` o `GM` | Aggiorna una location. |
| `DELETE` | `/api/locations/{id}` | `ADMIN` o `GM` | Elimina una location. |

### Items

| Metodo | Path | Accesso | Descrizione |
| --- | --- | --- | --- |
| `GET` | `/api/items` | Autenticato | Elenca gli item visibili all'utente corrente. |
| `GET` | `/api/items/{id}` | Autenticato | Dettaglio item con filtri di visibilita. |
| `GET` | `/api/items/world/{worldId}` | Autenticato | Elenca gli item di un mondo. |
| `POST` | `/api/items` | `ADMIN` o `GM` | Crea un item. |
| `PUT` | `/api/items/{id}` | `ADMIN` o `GM` | Aggiorna un item. |
| `DELETE` | `/api/items/{id}` | `ADMIN` o `GM` | Elimina un item. |

## Error handling osservato

Le API restituiscono errori in formato `ApiError` con:

- `timestamp`
- `status`
- `error`
- `message`
- `path`

Mappature principali presenti oggi:

- `400` per validazione e `IllegalArgumentException`
- `401` per credenziali/token mancanti o invalidi
- `403` per accesso negato
- `404` per risorsa non trovata
- `409` per conflitti applicativi
- `413` per upload oltre 10MB
- `500` per errori inattesi

## Note pratiche

- La configurazione DB attuale in `application.yml` punta a:
  - `jdbc:postgresql://localhost:5432/dd_manager`
  - username `postgres`
  - password `admin`
- La scadenza JWT configurata e di `3600` secondi.
- Le lingue chat riconosciute sono quelle dell'enum `CharacterLanguage`; se viene passato un valore non riconosciuto il backend oggi fa fallback a `COMMON`.
- Il DM in chat puo parlare solo come personaggio che possiede; non puo impersonare i personaggi dei player.
- Il documento e stato costruito leggendo i controller e la security correnti, quindi rappresenta lo stato effettivamente esposto dal backend oggi.
