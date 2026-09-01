package com.eggrice.timetable.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AppUpdateChecker 版本比较边界用例。
 */
class AppUpdateCheckerTest {

    private val checker = AppUpdateChecker()

    @Test
    fun isNewer_patchBump_true() {
        assertTrue(checker.isNewer("v11.0.17", "11.0.16"))
        assertTrue(checker.isNewer("11.0.16", "11.0.15"))
    }

    @Test
    fun isNewer_minorBump_true() {
        assertTrue(checker.isNewer("v11.1.0", "11.0.16"))
        assertTrue(checker.isNewer("v12.0.0", "11.99.99"))
    }

    @Test
    fun isNewer_majorBump_true() {
        assertTrue(checker.isNewer("v12.0.0", "11.0.16"))
    }

    @Test
    fun isNewer_sameOrOlder_false() {
        assertFalse(checker.isNewer("v11.0.16", "11.0.16")) // 相同
        assertFalse(checker.isNewer("v11.0.15", "11.0.16")) // 更旧
        assertFalse(checker.isNewer("v10.9.0", "11.0.0"))   // 主版本更旧
    }

    @Test
    fun isNewer_malformed_false() {
        assertFalse(checker.isNewer("", "11.0.16"))
        assertFalse(checker.isNewer("abc", "11.0.16"))
        assertFalse(checker.isNewer("v11", "11.0.16"))       // 不足三段
        assertFalse(checker.isNewer("v11.0.16", "bad"))       // 当前版本非法
    }

    @Test
    fun isNewer_withVAndWithoutV() {
        assertTrue(checker.isNewer("v11.0.16", "11.0.15"))
        assertTrue(checker.isNewer("11.0.16", "v11.0.15"))
    }
}
