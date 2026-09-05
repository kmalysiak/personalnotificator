package pl.kmalysiak.personalnotificator.haier

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class HaierSnapshotMessage(
    val msgType: String,
    val payload: HaierHeatPumpWireDto,
)

@Serializable
data class HaierHeatPumpWireDto(
    val timestamp: String? = null,
    val operationalStatus: DictValue? = null,
    val heatPumpMode: DictValue? = null,
    val energyMode: DictValue? = null,
    val currentErrorCode: DictValue? = null,
    val r141LsbErrorCodeAlt: DictValue? = null,
    val defrostActive: Boolean? = null,
    val antifreezeProtectionActive: Boolean? = null,
    val threeWayValveToCh: Boolean? = null,
    val threeWayValveToDhw: Boolean? = null,
    val circulationPump: Boolean? = null,
    val backupResistiveHeater: Boolean? = null,
    val dhwBufferTemperature: Double? = null,
    val compressorFrequencyActual: Double? = null,
    val fan1Rpm: Double? = null,
    val dhwEnergyConsumption: Double? = null,
    val chEnergyConsumption: Double? = null,
    val waterInletTemperatureTwi: Double? = null,
    val waterOutletTemperatureTwo: Double? = null,
    val compressorInternalTemperature: Double? = null,
    val dischargeGasTemperatureTd: Double? = null,
    val suctionPressureActualPsa: Double? = null,
    val dischargePressureActualPdact: Double? = null,
    val ambientTemperatureTao: Double? = null,
    val copEstimated: Double? = null,
    val suctionGasTemperatureTs: Double? = null,
    val unknownQueryCount: Double? = null,
    val comment: String? = null,
    val errors: List<FieldError> = emptyList(),
)

fun HaierHeatPumpWireDto.toDto(): HaierHeatPumpDto = HaierHeatPumpDto(
    timestamp                    = timestamp?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() },
    operationalStatus            = operationalStatus,
    heatPumpMode                 = heatPumpMode,
    energyMode                   = energyMode,
    currentErrorCode             = currentErrorCode,
    defrostActive                = defrostActive,
    antifreezeProtectionActive   = antifreezeProtectionActive,
    threeWayValveToCh            = threeWayValveToCh,
    threeWayValveToDhw           = threeWayValveToDhw,
    circulationPump              = circulationPump,
    backupResistiveHeater        = backupResistiveHeater,
    dhwBufferTemperature         = dhwBufferTemperature?.toBigDecimal(),
    compressorFrequencyActual    = compressorFrequencyActual?.toBigDecimal(),
    fan1Rpm                      = fan1Rpm?.toBigDecimal(),
    dhwEnergyConsumption         = dhwEnergyConsumption?.toBigDecimal(),
    chEnergyConsumption          = chEnergyConsumption?.toBigDecimal(),
    waterInletTemperatureTwi     = waterInletTemperatureTwi?.toBigDecimal(),
    waterOutletTemperatureTwo    = waterOutletTemperatureTwo?.toBigDecimal(),
    compressorInternalTemperature = compressorInternalTemperature?.toBigDecimal(),
    dischargeGasTemperatureTd    = dischargeGasTemperatureTd?.toBigDecimal(),
    suctionPressureActualPsa     = suctionPressureActualPsa?.toBigDecimal(),
    dischargePressureActualPdact  = dischargePressureActualPdact?.toBigDecimal(),
    ambientTemperatureTao         = ambientTemperatureTao?.toBigDecimal(),
    copEstimated                  = copEstimated?.toBigDecimal(),
    suctionGasTemperatureTs       = suctionGasTemperatureTs?.toBigDecimal(),
    unknownQueryCount             = unknownQueryCount?.toBigDecimal(),
    comment = comment,
    errors  = errors,
)
