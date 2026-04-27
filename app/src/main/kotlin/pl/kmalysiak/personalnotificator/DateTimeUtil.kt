package pl.kmalysiak.personalnotificator

import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.TimeZone

class DateTimeUtil {
    companion object {

        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val dateFormatterHrMinSec = DateTimeFormatter.ofPattern("HH:mm:ss")

        // TimeZone.getTimeZone uses Android's system timezone database — more reliable
        // than ZoneId.of() which may use a different (possibly outdated) source on some devices
        private val warsawZone = TimeZone.getTimeZone("Europe/Warsaw").toZoneId()

        fun areTheSameDay(a: Long, b: Long): Boolean {
            val aLd = toWarsawDt(a)
            val bLd = toWarsawDt(b)

            return aLd.dayOfYear == bLd.dayOfYear
        }

        fun toWarsawDt(sec: Long): LocalDateTime {
            return if (sec == -1L) LocalDateTime.MIN else {
                Instant.ofEpochSecond(sec)
                    .atZone(warsawZone)
                    .toLocalDateTime()
            }
        }

        fun nowToWarsawDt(): LocalDateTime {
            return Instant.now()
                .atZone(warsawZone)
                .toLocalDateTime()
        }

        fun toWarsawDt(inst: Instant): LocalDateTime {
            return inst
                .atZone(warsawZone)
                .toLocalDateTime()
        }

        fun nowUtc(): Long {
            return Instant.now().epochSecond
        }
    }
}
