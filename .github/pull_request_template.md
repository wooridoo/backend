## Summary

- what changed:
- why:

## Validation

- [ ] `git rev-parse --short HEAD` matches `/internal/runtime/info.gitCommit`
- [ ] `scripts/dev/dev-restart.cmd` executed successfully
- [ ] `python backend/scripts/dev/upload-smoke.py` passed
- [ ] global error-code mapping regression tests passed (`GlobalExceptionHandlerTest`, `InternalRuntimeControllerTest`)
- [ ] backend build (`gradlew.bat compileJava`) passed
- [ ] frontend build (`npm run build`) passed (if frontend was touched)

## Notes

- rollout/ops note:
- known limitations:
