# Search Feature

Repo search with platform / language / sort filters. Paginated results.

## Structure

```
feature/search/
├── domain/   # SearchRepository; SearchPlatform (All/Android/Macos/Windows/Linux), ProgrammingLanguage, SortBy
├── data/     # SearchRepositoryImpl + dto + mappers + di
└── presentation/
    ├── SearchViewModel / State / Action / Event / Root
    └── components/  filter chips, ClipboardLinkBanner, etc.
```

## Key interface

```kotlin
interface SearchRepository {
    fun searchRepositories(
        query: String,
        platform: DiscoveryPlatform,
        language: ProgrammingLanguage,
        sortBy: SortBy,
        sortOrder: SortOrder,
        page: Int,
        source: SearchSource = SearchSource.GitHub,
    ): Flow<PaginatedDiscoveryRepositories>

    suspend fun exploreFromGithub(query, platform, page): ExploreResult
}

sealed interface SearchSource { object GitHub; data class Forgejo(host); /* Codeberg = Forgejo("codeberg.org") */ }
```

## Navigation

`GithubStoreGraph.SearchScreen`.

## Notes

- Platform filter → GitHub topic search (e.g. `android` topic). Language filter → `language:` qualifier. Debounce/throttle on queries.
- **Source toggle (`SearchSourceUi`):** UI surfaces `GitHub`, `Codeberg`, and per-user-added `Custom(host)` chips (from `TweaksRepository.customForgeHosts`). `state.selectedSource` → repository's `source: SearchSource` param. Forgejo branch in `SearchRepositoryImpl` hits `ForgejoClientRegistry.clientFor(host)` (`/repos/search`) — no backend mediator.
- Injects: `SearchRepository`, `InstalledAppsRepository`, `SyncInstalledAppsUseCase`, `FavouritesRepository`, `StarredRepository`, `SeenReposRepository`, `HiddenReposRepository`, `TweaksRepository`, `ProfileRepository`, `TelemetryRepository`, `SearchHistoryRepository`, `ShareManager`, `ClipboardHelper`, `Platform`, `GitHubStoreLogger`. Data layer additionally injects `ForgejoClientRegistry`.
- `computeVisibleRepos` filters `state.repositories` at render time by `hiddenRepoIds` AND (when `isHideSeenEnabled`) `seenRepoIds`. Unhide restores without re-fetch.
- Empty-grid-after-Hide-seen banner offers one-tap reset (issue #574) → `OnDisableHideSeenForResults`.
- Long-press card → shared `RepositoryActionsBottomSheet`.
- `DiscoveryRepositoryUi.isCurrentUserOwner` flipped by `observeCurrentUser` (E20).
- Clipboard auto-detect surfaces GitHub URLs from clipboard as a dismissible banner.
