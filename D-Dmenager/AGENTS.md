# Repository Guidelines

## 1. Panoramica del progetto

- Applicazione backend Spring Boot 3 / Java 17.
- Root package: `it.univ.ddmanager`.
- Dominio: gestione di mondi e campagne per GdR (D&D-like):
    - User con ruoli: `ROLE_ADMIN`, `ROLE_GM`, `ROLE_PLAYER`, `ROLE_VIEWER`.
    - World: mondo di gioco, ha un owner (User).
    - Campaign: campagna associata a un World, con owner (User) e stato.
    - Session: sessione di gioco associata a una Campaign.
- Solo API REST (nessun frontend), autenticazione tramite HTTP Basic.

---

## 2. Struttura dei package

Sotto `it.univ.ddmanager` usare una struttura domain-driven:

- `user`
    - `model` – `User`, `Role`
    - `dto` – `RegisterRequest`, `LoginRequest`, `UserResponse`, ecc.
    - `repository` – `UserRepository`, `RoleRepository`
    - `service` – `UserService`, `CurrentUserService`
    - `web` – `AuthController`, altri controller legati agli utenti
- `world` – entity, dto, repository, service, web per i World
- `campaign` – idem per Campaign
- `session` – idem per Session
- `security` – `SecurityConfig`, `UserPrincipal`, `UserDetailsService`, ecc.
- `error` / `common` – `ApiError`, `GlobalExceptionHandler`, eccezioni condivise
- `config` – `DataInitializer`, altre configurazioni

Regola:

- Controller in `*/controller` (al momento in web(sbagliato) -> da aggiornare)
- Service in `*/service`
- Repository in `*/repository`
- DTO in `*/dto`

Le eccezioni riusabili vanno sotto `error` o `common`, non sparse in giro.

---

## 3. Build, run & test

Comandi principali:

- `mvn clean package` → build del jar.
- `mvn test` → esegue i test JUnit/MockMvc (usa Postgres reale).
- `mvn spring-boot:run` → avvia l’API su `http://localhost:8080`.

Configurazione DB di sviluppo (predefinita):

- URL: `jdbc:postgresql://localhost:5432/dd_manager`
- username: `ddmanager`
- password: `ddmanager`

Per usare una porta diversa:

- `mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"`

I test usano un Postgres reale (no H2 in memoria); usare un DB locale dedicato.

---

## 4. Convenzioni di codifica

- Linguaggio: Java 17.
- Framework: Spring Boot 3 + Spring Security.
- Lombok per ridurre boilerplate:
    - Entity: annotazioni tipo `@Getter`, `@Setter` o `@Data`, più `@NoArgsConstructor`, `@AllArgsConstructor`.
    - DTO di risposta: spesso `@Builder`.
- Iniezione delle dipendenze:
    - usare **constructor injection** (es. `@RequiredArgsConstructor`), non field injection.
- Controller:
    - devono essere sottili: validazione input, mapping DTO/Response, delega ai service.
- Naming:
    - DTO: suffisso `Request` / `Response`.
    - Entity: nome al singolare (`World`, `Campaign`, `Session`).
    - Repository: `<Entity>Repository` estende `JpaRepository<Entity, Long>`.
- Validazione:
    - usare Jakarta (`jakarta.validation.*`, es. `@NotBlank`, `@Email`, `@Size`).
    - errori di validazione gestiti centralmente dal `GlobalExceptionHandler`.

Usare sempre package Jakarta (`jakarta.persistence.*`, `jakarta.validation.*`), non quelli legacy `javax`.

---

## 5. Sicurezza e regole di dominio

### 5.1 Autenticazione & ruoli

- Autenticazione: HTTP Basic.
    - username = email dell’utente.
    - password = hash BCrypt salvato in DB.
- `SecurityConfig` deve:
    - disabilitare CSRF per le API REST.
    - permettere senza autenticazione:
        - `POST /api/auth/register`
        - `POST /api/auth/login`
    - richiedere autenticazione per tutte le altre API.
    - applicare controlli di ruolo dove serve:
        - `POST /api/worlds` → consentito solo a `ROLE_GM` o `ROLE_ADMIN`.
        - `POST /api/campaigns` → consentito solo a `ROLE_GM` o `ROLE_ADMIN`.
        - `POST /api/campaigns/{campaignId}/sessions` → consentito solo a `ROLE_GM` o `ROLE_ADMIN`.

