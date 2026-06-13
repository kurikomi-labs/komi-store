package zed.rainxch.githubstore.app.di

import kotlinx.coroutines.CoroutineScope
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import zed.rainxch.apps.data.di.appsModule
import zed.rainxch.auth.data.di.authModule
import zed.rainxch.core.data.di.coreModule
import zed.rainxch.core.data.di.corePlatformModule
import zed.rainxch.core.data.di.databaseModule
import zed.rainxch.core.data.di.networkModule
import zed.rainxch.core.data.network.ProxyManager
import zed.rainxch.core.domain.repository.ProxyRepository
import zed.rainxch.details.data.di.detailsModule
import zed.rainxch.devprofile.data.di.devProfileModule
import zed.rainxch.feed.data.di.feedModule
import zed.rainxch.home.data.di.homeModule
import zed.rainxch.repopages.data.di.repoPagesModule
import zed.rainxch.search.data.di.searchModule

fun initKoin(config: KoinAppDeclaration? = null) {
    val app =
        startKoin {
            config?.invoke(this)
            modules(
                mainModule,
                corePlatformModule,
                coreModule,
                networkModule,
                databaseModule,
                viewModelsModule,
                whatsNewModule,
                appsModule,
                authModule,
                detailsModule,
                devProfileModule,
                homeModule,
                feedModule,
                repoPagesModule,
                searchModule,
            )
        }
    val koin = app.koin
    ProxyManager.bootstrap(
        repository = koin.get<ProxyRepository>(),
        appScope = koin.get<CoroutineScope>(),
    )
}
