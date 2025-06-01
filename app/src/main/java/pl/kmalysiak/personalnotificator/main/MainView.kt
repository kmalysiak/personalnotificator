package pl.kmalysiak.personalnotificator.main


import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

const val PREFS_NAME = "NotificationSettings"
const val KEY_NOTIF_GATE_OPENED = "notifications_enabled"
const val KEY_NOTIF_GATE_CLOSED = "notifications_enabled"
const val KEY_NOTIF_MOVEMENT = "notifications_enabled"
val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

@Composable
fun MainView(viewModel: AppViewModel = viewModel()) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    val token by viewModel.token.collectAsState()
    val tableData by viewModel.tableData.collectAsState()
    val permissionData by viewModel.permissionData.collectAsState()
    val lastGateOpened by viewModel.lastGateOpened.collectAsState()
    val lastGateClosed by viewModel.lastGateClosed.collectAsState()
    val lastMovement by viewModel.lastMovement.collectAsState()

    val username by viewModel.username
    val password by viewModel.password

    LaunchedEffect(Unit) {
       viewModel.loadCredentials(context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 30.dp) // Distance from the top edge

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

                when (selectedTabIndex) {
                    0 -> TabEvents(lastGateOpened, lastGateClosed, lastMovement, tableData)
                    1 -> TabSettings(prefs, token, permissionData)
                    2 -> TabCredentials(
                        username, password,
                    ) { inputUser, inputPass ->
                        Toast.makeText(context, "Verified user: $inputUser", Toast.LENGTH_SHORT).show()
                        viewModel.saveCredentials(context, inputUser, inputPass)
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
                    Icon(imageVector = Icons.Filled.Menu, contentDescription = null)
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
    prefs: SharedPreferences,
    token: String,
    permissionData: List<Triple<String, LocalDateTime, String>>
) {

    var notifGateOpened by remember {
        mutableStateOf(
            prefs.getBoolean(
                KEY_NOTIF_GATE_OPENED, true
            )
        )
    }
    var notifGateClosed by remember {
        mutableStateOf(
            prefs.getBoolean(
                KEY_NOTIF_GATE_CLOSED, true
            )
        )
    }
    var notifMovement by remember {
        mutableStateOf(
            prefs.getBoolean(
                KEY_NOTIF_MOVEMENT,
                true
            )
        )
    }


    Text(text = "Notification Config", style = MaterialTheme.typography.bodyLarge)
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween, // Space between the Switch and Text
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(
            checked = notifGateOpened, onCheckedChange = { isChecked ->
                notifGateOpened = isChecked
                prefs.edit { putBoolean(KEY_NOTIF_GATE_OPENED, isChecked) }
            })
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "Gate closure",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }

    Row(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(
            checked = notifGateClosed, onCheckedChange = { isChecked ->
                notifGateClosed = isChecked
                prefs.edit { putBoolean(KEY_NOTIF_GATE_CLOSED, isChecked) }
            })
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "Gate opening",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }

    Row(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(
            checked = notifMovement, onCheckedChange = { isChecked ->
                notifMovement = isChecked
                prefs.edit { putBoolean(KEY_NOTIF_MOVEMENT, isChecked) }
            })
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "Movement",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
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

@Composable
fun TabEvents(
    lastGateOpened: LocalDateTime,
    lastGateClosed: LocalDateTime,
    lastMovement: LocalDateTime,
    tableData: List<Triple<String, LocalDateTime, String>>
) {
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween, // Space between the Switch and Text
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Last opening:", style = MaterialTheme.typography.bodyLarge)
        Text(
            text = dateFormatter.format(lastGateOpened),
            style = MaterialTheme.typography.bodyLarge,

            )
    }

    Row(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween, // Space between the Switch and Text
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Last closure:", style = MaterialTheme.typography.bodyLarge)
        Text(
            text = dateFormatter.format(lastGateClosed),
            style = MaterialTheme.typography.bodyLarge,

            )
    }

    Row(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween, // Space between the Switch and Text
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Last movement:", style = MaterialTheme.typography.bodyLarge)
        Text(
            text = dateFormatter.format(lastMovement),
            style = MaterialTheme.typography.bodyLarge,
        )
    }

    Text("Events", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))

    Column {
        tableData.forEach { (event, dateTime, desc) ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    Text(text = event)
                }

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(text = dateFormatter.format(dateTime))
                    dateTime
                }

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(text = desc)
                }
            }
        }
    }
}
