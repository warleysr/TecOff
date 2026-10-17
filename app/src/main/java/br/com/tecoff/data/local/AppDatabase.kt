package br.com.tecoff.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import br.com.tecoff.data.local.dao.*
import br.com.tecoff.data.local.entity.*

@Database(
    entities = [
        PastaEntity::class,
        SubpastaEntity::class,
        CadernoEntity::class,
        QuestionEntity::class,
        CadernoQuestionCrossRef::class,
        ResolutionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cadernoDao(): CadernoDao
    abstract fun questionDao(): QuestionDao
    abstract fun resolutionDao(): ResolutionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tecoff.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
