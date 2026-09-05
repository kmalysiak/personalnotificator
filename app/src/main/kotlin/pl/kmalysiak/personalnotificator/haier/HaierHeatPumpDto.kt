package pl.kmalysiak.personalnotificator.haier

import java.math.BigDecimal
import java.time.LocalDateTime

data class HaierHeatPumpDto(
    val timestamp: LocalDateTime? = null,
    val operationalStatus: DictValue? = null,
    val heatPumpMode: DictValue? = null,
    val energyMode: DictValue? = null,
    val currentErrorCode: DictValue? = null,
    val r141LsbErrorCodeAlt: DictValue? = null,
    val defrostActive: Boolean? = null,
    val antifreezeProtectionActive: Boolean? = null,
    val threeWayValveToCh: Boolean? = null,
    val threeWayValveToDhw: Boolean? = null,
    val dhwBufferTemperature: BigDecimal? = null,
    val compressorFrequencyActual: BigDecimal? = null,
    val fan1Rpm: BigDecimal? = null,
    val circulationPump: Boolean? = null,
    val backupResistiveHeater: Boolean? = null,
    val dhwEnergyConsumption: BigDecimal? = null,
    val chEnergyConsumption: BigDecimal? = null,
    val waterInletTemperatureTwi: BigDecimal? = null,
    val waterOutletTemperatureTwo: BigDecimal? = null,
    val compressorInternalTemperature: BigDecimal? = null,
    val dischargeGasTemperatureTd: BigDecimal? = null,
    val suctionPressureActualPsa: BigDecimal? = null,
    val dischargePressureActualPdact: BigDecimal? = null,
    val ambientTemperatureTao: BigDecimal? = null,
    val copEstimated: BigDecimal? = null,
    val suctionGasTemperatureTs: BigDecimal? = null,
    val unknownQueryCount: BigDecimal? = null,
    val comment: String? = null,
    val errors: List<FieldError> = emptyList(),
)
