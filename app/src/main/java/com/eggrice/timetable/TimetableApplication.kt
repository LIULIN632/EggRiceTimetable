package com.eggrice.timetable

import android.app.Application
import com.eggrice.timetable.data.database.TimetableDatabase
import com.eggrice.timetable.data.repository.CourseRepository
import com.eggrice.timetable.di.AppContainer
import com.eggrice.timetable.network.CookieStore
import com.eggrice.timetable.network.ZhengfangImportMemory
import com.eggrice.timetable.util.CrashHandler

class TimetableApplication : Application() {
    val database: TimetableDatabase by lazy { TimetableDatabase.getInstance(this) }
    val repository: CourseRepository by lazy { CourseRepository(database.courseDao(), database.teacherDao()) }
    val appContainer: AppContainer by lazy { AppContainer(this) }
    lateinit var crashHandler: CrashHandler

    override fun onCreate() {
        super.onCreate()
        crashHandler = CrashHandler(this)
        CookieStore.init(this)
        ZhengfangImportMemory.init(this)
    }
}
