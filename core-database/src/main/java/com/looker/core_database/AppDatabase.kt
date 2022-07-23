package com.looker.core_database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.looker.core_database.dao.AppDao
import com.looker.core_database.dao.RepoDao
import com.looker.core_database.model.App
import com.looker.core_database.model.Repo
import com.looker.core_database.utils.Converter

@Database(
	version = 1,
	entities = [
		Repo::class,
		App::class
	]
)
@TypeConverters(Converter::class)
abstract class AppDatabase : RoomDatabase() {
	abstract val repoDao: RepoDao
	abstract val appDao: AppDao

	companion object {
		@Volatile
		private var INSTANCE: AppDatabase? = null

		// TODO Prepopulate with [https://developer.android.com/training/data-storage/room/prepopulate]
		fun getInstance(context: Context): AppDatabase {
			synchronized(this) {
				if (INSTANCE == null) {
					INSTANCE = Room
						.databaseBuilder(
							context.applicationContext,
							AppDatabase::class.java,
							"app_database.db"
						)
						.build()
				}
				return INSTANCE!!
			}
		}
	}
}