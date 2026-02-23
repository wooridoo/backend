# Local Dev Run

## Standard start/stop/restart

Windows (recommended):

```cmd
scripts\dev\dev-up.cmd
scripts\dev\dev-down.cmd
scripts\dev\dev-restart.cmd
```

First-time Django setup helper:

```cmd
scripts\dev\dev-up.cmd -AutoCreateVenv -AutoInstallDependencies
```

## What the scripts do

`dev-up`:
- checks ports `8000` (Django), `8080` (Spring)
- validates `backend/django/.venv` and Python version (`backend/django/.python-version`)
- optionally installs Django dependencies
- starts Django (`127.0.0.1:8000`) and Spring (`bootRun --no-daemon`)
- verifies Spring health using `GET /challenges?page=1&size=1`

`dev-down`:
- stops processes from PID files first
- falls back to port-based stop
- runs `gradlew --stop`

`dev-restart`:
- executes `dev-down` then `dev-up`

## Runtime identification endpoint

The endpoint is disabled by default.
Enable only when needed:
```cmd
set INTERNAL_RUNTIME_ENABLED=true
set DJANGO_INTERNAL_API_KEY=<your-internal-key>
```

```bash
curl -H "X-Internal-Api-Key: <DJANGO_INTERNAL_API_KEY>" \
  http://localhost:8080/internal/runtime/info
```

Use this to confirm current runtime commit/build and upload policy.

## Upload smoke test

```bash
python backend/scripts/dev/upload-smoke.py
```

Outputs are written under `.tmp/verify-*/`.
