package ro.holdone.swissborg.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ro.holdone.swissborg.server.CoinService
import ro.holdone.swissborg.server.ServerManager
import ro.holdone.swissborg.server.impl.CoinServiceImpl
import ro.holdone.swissborg.server.impl.ServerManagerImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SwissborgModule {

    @Binds
    @Singleton
    abstract fun bindServerManager(serverManagerImpl: ServerManagerImpl): ServerManager

    @Binds
    @Singleton
    abstract fun bindCoinService(coinsServiceImpl: CoinServiceImpl): CoinService
}