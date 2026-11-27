package com.squarerootsfoods.kiosk.ui

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.squarerootsfoods.kiosk.BuildConfig
import com.squarerootsfoods.kiosk.KioskViewModel
import com.squarerootsfoods.kiosk.PayUi
import com.squarerootsfoods.kiosk.R
import com.squarerootsfoods.kiosk.Screen
import com.squarerootsfoods.kiosk.VendUi
import com.squarerootsfoods.kiosk.data.CartLine as Line
import com.squarerootsfoods.kiosk.data.Product as Item
import com.squarerootsfoods.kiosk.data.formatMoney

@Composable
fun KioskRoot(
    model: KioskViewModel,
    screen: Screen,
    products: List<Item>,
    cart: List<Line>,
    offline: Boolean,
    banner: String?,
    pay: PayUi,
    vend: VendUi,
    install: com.squarerootsfoods.kiosk.InstallUi,
) {
    MaterialTheme(
        typography = remember { kioskTypography() },
        colorScheme = darkColorScheme(
            primary = Accent,
            onPrimary = Cream,
            background = Background,
            onBackground = Cream,
            surface = Surface,
            onSurface = Cream,
            surfaceVariant = Surface,
            onSurfaceVariant = Muted,
            outline = Sky,
        ),
    ) {
        var booting by rememberSaveable { mutableStateOf(true) }
        Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0f to BackgroundTop, 0.6f to Background, 1f to Background)),
        ) {
            val totalLabel = formatMoney(
                cart.sumOf { it.totalMinor },
                cart.firstOrNull()?.product?.currency ?: model.currency,
            )
            when (screen) {
                Screen.Setup -> SetupScreen(model)
                Screen.Attract -> AttractScreen(model)
                Screen.Home -> CatalogScreen(model, products, cart)
                is Screen.Detail -> DetailScreen(model, products.find { it.id == screen.productId })
                Screen.Cart -> BasketScreen(model, cart)
                Screen.Pay -> PayScreen(model, pay, totalLabel)
                Screen.Approve -> ApproveScreen(model, pay, totalLabel)
                Screen.PayFail -> PayFailScreen(model, pay)
                Screen.Vend -> VendScreen(model, vend)
                Screen.Done -> DoneScreen(model)
                Screen.Refund -> RefundScreen(model, vend, pay)
                Screen.Pin -> ServicePinScreen(model)
                Screen.Service -> ServiceScreen(model, products)
                Screen.TempPause -> TempPauseScreen(model)
                Screen.Settings -> SettingsScreen(model, banner)
                Screen.Install -> InstallScreen(model, install)
            }
        }
            if (booting) BootSplash(onFinished = { booting = false })
        }
    }
}

@Composable
private fun SetupScreen(model: KioskViewModel) {
    val saved = model.prefsSnapshot
    SettingsForm(
        title = "Set up this machine",
        saved = saved,
        primary = "Start selling",
        onSave = model::saveSetup,
    )
}

@Composable
private fun SettingsScreen(model: KioskViewModel, banner: String?) {
    val saved = model.prefsSnapshot
    Column(Modifier.fillMaxSize()) {
        SettingsForm(
            title = "Machine settings",
            saved = saved,
            primary = "Save",
            onSave = model::saveSetup,
            extra = {
                if (!banner.isNullOrBlank()) Text(banner, color = Ink, modifier = Modifier.padding(bottom = 8.dp))
                Text("Installed ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", color = Muted)
                Button(onClick = { model.checkForUpdate() }, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Cream)) {
                    Text("Check for update")
                }
                Button(onClick = { model.openInstall() }, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Cream)) {
                    Text("Install tests")
                }
                var lane by remember { mutableStateOf("1") }
                OutlinedTextField(value = lane, onValueChange = { lane = it.filter { ch -> ch.isDigit() } }, label = { Text("Test lane") })
                TextButton(onClick = { model.testVend(lane.toIntOrNull() ?: 1) }) { Text("Send a test vend") }
                TextButton(onClick = { model.goHome() }) { Text("Close") }
            },
        )
    }
}

@Composable
private fun SettingsForm(
    title: String,
    saved: com.squarerootsfoods.kiosk.data.MachinePrefs.Snapshot,
    primary: String,
    onSave: (String, String, String, String, String, Int, Boolean, String, Int, String, String) -> Unit,
    extra: @Composable () -> Unit = {},
) {
    var machine by remember { mutableStateOf(saved.machineNo) }
    var server by remember { mutableStateOf(saved.serverBase) }
    var currency by remember { mutableStateOf(saved.currency) }
    var pin by remember { mutableStateOf(saved.operatorPin) }
    var serial by remember { mutableStateOf(saved.serialDevice) }
    var baud by remember { mutableStateOf(saved.baudRate.toString()) }
    var simulate by remember { mutableStateOf(saved.simulateVend) }
    var mqttHost by remember { mutableStateOf(saved.mqttHost) }
    var mqttPort by remember { mutableStateOf(saved.mqttPort.toString()) }
    var mqttUser by remember { mutableStateOf(saved.mqttUser) }
    var mqttPassword by remember { mutableStateOf(saved.mqttPassword) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Medium, color = Ink)
        Spacer(Modifier.height(12.dp))
        Field("Machine number", machine) { machine = it }
        Field("Server", server) { server = it }
        Field("Currency", currency) { currency = it }
        Field("Operator PIN", pin) { pin = it }
        Field("Serial device", serial) { serial = it }
        Field("Baud", baud) { baud = it.filter { ch -> ch.isDigit() } }
        TextButton(onClick = { simulate = !simulate }) {
            Text(if (simulate) "Vend mode: simulated" else "Vend mode: serial port")
        }
        Field("MQTT host", mqttHost) { mqttHost = it }
        Field("MQTT port", mqttPort) { mqttPort = it.filter { ch -> ch.isDigit() } }
        Field("MQTT user", mqttUser) { mqttUser = it }
        Field("MQTT password", mqttPassword) { mqttPassword = it }
        extra()
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                onSave(
                    machine,
                    server,
                    currency,
                    pin,
                    serial,
                    baud.toIntOrNull() ?: 9600,
                    simulate,
                    mqttHost,
                    mqttPort.toIntOrNull() ?: 1883,
                    mqttUser,
                    mqttPassword,
                )
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Cream),
        ) { Text(primary) }
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        singleLine = true,
    )
}

@Composable
private fun rememberQr(contents: String) = remember(contents) {
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val matrix = QRCodeWriter().encode(contents, BarcodeFormat.QR_CODE, 512, 512, hints)
    val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
    for (x in 0 until 512) {
        for (y in 0 until 512) {
            bitmap.setPixel(x, y, if (matrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
        }
    }
    bitmap.asImageBitmap()
}
