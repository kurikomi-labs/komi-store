package zed.rainxch.favourites.presentation

import zed.rainxch.favourites.presentation.model.FavouriteRepository
import zed.rainxch.favourites.presentation.model.FavouritesSortRule

sealed interface FavouritesAction {
    data object OnNavigateBackClick : FavouritesAction

    data class OnToggleFavorite(
        val favouriteRepository: FavouriteRepository,
    ) : FavouritesAction

    data class OnRepositoryClick(
        val favouriteRepository: FavouriteRepository,
    ) : FavouritesAction

    data class OnDeveloperProfileClick(
        val username: String,
    ) : FavouritesAction

    data class OnSearchChange(
        val query: String,
    ) : FavouritesAction

    data class OnSortRuleSelected(
        val sortRule: FavouritesSortRule,
    ) : FavouritesAction

    data object OnImportStarsClick : FavouritesAction
}
