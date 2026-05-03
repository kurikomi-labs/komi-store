package zed.rainxch.githubstore.app.di

import org.koin.dsl.module
import zed.rainxch.core.domain.repository.WhatsNewLoader
import zed.rainxch.githubstore.app.whatsnew.KnownWhatsNewVersionCodes
import zed.rainxch.githubstore.app.whatsnew.WhatsNewLoaderImpl

val whatsNewModule =
    module {
        single<WhatsNewLoader> {
            WhatsNewLoaderImpl(
                knownVersionCodes = KnownWhatsNewVersionCodes.ALL,
                localizationManager = get(),
                logger = get(),
            )
        }
    }
