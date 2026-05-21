# Profile Feature

Account-level — GitHub user profile, login/logout, version info. **Settings live in `feature/tweaks/`** — this module narrow on purpose. Owns account identity + exposes `ProfileRepository.getUser()` to other features (home/search/details/starred/favourites/tweaks/dev-profile consume it for E20 self-owned badge and account-aware flows).

## Structure

```
feature/profile/
├── domain/        # ProfileRepository, UserProfile
├── data/          # ProfileRepositoryImpl, UserProfileMappers
└── presentation/
    ├── ProfileViewModel / State / Action / Event / Root
    ├── model/ProxyType (legacy — proxy lives in tweaks now)
    └── components/ LogoutDialog, SectionText, sections/{Account, AccountSection, ProfileSection}
```

## Key interface

```kotlin
interface ProfileRepository {
    val isUserLoggedIn: Flow<Boolean>
    fun getUser(): Flow<UserProfile?>
    fun getVersionName(): String
    suspend fun logout()
    fun observeCacheSize(): Flow<Long>
    suspend fun clearCache()
}
```

## Navigation

`GithubStoreGraph.ProfileScreen`.

## Notes

- `getUser()` cached via `CacheManager` key `profile:me`. Invalidates on logout.
- "Settings" tap → `TweaksScreen`. Don't add settings here.
- VM injects: `ProfileRepository`, `Platform`.
