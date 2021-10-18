package ro.holdone.swissborg.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ro.holdone.swissborg.server.ServerManager
import ro.holdone.swissborg.server.impl.ServerManagerImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class SwissborgModule {

    @Binds
    abstract fun bindServerManager(serverManagerImpl: ServerManagerImpl): ServerManager
}