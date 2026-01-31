package pl.kmalysiak.personalnotificator.main

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import pl.kmalysiak.personalnotificator.auth.BackendAuthResult
import pl.kmalysiak.personalnotificator.auth.BackendAuthService
import pl.kmalysiak.personalnotificator.preference.PreferencesUtil
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

    val username by viewModel.username
    val password by viewModel.password
    val backendAuthService = BackendAuthService()

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

                val scope = rememberCoroutineScope()
                var regResult by remember { mutableStateOf<BackendAuthResult?>(null) }

                when (selectedTabIndex) {
                    0 -> TabEvents(lastGateOpened, lastGateClosed, lastMovement, tableData)
                    { ->
                        scope.launch {
                            viewModel.clearDb()
                        }
                    }

                    1 -> TabSettings(token, permissionData)
                    2 -> TabCredentials(
                        username, password,
                    ) { inputUser, inputPass ->
                        scope.launch {
                            regResult = backendAuthService.authenticateAndSendCredHomeAsync(context)
                        }

                        regResult?.let {
                            if (it.isRegister) {
                                Toast.makeText(context, "User logged in: $inputUser", Toast.LENGTH_SHORT).show()
                                viewModel.saveCredentials(context, inputUser, inputPass)
                            } else {
                                Toast.makeText(context, "User not logged in: $inputUser: ${it.errorDesc}", Toast.LENGTH_SHORT).show()
                                viewModel.saveCredentials(context, inputUser, inputPass)
                            }
                        }
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
    token: String,
    permissionData: List<Triple<String, LocalDateTime, String>>
) {

    val items = remember {
        mutableStateOf(PreferencesUtil.get().getForDisplay())
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
                        PreferencesUtil.get().setPreference(item.name, checked)
                        items.value = PreferencesUtil.get().getForDisplay()
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
    tableData: List<Triple<String, LocalDateTime, String>>,
    onDbClear: () -> Unit
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
            text = if (lastGateOpened == LocalDateTime.MIN) "-" else dateFormatter.format(lastGateOpened),
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
            if (lastGateOpened == LocalDateTime.MIN) "-" else dateFormatter.format(lastGateClosed),
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
            if (lastGateOpened == LocalDateTime.MIN) "-" else dateFormatter.format(lastMovement),
            style = MaterialTheme.typography.bodyLarge,
        )
    }

    Text("Events", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))

    Button(
        onClick = { onDbClear() },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Force refresh list")
    }

    Button(
        onClick = { onDbClear() },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Clear events list")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(tableData) { (event, dateTime, desc) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 8.dp)
            ) {
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

