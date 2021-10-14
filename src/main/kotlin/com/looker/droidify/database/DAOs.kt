package com.looker.droidify.database

import android.database.SQLException
import androidx.room.*

@Dao
interface RepositoryDao {
    @Insert
    @Throws(SQLException::class)
    fun insert(vararg repository: Repository)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(vararg repository: Repository?)

    fun put(repository: Repository) {
        if (repository.id >= 0L) update(repository) else insert(repository)
    }

    @Query("SELECT * FROM repository WHERE _id = :id and deleted == 0")
    fun get(id: Long): Repository?

    @get:Query("SELECT * FROM repository WHERE deleted == 0 ORDER BY _id ASC")
    val all: List<Repository>

    @get:Query("SELECT _id, deleted FROM repository WHERE deleted != 0 and enabled == 0 ORDER BY _id ASC")
    val allDisabledDeleted: List<Repository.IdAndDeleted>

    @Delete
    fun delete(repository: Repository)

    @Query("DELETE FROM repository WHERE _id = :id")
    fun deleteById(vararg id: Long): Int

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun markAsDeleted(id: Long) {
        update(get(id).apply { this?.deleted = 1 })
    }
}

@Dao
interface ProductDao {
    @Query("SELECT COUNT(*) FROM product WHERE repository_id = :id")
    fun countForRepository(id: Long): Long

    @Query("SELECT * FROM product WHERE package_name = :packageName")
    fun get(packageName: String): Product?

    @Query("DELETE FROM product WHERE repository_id = :id")
    fun deleteById(vararg id: Long): Int
}

@Dao
interface CategoryDao {
    @Query(
        """SELECT DISTINCT category.name
        FROM category AS category
        JOIN repository AS repository
        ON category.repository_id = repository._id
        WHERE repository.enabled != 0 AND
        repository.deleted == 0"""
    )
    fun getAll(): List<String>

    @Query("DELETE FROM category WHERE repository_id = :id")
    fun deleteById(vararg id: Long): Int
}

@Dao
interface InstalledDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @Throws(SQLException::class)
    fun insert(vararg installed: Installed)

    @Query("SELECT * FROM installed WHERE package_name = :packageName")
    fun get(packageName: String): Installed?

    @Query("DELETE FROM installed WHERE package_name = :packageName")
    fun delete(packageName: String)
}

@Dao
interface LockDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @Throws(SQLException::class)
    fun insert(vararg lock: Lock)

    @Query("DELETE FROM lock WHERE package_name = :packageName")
    fun delete(packageName: String)
}