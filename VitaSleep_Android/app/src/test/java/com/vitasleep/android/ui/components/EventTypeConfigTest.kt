package com.vitasleep.android.ui.components

import com.vitasleep.android.ui.theme.Amber
import com.vitasleep.android.ui.theme.MintGreen
import com.vitasleep.android.ui.theme.SkyBlue
import org.junit.Assert.assertEquals
import org.junit.Test

class EventTypeConfigTest {

    @Test
    fun fixedType_returnsSkyBlue() {
        val config = getEventTypeConfig("fixed")
        assertEquals(SkyBlue, config.color)
        assertEquals("固定", config.label)
    }

    @Test
    fun workType_returnsMintGreen() {
        val config = getEventTypeConfig("work")
        assertEquals(MintGreen, config.color)
        assertEquals("工作", config.label)
    }

    @Test
    fun restType_returnsAmber() {
        val config = getEventTypeConfig("rest")
        assertEquals(Amber, config.color)
        assertEquals("休息", config.label)
    }

    @Test
    fun unknownType_defaultsToAmber() {
        val config = getEventTypeConfig("unknown_xyz")
        assertEquals(Amber, config.color)
    }
}
