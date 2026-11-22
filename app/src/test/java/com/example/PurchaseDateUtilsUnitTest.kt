package com.example

import com.example.data.model.PurchaseDatePrecision
import com.example.data.model.PurchaseDateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class PurchaseDateUtilsUnitTest {

    @Test
    fun precisionFromString_handlesValidAndUnknownValues() {
        assertEquals(PurchaseDatePrecision.FULL_DATE, PurchaseDatePrecision.fromString("FULL_DATE"))
        assertEquals(PurchaseDatePrecision.MONTH_YEAR, PurchaseDatePrecision.fromString("MONTH_YEAR"))
        assertEquals(PurchaseDatePrecision.YEAR_ONLY, PurchaseDatePrecision.fromString("YEAR_ONLY"))
        assertEquals(PurchaseDatePrecision.FULL_DATE, PurchaseDatePrecision.fromString("UNKNOWN"))
        assertEquals(PurchaseDatePrecision.FULL_DATE, PurchaseDatePrecision.fromString(null))
    }

    @Test
    fun normalizeAndFormat_respectsPrecision() {
        val fullMillis = PurchaseDateUtils.normalizeFullDateMillis(2022, Calendar.MAY, 14)
        val fullText = PurchaseDateUtils.formatPurchaseDate(fullMillis, PurchaseDatePrecision.FULL_DATE)
        assertTrue("Expected May 14, 2022 in '$fullText'", fullText.contains("2022") && fullText.contains("14"))

        val monthYearMillis = PurchaseDateUtils.normalizeMonthYearMillis(2021, Calendar.OCTOBER)
        val monthYearText = PurchaseDateUtils.formatPurchaseDate(monthYearMillis, PurchaseDatePrecision.MONTH_YEAR)
        assertTrue("Expected Oct 2021 in '$monthYearText'", monthYearText.contains("2021") && monthYearText.contains("Oct"))
        assertEquals(false, monthYearText.contains(","))

        val yearOnlyMillis = PurchaseDateUtils.normalizeYearOnlyMillis(1978)
        assertEquals("1978", PurchaseDateUtils.formatPurchaseDate(yearOnlyMillis, PurchaseDatePrecision.YEAR_ONLY))

        assertEquals("Not set", PurchaseDateUtils.formatPurchaseDate(null, PurchaseDatePrecision.FULL_DATE))
        assertEquals("Not set", PurchaseDateUtils.formatPurchaseDate(0L, PurchaseDatePrecision.YEAR_ONLY))
    }

    @Test
    fun normalizeFullDateMillis_clampsInvalidDayForMonth() {
        val febMillis = PurchaseDateUtils.normalizeFullDateMillis(2023, Calendar.FEBRUARY, 31)
        val cal = Calendar.getInstance().apply { timeInMillis = febMillis }
        assertEquals(2023, cal.get(Calendar.YEAR))
        assertEquals(Calendar.FEBRUARY, cal.get(Calendar.MONTH))
        assertEquals(28, cal.get(Calendar.DAY_OF_MONTH))
    }
}
