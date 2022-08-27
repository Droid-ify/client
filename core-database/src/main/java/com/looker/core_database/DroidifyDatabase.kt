package com.looker.core_database

import androidx.room.Database
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
abstract class DroidifyDatabase : RoomDatabase() {
	abstract fun repoDao(): RepoDao
	abstract fun appDao(): AppDao
}