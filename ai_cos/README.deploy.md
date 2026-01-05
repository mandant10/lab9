# ai_cos — deploy na Render + Cloudflare (analogicznie do lab8)

## Render (backend Java)
- Repo: ten sam GitHub, folder: `ai_cos/`
- Render → **New Web Service** → wybierz repo
- **Root Directory**: `ai_cos`
- **Build Command**: `./gradlew build -x test`
- **Start Command**: `java -jar build/libs/ai_cos-0.0.1-SNAPSHOT.jar`
- **Environment Variables**:
  - `GEMINI_API_KEY` (wymagane)
  - opcjonalnie: `GEMINI_MODEL`, `GEMINI_TEMPERATURE`, `GEMINI_INSTRUCTIONS`

Uwaga: aplikacja czyta port z `PORT` (`server.port=${PORT:8080}`), więc Render zadziała bez dodatkowych zmian.

## Cloudflare (UI + proxy /api/* → Render)
W folderze `ai_cos/` są pliki:
- `wrangler.toml`
- `wrangler.jsonc`
- `cf/worker.ts`
- `ui/index.html`

Kroki:
1. Ustaw `BACKEND_URL` w Cloudflare Worker Variables na URL z Render (np. `https://twoj-serwis.onrender.com`).
2. Deploy workera z folderu `ai_cos/`:
   - `npm install`
   - `npm run deploy`

Efekt:
- Cloudflare serwuje UI z `ui/index.html`
- wywołania `/api/*` idą do backendu na Render

## Bezpieczeństwo
- Klucz Gemini został usunięty z `application.properties`.
- Trzymaj `GEMINI_API_KEY` tylko w zmiennych środowiskowych Render.
