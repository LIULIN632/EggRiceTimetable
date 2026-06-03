package com.eggrice.timetable.data.dao

import androidx.room.*
import com.eggrice.timetable.data.entity.CourseEntity
import com.eggrice.timetable.data.entity.GoodItemEntity
import com.eggrice.timetable.data.entity.TreeHoleEntity
import com.eggrice.timetable.data.entity.HomeworkEntity
import com.eggrice.timetable.data.entity.SchemeEntity
import com.eggrice.timetable.data.entity.TaskEntity
import com.eggrice.timetable.data.entity.TimeSlotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    // ── Courses ──
    @Query("SELECT * FROM courses WHERE schemeId = :schemeId ORDER BY dayOfWeek, startSlot")
    fun getCoursesByScheme(schemeId: Long): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses ORDER BY dayOfWeek, startSlot")
    fun getAllCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun getById(id: Long): CourseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(course: CourseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(courses: List<CourseEntity>): List<Long>

    @Update
    suspend fun update(course: CourseEntity)

    @Update
    suspend fun updateAll(courses: List<CourseEntity>)

    @Delete
    suspend fun delete(course: CourseEntity)

    @Query("DELETE FROM courses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM courses")
    suspend fun deleteAll()

    @Query("DELETE FROM courses WHERE schemeId = :schemeId")
    suspend fun deleteByScheme(schemeId: Long)

    @Query("DELETE FROM courses WHERE dayOfWeek = :day AND startSlot = :startSlot AND endSlot = :endSlot AND schemeId = :schemeId")
    suspend fun deleteByTimeSlot(day: Int, startSlot: Int, endSlot: Int, schemeId: Long)

    @Query("DELETE FROM courses WHERE name = :name AND schemeId = :schemeId")
    suspend fun deleteByName(name: String, schemeId: Long)

    // ── Schemes ──
    @Query("SELECT * FROM schemes ORDER BY sortOrder")
    fun getAllSchemes(): Flow<List<SchemeEntity>>

    @Query("SELECT * FROM schemes WHERE id = :id")
    suspend fun getSchemeById(id: Long): SchemeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheme(scheme: SchemeEntity): Long

    @Update
    suspend fun updateScheme(scheme: SchemeEntity)

    @Query("DELETE FROM schemes WHERE id = :id")
    suspend fun deleteScheme(id: Long)

    @Query("SELECT COUNT(*) FROM schemes")
    suspend fun getSchemeCount(): Int

    // ── Time slots ──
    @Query("SELECT * FROM time_slots ORDER BY slot")
    fun getAllTimeSlots(): Flow<List<TimeSlotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeSlots(slots: List<TimeSlotEntity>)

    @Query("DELETE FROM time_slots")
    suspend fun deleteAllTimeSlots()

    // ── Homework ──
    @Query("SELECT * FROM homework WHERE schemeId = :schemeId ORDER BY createdAt DESC")
    fun getHomeworkByScheme(schemeId: Long): Flow<List<HomeworkEntity>>

    @Query("SELECT * FROM homework ORDER BY createdAt DESC")
    fun getAllHomework(): Flow<List<HomeworkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomework(homework: HomeworkEntity): Long

    @Update
    suspend fun updateHomework(homework: HomeworkEntity)

    @Query("UPDATE homework SET completed = :completed WHERE id = :id")
    suspend fun setHomeworkCompleted(id: Long, completed: Boolean)

    @Query("DELETE FROM homework WHERE id = :id")
    suspend fun deleteHomework(id: Long)

    @Query("SELECT DISTINCT courseName FROM homework WHERE schemeId = :schemeId AND completed = 0")
    fun getActiveHomeworkCourseNames(schemeId: Long): Flow<List<String>>

    // ── Tasks ──
    @Query("SELECT * FROM tasks WHERE schemeId = :schemeId ORDER BY sortOrder")
    fun getTasksByScheme(schemeId: Long): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    // ── Good Items ──
    @Query("SELECT * FROM good_items WHERE schemeId = :schemeId ORDER BY sortOrder")
    fun getGoodItemsByScheme(schemeId: Long): Flow<List<GoodItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoodItem(item: GoodItemEntity): Long

    @Update
    suspend fun updateGoodItem(item: GoodItemEntity)

    @Query("DELETE FROM good_items WHERE id = :id")
    suspend fun deleteGoodItem(id: Long)

    // ── Tree Hole ──
    @Query("SELECT * FROM tree_holes WHERE schemeId = :schemeId ORDER BY createdAt DESC")
    fun getTreeHolesByScheme(schemeId: Long): Flow<List<TreeHoleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTreeHole(item: TreeHoleEntity): Long

    @Query("DELETE FROM tree_holes WHERE id = :id")
    suspend fun deleteTreeHole(id: Long)
}
