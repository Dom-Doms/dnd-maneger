# Devlog - DD Manager Backend

Diario di sviluppo delle funzionalita backend.

## Task 1 - Setup e Auth di base
- Inizializzato progetto Spring Boot 3 con Java 17, Spring Web, Data JPA, Security, Lombok, PostgreSQL Driver.
- Configurato docker-compose.yml per PostgreSQL locale.
- Implementato User entity (email, password, nickname, role) e UserRepository.
- Configurato SecurityFilterChain per abilitare CORS e basic auth (temporaneo); password encoder BCrypt.
- Creato AuthController per register (pubblico) e login (restituisce UserResponse).
- Gestione eccezioni centralizzata con GlobalExceptionHandler (409 per duplicati, 400 per validazione).

## Task 2 - World management
- Creata entita World (name, description, owner) relazione ManyToOne con User.
- Implementato CRUD completo in WorldController con sicurezza a livello di metodo: solo l'owner puo modificare il proprio mondo.
- Aggiunti test di integrazione con Testcontainers (o H2 se preferito) per verificare isolamento dati tra utenti.

## Task 3 - Campaign e Session basics
- Entita Campaign (world, name, status) e Session (campaign, title, date).
- Implementati service e controller. Regola di business: una campagna appartiene a un world, e solo l'owner del world (GM) puo creare compagne in quel world.
- Endpoint nidificati: POST /api/campaigns/{id}/sessions.

## Task 4 - NPC e visibilita
- Entita NPC estesa con campi "statblock" base (race, class, level, hp, ac) ma soprattutto flag `isVisibleToPlayers`.
- Logica di filtraggio nei service: il GM vede tutto, i player vedono solo `isVisibleToPlayers=true` tramite @PostFilter o query personalizzate.
- Endpoint public/private per recupero lista NPC di un mondo.

## Task 5 - Location e Item
- Struttura simile agli NPC: entita Location (parentLocation opzionale) e Item (location opzionale).
- CRUD standard con le stesse regole di visibilita per i player.
- Seed dati iniziale per testare la gerarchia delle location.

## Task 6 - Session Events e Log
- Entita SessionEvent per tracciare cosa accade in sessione (diario).
- Campi: title, description, inGameTime, realTime (timestamp), isVisible.
- Utile per la feature "riassunto sessione precedente" nel frontend.

## Task 7 - Refactoring e test coverage
- Revisione dei DTO per non esporre entita dirette.
- Aggiunti unit test per i service layer con Mockito.
- Verifica performance delle query (n+1 problem su liste lunghe) e aggiunto qualche @EntityGraph se necessario.

## Task 8 - Security hardening
- Passaggio definitivo a JWT (o mantenimento sessione stateless ben configurata).
- Annotazioni @PreAuthorize su tutti gli endpoint sensibili.
- CORS policy raffinata per accettare richieste solo dal dominio del frontend (localhost:3000 in dev).

## Task 9 - Dashboard API
- Endpoint aggregatore /api/dashboard che restituisce un JSON complesso con:
  - Lista ultimi world/campaign attivi.
  - Prossima sessione schedulata.
  - Quick stats (numero NPC, locazioni).
- Ottimizzato per ridurre il numero di chiamate HTTP all'avvio dell'app mobile/web.

## Task 10 - Refinement e bugfix
- Risolti bug su cancellazione a cascata (es. cancellare un world non cancellava gli NPC, ora sistemato con `orphanRemoval=true` o cascade ALL).
- Introdotto `ApiError` record/class per risposte di errore omogenee (timestamp, status, message).

