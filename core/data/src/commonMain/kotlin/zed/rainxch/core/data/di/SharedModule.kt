package zed.rainxch.core.data.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.koin.core.qualifier.named
import org.koin.dsl.module
import zed.rainxch.core.data.network.createPlatformHttpClient
import zed.rainxch.core.data.cache.CacheManager
import zed.rainxch.core.data.data_source.TokenStore
import zed.rainxch.core.data.download.MultiSourceDownloaderImpl
import zed.rainxch.core.data.download.SlowDownloadDetectorImpl
import zed.rainxch.core.data.services.BuildKonfigAppVersionInfo
import zed.rainxch.core.data.services.DefaultDownloadOrchestrator
import zed.rainxch.core.data.data_source.impl.DefaultTokenStore
import zed.rainxch.core.data.local.db.AppDatabase
import zed.rainxch.core.data.local.db.dao.CacheDao
import zed.rainxch.core.data.local.db.dao.ExternalLinkDao
import zed.rainxch.core.data.local.db.dao.FavoriteRepoDao
import zed.rainxch.core.data.local.db.dao.InstalledAppDao
import zed.rainxch.core.data.local.db.dao.SearchHistoryDao
import zed.rainxch.core.data.local.db.dao.SeenRepoDao
import zed.rainxch.core.data.local.db.dao.SigningFingerprintDao
import zed.rainxch.core.data.local.db.dao.StarredRepoDao
import zed.rainxch.core.data.local.db.dao.UpdateHistoryDao
import zed.rainxch.core.data.logging.KermitLogger
import zed.rainxch.core.data.mirror.MirrorRepositoryImpl
import zed.rainxch.core.data.network.BackendApiClient
import zed.rainxch.core.data.network.BackendExternalMatchApi
import zed.rainxch.core.data.network.ExternalMatchApi
import zed.rainxch.core.data.network.ExternalMatchApiSelector
import zed.rainxch.core.data.network.GitHubClientProvider
import zed.rainxch.core.data.network.MirrorApiClient
import zed.rainxch.core.data.network.MockExternalMatchApi
import zed.rainxch.core.data.network.ProxyManager
import zed.rainxch.core.data.network.ProxyManagerSeeding
import zed.rainxch.core.data.network.ProxyTesterImpl
import zed.rainxch.core.data.network.TranslationClientProvider
import zed.rainxch.core.data.repository.AuthenticationStateImpl
import zed.rainxch.core.data.repository.ExternalImportRepositoryImpl
import zed.rainxch.core.data.repository.FavouritesRepositoryImpl
import zed.rainxch.core.data.repository.InstalledAppsRepositoryImpl
import zed.rainxch.core.data.repository.ProxyRepositoryImpl
import zed.rainxch.core.data.repository.DeviceIdentityRepositoryImpl
import zed.rainxch.core.data.repository.RateLimitRepositoryImpl
import zed.rainxch.core.data.repository.SearchHistoryRepositoryImpl
import zed.rainxch.core.data.repository.TelemetryRepositoryImpl
import zed.rainxch.core.data.repository.SeenReposRepositoryImpl
import zed.rainxch.core.data.repository.StarredRepositoryImpl
import zed.rainxch.core.data.repository.AnnouncementsCacheStoreImpl
import zed.rainxch.core.data.repository.AnnouncementsRepositoryImpl
import zed.rainxch.core.data.repository.TweaksRepositoryImpl
import zed.rainxch.core.domain.getPlatform
import zed.rainxch.core.domain.logging.GitHubStoreLogger
import zed.rainxch.core.domain.model.Platform
import zed.rainxch.core.domain.model.ProxyConfig
import zed.rainxch.core.domain.model.ProxyScope
import zed.rainxch.core.domain.network.ProxyTester
import zed.rainxch.core.domain.network.SlowDownloadDetector
import zed.rainxch.core.domain.system.AppVersionInfo
import zed.rainxch.core.domain.system.DownloadOrchestrator
import zed.rainxch.core.domain.system.ExternalAppScanner
import zed.rainxch.core.domain.system.MultiSourceDownloader
import zed.rainxch.core.domain.repository.AnnouncementsCacheStore
import zed.rainxch.core.domain.repository.AnnouncementsRepository
import zed.rainxch.core.domain.repository.AuthenticationState
import zed.rainxch.core.domain.repository.DeviceIdentityRepository
import zed.rainxch.core.domain.repository.ExternalImportRepository
import zed.rainxch.core.domain.repository.FavouritesRepository
import zed.rainxch.core.domain.repository.InstalledAppsRepository
import zed.rainxch.core.domain.repository.MirrorRepository
import zed.rainxch.core.domain.repository.ProxyRepository
import zed.rainxch.core.domain.repository.RateLimitRepository
import zed.rainxch.core.domain.repository.SearchHistoryRepository
import zed.rainxch.core.domain.repository.SeenReposRepository
import zed.rainxch.core.domain.repository.StarredRepository
import zed.rainxch.core.domain.repository.TelemetryRepository
import zed.rainxch.core.domain.repository.TweaksRepository
import zed.rainxch.core.domain.use_cases.SyncInstalledAppsUseCase

