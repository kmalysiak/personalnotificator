package pl.kmalysiak.personalnotificator

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class DateTimeUtil {
    companion object {
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