## Task 11 - Campaign Join Request
- Implementato flusso di join alle campaign introducendo CampaignPlayer + enum CampaignPlayerStatus con repository, DTO e CampaignPlayerService (request, approva/rifiuta, liste per campaign/utente) e nuovo controller dedicato per gli endpoint /api/campaigns/{id}/join-requests, .../approve|reject, /api/campaigns/{id}/players e /api/campaign-players/my; aggiunta ConflictException riusabile per i 409.
- Aggiornate le suite di integrazione con PlayerCharacterIntegrationTest e CampaignPlayerIntegrationTest per coprire creazione/listing/permessi, filtri di visibilita, duplicati, validazioni world, sicurezza su join-request e visibilita dei partecipanti.
- Documentazione (README, project-documentation, frontend-api-reference, devlog, Postman) aggiornata con il nuovo dominio player characters e il flusso di richiesta/approvazione campagne.

## Task 13 - PlayerCharacter 5e enrichments
- Esteso PlayerCharacter con tutti i campi chiave di una scheda D&D 5e (subclass, inspiration, proficiency bonus, salvataggi e skill proficient, iniziativa/HP temporanei/hit dice, death saves, passive scores, proficiencies & languages, attacks & spellcasting, spellcasting class/save/attack, known/prepared spells, allies/treasure, ecc.) e allineati DTO+service con nuove mappature e default coerenti.
- CampaignPlayerResponse include ora anche la subclass del PG e il service di join verifica in modo robusto la coerenza world/character; le response mantengono il filtraggio visibilita sui PG nascosti.
- Aggiornate PlayerCharacterIntegrationTest e CampaignPlayerIntegrationTest con payload 5e realistici e nuove asserzioni (inspiration, proficiencies, subclass, spell slots); introdotto un TestDatabaseCleanup centralizzato per troncare le tabelle tra i test e prevenire violazioni FK.
- Documentazione e Postman aggiornati con la descrizione ampliata delle schede PG (abilita, skill, spellcasting) e con esempi JSON arricchiti nei request/response.

## Task 14 - Player vs DM dashboards alignment
- Introdotto il flag `isPublic` sui World (entity, DTO, service e controller) con endpoint dedicati di discovery (/api/worlds/public e /api/worlds/public/{id}) accessibili a tutti gli utenti autenticati; aggiornati WorldService e i test WorldCampaignSmokeTest per coprire la visibilita pubblica.
- Rafforzate le API dei PlayerCharacter assicurando le regole di ownership/visibilita lato player e GM.
- Consolidato il flusso di join campagne aggiungendo /api/campaigns/{campaignId}/my-join-request e /api/dm/join-requests, nuove query nel repository e logica di servizio per filtrare per owner o admin; il DashboardService riusa ora il CampaignPlayerService per la vista PENDING.
- Aggiornati CampaignPlayerIntegrationTest, project-documentation, frontend-api-reference e la Postman collection per descrivere i nuovi endpoint e l'esperienza Player vs DM.

