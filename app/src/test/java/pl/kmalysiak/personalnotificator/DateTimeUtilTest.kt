package pl.kmalysiak.personalnotificator

import org.junit.Assert.*
import org.junit.Test
import pl.kmalysiak.personalnotificator.main.DateTimeUtil
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class DateTimeUtilTest {

    private val warsaw = ZoneId.of("Europe/Warsaw")

    // toWarsawDt(Long)

    @Test
    fun toWarsawDt_sentinelMinus1_returnsMin() {
        assertEquals(LocalDateTime.MIN, DateTimeUtil.toWarsawDt(-1L))
    }

    @Test
    fun toWarsawDt_epoch0_appliesWinterOffset() {
        // 1970-01-01 00:00:00 UTC = 1970-01-01 01:00:00 CET (UTC+1)
        val result = DateTimeUtil.toWarsawDt(0L)
        assertEquals(LocalDateTime.of(1970, 1, 1, 1, 0, 0), result)
    }

    @Test
    fun toWarsawDt_summerTime_appliesCESTOffset() {
        // 2024-07-01 00:00:00 UTC = 2024-07-01 02:00:00 CEST (UTC+2)
        val epochUtc = ZonedDateTime.of(2024, 7, 1, 0, 0, 0, 0, ZoneId.of("UTC")).toEpochSecond()
        val result = DateTimeUtil.toWarsawDt(epochUtc)
        assertEquals(LocalDateTime.of(2024, 7, 1, 2, 0, 0), result)
    }

    @Test
    fun toWarsawDt_instantOverload_matchesEpochSecondOverload() {
        val epoch = ZonedDateTime.of(2024, 5, 20, 14, 30, 0, 0, warsaw).toEpochSecond()
        val fromLong = DateTimeUtil.toWarsawDt(epoch)
        val fromInstant = DateTimeUtil.toWarsawDt(java.time.Instant.ofEpochSecond(epoch))
        assertEquals(fromLong, fromInstant)
    }

    // areTheSameDay

    @Test
    fun areTheSameDay_sameDay_returnsTrue() {
        val morning = ZonedDateTime.of(2024, 3, 15, 8, 0, 0, 0, warsaw).toEpochSecond()
        val evening = ZonedDateTime.of(2024, 3, 15, 22, 30, 0, 0, warsaw).toEpochSecond()
        assertTrue(DateTimeUtil.areTheSameDay(morning, evening))
    }

    @Test
    fun areTheSameDay_consecutiveDaysBoundary_returnsFalse() {
        val endOfDay      = ZonedDateTime.of(2024, 6, 10, 23, 59, 59, 0, warsaw).toEpochSecond()
        val startNextDay  = ZonedDateTime.of(2024, 6, 11,  0,  0,  0, 0, warsaw).toEpochSecond()
        assertFalse(DateTimeUtil.areTheSameDay(endOfDay, startNextDay))
    }

    @Test
    fun areTheSameDay_sameDayOfYearDifferentYears_returnsFalse() {
        // Uwaga: aktualnie areTheSameDay porównuje tylko dayOfYear bez roku —
        // ten test dokumentuje buga: Jan 15 2023 i Jan 15 2024 mają dayOfYear==15,
        // funkcja błędnie zwraca true.
        val jan15_2023 = ZonedDateTime.of(2023, 1, 15, 12, 0, 0, 0, warsaw).toEpochSecond()
        val jan15_2024 = ZonedDateTime.of(2024, 1, 15, 12, 0, 0, 0, warsaw).toEpochSecond()
        assertFalse(DateTimeUtil.areTheSameDay(jan15_2023, jan15_2024))
    }

    @Test
    fun areTheSameDay_daylightSavingTransitionDay_sameDay_returnsTrue() {
        // 31 marca 2024: zmiana czasu letniego w Polsce (2:00 → 3:00)
        val before = ZonedDateTime.of(2024, 3, 31,  1, 59, 59, 0, warsaw).toEpochSecond()
        val after  = ZonedDateTime.of(2024, 3, 31, 12,  0,  0, 0, warsaw).toEpochSecond()
        assertTrue(DateTimeUtil.areTheSameDay(before, after))
    }

    // Formatters

    @Test
    fun dateFormatterHrMinSec_formatsCorrectly() {
        val dt = LocalDateTime.of(2024, 3, 15, 9, 5, 3)
        assertEquals("09:05:03", DateTimeUtil.dateFormatterHrMinSec.format(dt))
    }

    @Test
    fun dateFormatter_formatsCorrectly() {
        val dt = LocalDateTime.of(2024, 3, 15, 9, 5, 3)
        assertEquals("2024-03-15", DateTimeUtil.dateFormatter.format(dt))
    }

    @Test
    fun dateTimeFormatter_formatsCorrectly() {
        val dt = LocalDateTime.of(2024, 3, 15, 9, 5, 3)
        assertEquals("2024-03-15 09:05:03", DateTimeUtil.dateTimeFormatter.format(dt))
    }
}
