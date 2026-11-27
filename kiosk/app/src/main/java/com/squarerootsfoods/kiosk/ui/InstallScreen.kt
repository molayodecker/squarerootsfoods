package com.squarerootsfoods.kiosk.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.squarerootsfoods.kiosk.InstallUi
import com.squarerootsfoods.kiosk.KioskViewModel

@Composable
fun InstallScreen(model: KioskViewModel, install: InstallUi) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Install", fontSize = 28.sp, fontWeight = FontWeight.Medium, color = Ink)
        Text(
            "Leave the shop screen until a coil turns and one payment completes.",
            color = Muted,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        if (install.phase == "setup") {
            SetupStep(model, install)
        } else {
            CoilStep(model, install)
        }
        TextButton(onClick = { model.closeInstall() }) { Text("Back") }
    }
}

@Composable
private fun SetupStep(model: KioskViewModel, install: InstallUi) {
    var rows by remember { mutableStateOf(install.rows.toString()) }
    var coils by remember { mutableStateOf(install.coils.toString()) }
    var board by remember { mutableStateOf(install.board) }
    var serial by remember { mutableStateOf(install.serial) }
    var baud by remember { mutableIntStateOf(install.baud) }
    var simulate by remember { mutableStateOf(install.simulate) }

    Text("1. Write down the machine number in settings, which port exists, and which coil moves for lane 1.", color = Ink)
    Spacer(Modifier.height(8.dp))
    NumberField("Rows", rows) { rows = it }
    NumberField("Coils per row", coils) { coils = it }
    Text("Lane 1 is row 1, coil 1. Count across the row, then down.", color = Muted, fontSize = 13.sp)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Choice("Vendele", board == "vendele") { board = "vendele" }
        Choice("Not sure", board == "unknown") { board = "unknown" }
    }
    Text(
        "Vendele sends the slot command from the old app. If the wrong coil moves, stop and note the numbering.",
        color = Muted,
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 4.dp),
    )
    Spacer(Modifier.height(8.dp))
    Text("Ports on this board", fontWeight = FontWeight.Medium, color = Ink)
    if (install.ports.none { it.present }) {
        Text("None of the usual ports are visible. Run this on the cabinet, or type the path the old app used.", color = Muted, fontSize = 13.sp)
    }
    install.ports.forEach { port ->
        TextButton(onClick = {
            serial = port.path
            if (port.present) simulate = false
        }) {
            Text(if (port.present) "${port.path}  present" else port.path)
        }
    }
    NumberField("Serial device", serial) { serial = it }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(9600, 19200, 38400, 57600).forEach { rate ->
            Choice(rate.toString(), baud == rate) { baud = rate }
        }
    }
    Text("The old ADH815 setup used ttyS1 at 38400. Try that only if Vendele does not turn a coil.", color = Muted, fontSize = 13.sp)
    TextButton(onClick = { simulate = !simulate }) {
        Text(if (simulate) "Rehearsal: motors stay off" else "Live: the selected port will be commanded")
    }
    Button(
        onClick = {
            model.prepareCoils(
                rows.toIntOrNull() ?: 1,
                coils.toIntOrNull() ?: 1,
                board,
                serial,
                baud,
                simulate,
            )
        },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Cream),
    ) { Text("Start the coil walk") }
}

@Composable
private fun CoilStep(model: KioskViewModel, install: InstallUi) {
    val check = install.checks.getOrNull(install.index)
    val tested = install.checks.count { it.result != "untested" }
    Text("${tested} of ${install.checks.size} marked", color = Muted)
    if (install.simulate) {
        Text("Rehearsal is on, so this does not prove a motor. Turn it off on the cabinet.", color = Danger, modifier = Modifier.padding(vertical = 6.dp))
    }
    if (check != null) {
        Text("Row ${check.row}   Coil ${check.coil}", fontSize = 32.sp, fontWeight = FontWeight.Medium, color = Ink)
        Text("Lane ${check.lane}", fontSize = 18.sp, color = Muted)
        if (check.result != "untested") Text("Marked ${check.result}", color = Ink, modifier = Modifier.padding(top = 4.dp))
    }
    if (!install.message.isNullOrBlank()) {
        Text(install.message, color = Ink, modifier = Modifier.padding(vertical = 8.dp))
    }
    Button(
        onClick = { model.sendCoil() },
        enabled = !install.busy && check != null,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Cream),
    ) { Text(if (install.busy) "Sending" else "Send vend") }
    Spacer(Modifier.height(8.dp))
    Text("Mark what you see, not what the screen hopes.", color = Muted, fontSize = 13.sp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Choice("Turned", false) { model.markCoil("turned") }
        Choice("Stuck", false) { model.markCoil("stuck") }
        Choice("Empty", false) { model.markCoil("empty") }
    }
    Row {
        TextButton(onClick = { model.stepCoil(-1) }) { Text("Previous") }
        TextButton(onClick = { model.stepCoil(1) }) { Text("Next") }
    }
    val turned = install.checks.count { it.result == "turned" }
    Text("Turned $turned · stuck ${install.checks.count { it.result == "stuck" }} · empty ${install.checks.count { it.result == "empty" }}", color = Ink)
    if (install.paymentStatus.isNotBlank()) {
        Text("Payment on lane ${install.paymentLane}: ${install.paymentStatus}", color = Ink, modifier = Modifier.padding(top = 6.dp))
    }
    Button(
        onClick = { model.startPaymentTest() },
        enabled = turned > 0 && !install.busy,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Cream),
    ) { Text("Test payment on a coil that turned") }
    Button(
        onClick = { model.saveInstall() },
        enabled = !install.busy,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Surface, contentColor = Cream),
    ) { Text("Save this install to the desk") }
    TextButton(onClick = { model.editInstallSetup() }) { Text("Change rows, port, or baud") }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        singleLine = true,
    )
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(if (selected) "• $label" else label, color = if (selected) Pine else Ink, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
    }
}
