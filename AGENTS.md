# Pulse Log Agent Guide

## Project

Pulse Log is an Android app for daily blood pressure, weight, and note logging.

Stack:

- Kotlin
- Jetpack Compose
- Material 3
- Room
- Gradle Kotlin DSL

## Working Rules

- Read `docs/harness-engineering-design.md` before structural changes.
- Keep changes scoped to the requested feature or harness phase.
- Prefer testable boundaries over direct Android or system calls.
- Do not add new app behavior without a corresponding harness plan or test path.
- Do not remove user data paths or migration code without an explicit migration strategy.

## Harness Priorities

Work in this order unless the user asks otherwise:

1. Make local Gradle checks executable.
2. Extract pure policies into testable units.
3. Introduce `ClockProvider`.
4. Introduce `HealthRepository` as an interface.
5. Add fake implementations for ViewModel tests.
6. Add `NotificationScheduler` before real OS notification work.
7. Add Compose `testTag` values before UI tests.
8. Replace destructive Room migration with migration tests.

## Test Expectations

For logic changes:

- Add or update unit tests under `app/src/test`.
- Prefer pure policy tests for validation, calendar, and graph rules.

For data changes:

- Add or update Room tests under `app/src/androidTest`.
- Preserve existing user data unless a migration plan says otherwise.

For UI changes:

- Add stable `Modifier.testTag()` values for important controls.
- Prefer testing by tags and semantics instead of fragile text matching.

## Local Checks

Use these scripts from the repository root:

```powershell
.\scripts\check.ps1
```

or:

```bash
bash ./scripts/check.sh
```

Before running Gradle checks, ensure `JAVA_HOME` points to a valid JDK and `java` is on `PATH`.

## Important Documents

- `docs/harness-engineering-design.md`
- `docs/development-todo.md`
- `docs/issues.md`
- `docs/design.md`
- `docs/requirements.md`
- `docs/open-questions.md`

## Current Known Gaps

- No real OS notification scheduling yet.
- Room still needs a non-destructive migration strategy.
- Existing tests are sample tests only.
- Local Gradle checks currently require Java environment setup.
