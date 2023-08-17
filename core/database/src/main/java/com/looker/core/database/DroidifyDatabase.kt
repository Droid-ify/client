package com.looker.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.looker.core.database.dao.AppDao
import com.looker.core.database.dao.InstalledDao
import com.looker.core.database.dao.RepoDao
import com.looker.core.database.model.AppEntity
import com.looker.core.database.model.InstalledEntity
import com.looker.core.database.model.RepoEntity

@Database(
	version = 1,
	entities = [
		AppEntity::class,
		RepoEntity::class,
		InstalledEntity::class
	]
)
@TypeConverters(
	CollectionConverter::class,
	LocalizedConverter::class,
	PackageEntityConverter::class,
	RepoConverter::class
)
abstract class DroidifyDatabase : RoomDatabase() {

	abstract fun appDao(): AppDao

	abstract fun repoDao(): RepoDao

	abstract fun installedDao(): InstalledDao

}