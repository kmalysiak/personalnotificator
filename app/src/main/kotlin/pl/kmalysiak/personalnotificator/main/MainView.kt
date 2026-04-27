package pl.kmalysiak.personalnotificator.main

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import pl.kmalysiak.personalnotificator.DateTimeUtil
import pl.kmalysiak.personalnotificator.auth.BackendAuthResult
import pl.kmalysiak.personalnotificator.BuildConfig
import pl.kmalysiak.personalnotificator.config.ConfigRepository
import pl.kmalysiak.personalnotificator.ws.WebSocketManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.coroutines.cancellation.CancellationException

val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

@Composable
fun MainView(viewModel: AppViewModel = viewModel()) {
    val context = LocalContext.current
    val token by viewModel.token.collectAsState()
    val tableData by viewModel.tableData.collectAsState()
    val permissionData by viewModel.permissionData.collectAsState()
    val lastGateOpened by viewModel.lastGateOpened.collectAsState()
    val lastGateClosed by viewModel.lastGateClosed.collectAsState()
    val lastMovement by viewModel.lastMovement.collectAsState()
    val lastHeartbeat by viewModel.lastHeartbeat.collectAsState()
    val wsState by viewModel.wsState.collectAsState()
    val lastReconnectRequest by viewModel.lastReconnectRequest.collectAsState()
    val ttsReady by viewModel.ttsReady.collectAsState()
    val authResult by viewModel.authResult.collectAsState()
    val username by viewModel.username
    val password by viewModel.password

    LaunchedEffect(Unit) {
        viewModel.loadCredentials()
    }

    LaunchedEffect(authResult) {
        if (authResult.isRegister) {
            Toast.makeText(context, "Zalogowano", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Błąd: ${authResult.errorDesc}", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {


            var selectedTabIndex by remember { mutableStateOf(0) }
            val tabs = listOf("Events", "Settings", "Credentials")

            Column {
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }

                val scope = rememberCoroutineScope()
                var regResult by remember { mutableStateOf<BackendAuthResult?>(null) }

                when (selectedTabIndex) {
                    0 -> TabEvents(lastGateOpened, lastGateClosed, lastMovement, lastReconnectRequest, lastHeartbeat, wsState, ttsReady, tableData)
                    { ->
                        scope.launch {
                            viewModel.clearDb()
                        }
                    }

                    1 -> TabSettings(token, permissionData, viewModel)
                    2 -> TabCredentials(username, password) {
                        inputUser, inputPass ->
                        viewModel.saveCredentials(inputUser, inputPass)
                        viewModel.authenticate()
                    }
                }
            }
        }
    }
}


@Composable
fun TabCredentials(
    username: String,
    password: String,
    onVerifyClick: (username: String, password: String) -> Unit
) {

    var inputUser by remember { mutableStateOf(username) }
    var inputPass by remember { mutableStateOf(password) }

    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Backend authentitcation")

        Spacer(modifier = Modifier.width(32.dp))

        OutlinedTextField(
            value = inputUser,
            onValueChange = { inputUser = it },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.width(32.dp))

        OutlinedTextField(
            value = inputPass,
            onValueChange = { inputPass = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = Icons.Filled.Lock, contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.width(32.dp))

        Button(
            onClick = { onVerifyClick(inputUser, inputPass) },
            modifier = Modifier.fillMaxWidth(),
            enabled = inputUser.isNotBlank() && inputPass.isNotBlank(),
        ) {
            Text("Verify and set credentials")
        }
    }

}

@Composable
fun TabSettings(
    token: String,
    permissionData: List<Triple<String, LocalDateTime, String>>,
    viewModel: AppViewModel
) {
    val configMode by viewModel.configMode.collectAsState()
    val scrollState = rememberScrollState()
    val buildDate = remember {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        sdf.format(java.util.Date(BuildConfig.BUILD_TIME))
    }

    Column(modifier = Modifier.verticalScroll(scrollState)) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Build", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        Text(
            "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})  ·  $buildDate",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }

    HorizontalDivider()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "API env:",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.width(72.dp)
        )
        ConfigRepository.ConfigMode.entries.forEach { mode ->
            RadioButton(
                selected = configMode == mode,
                onClick = { viewModel.setConfigMode(mode) }
            )
            Text(
                text = mode.name.lowercase().replaceFirstChar { it.uppercaseChar() },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(end = 16.dp)
            )
        }
    }

    HorizontalDivider()

    val items = remember {
        mutableStateOf(viewModel.getPrefsForDisplay())
    }

    Column {

        items.value.forEach { item ->

            Row(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Switch(
                    checked = item.enabled,
                    onCheckedChange = { checked ->
                        viewModel.setPreference(item.name, checked)
                        items.value = viewModel.getPrefsForDisplay()
                    }
                )
                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge

                )
            }
        }
    }

    HorizontalDivider()

    val ttsVolume by viewModel.ttsVolume.collectAsState()
    Row(
        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("TTS volume", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(100.dp))
        Slider(
            value = ttsVolume,
            onValueChange = { viewModel.setTtsVolume(it) },
            valueRange = 0f..1f,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${(ttsVolume * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(40.dp),
        )
    }

    HorizontalDivider()

    Row(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(text = "FCM Token:", style = MaterialTheme.typography.bodyLarge)
        Text(
            text = token,
            style = MaterialTheme.typography.bodyLarge,

            )
    }
    HorizontalDivider()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Permissions",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
        Button(
            onClick = { /* Your refresh logic here */ },
            contentPadding = PaddingValues(4.dp),
            modifier = Modifier.sizeIn(minHeight = 32.dp, minWidth = 32.dp)
        ) {
            Text(text = "↻ Refresh", fontSize = 14.sp)
        }
    }

    Column {
        permissionData.forEach { (permission, datetime, status) ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    Text(text = permission)
                }

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(text = dateFormatter.format(datetime))
                }

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(text = status)
                }
            }
        }
    }

        HorizontalDivider(thickness = 1.dp)
    }
}

