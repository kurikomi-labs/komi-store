# AGENTS.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

Komi Store is a cross-platform app store for GitHub releases built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**. It targets **Android** (min API 26, target 36) and **Desktop** (Windows, macOS, Linux via JVM).

Package: `zed.rainxch.githubstore`

## Build & Run Commands

```bash
# Android debug build
./gradlew :composeApp:assembleDebug

# Desktop (run in dev mode)
./gradlew :composeApp:run

# Full build check (both platforms)
./gradlew build

# Lint (ktlint auto-formats on preBuild/compileKotlin* tasks automatically)
./gradlew ktlintFormat           # manual format all modules
./gradlew ktlintCheck            # check without fixing

# Desktop installers
./gradlew :composeApp:packageDmg    # macOS
./gradlew :composeApp:packageExe    # Windows
./gradlew :composeApp:packageDeb    # Linux
```

**Requirements:** JDK 21+ (Temurin recommended), Android SDK for Android builds.

**Setup:** Create a GitHub OAuth App and put `GITHUB_CLIENT_ID=<your_id>` in `local.properties` (root). Callback URL: `githubstore://callback`.

## Architecture

**Clean Architecture + MVVM** with strict layer separation:

- **Domain** — Repository interfaces, models, use cases. No framework dependencies.
- **Data** — Repository implementations, Ktor API clients, Room DAOs, DTOs, mappers. Each feature's DI module lives in `data/di/SharedModule.kt`.
- **Presentation** — ViewModels with `StateFlow`/`Channel`, Compose screens.

### State Management Pattern (every screen)

Every ViewModel follows the same State/Action/Event pattern:

- `State` — data class holding all UI state, exposed via `StateFlow`
- `Action` — sealed interface for user input (clicks, refreshes)
- `Event` — sealed interface for one-off effects (navigation, toasts), sent via `Channel.receiveAsFlow()`

### Module Layout

```text
composeApp/          # App entry points, navigation, DI wiring
  src/commonMain/    # Shared UI & wiring
  src/androidMain/   # Android entry (MainActivity)
  src/jvmMain/       # Desktop entry (DesktopApp.kt)
core/
  domain/            # Shared interfaces, models, use cases
  data/              # Networking (Ktor), database (Room), DI, platform impls
  presentation/      # Material 3 theming, reusable UI components, localized strings (13 languages)
feature/<name>/
  domain/            # Feature-specific interfaces & models
  data/              # Feature-specific implementations & Koin DI module
  presentation/      # Feature ViewModel + Compose screens
build-logic/convention/  # Custom Gradle convention plugins
```

Some features (favourites, starred, recently-viewed, tweaks) are **presentation-only** — they use core repositories directly and register ViewModels in `composeApp/.../di/ViewModelsModule.kt` instead of having a `data/di/` layer.

### Convention Plugins (build-logic)

| Plugin ID | Use For |
| :--- | :--- |
| `convention.kmp.library` | KMP shared library modules (domain, data) |
| `convention.cmp.library` | Compose Multiplatform library modules |
| `convention.cmp.feature` | Feature presentation modules (auto-adds Compose + Koin + core:presentation) |
| `convention.cmp.application` | Main app module |
| `convention.room` | Room database modules |
| `convention.buildkonfig` | Build-time config (reads from local.properties) |

### Navigation

Type-safe navigation using `@Serializable` sealed interface `GithubStoreGraph` in `composeApp/.../navigation/GithubStoreGraph.kt`. Routes are wired in `AppNavigation.kt`. Parameterized routes: `DetailsScreen(repositoryId, owner, repo, isComingFromUpdate)`, `DeveloperProfileScreen(username)`.

### Dependency Injection

**Koin** — each feature's data layer defines a module in `data/di/SharedModule.kt`. All modules are registered in `composeApp/.../di/initKoin.kt`. ViewModels injected via `koinViewModel()`. `DetailsViewModel` and `MirrorPickerViewModel` use manual Koin `viewModel { }` with `parametersOf()` for constructor args; all others use `viewModelOf(::ClassName)`.

### Key Cross-Cutting Concerns

- **Auth flow:** GitHub device-flow OAuth. Primary path goes through backend proxy (`/v1/auth/device/start`, `/v1/auth/device/poll`); falls back to direct GitHub only on infrastructure errors (5xx, timeouts). HTTP 4xx and GitHub's negative 200-bodies never trigger fallback. Backend rate limits (10 starts/hr, 200 polls/hr per IP) are hard — do not add retry loops.
- **`X-GitHub-Token` header:** Forwarded on every backend passthrough route — `/v1/search`, `/v1/search/explore`, `/v1/repo/{owner}/{name}`, `/v1/releases/{owner}/{name}`, `/v1/readme/{owner}/{name}`, `/v1/user/{username}`. Backend re-sends as `Authorization: token $token` so upstream GitHub calls run under the user's 5000/hr OAuth quota; without it the request falls back to the shared 60/hr anonymous bucket and a single 4xx can poison the backend's 15-min negative cache for everyone. DB-only routes (`/v1/categories`, `/v1/topics`, `/v1/events`, `/v1/auth/device/*`, `/v1/badge/*`) never get the header. Sourced via `BackendApiClient.currentUserGithubToken()` (`private`), never logged. 401 from passthrough routes ≠ session expired — `AuthenticationStateImpl` debounces consecutive 401s under the same token before clearing the session.
- **Platform branching:** Source sets are `commonMain` (shared), `androidMain` (Android), `jvmMain` (Desktop). Some features (apps, installation, Shizuku) are Android-only.
- **Shizuku (Android):** Optional silent install via AIDL service. Falls back to standard installer on failure.

## Coding Conventions

- Packages: `zed.rainxch.{module}.{layer}` (e.g. `zed.rainxch.home.data.repository`)
- Private state: underscore prefix `_state`, `_events`
- Sealed classes/interfaces for type-safe routes, actions, events
- Repository pattern: interface in `domain/`, implementation in `data/`
- Ktlint auto-runs on `preBuild`/`compileKotlin*` tasks; `ignoreFailures = true`
- Ktlint rules: wildcard imports allowed, filename rule disabled, `@Composable` functions exempt from function naming rule (see `.editorconfig`)

## Adding a New Feature

1. Create `feature/<name>/domain/`, `feature/<name>/data/`, `feature/<name>/presentation/`
2. Add `build.gradle.kts` in each using the appropriate convention plugin
3. Add `include` entries in `settings.gradle.kts`
4. Define domain interfaces/models in `domain/`
5. Implement repository + Koin DI module in `data/di/SharedModule.kt`
6. Create ViewModel (State/Action/Event pattern) and Screen in `presentation/`
7. Add navigation route to `GithubStoreGraph.kt` and wire in `AppNavigation.kt`
8. Register the Koin module in `initKoin.kt`

## Feature-Level Documentation

Each `feature/` directory contains its own `CLAUDE.md` with module structure, key interfaces, navigation routes, and implementation notes. Read those for feature-specific guidance.

## Versions

All library versions managed in `gradle/libs.versions.toml`. Key versions: Kotlin 2.3.10, Compose Multiplatform 1.10.3, Ktor 3.4.0, Room 2.8.4, Koin 4.1.1.
