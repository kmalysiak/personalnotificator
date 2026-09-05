package pl.kmalysiak.personalnotificator.main

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import pl.kmalysiak.personalnotificator.haier.DictValue
import pl.kmalysiak.personalnotificator.haier.HaierHeatPumpDto
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun TabHaier(data: HaierHeatPumpDto?) {
    val errorMap = data?.errors?.associate { it.field to (it.comment ?: "") } ?: emptyMap()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp)
    ) {
        Text(
            text = if (data?.timestamp != null) "Updated: ${dateFormatter.format(data.timestamp)}" else "Awaiting data…",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // --- Status & Modes ---
        HaierRow {
            HaierDictCell("Oper. status",  data?.operationalStatus,  errorMap["operationalStatus"],  Modifier.weight(1f))
            HaierDictCell("HP mode",       data?.heatPumpMode,        errorMap["heatPumpMode"],        Modifier.weight(1f))
            HaierDictCell("Energy mode",   data?.energyMode,          errorMap["energyMode"],          Modifier.weight(1f))
        }
        HaierRow {
            HaierDictCell("Error code",        data?.currentErrorCode,    errorMap["currentErrorCode"],    Modifier.weight(1f))
            HaierDictCell("Alt. error (R141)", data?.r141LsbErrorCodeAlt, errorMap["r141LsbErrorCodeAlt"], Modifier.weight(1f))
            HaierBoolCell("Defrost",           data?.defrostActive,       errorMap["defrostActive"],       Modifier.weight(1f))
        }
        HaierRow {
            HaierBoolCell("Antifreeze",   data?.antifreezeProtectionActive, errorMap["antifreezeProtectionActive"], Modifier.weight(1f))
            HaierBoolCell("Valve → CH",   data?.threeWayValveToCh,          errorMap["threeWayValveToCh"],          Modifier.weight(1f))
            HaierBoolCell("Valve → DHW",  data?.threeWayValveToDhw,         errorMap["threeWayValveToDhw"],         Modifier.weight(1f))
        }
        HaierRow {
            HaierValueCell("DHW temp",    data?.dhwBufferTemperature.fmt(), "°C", errorMap["dhwBufferTemperature"], Modifier.weight(1f))
            HaierBoolCell("Circ. pump",   data?.circulationPump,            errorMap["circulationPump"],            Modifier.weight(1f))
            HaierBoolCell("Backup heat.", data?.backupResistiveHeater,      errorMap["backupResistiveHeater"],      Modifier.weight(1f))
        }

        HaierSectionLabel("Compressor / Fan")

        HaierRow {
            HaierValueCell("Compressor",    data?.compressorFrequencyActual.fmt(0), "Hz",  errorMap["compressorFrequencyActual"], Modifier.weight(1f))
            HaierValueCell("Fan 1",         data?.fan1Rpm.fmt(0),                   "rpm", errorMap["fan1Rpm"],                   Modifier.weight(1f))
            HaierValueCell("T ambient (Tao)", data?.ambientTemperatureTao.fmt(),    "°C",  errorMap["ambientTemperatureTao"],     Modifier.weight(1f))
        }
        HaierRow {
            HaierValueCell("DHW energy", data?.dhwEnergyConsumption.fmt(), "kWh", errorMap["dhwEnergyConsumption"], Modifier.weight(1f))
            HaierValueCell("CH energy",  data?.chEnergyConsumption.fmt(),  "kWh", errorMap["chEnergyConsumption"],  Modifier.weight(1f))
            HaierValueCell("COP est.",   data?.copEstimated.fmt(2),        "",    errorMap["copEstimated"],          Modifier.weight(1f))
        }

        HaierSectionLabel("Temperatures & Pressures")

        HaierRow {
            HaierValueCell("T in (Twi)",    data?.waterInletTemperatureTwi.fmt(),     "°C", errorMap["waterInletTemperatureTwi"],     Modifier.weight(1f))
            HaierValueCell("T out (Two)",   data?.waterOutletTemperatureTwo.fmt(),    "°C", errorMap["waterOutletTemperatureTwo"],    Modifier.weight(1f))
            HaierValueCell("T compressor",  data?.compressorInternalTemperature.fmt(),"°C", errorMap["compressorInternalTemperature"],Modifier.weight(1f))
        }
        HaierRow {
            HaierValueCell("T discharge (Td)", data?.dischargeGasTemperatureTd.fmt(), "°C",  errorMap["dischargeGasTemperatureTd"], Modifier.weight(1f))
            HaierValueCell("T suction (Ts)",   data?.suctionGasTemperatureTs.fmt(),   "°C",  errorMap["suctionGasTemperatureTs"],   Modifier.weight(1f))
            HaierValueCell("P suction (Psa)",  data?.suctionPressureActualPsa.fmt(2), "bar", errorMap["suctionPressureActualPsa"],  Modifier.weight(1f))
        }
        HaierRow {
            HaierValueCell("P discharge (Pd)", data?.dischargePressureActualPdact.fmt(2), "bar", errorMap["dischargePressureActualPdact"], Modifier.weight(1f))
            Spacer(Modifier.weight(2f))
        }

        HaierSectionLabel("Debug")

        HaierRow {
            HaierValueCell("Unknown queries", data?.unknownQueryCount.fmt(0), "", errorMap["unknownQueryCount"], Modifier.weight(1f))
            Spacer(Modifier.weight(2f))
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HaierRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        content = content
    )
}

@Composable
private fun HaierSectionLabel(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            "  $title  ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun HaierValueCell(
    label: String,
    value: String,
    unit: String,
    error: String? = null,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }
    val borderColor = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
    val labelColor  = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline

    Column(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = error != null) { showDialog = true }
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = labelColor)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.bodyMedium)
            if (unit.isNotEmpty()) {
                Spacer(Modifier.width(2.dp))
                Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }

    if (showDialog && error != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(label) },
            text  = { Text(error) },
            confirmButton = { TextButton(onClick = { showDialog = false }) { Text("OK") } }
        )
    }
}

@Composable
private fun HaierDictCell(label: String, value: DictValue?, error: String? = null, modifier: Modifier = Modifier) {
    HaierValueCell(label, value.fmt(), "", error, modifier)
}

@Composable
private fun HaierBoolCell(
    label: String,
    value: Boolean?,
    error: String? = null,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }
    val borderColor = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
    val labelColor  = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
    val (text, valueColor) = when (value) {
        true  -> "ON"  to Color(0xFF4CAF50)
        false -> "OFF" to MaterialTheme.colorScheme.outline
        null  -> "--"  to MaterialTheme.colorScheme.outline
    }

    Column(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = error != null) { showDialog = true }
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = labelColor)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }

    if (showDialog && error != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(label) },
            text  = { Text(error) },
            confirmButton = { TextButton(onClick = { showDialog = false }) { Text("OK") } }
        )
    }
}

private fun BigDecimal?.fmt(decimals: Int = 1): String =
    this?.setScale(decimals, RoundingMode.HALF_UP)?.toPlainString() ?: "--"

private fun DictValue?.fmt(): String = when {
    this == null  -> "--"
    value != null -> "$kod : $value"
    kod != null   -> kod
    else          -> "--"
}
