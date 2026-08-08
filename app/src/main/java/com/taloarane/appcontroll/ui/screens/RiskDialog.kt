package com.taloarane.appcontroll.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taloarane.appcontroll.data.Risk
import com.taloarane.appcontroll.ui.LocalStrings
import com.taloarane.appcontroll.ui.theme.NeonRed
import com.taloarane.appcontroll.ui.theme.NeonYellow

@Composable
fun RiskDialog(
    risk: Risk,
    appLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val s = LocalStrings.current
    var typed by remember { mutableStateOf("") }
    val danger = risk == Risk.DANGER
    val accent = if (danger) NeonRed else NeonYellow

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (danger) s.danger else s.warning,
                color = accent,
                fontSize = 18.sp,
            )
        },
        text = {
            Column {
                Text(if (danger) s.dangerBody else s.warningBody, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Text(appLabel, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (danger) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = typed,
                        onValueChange = { typed = it },
                        label = { Text("Ketik: LANJUT") },
                        singleLine = true,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !danger || typed.trim().equalsIgnoreCase("LANJUT"),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
            ) { Text(s.confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

private fun String.equalsIgnoreCase(other: String) = this.equals(other, ignoreCase = true)
