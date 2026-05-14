package zed.rainxch.githubstore.app.di

import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import zed.rainxch.apps.presentation.AppsViewModel
import zed.rainxch.apps.presentation.import.ExternalImportViewModel
import zed.rainxch.apps.presentation.starred.StarredPickerViewModel
import zed.rainxch.auth.presentation.AuthenticationViewModel
import zed.rainxch.details.presentation.DetailsViewModel
import zed.rainxch.devprofile.presentation.DeveloperProfileViewModel
import zed.rainxch.favourites.presentation.FavouritesViewModel
import zed.rainxch.githubstore.app.announcements.AnnouncementsViewModel
import zed.rainxch.githubstore.app.whatsnew.WhatsNewViewModel
import zed.rainxch.home.presentation.HomeViewModel
import zed.rainxch.profile.presentation.ProfileViewModel
import zed.rainxch.recentlyviewed.presentation.RecentlyViewedViewModel
import zed.rainxch.search.presentation.SearchViewModel
import zed.rainxch.search.presentation.model.SearchPlatformUi
import zed.rainxch.starred.presentation.StarredReposViewModel
import zed.rainxch.tweaks.presentation.TweaksViewModel
import zed.rainxch.tweaks.presentation.feedback.FeedbackViewModel
import zed.rainxch.tweaks.presentation.hidden.HiddenRepositoriesViewModel
import zed.rainxch.tweaks.presentation.mirror.AutoSuggestMirrorViewModel
import zed.rainxch.tweaks.presentation.mirror.MirrorPickerViewModel
import zed.rainxch.tweaks.presentation.skipped.SkippedUpdatesViewModel

val viewModelsModule =
    module {
        viewModelOf(::AppsViewModel)
        viewModelOf(::ExternalImportViewModel)
        viewModelOf(::AuthenticationViewModel)
        viewModel { params ->
            // Indexed access because `ownerParam` and `repoParam` are both
            // Strings — positional `params.get()` would silently pick the
            // first matching by type and could swap the two if Koin ever
            // changes its resolution order.
            DetailsViewModel(
                repositoryId = params.get(0),
                ownerParam = params.get(1),
                repoParam = params.get(2),
                isComingFromUpdate = params.get(3),
                detailsRepository = get(),
                downloader = get(),
                installer = get(),
                platform = get(),
                helper = get(),
                shareManager = get(),
                installedAppsRepository = get(),
                favouritesRepository = get(),
                starredRepository = get(),
                packageMonitor = get(),
                syncInstalledAppsUseCase = get(),
                translationRepository = get(),
                logger = get(),
                tweaksRepository = get(),
                seenReposRepository = get(),
                installationManager = get(),
                attestationVerifier = get(),
                downloadOrchestrator = get(),
                telemetryRepository = get(),
                externalImportRepository = get(),
                apkInspector = get(),
                authenticationState = get(),
                systemInstallSerializer = get(),
                profileRepository = get(),
            )
        }
        viewModelOf(::DeveloperProfileViewModel)
        viewModelOf(::FavouritesViewModel)
        viewModelOf(::HomeViewModel)
        viewModelOf(::RecentlyViewedViewModel)
        viewModel { params ->
            SearchViewModel(
                searchRepository = get(),
                installedAppsRepository = get(),
                syncInstalledAppsUseCase = get(),
                favouritesRepository = get(),
                starredRepository = get(),
                logger = get(),
                shareManager = get(),
                platform = get(),
                clipboardHelper = get(),
                tweaksRepository = get(),
                seenReposRepository = get(),
                searchHistoryRepository = get(),
                telemetryRepository = get(),
                profileRepository = get(),
                hiddenReposRepository = get(),
                initialPlatform = params.getOrNull<SearchPlatformUi>(),
            )
        }
        viewModelOf(::ProfileViewModel)
        viewModelOf(::TweaksViewModel)
        viewModelOf(::FeedbackViewModel)
        viewModelOf(::StarredReposViewModel)
        viewModelOf(::StarredPickerViewModel)
        viewModelOf(::AutoSuggestMirrorViewModel)
        viewModelOf(::SkippedUpdatesViewModel)
        viewModelOf(::HiddenRepositoriesViewModel)
        viewModelOf(::WhatsNewViewModel)
        viewModelOf(::AnnouncementsViewModel)
        viewModel {
            MirrorPickerViewModel(
                mirrorRepository = get(),
                testHttpClient =
                    get(
                        qualifier =
                            org.koin.core.qualifier
                                .named("test"),
                    ),
            )
        }
    }
