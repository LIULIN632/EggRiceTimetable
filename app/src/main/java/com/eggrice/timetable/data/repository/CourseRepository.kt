package com.eggrice.timetable.data.repository

import com.eggrice.timetable.data.dao.CourseDao
import com.eggrice.timetable.data.entity.CourseEntity
import com.eggrice.timetable.data.entity.GoodItemEntity
import com.eggrice.timetable.data.entity.TreeHoleEntity
import com.eggrice.timetable.data.entity.HomeworkEntity
import com.eggrice.timetable.data.entity.SchemeEntity
import com.eggrice.timetable.data.entity.TaskEntity
import com.eggrice.timetable.data.entity.TimeSlotEntity
import kotlinx.coroutines.flow.Flow

class CourseRepository(private val dao: CourseDao) {
    val allCourses: Flow<List<CourseEntity>> = dao.getAllCourses()
    val allTimeSlots: Flow<List<TimeSlotEntity>> = dao.getAllTimeSlots()
    val allSchemes: Flow<List<SchemeEntity>> = dao.getAllSchemes()

    fun getCoursesByScheme(schemeId: Long): Flow<List<CourseEntity>> = dao.getCoursesByScheme(schemeId)

    suspend fun getById(id: Long) = dao.getById(id)
    suspend fun insert(course: CourseEntity) = dao.insert(course)
    suspend fun update(course: CourseEntity) = dao.update(course)
    suspend fun delete(course: CourseEntity) = dao.delete(course)
    suspend fun deleteById(id: Long) = dao.deleteById(id)
    suspend fun deleteAll() = dao.deleteAll()
    suspend fun deleteByScheme(schemeId: Long) = dao.deleteByScheme(schemeId)
    suspend fun deleteByTimeSlot(day: Int, startSlot: Int, endSlot: Int, schemeId: Long) =
        dao.deleteByTimeSlot(day, startSlot, endSlot, schemeId)
    suspend fun deleteByName(name: String, schemeId: Long) = dao.deleteByName(name, schemeId)
    suspend fun insertAll(courses: List<CourseEntity>) {
        if (courses.isNotEmpty()) dao.insertAll(courses)
    }
    suspend fun updateAll(courses: List<CourseEntity>) {
        if (courses.isNotEmpty()) dao.updateAll(courses)
    }

    // ── Scheme operations ──
    suspend fun createScheme(name: String): Long {
        val count = dao.getSchemeCount()
        return dao.insertScheme(SchemeEntity(name = name, sortOrder = count))
    }
    suspend fun updateScheme(scheme: SchemeEntity) = dao.updateScheme(scheme)
    suspend fun deleteScheme(id: Long) = dao.deleteScheme(id)
    suspend fun getSchemeById(id: Long) = dao.getSchemeById(id)

    suspend fun deleteAllTimeSlots() = dao.deleteAllTimeSlots()
    suspend fun replaceTimeSlots(slots: List<TimeSlotEntity>) {
        dao.deleteAllTimeSlots()
        if (slots.isNotEmpty()) dao.insertTimeSlots(slots)
    }

    // ── Homework operations ──
    fun getHomeworkByScheme(schemeId: Long) = dao.getHomeworkByScheme(schemeId)
    fun getAllHomework() = dao.getAllHomework()
    suspend fun insertHomework(homework: HomeworkEntity) = dao.insertHomework(homework)
    suspend fun updateHomework(homework: HomeworkEntity) = dao.updateHomework(homework)
    suspend fun setHomeworkCompleted(id: Long, completed: Boolean) = dao.setHomeworkCompleted(id, completed)
    suspend fun deleteHomework(id: Long) = dao.deleteHomework(id)
    fun getActiveHomeworkCourseNames(schemeId: Long) = dao.getActiveHomeworkCourseNames(schemeId)

    // ── Task operations ──
    fun getTasksByScheme(schemeId: Long) = dao.getTasksByScheme(schemeId)
    suspend fun insertTask(task: TaskEntity) = dao.insertTask(task)
    suspend fun updateTask(task: TaskEntity) = dao.updateTask(task)
    suspend fun deleteTask(id: Long) = dao.deleteTask(id)

    // ── Good Item operations ──
    fun getGoodItemsByScheme(schemeId: Long) = dao.getGoodItemsByScheme(schemeId)
    suspend fun insertGoodItem(item: GoodItemEntity) = dao.insertGoodItem(item)
    suspend fun updateGoodItem(item: GoodItemEntity) = dao.updateGoodItem(item)
    suspend fun deleteGoodItem(id: Long) = dao.deleteGoodItem(id)

    // ── Tree Hole operations ──
    fun getTreeHolesByScheme(schemeId: Long) = dao.getTreeHolesByScheme(schemeId)
    suspend fun insertTreeHole(item: TreeHoleEntity) = dao.insertTreeHole(item)
    suspend fun deleteTreeHole(id: Long) = dao.deleteTreeHole(id)

    // 首次启动填充默认12节课时间段；仅在time_slots表为空时调用
    suspend fun initTimeSlots() {
        val defaultSlots = listOf(
            TimeSlotEntity(1, "08:00", "08:45"),
            TimeSlotEntity(2, "08:55", "09:40"),
            TimeSlotEntity(3, "10:00", "10:45"),
            TimeSlotEntity(4, "10:55", "11:40"),
            TimeSlotEntity(5, "13:30", "14:15"),
            TimeSlotEntity(6, "14:25", "15:10"),
            TimeSlotEntity(7, "15:30", "16:15"),
            TimeSlotEntity(8, "16:25", "17:10"),
            TimeSlotEntity(9, "18:30", "19:15"),
            TimeSlotEntity(10, "19:25", "20:10"),
            TimeSlotEntity(11, "20:20", "21:05"),
            TimeSlotEntity(12, "21:15", "22:00")
        )
        dao.insertTimeSlots(defaultSlots)
    }
}
