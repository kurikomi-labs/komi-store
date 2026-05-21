package zed.rainxch.search.data.di

import org.koin.dsl.module
import zed.rainxch.domain.repository.SearchHistoryRepository
import zed.rainxch.domain.repository.SearchRepository
import zed.rainxch.search.data.repository.SearchHistoryRepositoryImpl
import zed.rainxch.search.data.repository.SearchRepositoryImpl

val searchModule =
    module {
        single<SearchRepository> {
            SearchRepositoryImpl(
                clientProvider = get(),
                backendApiClient = get(),
                cacheManager = get(),
                forgejoClientRegistry = get(),
            )
        }

        single<SearchHistoryRepository> {
            SearchHistoryRepositoryImpl(
                searchHistoryDao = get(),
            )
        }
    }
