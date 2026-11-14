package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProjectEntity::class,
        RecentFileEntity::class,
        OpenTabEntity::class,
        EditorDraftEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class IdeDatabase : RoomDatabase() {
    abstract fun ideDao(): IdeDao

    companion object {
        @Volatile
        private var INSTANCE: IdeDatabase? = null

        fun getInstance(context: Context): IdeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    IdeDatabase::class.java,
                    "mobile_code_studio.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
