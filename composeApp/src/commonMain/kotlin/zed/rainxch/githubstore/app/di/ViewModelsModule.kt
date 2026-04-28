package zed.rainxch.githubstore.app.di

import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import zed.rainxch.apps.presentation.AppsViewModel
import zed.rainxch.apps.presentation.import.ExternalImportViewModel
import zed.rainxch.auth.presentation.AuthenticationViewModel
import zed.rainxch.details.presentation.DetailsViewModel
import zed.rainxch.devprofile.presentation.DeveloperProfileViewModel
import zed.rainxch.favourites.presentation.FavouritesViewModel
import zed.rainxch.home.presentation.HomeViewModel
import zed.rainxch.profile.presentation.ProfileViewModel
import zed.rainxch.recentlyviewed.presentation.RecentlyViewedViewModel
import zed.rainxch.search.presentation.SearchViewModel
import zed.rainxch.starred.presentation.StarredReposViewModel
import zed.rainxch.tweaks.presentation.TweaksViewModel

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
            )
        }
        viewModelOf(::DeveloperProfileViewModel)
        viewModelOf(::FavouritesViewModel)
        viewModelOf(::HomeViewModel)
        viewModelOf(::RecentlyViewedViewModel)
        viewModelOf(::SearchViewModel)
        viewModelOf(::ProfileViewModel)
        viewModelOf(::TweaksViewModel)
        viewModelOf(::StarredReposViewModel)
    }
