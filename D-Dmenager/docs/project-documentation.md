# Documentazione del Progetto DD Manager

## 1. Introduzione

DD Manager e un backend Spring Boot 3 / Java 17 per la gestione di contenuti GdR: mondi, campagne, sessioni, personaggi, NPC, location, oggetti, timeline di sessione, chat e file allegati.

L'applicazione espone solo API REST e oggi usa autenticazione JWT Bearer stateless.

## 2. Architettura

- Controller REST sotto `/api/**`
- Service layer con logica di dominio e autorizzazione
- Repository JPA su PostgreSQL
- Spring Security con filtro JWT
- CORS permissivo per sviluppo tramite `CorsConfig`

Tecnologie principali:

- Java 17
- Spring Boot 3
- Spring MVC
- Spring Security
- Spring Data JPA
- PostgreSQL
- Lombok
- Jakarta Validation

## 3. Modello di dominio

- `User`: email univoca, password BCrypt, nickname, ruoli.
- `Role`: nome ruolo (`ROLE_ADMIN`, `ROLE_GM`, `ROLE_PLAYER`, `ROLE_VIEWER`).
- `World`: contenitore principale, con owner e flag `isPublic`.
- `Campaign`: appartiene a un world, ha owner e stato.
- `Session`: appartiene a una campaign.
- `PlayerCharacter`: scheda personaggio estesa, con `knownLanguages`.
- `CampaignPlayer`: relazione tra player character e campaign con stato:
  - `PENDING`
  - `APPROVED`
  - `REJECTED`
  - `LEFT`
  - `KICKED`
- `Npc`, `Location`, `Item`: contenuti world-level con campi pubblici e campi GM-only.
- `SessionEvent`: timeline degli eventi di sessione.
- `SessionChatMessage`: messaggi chat pubblici o privati.
- `SessionResource`: file caricati per una sessione.

## 4. Sicurezza

### Autenticazione

- Login con `POST /api/auth/login`
- Token JWT firmato con HMAC SHA-256
- Claim principali:
  - `sub`
  - `userId`
  - `email`
  - `nickname`
  - `roles`
  - `iat`
  - `exp`