val coreModule =
    module {
        single {
            CoroutineScope(Dispatchers.IO + SupervisorJob())
        }

        single<GitHubStoreLogger> {
            KermitLogger
        }

        single<Platform> {
            getPlatform()
        }

        single<AuthenticationState> {
            AuthenticationStateImpl(
                tokenStore = get(),
            )
        }

        single<FavouritesRepository> {
            FavouritesRepositoryImpl(
                favoriteRepoDao = get(),
                installedAppsDao = get(),
            )
        }

        single<InstalledAppsRepository> {
            InstalledAppsRepositoryImpl(
                database = get(),
                installedAppsDao = get(),
                historyDao = get(),
                installer = get(),
                clientProvider = get(),
                backendApiClient = get(),
            )
        }

        single<StarredRepository> {
            StarredRepositoryImpl(
                installedAppsDao = get(),
                starredRepoDao = get(),
                platform = get(),
                clientProvider = get(),
                backendApiClient = get(),
            )
        }

        single<TweaksRepository> {
            TweaksRepositoryImpl(
                preferences = get(),
            )
        }

        single<AppVersionInfo> {
            BuildKonfigAppVersionInfo()
        }

        single<AnnouncementsCacheStore> {
            AnnouncementsCacheStoreImpl(
                preferences = get(qualifier = org.koin.core.qualifier.named("announcements")),
            )
        }

        single<AnnouncementsRepository> {
            AnnouncementsRepositoryImpl(
                backendApiClient = get(),
                tweaksRepository = get(),
                cacheStore = get(),
                localizationManager = get(),
                appVersionInfo = get(),
            )
        }

        single<MirrorApiClient> {
            MirrorApiClient(
                backendApiClient = get(),
            )
        }

        single<MirrorRepository> {
            val repo =
                MirrorRepositoryImpl(
                    preferences = get(),
                    apiClient = get(),
                    appScope = get(),
                )
            // Kick off the ProxyManager mirror-template snapshot collector
            // so the Ktor interceptor can resolve the template synchronously.
            ProxyManager.startMirrorCollector(repo, get())
            repo
        }

        single<SeenReposRepository> {
            SeenReposRepositoryImpl(
                seenRepoDao = get(),
            )
        }

        single<SearchHistoryRepository> {
            SearchHistoryRepositoryImpl(
                searchHistoryDao = get(),
            )
        }

        single<ProxyRepository> {
            ProxyRepositoryImpl(
                preferences = get(),
                logger = get(),
            )
        }

        single<ProxyTester> {
            ProxyTesterImpl()
        }

        single<SyncInstalledAppsUseCase> {
            SyncInstalledAppsUseCase(
                packageMonitor = get(),
                installedAppsRepository = get(),
                platform = get(),
                logger = get(),
            )
        }

        single<CacheManager> {
            CacheManager(cacheDao = get())
        }

        single<BackendApiClient> {
            // Request the seeding sentinel so Koin guarantees ProxyManager
            // has the user's persisted config loaded before we snapshot
            // the discovery flow for the initial client build.
            get<ProxyManagerSeeding>()
            BackendApiClient(
                proxyConfigFlow = ProxyManager.configFlow(ProxyScope.DISCOVERY),
                tokenStore = get(),
            )
        }
        // NOTE: the reviewer asked for a Koin onClose hook to call
        // BackendApiClient.close()/GitHubClientProvider.close()/
        // TranslationClientProvider.close() at Koin shutdown. Koin 4.x
        // (4.1.1 on this project) doesn't expose that hook at the
        // module DSL level — it existed in 3.x and was removed — and
        // there's no clean replacement short of wrapping each provider
        // in a Koin scope. On Android/Desktop the process exit
        // releases these resources anyway, so we intentionally leave
        // the hooks off rather than fake them with an API that doesn't
        // fit. Revisit if we upgrade Koin or adopt scope-based DI.

        single<DeviceIdentityRepository> {
            DeviceIdentityRepositoryImpl(
                preferences = get(),
            )
        }

        single<TelemetryRepository> {
            TelemetryRepositoryImpl(
                backendApiClient = get(),
                deviceIdentity = get(),
                tweaksRepository = get(),
                platform = get(),
                appScope = get(),
                logger = get(),
            )
        }

        single { BackendExternalMatchApi(get()) }

        single { MockExternalMatchApi() }

        single<ExternalMatchApi> {
            ExternalMatchApiSelector(
                real = get(),
                mock = get(),
                tweaks = get(),
                scope = get(),
            )
        }

        single<ExternalImportRepository> {
            ExternalImportRepositoryImpl(
                scanner = get<ExternalAppScanner>(),
                externalLinkDao = get(),
                signingFingerprintDao = get(),
                preferences = get(),
                externalMatchApi = get(),
                backendClient = get(),
                telemetry = get(),
            )
        }

        single<MultiSourceDownloader> {
            MultiSourceDownloaderImpl(
                downloader = get(),
            )
        }

        single<SlowDownloadDetector> {
            SlowDownloadDetectorImpl(
                preferences = get(),
                appScope = get(),
            )
        }

        // Application-scoped download / install orchestrator. Lives
        // for the process lifetime so downloads survive screen
        // navigation. ViewModels are observers, never owners.
        single<DownloadOrchestrator> {
            DefaultDownloadOrchestrator(
                downloader = get(),
                multiSourceDownloader = get(),
                digestVerifier = get(),
                installer = get(),
                installedAppsRepository = get(),
                pendingInstallNotifier = get(),
                slowDownloadDetector = get(),
                appScope = get(),
            )
        }
    }

