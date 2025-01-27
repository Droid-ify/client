package com.looker.droidify.di

import android.content.Context
import androidx.room.Room
import com.looker.droidify.data.local.DroidifyDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(
        @ApplicationContext
        context: Context,
    ): DroidifyDatabase = Room.databaseBuilder(
        context = context,
        klass = DroidifyDatabase::class.java,
        name = "droidify_room",
    ).fallbackToDestructiveMigration().build()
}
