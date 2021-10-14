package com.looker.droidify.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        Repository::class,
        Product::class,
        Category::class,
        Installed::class,
        Lock::class
    ], version = 1
)
@TypeConverters(Converters::class)
abstract class DatabaseX : RoomDatabase() {
    abstract val repositoryDao: RepositoryDao
    abstract val productDao: ProductDao
    abstract val categoryDao: CategoryDao
    abstract val installedDao: InstalledDao
    abstract val lockDao: LockDao

    companion object {
        @Volatile
        private var INSTANCE: DatabaseX? = null

        fun getInstance(context: Context): DatabaseX {
            synchronized(this) {
                if (INSTANCE == null) {
                    INSTANCE = Room
                        .databaseBuilder(
                            context.applicationContext,
                            DatabaseX::class.java,
                            "main_database.db"
                        )
                        .fallbackToDestructiveMigration()
                        .allowMainThreadQueries()
                        .build()
                }
                return INSTANCE!!
            }
        }
    }

    fun cleanUp(pairs: Set<Pair<Long, Boolean>>) {
        val result = pairs.windowed(10, 10, true).map {
            val ids = it.map { it.first }.toLongArray()
            val productsCount = productDao.deleteById(*ids)
            val categoriesCount = categoryDao.deleteById(*ids)
            val deleteIds = it.filter { it.second }.map { it.first }.toLongArray()
            repositoryDao.deleteById(*deleteIds)
            productsCount != 0 || categoriesCount != 0
        }
        // Use live objects and observers instead
        /*if (result.any { it }) {
            com.looker.droidify.database.Database.notifyChanged(com.looker.droidify.database.Database.Subject.Products)
        }*/
    }
}