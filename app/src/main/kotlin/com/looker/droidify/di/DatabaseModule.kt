package com.looker.droidify.di

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.looker.droidify.data.PrivacyRepository
import com.looker.droidify.data.local.droidifyDb
import com.looker.droidify.data.local.sql.DroidifyDb
import com.looker.droidify.datastore.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DB_NAME = "droidify_v2.db"

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideSqlDriver(
        @ApplicationContext context: Context,
    ): SqlDriver = AndroidSqliteDriver(
        schema = DroidifyDb.Schema,
        context = context,
        name = DB_NAME,
        callback = object : AndroidSqliteDriver.Callback(DroidifyDb.Schema) {
            override fun onConfigure(db: SupportSQLiteDatabase) {
                super.onConfigure(db)
                db.setForeignKeyConstraintsEnabled(true)
            }
        },
    )

    @Singleton
    @Provides
    fun provideDroidifyDb(driver: SqlDriver): DroidifyDb = droidifyDb(driver)

    @Singleton
    @Provides
    fun providePrivacyRepository(
        settingsRepository: SettingsRepository,
    ): PrivacyRepository = PrivacyRepository(
        settingsRepo = settingsRepository,
    )
}
