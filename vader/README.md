# Vader

Phoenix service for the cabinet app and the Next.js desk in `../skywalker`.

The cabinet keeps calling the same paths (`/api/app/v1/goods/getGoodsList`, Paystack initialize and verify, delivery, `/api/app/v2/appLastedVersion`).

## Run

Configuration comes from the environment. Copy `.env.example` to `.env` and fill in what this machine needs. Development falls back to the OS user, Postgres on localhost port 5432, and database `vader_dev` when those variables are unset. `PORT` defaults to 8080. The process logs to standard output and keeps no session data of its own.

```bash
cd vader
mix ecto.setup
mix phx.server
```

Listens on every interface, so a cabinet on the same network can reach this computer. Point the kiosk server field at `http://<this-lan-ip>:8080`.

A production build is a release, not a Mix checkout:

```bash
MIX_ENV=prod mix release
_build/prod/rel/vader/bin/migrate
_build/prod/rel/vader/bin/seed
SECRET_KEY_BASE=... DATABASE_URL=ecto://USER:PASS@HOST/DATABASE PHX_SERVER=true _build/prod/rel/vader/bin/server
```

`migrate` and `seed` are one-off commands. They exit when finished. `server` is the only long-running process. `docker compose up` builds that same release and attaches Postgres as `DATABASE_URL`.

```bash
cd skywalker
npm run dev
```

Open http://127.0.0.1:3000. The seeded operator is `operator@localhost` / `change-me` unless `ADMIN_EMAIL` and `ADMIN_PASSWORD` are set before the seed. Set `PAYSTACK_SECRET_KEY` for live charges. Without it, checkout stays in demo mode. Set `PUBLIC_BASE_URL` to the address a phone can open, the same way as the Python server.

`o.sinsivm.com` is the vendor console. This API does not log into it.
