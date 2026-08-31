package com.eggrice.timetable.ui.timetable

import com.eggrice.timetable.data.entity.CourseEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CourseMerger 边界用例：合并 + 冲突 + 单双周降级 + 区间减法。
 */
class CourseMergerTest {

    private fun course(
        name: String,
        day: Int,
        start: Int,
        end: Int,
        weekType: String = "all",
        weeks: String = "",
        colorIndex: Int = 0
    ) = CourseEntity(
        name = name, dayOfWeek = day, startSlot = start, endSlot = end,
        weekType = weekType, weeks = weeks, colorIndex = colorIndex
    )

    // ── 合并规则 ──

    @Test
    fun merge_adjacentSameNameSameColor_mergesIntoOneBlock() {
        val blocks = mergeCourses(
            listOf(course("高数", 1, 3, 4), course("高数", 1, 1, 2))
        )
        assertEquals(1, blocks.size)
        val b = blocks[0]
        assertEquals(1.0f, b.start)
        assertEquals(5.0f, b.end) // 1-4 节，含尾 → 开区间 [1.0, 5.0)
        assertEquals(2, b.courses.size)
        assertFalse(b.isConflict)
    }

    @Test
    fun merge_adjacentDifferentCourses_twoBlocks() {
        val blocks = mergeCourses(
            listOf(course("英语", 1, 3, 4), course("数学", 1, 1, 2))
        )
        assertEquals(2, blocks.size)
        assertEquals("数学", blocks[0].courses.first().name)
        assertEquals("英语", blocks[1].courses.first().name)
        assertFalse(blocks[0].isConflict)
        assertFalse(blocks[1].isConflict)
    }

    @Test
    fun merge_partialOverlap_isConflict() {
        // 课 A 1-3 节 + 课 B 2-4 节（部分重叠）
        val blocks = mergeCourses(
            listOf(course("A", 1, 2, 4), course("A", 1, 1, 3))
        )
        assertEquals(1, blocks.size)
        val b = blocks[0]
        assertTrue(b.isConflict)
        assertEquals(2, b.courses.size)
        assertEquals(1.0f, b.start)
        assertEquals(5.0f, b.end)
    }

    @Test
    fun merge_fullOverlap_isConflict() {
        // 同格两门选修（完全重叠）
        val blocks = mergeCourses(
            listOf(course("选修1", 2, 3, 4), course("选修2", 2, 3, 4))
        )
        assertEquals(1, blocks.size)
        val b = blocks[0]
        assertTrue(b.isConflict)
        assertEquals(2, b.courses.size)
        assertEquals(3.0f, b.start)
        assertEquals(5.0f, b.end)
    }

    @Test
    fun merge_differentDays_separateBlocks() {
        val blocks = mergeCourses(
            listOf(course("周一课", 1, 1, 2), course("周二课", 2, 1, 2))
        )
        assertEquals(2, blocks.size)
        assertEquals(1, blocks[0].day)
        assertEquals(2, blocks[1].day)
    }

    @Test
    fun merge_chainOverlap_singleConflictBlock() {
        // A 1-2 + B 2-3 + C 3-4：链式重叠应归入同一块
        val blocks = mergeCourses(
            listOf(course("A", 1, 1, 2), course("B", 1, 2, 3), course("C", 1, 3, 4))
        )
        assertEquals(1, blocks.size)
        assertEquals(3, blocks[0].courses.size)
        assertTrue(blocks[0].isConflict)
        assertEquals(1.0f, blocks[0].start)
        assertEquals(5.0f, blocks[0].end)
    }

    @Test
    fun merge_gapBetweenSameName_notMerged() {
        // 同名但隔开（1-2 与 5-6，中间 3-4 空档）→ 不合并
        val blocks = mergeCourses(
            listOf(course("高数", 1, 5, 6), course("高数", 1, 1, 2))
        )
        assertEquals(2, blocks.size)
    }

    @Test
    fun merge_emptyInput_returnsEmpty() {
        assertTrue(mergeCourses(emptyList()).isEmpty())
    }

    @Test
    fun merge_unsortedInput_sortedByDayAndSlot() {
        val blocks = mergeCourses(
            listOf(
                course("周三课", 3, 3, 4),
                course("周一晚", 1, 9, 10),
                course("周一早", 1, 1, 2)
            )
        )
        assertEquals(listOf(1, 1, 3), blocks.map { it.day })
        assertEquals(listOf(1.0f, 9.0f, 3.0f), blocks.map { it.start })
    }

    @Test
    fun merge_inclusiveEndConvention_startAndEnd() {
        // 单节课程 1-1 → [1.0, 2.0)；单节 3-3 → [3.0, 4.0)
        val b1 = mergeCourses(listOf(course("x", 1, 1, 1)))[0]
        assertEquals(1.0f, b1.start)
        assertEquals(2.0f, b1.end)
        val b3 = mergeCourses(listOf(course("x", 1, 3, 3)))[0]
        assertEquals(3.0f, b3.start)
        assertEquals(4.0f, b3.end)
    }

    // ── 单双周 / 周过滤 ──

    @Test
    fun isActive_oddWeek_oddType() {
        val odd = course("x", 1, 1, 2, weekType = "odd")
        val even = course("x", 1, 1, 2, weekType = "even")
        assertTrue(isCourseActiveInWeek(odd, 1))   // 单周
        assertFalse(isCourseActiveInWeek(odd, 2))  // 双周
        assertFalse(isCourseActiveInWeek(even, 1))
        assertTrue(isCourseActiveInWeek(even, 2))
        assertTrue(isCourseActiveInWeek(course("x", 1, 1, 2), 3)) // all 恒活跃
    }

