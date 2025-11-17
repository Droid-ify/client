package com.looker.droidify.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.looker.droidify.data.local.model.AuthenticationEntity

@Dao
interface AuthDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(authentication: AuthenticationEntity)

    @Query("SELECT * FROM authentication WHERE repoId = :repoId")
    suspend fun authFor(repoId: Int): AuthenticationEntity?
}
