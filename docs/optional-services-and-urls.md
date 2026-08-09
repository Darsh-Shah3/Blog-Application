# Optional services & public URLs

How to **turn off** CAPTCHA, Google OAuth, and SMTP when you want a simpler stack, and how to **point Threadly at new URLs** after you deploy (or move) the site.

All settings live in **`.env`** (copy from `.env.example`). Never commit real secrets.

After almost every `.env` change below, **recreate the containers that read those variables** so the process reloads config:

```bash
# From the repo root (where docker-compose.yml and .env live)
docker compose up -d --force-recreate user-auth-service

# If you also changed public API/frontend URLs (see §4), rebuild frontend too:
docker compose up -d --build --force-recreate frontend user-auth-service api-gateway
```

---

## 1. Signup CAPTCHA (Google reCAPTCHA v2)

Used on **email/password register** only. Protects against automated bulk signups.

| Goal | What to set in `.env` |
|------|------------------------|
| **Enable** | Both `RECAPTCHA_SITE_KEY` and `RECAPTCHA_SECRET_KEY` filled (from [reCAPTCHA admin](https://www.google.com/recaptcha/admin), **v2 checkbox**). Domains must include `localhost` and/or your public host. |
| **Disable** | Clear both values (leave empty): |
| | `RECAPTCHA_SITE_KEY=` |
| | `RECAPTCHA_SECRET_KEY=` |

When both are empty:

- Signup works **without** a CAPTCHA widget.
- `GET /api/v1/auth/captcha-config` returns `"enabled": false`.

When both are set:

- Server **rejects** signup without a valid token (bots cannot skip the UI).
- Register page shows “I’m not a robot” after the consent step.

Apply:

```bash
docker compose up -d --force-recreate user-auth-service
```

Frontend loads the site key from the API, so a frontend rebuild is **not** required just to toggle CAPTCHA.

Check:

```bash
curl -s http://localhost:8080/api/v1/auth/captcha-config
# enabled: true  + siteKey present  → CAPTCHA on
# enabled: false                    → CAPTCHA off
```

---

## 2. Google OAuth (“Continue with Google”)

Optional social login/signup. Email/password still works either way.

| Goal | What to set in `.env` |
|------|------------------------|
| **Enable** | `OAUTH_GOOGLE_CLIENT_ID` and `OAUTH_GOOGLE_CLIENT_SECRET` from Google Cloud → APIs & Services → Credentials → OAuth 2.0 **Web** client. |
| | `OAUTH_REDIRECT_BASE_URL` = the public origin of the **frontend** (no trailing slash), e.g. `http://localhost:3000` or `https://threadly.example.com`. |
| **Disable** | Clear client id and secret: |
| | `OAUTH_GOOGLE_CLIENT_ID=` |
| | `OAUTH_GOOGLE_CLIENT_SECRET=` |

When disabled, OAuth start returns unavailable and the UI treats Google sign-in as not configured.

### Google Console checklist (when enabled)

For every frontend URL users actually use, add:

| Console field | Value |
|---------------|--------|
| Authorized JavaScript origins | `{OAUTH_REDIRECT_BASE_URL}` e.g. `http://localhost:3000` or `http://YOUR_IP:3000` |
| Authorized redirect URIs | `{OAUTH_REDIRECT_BASE_URL}/oauth/callback/google` |

If you only change deploy URL and forget the console, Google sign-in will fail even with correct `.env`.

Apply:

```bash
docker compose up -d --force-recreate user-auth-service
```

---

## 3. SMTP (password-reset email)

Sends password-reset mail when someone uses **Forgot password**. If SMTP is off, the API still responds safely (no account enumeration), but **no email is sent**.

| Goal | What to set in `.env` |
|------|------------------------|
| **Enable** | `MAIL_ENABLED=true` plus real host credentials: |
| | `MAIL_HOST`, `MAIL_PORT` (usually `587`), `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` |
| | Keep `MAIL_SMTP_AUTH=true` and `MAIL_SMTP_STARTTLS=true` for typical Gmail app passwords. |
| **Disable** | `MAIL_ENABLED=false` |
| | You can leave host/user/password filled; they are **ignored** while `MAIL_ENABLED=false`. |

Related:

- `FRONTEND_BASE_URL` is used to build the **reset link** in the email. It must match the URL users open in the browser.
- `MAIL_RESET_TOKEN_MINUTES` — how long a reset token stays valid (default often `30`).

Apply:

```bash
docker compose up -d --force-recreate user-auth-service
```

(Other services use their own mail settings only if present; password reset is handled by **user-auth-service**.)

---

## 4. Changing URLs after deploy

Whenever the site is reachable under a **new IP, domain, or scheme** (HTTP → HTTPS), update `.env` and Google consoles for OAuth/CAPTCHA.

### 4.1 Variables that must match the public site

| Variable | Who uses it | Example after deploy |
|----------|-------------|----------------------|
| `FRONTEND_BASE_URL` | Auth (OAuth base, password-reset links, some redirects) | `http://203.0.113.10:3000` or `https://app.example.com` |
| `OAUTH_REDIRECT_BASE_URL` | Auth OAuth redirect; usually **same as** frontend base | Same as `FRONTEND_BASE_URL` |
| `NEXT_PUBLIC_API_URL` | **Browser** → API gateway | `http://203.0.113.10:8080` or `https://api.example.com` |
| `NEXT_PUBLIC_WS_URL` | WebSocket (optional). Leave empty to derive from API URL when the app supports that. | `ws://203.0.113.10:8080` or `wss://api.example.com` |
| `NEXT_PUBLIC_GRAFANA_URL` / `NEXT_PUBLIC_PROMETHEUS_URL` | Optional ops links only | New host:port if you expose them |

Rules:

- No trailing slash.
- Use the same host users type in the address bar (IP vs domain must match what you put in Google consoles).
- `NEXT_PUBLIC_*` values are **baked into the frontend image at build time**. Changing them requires **`--build` frontend**, not only recreate.

### 4.2 Apply URL changes (recommended sequence)

1. Edit `.env` on the server with new bases/API URLs.
2. Rebuild & recreate consumers of those vars:

```bash
docker compose up -d --build --force-recreate frontend user-auth-service api-gateway
```

3. **Google OAuth** (if still enabled): add the new origin + redirect URI (see §2). You can keep old localhost entries for local dev.
4. **reCAPTCHA** (if still enabled): in reCAPTCHA admin → your key → **Domains**, add the new hostname/IP. Keep `localhost` if you still develop locally.
5. Smoke test:
   - Open the new frontend URL → browse feed.
   - Sign up (CAPTCHA if on).
   - Google sign-in (if on).
   - Forgot password (if SMTP on) → link in email should use `FRONTEND_BASE_URL`.

### 4.3 Quick reference: which service after which change

| You changed… | Recreate / rebuild |
|--------------|--------------------|
| CAPTCHA keys only | `user-auth-service` (force-recreate) |
| OAuth id/secret or `OAUTH_REDIRECT_BASE_URL` | `user-auth-service` |
| SMTP / `MAIL_*` / `FRONTEND_BASE_URL` (for reset links) | `user-auth-service` |
| `NEXT_PUBLIC_API_URL` or other `NEXT_PUBLIC_*` | **`frontend` with `--build`** |
| Gateway public host only (same path routes) | Usually none of the Java services; users just call the new URL. Frontend `NEXT_PUBLIC_API_URL` must still point at that gateway. |

---

## 5. Minimal local stack (everything optional off)

Useful for coding without Google/email:

```env
RECAPTCHA_SITE_KEY=
RECAPTCHA_SECRET_KEY=

OAUTH_GOOGLE_CLIENT_ID=
OAUTH_GOOGLE_CLIENT_SECRET=

MAIL_ENABLED=false

FRONTEND_BASE_URL=http://localhost:3000
OAUTH_REDIRECT_BASE_URL=http://localhost:3000
NEXT_PUBLIC_API_URL=http://localhost:8080
```

Then:

```bash
docker compose up -d --force-recreate user-auth-service
# Rebuild frontend only if you changed NEXT_PUBLIC_* from a previous deploy value
```

You still get email/password signup, login, and the rest of the platform.

---

## 6. Full production-style enable checklist

```env
# Public URLs (replace with real values)
FRONTEND_BASE_URL=https://your-host.example
OAUTH_REDIRECT_BASE_URL=https://your-host.example
NEXT_PUBLIC_API_URL=https://api.your-host.example

# CAPTCHA
RECAPTCHA_SITE_KEY=...
RECAPTCHA_SECRET_KEY=...

# OAuth
OAUTH_GOOGLE_CLIENT_ID=...
OAUTH_GOOGLE_CLIENT_SECRET=...

# SMTP
MAIL_ENABLED=true
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=...
MAIL_PASSWORD=...
MAIL_FROM=noreply@your-host.example
```

```bash
docker compose up -d --build --force-recreate frontend user-auth-service api-gateway
```

Update Google OAuth and reCAPTCHA domain lists to match **your-host.example**.

---

## Related

- Template variables: [`.env.example`](../.env.example)
- Free-tier / VM deploy: [deploy.md](./deploy.md)
- Security notes: [security.md](./security.md)
