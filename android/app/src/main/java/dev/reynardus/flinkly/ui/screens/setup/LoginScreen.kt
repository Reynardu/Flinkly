package dev.reynardus.flinkly.ui.screens.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LoginScreen(
    onSuccess: (hasHousehold: Boolean) -> Unit,
    vm: AuthViewModel = hiltViewModel(),
) {
    if (vm.pendingSecret != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Dein Login-Code") },
            text = {
                Column {
                    Text("Speichere diesen Code — er ist dein einziger Weg, dich wieder einzuloggen:")
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            text = vm.pendingSecret!!,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { vm.confirmSecretSaved(onSuccess) }) {
                    Text("Gespeichert — weiter")
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Willkommen", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(32.dp))

        TabRow(selectedTabIndex = if (vm.isLoginMode) 0 else 1) {
            Tab(
                selected = vm.isLoginMode,
                onClick = { if (!vm.isLoginMode) vm.toggleMode() },
                text = { Text("Einloggen") },
            )
            Tab(
                selected = !vm.isLoginMode,
                onClick = { if (vm.isLoginMode) vm.toggleMode() },
                text = { Text("Registrieren") },
            )
        }

        Spacer(Modifier.height(24.dp))

        if (vm.isLoginMode) {
            OutlinedTextField(
                value = vm.userSecret,
                onValueChange = vm::onSecretChange,
                label = { Text("Login-Code") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { vm.submit(onSuccess) }),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            OutlinedTextField(
                value = vm.displayName,
                onValueChange = vm::onDisplayNameChange,
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = vm.password,
                onValueChange = vm::onPasswordChange,
                label = { Text("Passwort") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { vm.submit(onSuccess) }),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        vm.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { vm.submit(onSuccess) },
            enabled = !vm.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (vm.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(if (vm.isLoginMode) "Einloggen" else "Konto erstellen")
            }
        }
    }
}
