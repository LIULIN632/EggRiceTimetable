package com.eggrice.timetable.ui.timetable

import com.eggrice.timetable.data.entity.CourseEntity

/**
 * 课表合并展示模型与算法（纯 Kotlin，无 Compose / Room 依赖，可直接 JUnit 单测）。
 *
 * 坐标约定（与 PeriodGrid 现有渲染一致）：`endSlot` **含尾**（1-4 节 = span 4）；
 * 转 Float 逻辑刻度时 `end = endSlot + 1.0f`，即 "1-4 节" = `start 1.0f .. end 5.0f`（开区间尾）。
 * 用 Float 而非 Int：为将来支持自定义时间课程的比例渲染（timeToLogicalScale 那套）留出坐标空间。
 */
data class MergedCourseBlock(
    val courses: List<CourseEntity>,
    val day: Int,            // 星期几 1-7
    val start: Float,        // 逻辑节次坐标（开区间起点，如 1.0f）
    val end: Float,          // 逻辑节次坐标（开区间尾，如 5.0f 表示覆盖 1-4 节）
    val isConflict: Boolean = false,     // 块内本周活跃课程 ≥2（重叠冲突）
    val isVisualDemoted: Boolean = false // 块内本周活跃课程 = 0（整块视觉降级，如"本周不上的单双周课"）
)

/**
 * 判断课程在指定周是否上课（与 [TimetableViewModel] 的 filteredCourses 过滤逻辑保持一致）：
 * - weekType "odd"/"even" 按奇偶周过滤
 * - weeks 为逗号分隔的具体周次列表，非空且可解析时按列表过滤
 * - week <= 0（未知周）→ 一律视为活跃，避免误降级
 */
fun isCourseActiveInWeek(course: CourseEntity, week: Int): Boolean {
    if (week <= 0) return true
    val isOdd = week % 2 == 1
    val wt = course.weekType
    if (wt == "odd" && !isOdd) return false
    if (wt == "even" && isOdd) return false
    if (course.weeks.isNotEmpty()) {
        val wks = course.weeks.split(",").mapNotNull { it.toIntOrNull() }
        if (wks.isNotEmpty() && week !in wks) return false
    }
    return true
}

/**
 * 贪心合并课程块：
 * 1. 按 (星期, 开始节次, 结束节次) 排序；
 * 2. 与上一块**同天且重叠**（start < last.end）→ 并入同一块并标记 isConflict；
 * 3. 与上一块**同天相邻且同名同色**（start == last.end）→ 合并成连续大块（把拆开的两段显示成连续）；
 * 4. 否则新开一块。
 *
 * @param currentWeek 传入 > 0 时按周重算 isConflict（活跃课程 ≥2）与 isVisualDemoted（活跃 = 0）；
 *                    传默认值 -1 时不做周判定（全部视为活跃），isConflict 即"块内课程数 > 1"。
 */
fun mergeCourses(courses: List<CourseEntity>, currentWeek: Int = -1): List<MergedCourseBlock> {
    val sorted = courses.sortedWith(
        compareBy({ it.dayOfWeek }, { it.startSlot }, { it.endSlot })
    )
    val blocks = mutableListOf<MergedCourseBlock>()
    for (c in sorted) {
        val start = c.startSlot.toFloat()
        val end = (c.endSlot + 1).toFloat() // 含尾约定 → 开区间
        val last = blocks.lastOrNull()
        when {
            // 同天重叠 → 并入上一块（冲突）
            last != null && last.day == c.dayOfWeek && start < last.end -> {
                blocks[blocks.lastIndex] = last.copy(
                    courses = last.courses + c,
                    end = maxOf(last.end, end),
                    isConflict = true
                )
            }
            // 同天相邻且同名同色 → 合并连续大块
            last != null && last.day == c.dayOfWeek
                && start == last.end
                && last.courses.first().name == c.name
                && last.courses.first().colorIndex == c.colorIndex -> {
                blocks[blocks.lastIndex] = last.copy(
                    courses = last.courses + c,
                    end = maxOf(last.end, end)
                )
            }
            else -> blocks += MergedCourseBlock(
                courses = listOf(c),
                day = c.dayOfWeek,
                start = start,
                end = end
            )
        }
    }
    if (currentWeek > 0) {
        blocks.forEachIndexed { i, b ->
            val activeCount = b.courses.count { isCourseActiveInWeek(it, currentWeek) }
            blocks[i] = b.copy(
                isConflict = activeCount > 1,
                isVisualDemoted = activeCount == 0
            )
        }
    }
    return blocks
}

/**
 * 区间减法：从 [inactive] 区间中逐段减去 [active] 区间覆盖的部分，返回剩余的"纯非活跃"区间。
 *
 * 用途（单双周遮罩）：块内"本周不上"的课程区域，被"本周上课"课程覆盖的部分不画遮罩，
 * 剩下的才画压暗/斜纹。与时光课表 nonActiveRanges 的区间切割算法一致。
 *
 * 示例：inactive=[1.0..5.0]，active=[2.0..4.0] → 返回 [1.0..2.0, 4.0..5.0]。
 */
fun subtractIntervals(
    active: List<Pair<Float, Float>>,
    inactive: List<Pair<Float, Float>>
): List<Pair<Float, Float>> {
    if (inactive.isEmpty()) return emptyList()
    val result = mutableListOf<Pair<Float, Float>>()
    inactive.forEach { (naStart, naEnd) ->
        var segments = listOf(naStart to naEnd)
        active.forEach { (aStart, aEnd) ->
            val next = mutableListOf<Pair<Float, Float>>()
            segments.forEach { (sStart, sEnd) ->
                if (aStart >= sEnd || aEnd <= sStart) {
                    // 无交集，保留原片段
                    next.add(sStart to sEnd)
                } else {
                    // 有交集，切割：左侧 [sStart, aStart] + 右侧 [aEnd, sEnd]
                    if (aStart > sStart) next.add(sStart to aStart)
                    if (aEnd < sEnd) next.add(aEnd to sEnd)
                }
            }
            segments = next
        }
        result.addAll(segments)
    }
    return result
}
