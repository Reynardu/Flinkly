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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun HouseholdSetupScreen(
    onSuccess: () -> Unit,
    vm: HouseholdSetupViewModel = hiltViewModel(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Haushalt", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Erstelle einen neuen Haushalt oder tritt einem bestehenden bei.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))

        TabRow(selectedTabIndex = if (vm.isCreateMode) 0 else 1) {
            Tab(
                selected = vm.isCreateMode,
                onClick = { if (!vm.isCreateMode) vm.toggleMode() },
                text = { Text("Erstellen") },
            )
            Tab(
                selected = !vm.isCreateMode,
                onClick = { if (vm.isCreateMode) vm.toggleMode() },
                text = { Text("Beitreten") },
            )
        }

        Spacer(Modifier.height(24.dp))

        if (vm.isCreateMode) {
            OutlinedTextField(
                value = vm.householdName,
                onValueChange = vm::onNameChange,
                label = { Text("Name des Haushalts") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { vm.submit(onSuccess) }),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            OutlinedTextField(
                value = vm.inviteInput,
                onValueChange = vm::onInviteChange,
                label = { Text("Einladungslink oder Code") },
                singleLine = true,
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
                Text(if (vm.isCreateMode) "Haushalt erstellen" else "Beitreten")
            }
        }
    }
}
