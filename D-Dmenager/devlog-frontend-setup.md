## Frontend toolchain setup
Data: 2025-12-04 12:19

Node.js: v22.15.0
npm: 10.9.2
npx: 10.9.2

Note: toolchain gia presente; npm/npx eseguiti via cmd per policy PowerShell. Verificato comando npm create vite@latest -- --help.

## Backend CORS setup
Data: 2025-12-04 13:00

- Aggiunta `CorsConfig` con `WebMvcConfigurer` per consentire `http://localhost:5173` e propagare l'header `Authorization` per i token JWT.
- Aggiornata `SecurityConfig` per abilitare `http.cors()` prima della configurazione esistente.
- Verificato che il frontend Vue in sviluppo puo chiamare le API del backend senza errori CORS.