## Task 15 - PlayerCharacter creation hardening
- PlayerCharacterRequest ora richiede esplicitamente name, race, characterClass, level (>=1), armorClass, max/current HP e mantiene i limiti sugli ability score; tutti gli endpoint utilizzano @Valid cosi gli errori vengono restituiti via ApiError.
- La creazione ignora il worldId del client e lascia il PG non associato a mondi, mentre solo GM/ADMIN possono assegnare o cambiare il world tramite update; per supportare il nuovo flusso e stato introdotto un DatabaseSchemaAdjuster che rende nullable la colonna world_id in Postgres.
- Rimossa la verifica world vs campaign dal CampaignPlayerService cosi lo stesso PG puo unirsi a campagne in mondi diversi senza 400.
- Estesi PlayerCharacterIntegrationTest con scenari KO su campi mancanti/ability score fuori range, verifica del blocco world lato player e della possibilita per i GM di assegnarlo; aggiornato CampaignPlayerIntegrationTest per il nuovo comportamento cross-world e mantenuta la copertura Dashboard.
## Task 16 - NPC statblock 5e
- Estesa l'entita Npc con tutti i campi principali della scheda D&D 5e (alignment, size, creatureType, AC, HP, hit dice, speed, ability score, savingThrows/skills descrittivi, resistenze/immunita, senses, languages, challengeRating/XP/difficultyClass, traits/actions/legendary/reactions/lair/regional) mantenendoli nullable per supportare la creazione rapida dalla dashboard.
- Aggiornati NpcRequest/NpcResponse, il servizio e il controller per leggere/scrivere i nuovi campi, riusando un mapper unico che mostra le gmNotes solo a owner/GM/ADMIN e lasciando invariati i permessi; anche il DashboardService ora usa il nuovo mapper.
- Aggiunti test di integrazione per la create rapida (solo worldId+name, tutti i nuovi campi null) e per l'update completo che riempie l'intero statblock e verifica il persistere dei valori nel DB.
- Documentazione aggiornata (project-documentation, frontend-api-reference, devlog) e Postman arricchito con richieste "Create NPC Quick" e "Create NPC (Full 5e)" piu body di update con i nuovi campi, cosi il frontend puo sfruttare sia il flusso rapido sia quello completo.
## Task 17 - Session CRUD completion
- Completato il CRUD delle sessioni introducendo GET/PUT/DELETE /api/sessions/{id} nel SessionController, riusando SessionRequest/Response e mantenendo gli endpoint esistenti su /api/campaigns/{campaignId}/sessions.
- Il SessionService ora espone get/update/delete con controllo transazionale e una guardia condivisa che consente mutate solo a ROLE_ADMIN o al GM owner della campaign; createSession verifica le stesse regole e continua a popolare l'owner della sessione.
- Aggiunto SessionCrudIntegrationTest che copre happy path (get/update/delete), 404 per sessionId inesistente e 403 per GM non owner.
- Aggiornati README, project-documentation, frontend-api-reference e la Postman collection con i nuovi endpoint di dettaglio/update/delete sessione; devlog aggiornato.
## Task 18 - Session chat multilingua
- Introdotto CharacterLanguage enum e il set knownLanguages su PlayerCharacter (persistito tramite ElementCollection) con default COMMON; DTO/response/service aggiornati per leggere/scrivere le lingue e TestDatabaseCleanup aggiornata per le nuove tabelle.
- Creato dominio chat con SessionChatMessage (session, senderUser, senderCharacter opzionale, language, contentRaw, messageType, createdAt), repository, DTO e LanguageObfuscator che restituisce varianti ascii leggibili stile <<ELVISH>>/** per gli utenti che non conoscono la lingua.
- Implementato SessionChatService thread-safe con lock per sessione, verifica membership (campaign owner/admin o CampaignPlayer APPROVED), controllo lingua del PG e trasformazione runtime di contentVisible; aggiunto SessionChatController con POST/GET (parametro from).
- Aggiunto SessionChatIntegrationTest che crea mondo/campaign/session, approva due player e copre invio player/GM, obfuscazione per chi non conosce la lingua, forbiddance per non partecipanti e 400 su lingua sconosciuta; Postman/README/project-documentation/frontend-api-reference/devlog aggiornati con la nuova API e con la descrizione di visibility per-lingua.

## Task 19 - Migrazione JWT Bearer
- Aggiunta la dipendenza java-jwt, le proprieta pp.jwt.* e il servizio JwtTokenService con claim userId/email/nickname/roles e scadenza configurabile; creato JwtAuthenticationFilter registrato nella catena Spring Security.
- SecurityConfig ora disabilita HTTP Basic, imposta SessionCreationPolicy.STATELESS, usa solo Bearer token e mantiene i RestEntryPoint JSON; AuthController.login ritorna AuthResponse con token e user, mentre /api/auth/me e gli altri endpoint usano il token.
- Creato TestAuthenticationHelper per generare token nei test, sostituendo tutti gli httpBasic con Bearer; aggiornati AuthIntegrationTest (nuove asserzioni sul token), tutte le suite d'integrazione, README, documentazione (project/frontend), devlog e Postman per descrivere il flusso JWT.

## Task 20 - File Upload and Security Refinements
- Introdotta l'entita `SessionResource` (session, fileName, fileUrl, type, size, uploadedAt) con relativo repository e service per gestire il caricamento di file (immagini, PDF, etc.) nelle sessioni.
- Implementato `SessionResourceController` con endpoint di upload (multipart 10MB) e download; aggiornata `GlobalExceptionHandler` per gestire `MaxUploadSizeExceededException`.
- Migliorata la sicurezza del download: introdotto il supporto al query param `?token=` nel `JwtAuthenticationFilter` per consentire l'apertura sicura dei file in nuove tab del browser (dove l'header Authorization non e possibile).
- Risolti problemi di serializzazione JSON tramite `SessionResourceDTO` per evitare ricorsioni infinite con l'entita `Session`.
- Aggiornata la documentazione (project-documentation, frontend-api-reference e TODO) per riflettere le nuove funzionalita di gestione risorse e i requisiti di autenticazione per i download diretti.

## Task 21 - Documento di riepilogo flussi e API
- Aggiunto `docs/flussi-e-api-attuali.md` come documento di ricognizione rapida del backend, basato sui controller e sulla `SecurityConfig` realmente presenti nel codice.
- Riassunti i flussi principali applicativi: autenticazione JWT, journey GM, journey player, dashboard, join request, chat, session resources.
- Elencati tutti gli endpoint esposti oggi dal backend con metodo, path, livello di accesso e descrizione sintetica, incluse le utility (`/api/dice/**`, `/api/health`, `/api/debug/me`).
- Evidenziata la differenza tra linee guida storiche e implementazione attuale: il progetto oggi e stateless JWT Bearer, non HTTP Basic.

## Task 22 - Allineamento documentazione post-merge
- Riallineati `readme.md`, `docs/project-documentation.md`, `docs/frontend-api-reference.md` e `docs/flussi-e-api-attuali.md` allo stato attuale del codice dopo il merge delle feature JWT, chat privata, upload risorse e fix sulle lingue.
- Documentate le regole aggiornate della chat: whisper tramite `recipientUserId`, fallback a `COMMON` per lingue non riconosciute e vincolo che il DM puo usare solo personaggi che possiede.
- Consolidato il catalogo endpoint e le note per frontend/mobile, incluse CORS permissiva in sviluppo, download file con `?token=` e gli status completi del flusso `CampaignPlayer`.

## Task 23 - Ripristino endpoint scheda personaggio
- Ripristinati nel backend refactorato i mapping mancanti su `PlayerCharacterController` per `PATCH /api/characters/{id}/hp`, `/death-saves`, `/inventory`, `/spell-slots` e `POST /api/characters/{id}/long-rest`, senza toccare gli endpoint gia esistenti.
- Reintrodotti solo i DTO strettamente necessari (`UpdateHitPointsRequest`, `UpdateDeathSavesRequest`, `UpdateInventoryRequest`, `UpdateSpellSlotsRequest`) e i metodi di `PlayerCharacterService` che aggiornano i campi della scheda usando i controlli permesso gia presenti.
- Aggiunto un test d'integrazione che verifica l'intero flusso della scheda personaggio, incluso il long rest con reset HP/death saves e ripristino best-effort degli spell slot nel formato `current/max`.

## Task 24 - Configurazione parametrica e Docker hardening
- Parametrizzato `src/main/resources/application.yml` con fallback via variabili ambiente per porta server, datasource PostgreSQL, secret JWT, scadenza token e directory upload, mantenendo i default di sviluppo locali.
- Aggiornato `LocalFileStorageService` per leggere `app.upload.dir` da configurazione invece di usare `uploads` hardcoded, preservando `uploads` come fallback implicito.
- Aggiunti `Dockerfile` multi-stage basato su Temurin 17 Alpine e `.dockerignore`, con runtime non-root, jar copiato come `app.jar` ed esclusione di artefatti locali dal build context.
