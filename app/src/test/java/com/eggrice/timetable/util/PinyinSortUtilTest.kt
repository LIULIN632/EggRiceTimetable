package com.eggrice.timetable.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PinyinSortUtilTest {

    @Test
    fun firstLetterCommonChars() {
        assertEquals('B', PinyinSortUtil.firstLetter('北'))
        assertEquals('D', PinyinSortUtil.firstLetter('大'))
        assertEquals('G', PinyinSortUtil.firstLetter('广'))
        assertEquals('S', PinyinSortUtil.firstLetter('苏'))
        assertEquals('T', PinyinSortUtil.firstLetter('同'))
        assertEquals('Q', PinyinSortUtil.firstLetter('庆'))
        assertEquals('H', PinyinSortUtil.firstLetter('华'))
        assertEquals('N', PinyinSortUtil.firstLetter('南'))
        assertEquals('Y', PinyinSortUtil.firstLetter('药'))
        assertEquals('X', PinyinSortUtil.firstLetter('学'))
    }

    @Test
    fun sortKeySchoolNames() {
        assertEquals("GDYKDX", PinyinSortUtil.sortKey("广东药科大学"))
        assertEquals("BJDX", PinyinSortUtil.sortKey("北京大学"))
        assertEquals("HNLGDX", PinyinSortUtil.sortKey("华南理工大学"))
        assertEquals("TJDX", PinyinSortUtil.sortKey("同济大学"))
        assertEquals("SDSFDX", PinyinSortUtil.sortKey("山东师范大学"))
    }

    @Test
    fun sortKeyNonChinese() {
        assertEquals("SWUFE", PinyinSortUtil.sortKey("SWUFE"))
        assertEquals("DX", PinyinSortUtil.sortKey("123大学"))
        assertEquals("", PinyinSortUtil.sortKey(""))
        assertEquals("", PinyinSortUtil.sortKey("123"))
        assertNull(PinyinSortUtil.firstLetter('1'))
    }
}
