package com.formacoach.app

import android.content.Context
import com.formacoach.data.FitnessRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton fun repository(@ApplicationContext context: Context): FitnessRepository =
        FitnessRepository.open(context)
}
