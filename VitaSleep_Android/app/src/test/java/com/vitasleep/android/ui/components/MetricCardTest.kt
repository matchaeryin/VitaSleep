package com.vitasleep.android.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class MetricCardDisplayTest {

    @Test
    fun nullValue_displaysDashPlaceholder() {
        val displayValue = formatMetricValue(null)
        assertEquals("—", displayValue)
    }

    @Test
    fun zeroValue_displaysActualZero() {
        val displayValue = formatMetricValue(0)
        assertEquals("0", displayValue)
    }

    @Test
    fun positiveValue_displaysValue() {
        val displayValue = formatMetricValue(72)
        assertEquals("72", displayValue)
    }

    @Test
    fun bloodPressurePair_formatsCorrectly() {
        val displayValue = formatBloodPressure(118, 76)
        assertEquals("118/76", displayValue)
    }

    @Test
    fun nullBloodPressure_displaysDash() {
        val displayValue = formatBloodPressure(null, null)
        assertEquals("—", displayValue)
    }
}
