package com.looker.droidify.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.looker.droidify.data.local.converters.Converters
import com.looker.droidify.data.local.converters.PermissionConverter
import com.looker.droidify.data.local.dao.AppDao
import com.looker.droidify.data.local.dao.AuthDao
import com.looker.droidify.data.local.dao.IndexDao
import com.looker.droidify.data.local.dao.InstalledDao
import com.looker.droidify.data.local.dao.RBLogDao
import com.looker.droidify.data.local.dao.RepoDao
import com.looker.droidify.data.local.model.AntiFeatureAppRelation
import com.looker.droidify.data.local.model.AntiFeatureEntity
import com.looker.droidify.data.local.model.AntiFeatureRepoRelation
import com.looker.droidify.data.local.model.AppEntity
import com.looker.droidify.data.local.model.AuthenticationEntity
import com.looker.droidify.data.local.model.AuthorEntity
import com.looker.droidify.data.local.model.CategoryAppRelation
import com.looker.droidify.data.local.model.CategoryEntity
import com.looker.droidify.data.local.model.CategoryRepoRelation
import com.looker.droidify.data.local.model.DonateEntity
import com.looker.droidify.data.local.model.GraphicEntity
import com.looker.droidify.data.local.model.InstalledEntity
import com.looker.droidify.data.local.model.LinksEntity
import com.looker.droidify.data.local.model.MirrorEntity
import com.looker.droidify.data.local.model.RBLogEntity
import com.looker.droidify.data.local.model.RepoEntity
import com.looker.droidify.data.local.model.ScreenshotEntity
import com.looker.droidify.data.local.model.VersionEntity

@Database(
    version = 1,
    exportSchema = true,
    entities = [
        AntiFeatureEntity::class,
        AntiFeatureAppRelation::class,
        AntiFeatureRepoRelation::class,
        AuthenticationEntity::class,
        AuthorEntity::class,
        AppEntity::class,
        CategoryEntity::class,
        CategoryAppRelation::class,
        CategoryRepoRelation::class,
        DonateEntity::class,
        GraphicEntity::class,
        InstalledEntity::class,
        LinksEntity::class,
        MirrorEntity::class,
        RepoEntity::class,
        ScreenshotEntity::class,
        VersionEntity::class,
        RBLogEntity::class,
    ],
)
@TypeConverters(
    PermissionConverter::class,
    Converters::class,
)
abstract class DroidifyDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun repoDao(): RepoDao
    abstract fun authDao(): AuthDao
    abstract fun indexDao(): IndexDao
    abstract fun rbLogDao(): RBLogDao
    abstract fun installedDao(): InstalledDao
}

fun droidifyDatabase(context: Context): DroidifyDatabase = Room
    .databaseBuilder(
        context = context,
        klass = DroidifyDatabase::class.java,
        name = "droidify_room",
    )
    .addCallback(
        object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.query("PRAGMA synchronous = OFF")
                db.query("PRAGMA journal_mode = WAL")
            }
        },
    )
    .build()