UserDetailsService:

- deve caricare l’utente tramite `UserRepository.findByEmail(email)` e convertirlo in `UserPrincipal`.

### 5.2 Registrazione utente

Endpoint:

- `POST /api/auth/register`

Richiesta (`RegisterRequest`):

- `email`
- `password`
- `nickname`
- `role` ∈ {`PLAYER`, `DM`, `VIEWER`} (case insensitive)

Regole di business:

- Se l’email è già in uso → lanciare `EmailAlreadyUsedException("Email already in use")` → HTTP 409.
- Password deve essere codificata con `BCryptPasswordEncoder`.
- Nickname copiato direttamente dal DTO.
- Il campo `role` rappresenta la scelta dell’utente nella select del sito (PLAYER, DM, VIEWER) e va mappato nei ruoli applicativi:

    - `PLAYER` → assegna `ROLE_PLAYER`.
    - `DM` → assegna almeno `ROLE_GM` (eventualmente anche `ROLE_PLAYER` se il resto del codice lo richiede).
    - `VIEWER` → assegna `ROLE_VIEWER`.

- Se `role` ha un valore diverso da PLAYER/DM/VIEWER (dopo normalizzazione case-insensitive):
    - lanciare `InvalidRoleException("Invalid role: " + value)` → HTTP 400.

I ruoli vanno recuperati con `RoleRepository` (es. `findByName("ROLE_GM")`) e aggiunti alla collection di ruoli dell’utente.

### 5.3 Login & utente corrente

Endpoint login:

- `POST /api/auth/login`

Richiesta (`LoginRequest`):

- `email`
- `password`

Regole:

- `UserService.authenticate(LoginRequest)` deve:
    - cercare l’utente per email,
    - se non trovato → `ResourceNotFoundException` → 404,
    - se la password non coincide (`passwordEncoder.matches`) → lanciare `InvalidCredentialsException("Invalid credentials")` → HTTP 401,
    - se tutto ok → ritornare l’entità `User`.

- `AuthController.login`:
    - chiama `userService.authenticate`,
    - mappa `User` in `UserResponse`,
    - ritorna HTTP 200 con `UserResponse`.

Endpoint utente corrente:

- `GET /api/auth/me`
    - richiede HTTP Basic.
    - deve usare il principal o `CurrentUserService` per ottenere l’utente corrente.
    - ritorna `UserResponse` (id, email, nickname, ruoli).
    - se non autenticato → 401.

### 5.4 World, Campaign, Session

Regole generali:

- Ogni entità creata via API che ha un owner deve utilizzare l’utente autenticato come proprietario:
    - i service devono usare `CurrentUserService.getCurrentUserOrThrow()`.

World:

- `GET /api/worlds` → lista di tutti i mondi disponibili.
- `GET /api/worlds/{id}` → dettaglio di un singolo mondo.
- `POST /api/worlds` → crea un mondo, visibile solo a ruoli autorizzati (GM/ADMIN).
- DTO di risposta per `GET /api/worlds` deve includere almeno:
    - `id`
    - `name`
    - `ownerId`
    - `ownerNickname`
    - `description`
    - `campaignCount` (numero di campagne associate a quel mondo).
    - `data` di creazione/aggiornamento opzionali.

Campaign:

- Ogni Campaign è legata a un World (campo `worldId`).
- I nomi delle campaign NON devono essere unici globalmente ma nello stesso mondo si: si può creare più di una campagna con lo stesso `name` ma non nello stesso mondo.
- Endpoint:
    - `GET /api/campaigns`
    - `GET /api/campaigns/{id}`
    - `GET /api/campaigns/world/{worldId}`
    - `POST /api/campaigns` → crea una campaign per uno specifico world, stato di default `ACTIVE`.

Session:

- Ogni Session è legata a una Campaign.
- Endpoint:
    - `GET /api/campaigns/{campaignId}/sessions`
    - `POST /api/campaigns/{campaignId}/sessions` → crea una sessione per la campagna.

---

## 6. Gestione errori

