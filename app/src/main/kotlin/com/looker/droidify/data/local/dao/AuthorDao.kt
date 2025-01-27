package com.looker.droidify.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.looker.droidify.data.local.model.AuthorEntity

@Dao
interface AuthorDao {

    @Upsert
    suspend fun upsert(authorEntity: AuthorEntity): Long

    @Query("DELETE FROM author WHERE id = :id")
    suspend fun delete(id: Int)

}
