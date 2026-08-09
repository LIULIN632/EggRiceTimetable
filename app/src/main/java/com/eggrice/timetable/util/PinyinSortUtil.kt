package com.eggrice.timetable.util

/**
 * 轻量拼音首字母工具（零依赖）：基于 GB2312 区位码区间映射首字母，
 * 用于学校列表按拼音首字母排序。对英文/数字开头名称取原字符。
 */
object PinyinSortUtil {

    // 各首字母区间起点（区位码 = 区*100 + 位），对应分界字：啊芭擦搭蛾发噶哈击喀垃妈拿哦啪期然撒塌挖昔压匝
    private val BOUNDARIES = intArrayOf(
        1601, 1637, 1833, 2078, 2274, 2302, 2433, 2594, 2787,
        3106, 3212, 3472, 3635, 3722, 3730, 3858, 4027, 4086,
        4390, 4558, 4684, 4925, 5249
    )
    private val LETTERS = "ABCDEFGHJKLMNOPQRSTWXYZ"

    /** 取单个字符的拼音首字母；非汉字返回原大写字母；其他字符返回 null */
    fun firstLetter(ch: Char): Char? {
        if (ch.code < 0x4E00 || ch.code > 0x9FA5) {
            return if (ch.isLetter()) ch.uppercaseChar() else null
        }
        val code = gb2312Code(ch) ?: return null
        var i = BOUNDARIES.size - 1
        while (i >= 0) {
            if (code >= BOUNDARIES[i]) return LETTERS[i]
            i--
        }
        return null
    }

    /** 生成排序用首字母串（如「广东药科大学」→ GDYKDX） */
    fun sortKey(name: String): String = buildString {
        for (c in name) {
            val f = firstLetter(c) ?: continue
            append(f)
        }
    }

    private fun gb2312Code(ch: Char): Int? = try {
        val bytes = ch.toString().toByteArray(java.nio.charset.Charset.forName("GB2312"))
        if (bytes.size == 2) {
            val qu = (bytes[0].toInt() and 0xFF) - 0xA0
            val wei = (bytes[1].toInt() and 0xFF) - 0xA0
            if (qu in 1..94 && wei in 1..94) qu * 100 + wei else null
        } else null
    } catch (_: Exception) {
        null
    }
}
