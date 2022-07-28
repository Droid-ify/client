package com.looker.core_database.di

import android.content.Context
import androidx.room.Room
import com.looker.core_database.DroidifyDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

	@Provides
	@Singleton
	fun provideDroidifyDatabase(
		@ApplicationContext context: Context
	): DroidifyDatabase = Room.databaseBuilder(
		context,
		DroidifyDatabase::class.java,
		"app_database.db"
	).createFromAsset("database/repo.db").build()
}