package com.looker.droidify.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface RepoDao {

    @Query("DELETE FROM repository WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM authentication WHERE repoId = :repoId")
    suspend fun deleteAuth(repoId: Int)

    @Transaction
    suspend fun deleteRepo(repoId: Int) {
        delete(repoId)
        deleteAuth(repoId)
    }
}
