# Runtime Consistency Runbook

## Why this exists

Image upload endpoints can appear broken when an old Spring runtime is still bound to port `8080`.
This can look like a code regression even when `develop` source is already correct.

## Symptom -> likely cause

| Symptom | Likely cause | Check |
|---|---|---|
| `POST /challenges/{id}/posts/images` returns `500` | stale Spring process | compare runtime commit via `/internal/runtime/info` |
| upload size errors return old code text like `POST_006` | stale runtime from old classpath | restart with `scripts/dev/dev-restart.cmd` |
| ledger or brix-related routes fail only | Django not running on `127.0.0.1:8000` | check port `8000` and Django log |

## Immediate checks

1. Runtime info
`/internal/runtime/info` is disabled by default.  
Enable it only when needed:
```cmd
set INTERNAL_RUNTIME_ENABLED=true
set DJANGO_INTERNAL_API_KEY=<your-internal-key>
```

Then call:
```bash
curl -H "X-Internal-Api-Key: <DJANGO_INTERNAL_API_KEY>" http://localhost:8080/internal/runtime/info
```

2. Compare commit
```bash
git -C backend rev-parse --short HEAD
```
`runtime.gitCommit` and local HEAD should match.

3. Port ownership
```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen
Get-NetTCPConnection -LocalPort 8000 -State Listen
```

## Standard recovery

1. Stop both servers
```cmd
scripts\dev\dev-down.cmd
```

2. Restart both servers
```cmd
scripts\dev\dev-restart.cmd
```

Optional first-time setup:
```cmd
scripts\dev\dev-restart.cmd -AutoCreateVenv -AutoInstallDependencies
```

## Smoke validation

```bash
python backend/scripts/dev/upload-smoke.py
```

Expected key checks:
- post image upload 1 file success
- 11 files -> `IMAGE_004`
- oversize -> `IMAGE_002`
- non-member -> `MEMBER_001`
- bad resolution for banner/thumbnail/profile -> `IMAGE_003`

## Logs and PID files

Location: `.tmp/dev-runtime/`

- `spring.log`, `django.log`
- `spring.pid`, `django.pid`

## Quick triage checklist

1. Process up check:
- `Get-NetTCPConnection -LocalPort 8080,8000 -State Listen`
2. HTTP ready check:
- `GET /challenges?page=1&size=1` must return `200`
3. Contract check:
- upload failure codes should be `IMAGE_*`, not legacy `POST_00x`
