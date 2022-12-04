package com.looker.core.database

import androidx.room.BuiltInTypeConverters
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.looker.core.database.dao.AppDao
import com.looker.core.database.dao.RepoDao
import com.looker.core.database.model.AppEntity
import com.looker.core.database.model.RepoEntity

@Database(
	version = 1,
	entities = [
		AppEntity::class,
		RepoEntity::class
	]
)
@TypeConverters(
	CollectionConverter::class,
	LocalizedConverter::class,
	PackageEntityConverter::class,
	PermissionConverter::class,
	builtInTypeConverters = BuiltInTypeConverters(enums = BuiltInTypeConverters.State.ENABLED)
)
abstract class DroidifyDatabase : RoomDatabase() {

	abstract fun appDao(): AppDao
	abstract fun repoDao(): RepoDao

}