    @Test
    fun isActive_weeksList_filtering() {
        val c = course("x", 1, 1, 2, weeks = "1,3,5")
        assertTrue(isCourseActiveInWeek(c, 1))
        assertTrue(isCourseActiveInWeek(c, 3))
        assertFalse(isCourseActiveInWeek(c, 2))
        // weeks 为空串 → 视为全部
        assertTrue(isCourseActiveInWeek(course("y", 1, 1, 2, weeks = ""), 7))
        // 未知周（<=0）→ 活跃
        assertTrue(isCourseActiveInWeek(c, 0))
    }

    @Test
    fun merge_currentWeek_oddEven_fullOverlap() {
        // 同格 A(单周) + B(双周)，当前单周：A 活跃、B 不活跃。
        // 块级语义：isVisualDemoted 只在"块内无本周活跃课程"时为 true；
        // 本例 A 活跃 → 不降级、不误报冲突；B 的区域被 A 完全覆盖 → 遮罩区间为空（B 不可见）。
        val blocks = mergeCourses(
            listOf(
                course("A", 1, 3, 4, weekType = "odd"),
                course("B", 1, 3, 4, weekType = "even")
            ),
            currentWeek = 1
        )
        assertEquals(1, blocks.size)
        val b = blocks[0]
        assertFalse(b.isConflict)
        assertFalse(b.isVisualDemoted)
        assertEquals(1, b.courses.count { isCourseActiveInWeek(it, 1) })
        // B 的区间被 A 完全覆盖 → 遮罩区域为空
        val maskRanges = subtractIntervals(
            active = listOf(3.0f to 5.0f),      // A 覆盖 [3,5)
            inactive = listOf(3.0f to 5.0f)     // B 占据 [3,5)
        )
        assertTrue(maskRanges.isEmpty())
    }

    @Test
    fun merge_currentWeek_oddEven_partialOverlap_maskRegion() {
        // 部分重叠：A 1-3 节(单周) + B 2-4 节(双周)，当前单周。
        // 块 [1,5)：A 活跃覆盖 [1,4)；B 不活跃，其 [2,5) 减去 A 的 [1,4) → 剩 [4,5) 画遮罩（P1 场景）。
        val blocks = mergeCourses(
            listOf(
                course("A", 1, 1, 3, weekType = "odd"),
                course("B", 1, 2, 4, weekType = "even")
            ),
            currentWeek = 1
        )
        assertEquals(1, blocks.size)
        val b = blocks[0]
        assertFalse(b.isConflict)
        assertFalse(b.isVisualDemoted) // A 活跃，块不降级
        val maskRanges = subtractIntervals(
            active = listOf(1.0f to 4.0f),   // A 活跃区间
            inactive = listOf(2.0f to 5.0f)  // B 区间
        )
        assertEquals(listOf(4.0f to 5.0f), maskRanges)
    }

    @Test
    fun merge_currentWeek_bothActive_conflict() {
        // 同格两门都上（双周）→ 冲突，不降级
        val blocks = mergeCourses(
            listOf(
                course("A", 1, 3, 4, weekType = "even"),
                course("B", 1, 3, 4, weekType = "even")
            ),
            currentWeek = 2
        )
        assertEquals(1, blocks.size)
        assertTrue(blocks[0].isConflict)
        assertFalse(blocks[0].isVisualDemoted)
    }

    @Test
    fun merge_noCurrentWeek_unknownWeek_noDemotion() {
        // 默认 currentWeek=-1 → 不降级，重叠即冲突
        val blocks = mergeCourses(
            listOf(course("A", 1, 1, 2, weekType = "odd"), course("B", 1, 1, 2, weekType = "even"))
        )
        assertTrue(blocks[0].isConflict)
        assertFalse(blocks[0].isVisualDemoted)
    }

    // ── 区间减法（单双周遮罩用）──

    @Test
    fun subtract_noOverlap_unchanged() {
        val result = subtractIntervals(
            active = listOf(3.0f to 4.0f),
            inactive = listOf(1.0f to 2.0f)
        )
        assertEquals(listOf(1.0f to 2.0f), result)
    }

    @Test
    fun subtract_partialOverlap_cutsBothSides() {
        val result = subtractIntervals(
            active = listOf(2.0f to 4.0f),
            inactive = listOf(1.0f to 5.0f)
        )
        assertEquals(listOf(1.0f to 2.0f, 4.0f to 5.0f), result)
    }

    @Test
    fun subtract_fullCover_removed() {
        val result = subtractIntervals(
            active = listOf(1.0f to 5.0f),
            inactive = listOf(1.0f to 5.0f)
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun subtract_multipleActive_cascadingCuts() {
        // 时光课表同款：非本周 [1,6) 被本周 [2,3) 和 [4,5) 逐段切割
        val result = subtractIntervals(
            active = listOf(2.0f to 3.0f, 4.0f to 5.0f),
            inactive = listOf(1.0f to 6.0f)
        )
        assertEquals(listOf(1.0f to 2.0f, 3.0f to 4.0f, 5.0f to 6.0f), result)
    }

    @Test
    fun subtract_activeOutsideRange_keepsAll() {
        val result = subtractIntervals(
            active = listOf(7.0f to 8.0f),
            inactive = listOf(1.0f to 5.0f)
        )
        assertEquals(listOf(1.0f to 5.0f), result)
    }

    @Test
    fun subtract_emptyInactive_returnsEmpty() {
        assertTrue(subtractIntervals(active = listOf(1.0f to 2.0f), inactive = emptyList()).isEmpty())
    }
}