@Composable
fun TabEvents(
    lastGateOpened: LocalDateTime,
    lastGateClosed: LocalDateTime,
    lastMovement: LocalDateTime,
    lastReconnectRequest: LocalDateTime,
    lastHeartbeat: LocalDateTime,
    wsState: WebSocketManager.State,
    ttsReady: Boolean?,
    tableData: List<EventViewDto>,
    onDbClear: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ReadOnlyField(dt = lastGateOpened, label = "Last  opened", modifier = Modifier.weight(1f))
        ReadOnlyField(dt = lastGateClosed, label = "Last  closed", modifier = Modifier.weight(1f))
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ReadOnlyField(dt = lastMovement, label = "Last  move", modifier = Modifier.weight(1f))
        ReadOnlyField(dt = lastReconnectRequest, label = "Last reconnect req", modifier = Modifier.weight(1f))
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val (wsIcon, wsColor) = when (wsState) {
            WebSocketManager.State.CONNECTED           -> Icons.Filled.Check   to MaterialTheme.colorScheme.primary
            WebSocketManager.State.CONNECTING          -> Icons.Filled.Refresh to MaterialTheme.colorScheme.primary
            WebSocketManager.State.RECONNECTING        -> Icons.Filled.Refresh to Color(0xFFFF9800)
            WebSocketManager.State.RECONNECTING_REAUTH -> Icons.Filled.Lock    to Color(0xFFFF9800)
            WebSocketManager.State.STOPPED             -> Icons.Filled.Warning to MaterialTheme.colorScheme.error
        }
        StatusField(
            value = wsState.name,
            label = "WS status",
            modifier = Modifier.weight(1f),
            leadingIcon = { Icon(imageVector = wsIcon, contentDescription = null, tint = wsColor) }
        )
        ReadOnlyField(dt = lastHeartbeat, label = "Last  heartbeat", modifier = Modifier.weight(1f))
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val (ttsIcon, ttsColor, ttsLabel) = when (ttsReady) {
            true  -> Triple(Icons.Filled.Check,   MaterialTheme.colorScheme.primary, "TTS OK")
            false -> Triple(Icons.Filled.Warning,  MaterialTheme.colorScheme.error,   "TTS NOK")
            null  -> Triple(Icons.Filled.Refresh,  MaterialTheme.colorScheme.outline, "TTS ...")
        }
        StatusField(
            value = ttsLabel,
            label = "TTS",
            modifier = Modifier.weight(1f),
            leadingIcon = { Icon(imageVector = ttsIcon, contentDescription = null, tint = ttsColor) }
        )
        Spacer(modifier = Modifier.weight(1f))
    }

    Text("Events", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {

        Button(
            onClick = { },
            modifier = Modifier
                .padding(2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary)
                .weight(1f)
                .height(48.dp)
        ) {
            Text("Refresh")
        }

        LongPressButton(
            text = "Clear",
            onConfirmed = {
                onDbClear()
            },
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            holdDuration = 2000L
        )

    }
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(tableData) { (event, dateTime, desc, isFirstAtDay, received) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 8.dp)
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    Text(text = event)
                }

                Box(modifier = Modifier.weight(0.6f), contentAlignment = Alignment.Center) {
                    Text(text = DateTimeUtil.dateFormatterHrMinSec.format(dateTime))
                }

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(text = desc)
                }
            }
            if (isFirstAtDay) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                ) {
                    Text(
                        text = DateTimeUtil.dateFormatter.format(dateTime.plusDays(1)),
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.5.dp)
                            .align(Alignment.CenterVertically)
                            .background(MaterialTheme.colorScheme.primary)
                    )

                }
            }
        }
    }
}

@Composable
fun ReadOnlyField(
    dt: LocalDateTime,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = if (dt == LocalDateTime.MIN) "-" else dateFormatter.format(dt),
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        singleLine = true,
        modifier = modifier.height(54.dp),
    )
}


@Composable
fun StatusField(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        singleLine = true,
        modifier = modifier.height(54.dp),
        leadingIcon = leadingIcon,
    )
}

@Composable
fun LongPressButton(
    text: String,
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    holdDuration: Long = 1000L
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true

                        val job = scope.launch {
                            try {
                                progress.animateTo(
                                    1f,
                                    animationSpec = tween(
                                        durationMillis = holdDuration.toInt(),
                                        easing = LinearEasing
                                    )
                                )
                                // Tylko jeśli animacja się skończyła (nie została anulowana)
                                if (isPressed) {
                                    onConfirmed()
                                }
                            } catch (e: CancellationException) {
                                // Animacja została przerwana - OK
                            }
                        }

                        tryAwaitRelease()

                        // Puszczono przycisk - anuluj
                        isPressed = false
                        job.cancel()
                        scope.launch {
                            progress.animateTo(0f, animationSpec = tween(200))
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {


        Text(
            text = text,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // Progress bar jako tło
        LinearProgressIndicator(
            progress = { progress.value },
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .alpha(0.6f), // Przezroczystość żeby tekst był widoczny
            color = Color.Yellow,
            trackColor = Color.Transparent,
            strokeCap = StrokeCap.Butt
        )
    }
}