val networkModule =
    module {
        // Seed [ProxyManager] from persisted per-scope configs *before*
        // any HTTP client is constructed. Registered as its own
        // [ProxyManagerSeeding] sentinel so client providers can depend
        // on it explicitly — without this the seeding would live inside
        // one provider's factory and silently race with others.
        //
        // Reads run in parallel under a single 1.5s budget (was 1.5s × 3
        // sequential). On timeout / DataStore failure we fall back to the
        // in-memory defaults — we'd rather the app network with the
        // System proxy than stall at launch on a slow disk.
        single<ProxyManagerSeeding>(createdAtStart = true) {
            val repository = get<ProxyRepository>()
            runBlocking {
                runCatching {
                    withTimeout(1_500L) {
                        coroutineScope {
                            ProxyScope.entries
                                .map { scope ->
                                    async {
                                        scope to repository.getProxyConfig(scope).first()
                                    }
                                }.awaitAll()
                        }
                    }
                }.onSuccess { results ->
                    results.forEach { (scope, config) ->
                        ProxyManager.setConfig(scope, config)
                    }
                }
            }
            ProxyManagerSeeding()
        }

        single<GitHubClientProvider>(createdAtStart = true) {
            get<ProxyManagerSeeding>()
            GitHubClientProvider(
                tokenStore = get(),
                rateLimitRepository = get(),
                authenticationState = get(),
                proxyConfigFlow = ProxyManager.configFlow(ProxyScope.DISCOVERY),
            )
        }

        single<TranslationClientProvider>(createdAtStart = true) {
            get<ProxyManagerSeeding>()
            TranslationClientProvider(
                proxyConfigFlow = ProxyManager.configFlow(ProxyScope.TRANSLATION),
            )
        }

        single<TokenStore> {
            DefaultTokenStore(
                dataStore = get(),
            )
        }

        single<RateLimitRepository> {
            RateLimitRepositoryImpl()
        }

        single<HttpClient>(qualifier = named("test")) {
            createPlatformHttpClient(ProxyConfig.System).config {
                install(HttpTimeout) {
                    requestTimeoutMillis = 5_000
                    connectTimeoutMillis = 5_000
                    socketTimeoutMillis = 5_000
                }
                defaultRequest {
                    header(HttpHeaders.UserAgent, "GithubStore/1.0 (KMP)")
                }
            }
        }
    }

val databaseModule =
    module {
        single<FavoriteRepoDao> {
            get<AppDatabase>().favoriteRepoDao
        }

        single<InstalledAppDao> {
            get<AppDatabase>().installedAppDao
        }

        single<StarredRepoDao> {
            get<AppDatabase>().starredReposDao
        }

        single<UpdateHistoryDao> {
            get<AppDatabase>().updateHistoryDao
        }

        single<CacheDao> {
            get<AppDatabase>().cacheDao
        }

        single<SeenRepoDao> {
            get<AppDatabase>().seenRepoDao
        }

        single<SearchHistoryDao> {
            get<AppDatabase>().searchHistoryDao
        }

        single<ExternalLinkDao> {
            get<AppDatabase>().externalLinkDao
        }

        single<SigningFingerprintDao> {
            get<AppDatabase>().signingFingerprintDao
        }
    }
