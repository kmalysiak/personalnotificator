package pl.kmalysiak.personalnotificator

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DateTimeUtil {
    companion object {

        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val dateFormatterHrMinSec = DateTimeFormatter.ofPattern("HH:mm:ss")


        fun areTheSameDay(a: Long, b: Long): Boolean {
            val aLd = toWarsawDt(a)
            val bLd = toWarsawDt(b)

            return aLd.dayOfYear == bLd.dayOfYear
        }

        fun toWarsawDt(sec: Long): LocalDateTime {

            return if (sec == -1L) LocalDateTime.MIN else {

                Instant.ofEpochSecond(sec)
                    .atZone(ZoneId.of("Europe/Warsaw"))
                    .toLocalDateTime()
            }
        }

        fun nowToWarsawDt(): LocalDateTime {
            return Instant.now()
                .atZone(ZoneId.of("Europe/Warsaw"))
                .toLocalDateTime()
        }


        fun toWarsawDt(inst: Instant): LocalDateTime {
            return inst
                .atZone(ZoneId.of("Europe/Warsaw"))
                .toLocalDateTime()
        }

        fun nowUtc(): Long {
            return Instant.now().epochSecond
        }
    }
}
