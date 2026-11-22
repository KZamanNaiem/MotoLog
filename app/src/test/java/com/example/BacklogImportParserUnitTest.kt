package com.example

import com.example.data.backlog.BacklogImportMode
import com.example.data.backlog.BacklogImportParser
import org.junit.Assert.*
import org.junit.Test

class BacklogImportParserUnitTest {

    @Test
    fun parseDateFormats_supportsMultipleCommonFormats() {
        assertNotNull(BacklogImportParser.parseDateToMillis("2026-06-12"))
        assertNotNull(BacklogImportParser.parseDateToMillis("2026/06/12"))
        assertNotNull(BacklogImportParser.parseDateToMillis("12/06/2026"))
        assertNotNull(BacklogImportParser.parseDateToMillis("Jun 12, 2026"))
        assertNotNull(BacklogImportParser.parseDateToMillis("12 Jun 2026"))
        assertEquals(1700000000000L, BacklogImportParser.parseDateToMillis("1700000000000"))
        assertEquals(1700000000000L, BacklogImportParser.parseDateToMillis("1700000000"))
        assertNull(BacklogImportParser.parseDateToMillis("not-a-date"))
    }

    @Test
    fun parseSampleTemplates_parsesAllThreeCategoriesAndMixed() {
        val fuelRes = BacklogImportParser.parseInput(
            BacklogImportParser.getSampleTemplate(BacklogImportMode.FUEL_CSV),
            vehicleId = 7L,
            mode = BacklogImportMode.FUEL_CSV
        )
        assertEquals(3, fuelRes.fuelLogs.size)
        assertTrue(fuelRes.warnings.isEmpty())
        assertEquals(7L, fuelRes.fuelLogs[0].vehicleId)
        assertEquals(11850.0, fuelRes.fuelLogs[0].odometer, 0.01)

        val serviceRes = BacklogImportParser.parseInput(
            BacklogImportParser.getSampleTemplate(BacklogImportMode.SERVICE_CSV),
            vehicleId = 7L,
            mode = BacklogImportMode.SERVICE_CSV
        )
        assertEquals(2, serviceRes.serviceLogs.size)
        assertTrue(serviceRes.warnings.isEmpty())
        assertEquals("Oil & Filter Change", serviceRes.serviceLogs[0].serviceType)

        val partsRes = BacklogImportParser.parseInput(
            BacklogImportParser.getSampleTemplate(BacklogImportMode.PARTS_CSV),
            vehicleId = 7L,
            mode = BacklogImportMode.PARTS_CSV
        )
        assertEquals(2, partsRes.partItems.size)
        assertTrue(partsRes.warnings.isEmpty())
        assertFalse(partsRes.partItems[0].isReplaced)
        assertTrue(partsRes.partItems[1].isReplaced)

        val mixedRes = BacklogImportParser.parseInput(
            BacklogImportParser.getSampleTemplate(BacklogImportMode.AUTO_MIXED),
            vehicleId = 7L,
            mode = BacklogImportMode.AUTO_MIXED
        )
        assertEquals(1, mixedRes.fuelLogs.size)
        assertEquals(1, mixedRes.serviceLogs.size)
        assertEquals(1, mixedRes.partItems.size)
        assertEquals(3, mixedRes.totalCount)
        assertTrue(mixedRes.warnings.isEmpty())
    }

    @Test
    fun parseCsvWarnings_reportsMalformedLinesGracefully() {
        val input = """
            # comment line
            badline
            fuel, invalid-date, 5000, 10.0, 1.5
            fuel, 2026-05-01, 5000, 10.0, 1.5, Shell, true, Valid entry
        """.trimIndent()

        val result = BacklogImportParser.parseInput(input, vehicleId = 2L, mode = BacklogImportMode.AUTO_MIXED)
        assertEquals(1, result.fuelLogs.size)
        assertEquals(15.0, result.fuelLogs[0].totalCost, 0.01)
        assertEquals(2, result.warnings.size)
    }
}
