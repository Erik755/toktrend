# TokTrend

TokTrend is an AI-assisted TikTok automation project. This repository must be portable: it should run locally, in GitHub Codespaces, in Docker, and on public hosts such as Railway or Render without depending on files from a specific computer.

## Public pages

GitHub Pages static landing page:

https://erik755.github.io/toktrend/

Railway production URL, when the backend is deployed:

https://function-bun-production-ea34.up.railway.app

## Requirements

- Node.js 20 or newer
- npm
- Docker, optional
- TikTok Developer credentials, only for real TikTok OAuth/upload
- OpenAI API key or another configured AI provider, only for real AI generation

## Local setup

```bash
git clone https://github.com/Erik755/toktrend.git
cd toktrend
cp .env.example .env
npm install
npm run check
npm start
```

Open:

```text
http://127.0.0.1:8789/
http://127.0.0.1:8789/health
```

## Railway setup

Use these settings in Railway:

```text
Build command: npm install
Start command: npm start
```

Required variables:

```env
NODE_ENV=production
APP_BASE_URL=https://function-bun-production-ea34.up.railway.app
FRONTEND_URL=https://function-bun-production-ea34.up.railway.app
BACKEND_URL=https://function-bun-production-ea34.up.railway.app
TIKTOK_CLIENT_KEY=
TIKTOK_CLIENT_SECRET=
TIKTOK_REDIRECT_URI=https://function-bun-production-ea34.up.railway.app/api/tiktok/callback
OPENAI_API_KEY=
SESSION_SECRET=replace_with_a_long_random_value
DATABASE_URL=
```

Important: Railway assigns `PORT` automatically. The app must listen on `process.env.PORT` and host `0.0.0.0`.

## Docker

```bash
cp .env.example .env
docker compose up --build
```

## TikTok Developers

Register the exact redirect URI used by the deployed app:

```text
https://function-bun-production-ea34.up.railway.app/api/tiktok/callback
```

For local development:

```text
http://127.0.0.1:8789/api/tiktok/callback
```

## Security rules

- Never commit `.env`.
- Never commit real API keys, tokens, passwords, cookies, or OAuth secrets.
- Keep secrets in Railway/Render environment variables.
- `TIKTOK_CLIENT_SECRET` must never be exposed to frontend code.

## Healthcheck

The backend exposes:

```text
GET /health
```

Expected response:

```json
{
  "status": "ok",
  "app": "TokTrend",
  "env": "production"
}
```

## GitHub setup

Automatic setup, if GitHub CLI is installed:

```bash
gh auth login
npm run github:setup
```

Manual setup:

```bash
git init
git add .
git commit -m "Initial TokTrend portable setup"
git branch -M master
git remote add origin https://github.com/Erik755/toktrend.git
git push -u origin master
```

## Project status

This repository has been corrected away from the default AI Studio README and toward the TokTrend deployment plan. See `CODEX_REPORT.md` for the technical report.