Endpoint pubblici:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/dice/**`

Tutti gli altri `/api/**` richiedono Bearer token.

### Autorizzazione

- `ADMIN` ha accesso globale.
- `GM` puo creare e gestire contenuti di gioco.
- `PLAYER` puo creare e gestire i propri personaggi e partecipare ai flussi player.
- `VIEWER` e autenticato ma senza operazioni di creazione protetta.

Le mutate su world, campaign, session, NPC, location, item e session event richiedono tipicamente `ADMIN` o `GM`.

## 5. Gestione errori

Formato standard: `ApiError`

Campi:

- `timestamp`
- `status`
- `error`
- `message`
- `path`

Mappature correnti principali:

- `400` validazione o `IllegalArgumentException`
- `401` token mancante/non valido o credenziali errate
- `403` accesso negato
- `404` risorsa non trovata
- `409` conflitti applicativi
- `413` upload oltre 10MB
- `500` errore inatteso

## 6. Flussi principali

### Registrazione e login

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `PUT /api/auth/me`

### Journey GM

1. Crea un world.
2. Crea una campaign nel world.
3. Crea una o piu sessioni.
4. Arricchisce il world con NPC, location e item.
5. Gestisce join request dei player.
6. Usa eventi, chat e upload risorse durante la sessione.

### Journey Player

1. Crea i propri player character.
2. Sfoglia mondi e campagne disponibili.
3. Invia join request con un proprio personaggio.
4. Se approvato, accede a sessioni, chat, eventi e risorse.

## 7. Chat di sessione

La chat supporta due modalita:

- messaggi pubblici
- whisper privati con `recipientUserId`

Regole attuali:

- i player possono parlare solo come personaggio approvato della campaign
- `senderCharacterId` e obbligatorio per i player
- GM/Admin possono parlare senza personaggio oppure con un personaggio che possiedono
- il DM non puo impersonare i personaggi dei player
- i contenuti restano salvati in chiaro e vengono trasformati in `contentVisible` in base alla lingua conosciuta dal viewer
- se la lingua richiesta non e riconosciuta, il backend oggi fa fallback a `COMMON`

Lingue supportate da enum:

- `COMMON`
- `DWARVISH`
- `ELVISH`
- `GIANT`
- `GNOMISH`
- `GOBLIN`
- `HALFLING`
- `ORC`
- `ABYSSAL`
- `CELESTIAL`
- `DRACONIC`
- `DEEP_SPEECH`
- `INFERNAL`
- `PRIMORDIAL`
- `SYLVAN`
- `UNDERCOMMON`
- `THIEVES_CANT`
- `EGYPTIAN`

## 8. Session resources

- Upload con `POST /api/sessions/{sessionId}/upload`
- Campo multipart richiesto: `file`
- Max size: `10MB`
- Tipi classificati come:
  - `IMAGE`
  - `PDF`
  - `OTHER`
- Download tramite `GET /api/resources/file/{fileName}`
- Supportato anche `?token=<jwt>`

## 9. Dashboard

`GET /api/dashboard` restituisce una vista aggregata con:

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

La vista cambia fra `GM` e `PLAYER`.

## 10. Endpoints principali

- Auth:
  - `POST /api/auth/register`
  - `POST /api/auth/login`
  - `GET /api/auth/me`
  - `PUT /api/auth/me`
- Debug:
  - `GET /api/debug/me`
- Health:
  - `GET /api/health`
- Worlds:
  - `GET /api/worlds`
  - `GET /api/worlds/my`
  - `GET /api/worlds/public`
  - `GET /api/worlds/public/{id}`
  - `GET /api/worlds/{id}`
  - `POST /api/worlds`
  - `PUT /api/worlds/{id}`
  - `DELETE /api/worlds/{id}`
- Campaigns:
  - `GET /api/campaigns`
  - `GET /api/campaigns/{id}`
  - `GET /api/campaigns/world/{worldId}`
  - `GET /api/campaigns/my`
  - `POST /api/campaigns`
  - `PUT /api/campaigns/{id}`
  - `DELETE /api/campaigns/{id}`
- Sessions:
  - `GET /api/campaigns/{campaignId}/sessions`
  - `POST /api/campaigns/{campaignId}/sessions`
  - `GET /api/sessions/my`
  - `GET /api/sessions/{sessionId}`
  - `PUT /api/sessions/{sessionId}`
  - `DELETE /api/sessions/{sessionId}`
- Campaign player flow:
  - `POST /api/campaigns/{campaignId}/join-requests`
  - `GET /api/campaigns/{campaignId}/join-requests`
  - `POST /api/campaigns/{campaignId}/join-requests/{campaignPlayerId}/approve`
  - `POST /api/campaigns/{campaignId}/join-requests/{campaignPlayerId}/reject`
  - `GET /api/campaigns/{campaignId}/my-join-request`
  - `GET /api/campaigns/{campaignId}/players`
  - `GET /api/campaign-players/my`
  - `GET /api/join-requests/my`
  - `GET /api/dm/join-requests`
- Characters:
  - `POST /api/characters`
  - `GET /api/characters/my`
  - `GET /api/characters/{id}`
  - `GET /api/characters/world/{worldId}`
  - `PUT /api/characters/{id}`
  - `DELETE /api/characters/{id}`
- Chat:
  - `GET /api/sessions/{sessionId}/chat/messages`
  - `POST /api/sessions/{sessionId}/chat/messages`
- Resources:
  - `POST /api/sessions/{sessionId}/upload`
  - `GET /api/sessions/{sessionId}/resources`
  - `GET /api/resources/file/{fileName}`
- Session events:
  - `GET /api/sessions/{sessionId}/events`
  - `POST /api/session-events`
  - `PUT /api/session-events/{id}`
  - `DELETE /api/session-events/{id}`
- NPCs, locations, items:
  - CRUD e listing sotto `/api/npcs`, `/api/locations`, `/api/items`

## 11. Testing

Suite principali presenti:

- `AuthIntegrationTest`
- `WorldCampaignSmokeTest`
- `CampaignCrudIntegrationTest`
- `SessionCrudIntegrationTest`
- `CampaignPlayerIntegrationTest`
- `PlayerCharacterIntegrationTest`
- `SessionChatIntegrationTest`
- `SessionEventIntegrationTest`
- `SessionResourceIntegrationTest`
- `NpcIntegrationTest`
- `LocationIntegrationTest`
- `ItemIntegrationTest`
- `DashboardIntegrationTest`

## 12. Note operative

- La documentazione storica che parla di HTTP Basic non e piu valida per lo stato corrente del codice.
- Lo stato reale da seguire e JWT Bearer.
- Per un catalogo endpoint piu operativo consultare anche `docs/flussi-e-api-attuali.md`.