Tutti gli errori devono essere restituiti come JSON usando `ApiError`, con almeno:

- `timestamp`
- `status` (codice HTTP)
- `error` (stringa tipo "Bad Request", "Unauthorized", ecc.)
- `message` (breve spiegazione)
- `path` (URI della richiesta)

`GlobalExceptionHandler` deve mappare:

- `InvalidCredentialsException` → 401 Unauthorized
- `EmailAlreadyUsedException` → 409 Conflict
- `InvalidRoleException` → 400 Bad Request
- `UnauthorizedException` (utente non autenticato in `CurrentUserService`) → 401 Unauthorized
- `AccessDeniedException` (o handler equivalente di Spring Security) → 403 Forbidden
- `ResourceNotFoundException` (world/campaign/session/user non trovato) → 404 Not Found
- `MethodArgumentNotValidException` (validazione DTO) → 400 Bad Request con elenco errori di campo
- Eccezione generica (`Exception`) → 500 Internal Server Error con messaggio generico tipo "Unexpected error"

Le eccezioni vanno loggate lato server con livello adeguato (`error` per 500, `warn` o `info` per errori attesi).

---

## 7. Testing

- I test stanno in `src/test/java/it/univ/ddmanager`, rispecchiando i package del codice.
- Usare JUnit 5 con:
    - `@SpringBootTest`
    - `@AutoConfigureMockMvc`
    - `spring-security-test` per `httpBasic(...)`.
- I test usano il DB Postgres configurato in `application.yml` (`replace = NONE`).
- È consigliato usare `deleteAll()` sui repository rilevanti in `@BeforeEach` per avere un DB pulito.

Test chiave da mantenere/estendere:

1. **AuthIntegrationTest** (nome indicativo)
    - Registrazione:
        - crea utente con ruolo PLAYER → in DB ha `ROLE_PLAYER`.
        - crea utente con ruolo DM → in DB ha `ROLE_GM` (ed eventuali altri ruoli coerenti).
        - crea utente con ruolo VIEWER → in DB ha `ROLE_VIEWER`.
        - email duplicata → 409 con `ApiError`.
        - ruolo non valido → 400 con `ApiError`.
    - Login:
        - credenziali corrette → 200 con `UserResponse`.
        - credenziali errate → 401 con `ApiError`.
        - email non esistente → 404 con `ApiError`.
    - `/api/auth/me`:
        - con auth → 200 con i dati giusti.
        - senza auth → 401.

2. **World/Campaign/Session tests** (smoke/integration)
    - GM può:
        - creare World,
        - creare Campaign su World,
        - creare Session su Campaign.
    - PLAYER/VIEWER non possono fare operazioni di create protette (403).
    - Endpoint protetti senza Authorization → 401.

---

## 8. Git & commit

- Messaggi di commit brevi e in forma imperativa:
    - `Add role selection to registration`
    - `Fix auth error handling`
    - `Add campaign sessions tests`
- Prima di un commit importante:
    - eseguire `mvn clean package`
    - eseguire `mvn test`
- Se una modifica cambia API (path, query, body), aggiornare:
    - eventuale documentazione (README, Postman Collection),
    - test di integrazione relativi.

---

## 9. Linee guida per modifiche future (per l’agent)

Per ogni nuova task:

1. Leggi e rispetta **tutte** le regole di questo file.
2. Se nel progetto corrente c'è qualche differenza rispetto a queste linee guida, segui sempre le regole indicate in questo file quindi aggiorna il progetto.
3. Prima di modificare, controlla sempre lo stato attuale del codice.
4. Applica la **minima modifica coerente** per:
    - implementare la nuova feature,
    - correggere il bug,
    - mantenere la coerenza di sicurezza, error handling e test.
5. aggiorna la documentazione README.md se cambi API o ne aggiungi di nuove.
6. aggiorna il file devlog.md con un breve riassunto della task completata.
7. Quando tocchi un comportamento (controller/service/security/error), aggiorna o aggiungi i test corrispondenti.
8. Restituisci sempre i file completi che modifichi (non solo snippet).
9. Dopo le modifiche il progetto deve:
    - compilare con `mvn clean package`,
    - avere una suite di test aggiornata e consistente con le nuove regole.
