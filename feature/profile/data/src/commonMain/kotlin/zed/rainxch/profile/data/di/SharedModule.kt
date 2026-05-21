package zed.rainxch.profile.data.di

import org.koin.dsl.module
import zed.rainxch.profile.data.repository.ProfileRepositoryImpl
import zed.rainxch.profile.domain.repository.ProfileRepository

val profileModule =
    module {
        single<ProfileRepository> {
            ProfileRepositoryImpl(
                userSessionRepository = get(),
                tokenStore = get(),
                clientProvider = get(),
                cacheManager = get(),
                logger = get(),
                fileLocationsProvider = get(),
            )
        }
    }
