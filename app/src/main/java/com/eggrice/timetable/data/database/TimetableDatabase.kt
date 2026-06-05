package com.eggrice.timetable.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.eggrice.timetable.data.entity.CourseEntity
import com.eggrice.timetable.data.entity.GoodItemEntity
import com.eggrice.timetable.data.entity.HomeworkEntity
import com.eggrice.timetable.data.entity.TreeHoleEntity
import com.eggrice.timetable.data.entity.SchemeEntity
import com.eggrice.timetable.data.entity.TaskEntity
import com.eggrice.timetable.data.entity.TimeSlotEntity
import com.eggrice.timetable.data.dao.CourseDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [CourseEntity::class, TimeSlotEntity::class, SchemeEntity::class, HomeworkEntity::class, TaskEntity::class, GoodItemEntity::class, TreeHoleEntity::class], version = 8, exportSchema = false)
abstract class TimetableDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao

    companion object {
        @Volatile private var INSTANCE: TimetableDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS schemes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, sortOrder INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("ALTER TABLE courses ADD COLUMN schemeId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("INSERT INTO schemes (id, name, sortOrder) VALUES (0, '默认课表', 0)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS homework (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, courseName TEXT NOT NULL, content TEXT NOT NULL DEFAULT '', dueDate TEXT NOT NULL DEFAULT '', createdAt INTEGER NOT NULL DEFAULT 0, completed INTEGER NOT NULL DEFAULT 0, schemeId INTEGER NOT NULL DEFAULT 0)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS tasks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, completed INTEGER NOT NULL DEFAULT 0, sortOrder INTEGER NOT NULL DEFAULT 0, schemeId INTEGER NOT NULL DEFAULT 0)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS good_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, category TEXT NOT NULL DEFAULT '', reason TEXT NOT NULL DEFAULT '', description TEXT NOT NULL DEFAULT '', referencePrice TEXT NOT NULL DEFAULT '', purchased INTEGER NOT NULL DEFAULT 0, sortOrder INTEGER NOT NULL DEFAULT 0, schemeId INTEGER NOT NULL DEFAULT 0)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS tree_holes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, content TEXT NOT NULL, createdAt INTEGER NOT NULL DEFAULT 0, schemeId INTEGER NOT NULL DEFAULT 0)")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tree_holes ADD COLUMN author TEXT NOT NULL DEFAULT ''")
                // Seed default tree hole messages
                db.execSQL("INSERT INTO tree_holes (content, author, createdAt, schemeId) VALUES ('蛋炒饭好吃', '爱吃蛋炒饭', ${System.currentTimeMillis()}, 0)")
                db.execSQL("INSERT INTO tree_holes (content, author, createdAt, schemeId) VALUES ('加油', '梦梦', ${System.currentTimeMillis()}, 0)")
            }
        }

        /** Ensure default scheme exists on fresh installs (DB created at v4, no migration runs). */
        private val CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL("INSERT OR IGNORE INTO schemes (id, name, sortOrder) VALUES (0, '默认课表', 0)")
                db.execSQL("INSERT OR IGNORE INTO time_slots (slot, startTime, endTime) VALUES (1, '08:00', '08:45')")
                db.execSQL("INSERT OR IGNORE INTO time_slots (slot, startTime, endTime) VALUES (2, '08:55', '09:40')")
                db.execSQL("INSERT OR IGNORE INTO time_slots (slot, startTime, endTime) VALUES (3, '10:00', '10:45')")
                db.execSQL("INSERT OR IGNORE INTO time_slots (slot, startTime, endTime) VALUES (4, '10:55', '11:40')")
                db.execSQL("INSERT OR IGNORE INTO time_slots (slot, startTime, endTime) VALUES (5, '13:30', '14:15')")
                db.execSQL("INSERT OR IGNORE INTO time_slots (slot, startTime, endTime) VALUES (6, '14:25', '15:10')")
                db.execSQL("INSERT OR IGNORE INTO time_slots (slot, startTime, endTime) VALUES (7, '15:30', '16:15')")
                db.execSQL("INSERT OR IGNORE INTO time_slots (slot, startTime, endTime) VALUES (8, '16:25', '17:10')")
                db.execSQL("INSERT OR IGNORE INTO time_slots (slot, startTime, endTime) VALUES (9, '18:30', '19:15')")
                db.execSQL("INSERT OR IGNORE INTO time_slots (slot, startTime, endTime) VALUES (10, '19:25', '20:10')")
                db.execSQL("INSERT OR IGNORE INTO time_slots (slot, startTime, endTime) VALUES (11, '20:20', '21:05')")
                db.execSQL("INSERT OR IGNORE INTO time_slots (slot, startTime, endTime) VALUES (12, '21:15', '22:00')")
            }
        }

        fun getInstance(context: Context): TimetableDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context, TimetableDatabase::class.java, "timetable.db")
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .addCallback(CALLBACK)
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
