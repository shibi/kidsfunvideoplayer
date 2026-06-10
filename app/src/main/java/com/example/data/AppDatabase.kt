package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Video::class, WatchStatus::class, AnalyticsEvent::class, AppSetting::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun watchStatusDao(): WatchStatusDao
    abstract fun analyticsDao(): AnalyticsDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kids_playtube_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        // Returns a pre-loaded safe default list of high-quality kids play content.
        fun getPreloadedVideos(): List<Video> {
            return listOf(
                Video(
                    id = "wr83zLFr78g",
                    title = "CoComelon - Wheels on the Bus",
                    url = "https://www.youtube.com/watch?v=wr83zLFr78g",
                    category = "Music"
                ),
                Video(
                    id = "rAL8X3b4gH0",
                    title = "Bluey - Playtime & Keepy Uppy!",
                    url = "https://www.youtube.com/watch?v=rAL8X3b4gH0",
                    category = "Cartoons"
                ),
                Video(
                    id = "680gI1A6A_Q",
                    title = "Peppa Pig - Playroom Fun & Games!",
                    url = "https://www.youtube.com/watch?v=680gI1A6A_Q",
                    category = "Cartoons"
                ),
                Video(
                    id = "yCjJyiqpAuU",
                    title = "Super Simple Songs - Twinkle Twinkle Little Star",
                    url = "https://www.youtube.com/watch?v=yCjJyiqpAuU",
                    category = "Music"
                ),
                Video(
                    id = "uC2_B6E-pUo",
                    title = "Blippi - Fun Dinosaur Exploration!",
                    url = "https://www.youtube.com/watch?v=uC2_B6E-pUo",
                    category = "Learning"
                ),
                Video(
                    id = "vL10g8-XWIs",
                    title = "Numberblocks - Learn Counting from 1 to 10!",
                    url = "https://www.youtube.com/watch?v=vL10g8-XWIs",
                    category = "Learning"
                )
            )
        }
    }
}
