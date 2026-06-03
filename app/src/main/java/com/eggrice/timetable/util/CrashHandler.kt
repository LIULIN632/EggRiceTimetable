package com.eggrice.timetable.util

import android.app.Application
import android.os.Build
import android.os.Environment
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashHandler(
    private val application: Application
) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        try {
            writeCrashLog(e)
        } catch (_: Exception) {
            // silently fail — don't crash the crash handler
        }
        defaultHandler?.uncaughtException(t, e)
    }

    private fun writeCrashLog(e: Throwable) {
        val logDir = getLogDir()
        if (logDir == null || !logDir.exists()) {
            logDir?.mkdirs()
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val logFile = File(logDir, "crash_$timestamp.txt")

        val sw = StringWriter()
        val pw = PrintWriter(sw)
        pw.println("========================================")
        pw.println("蛋炒饭课程表 - 崩溃日志")
        pw.println("========================================")
        pw.println("崩溃时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
        pw.println("设备型号: ${Build.MODEL} (${Build.MANUFACTURER})")
        pw.println("系统版本: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        pw.println("APP版本: ${getAppVersion()}")
        pw.println("----------------------------------------")
        pw.println("错误堆栈:")
        pw.println("----------------------------------------")
        e.printStackTrace(pw)
        pw.println("----------------------------------------")
        pw.flush()
        pw.close()

        logFile.writeText(sw.toString(), Charsets.UTF_8)
    }

    fun getLogDir(): File? {
        return try {
            val dir = application.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (dir != null) File(dir, "crash_logs") else null
        } catch (_: Exception) {
            null
        }
    }

    fun getLatestCrashLog(): File? {
        val dir = getLogDir() ?: return null
        if (!dir.exists()) return null
        val files = dir.listFiles { f -> f.isFile && f.name.startsWith("crash_") && f.name.endsWith(".txt") }
        return files?.maxByOrNull { it.lastModified() }
    }

    private fun getAppVersion(): String {
        return try {
            val pkgInfo = application.packageManager.getPackageInfo(application.packageName, 0)
            pkgInfo.versionName ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
    }
}
