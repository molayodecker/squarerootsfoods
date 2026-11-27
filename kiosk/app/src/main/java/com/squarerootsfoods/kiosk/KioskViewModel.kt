package com.squarerootsfoods.kiosk

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.squarerootsfoods.kiosk.data.CartLine
import com.squarerootsfoods.kiosk.data.MachineApi
import com.squarerootsfoods.kiosk.data.MachinePrefs
import com.squarerootsfoods.kiosk.data.PaySession
import com.squarerootsfoods.kiosk.data.Product
import com.squarerootsfoods.kiosk.InstallNote
import com.squarerootsfoods.kiosk.InstallStore
import com.squarerootsfoods.kiosk.update.UpdateInstaller
import com.squarerootsfoods.kiosk.vend.CabinetPorts
import com.squarerootsfoods.kiosk.vend.LaneMap
import com.squarerootsfoods.kiosk.vend.VendDriver
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

sealed interface Screen {
    data object Setup : Screen
    data object Attract : Screen
    data object Home : Screen
    data class Detail(val productId: String) : Screen
    data object Cart : Screen
    data object Pay : Screen
    data object Approve : Screen
    data object PayFail : Screen
    data object Vend : Screen
    data object Done : Screen
    data object Refund : Screen
    data object Pin : Screen
    data object Service : Screen
    data object TempPause : Screen
    data object Settings : Screen
    data object Install : Screen
}

data class PayUi(
    val session: PaySession? = null,
    val waiting: Boolean = false,
    val error: String? = null,
    val mode: String = "qr",
    val phone: String = "",
    val network: String = "MTN MoMo",
    val expiresSec: Int = 120,
    val failReason: String = "",
)

data class VendLineUi(
    val name: String,
    val lane: Int,
    val delivered: Int,
    val ok: Boolean,
    val detail: String,
    val status: String = "Queued",
)

data class VendUi(
    val lines: List<VendLineUi> = emptyList(),
    val running: Boolean = true,
    val refundNote: String? = null,
)

class KioskViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = MachinePrefs(app)
    private val installStore = InstallStore(app)
    private val api = MachineApi(prefs)
    private val driver = VendDriver()
    private var payJob: Job? = null
    private var installReturn = false

    private val _screen = MutableStateFlow<Screen>(if (prefs.setupComplete) Screen.Attract else Screen.Setup)
    val screen: StateFlow<Screen> = _screen

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _cart = MutableStateFlow<List<CartLine>>(emptyList())
    val cart: StateFlow<List<CartLine>> = _cart

    private val _offline = MutableStateFlow(false)
    val offline: StateFlow<Boolean> = _offline

    private val _banner = MutableStateFlow<String?>(null)
    val banner: StateFlow<String?> = _banner

    private val _pay = MutableStateFlow(PayUi())
    val pay: StateFlow<PayUi> = _pay

    private val _vend = MutableStateFlow(VendUi())
    val vend: StateFlow<VendUi> = _vend

    private val _install = MutableStateFlow(InstallUi())
    val install: StateFlow<InstallUi> = _install

    private val _soldOut = MutableStateFlow<String?>(null)
    val soldOut: StateFlow<String?> = _soldOut

    private val _doneSeconds = MutableStateFlow(10)
    val doneSeconds: StateFlow<Int> = _doneSeconds

    private val _pinError = MutableStateFlow(false)
    val pinError: StateFlow<Boolean> = _pinError

    private val _refundMinor = MutableStateFlow(0L)
    val refundMinor: StateFlow<Long> = _refundMinor

    private val _deliveredCount = MutableStateFlow(0)
    val deliveredCount: StateFlow<Int> = _deliveredCount

    val machineNo: String get() = prefs.machineNo
    val currency: String get() = prefs.currency
    val prefsSnapshot get() = prefs.snapshot()

    init {
        if (prefs.setupComplete) reloadGoods()
    }

    fun reloadGoods() {
        viewModelScope.launch {
            try {
                _products.value = api.getGoods()
                _offline.value = false
            } catch (error: Exception) {
                _products.value = onboardCatalog()
                _offline.value = true
                _banner.value = error.message
            }
        }
    }

    fun add(product: Product) {
        if (product.stock <= 0) {
            _soldOut.value = product.name
            return
        }
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val line = current[index]
            if (line.qty >= product.stock) return
            current[index] = line.copy(qty = line.qty + 1)
        } else {
            current += CartLine(product, 1)
        }
        _cart.value = current
    }

    fun changeQty(productId: String, delta: Int) {
        _cart.value = _cart.value.mapNotNull { line ->
            if (line.product.id != productId) return@mapNotNull line
            val next = line.qty + delta
            when {
                next <= 0 -> null
                next > line.product.stock -> line
                else -> line.copy(qty = next)
            }
        }
    }

    fun openCart() {
        if (_cart.value.isNotEmpty()) _screen.value = Screen.Cart
    }

    fun openDetail(productId: String) {
        _screen.value = Screen.Detail(productId)
    }

    fun toAttract() {
        _soldOut.value = null
        _screen.value = Screen.Attract
    }

    fun dismissSoldOut() {
        _soldOut.value = null
    }

    fun requestPin() {
        _pinError.value = false
        _screen.value = Screen.Pin
    }

    fun submitPin(pin: String) {
        if (pin == prefs.operatorPin) {
            _pinError.value = false
            _screen.value = Screen.Service
        } else {
            _pinError.value = true
        }
    }

    fun saveSetup(
        machineNo: String,
        server: String,
        currency: String,
        pin: String,
        serial: String,
        baud: Int,
        simulate: Boolean,
        mqttHost: String,
        mqttPort: Int,
        mqttUser: String,
        mqttPassword: String,
    ) {
        prefs.machineNo = machineNo.ifBlank { "VM-001" }
        prefs.serverBase = server.ifBlank { "http://10.0.2.2:8080" }
        prefs.currency = currency.ifBlank { "GHS" }
        if (pin.length >= 4) prefs.operatorPin = pin
        prefs.serialDevice = serial
        prefs.baudRate = baud.takeIf { it > 0 } ?: 9600
        prefs.simulateVend = simulate
        prefs.mqttHost = mqttHost
        prefs.mqttPort = mqttPort.takeIf { it > 0 } ?: 1883
        prefs.mqttUser = mqttUser
        prefs.mqttPassword = mqttPassword
        prefs.setupComplete = true
        _screen.value = Screen.Attract
        reloadGoods()
    }

    fun startPay(email: String) {
        val lines = _cart.value
        if (lines.isEmpty()) return
        val amount = lines.sumOf { it.totalMinor }
        val reference = "${prefs.machineNo}-${System.currentTimeMillis()}"
        _screen.value = Screen.Pay
        _pay.value = _pay.value.copy(waiting = true, error = null, mode = "qr", failReason = "")
        payJob?.cancel()
        payJob = viewModelScope.launch {
            try {
                val session = api.initializePay(
                    reference = reference,
                    email = email.ifBlank { "payments@example.com" },
                    amountMinor = amount,
                    currency = lines.first().product.currency.ifBlank { prefs.currency },
                    lines = lines,
                )
                _pay.value = PayUi(session = session, waiting = true)
                waitForPayment(session)
            } catch (error: Exception) {
                _pay.value = PayUi(error = error.message ?: "Could not start Paystack")
            }
        }
    }

    private suspend fun waitForPayment(session: PaySession) {
        var failures = 0
        repeat(90) {
            delay(2_000)
            try {
                val result = api.verifyPay(session.reference)
                failures = 0
                if (result.status == "success") {
                    dispense(session)
                    return
                }
                if (result.status == "failed") {
                    _pay.value = _pay.value.copy(
                        waiting = false,
                        failReason = result.message.ifBlank { "${_pay.value.network} declined the request." },
                    )
                    _screen.value = Screen.PayFail
                    return
                }
            } catch (error: Exception) {
                failures += 1
                if (failures >= 5) {
                    _pay.value = _pay.value.copy(waiting = false, error = error.message ?: "Lost contact with the server")
                    return
                }
            }
        }
        _pay.value = _pay.value.copy(waiting = false, error = "Payment timed out. If you were charged, ask the operator.")
    }

    fun setPayMode(mode: String) {
        if (mode == "phone") payJob?.cancel()
        _pay.value = _pay.value.copy(mode = mode)
        if (_screen.value == Screen.Approve || _screen.value == Screen.PayFail) _screen.value = Screen.Pay
        val session = _pay.value.session
        if (mode == "qr" && session != null && _pay.value.error == null) {
            payJob = viewModelScope.launch { waitForPayment(session) }
        }
    }

    fun setNetwork(network: String) {
        _pay.value = _pay.value.copy(network = network)
    }

    fun phoneKey(key: String) {
        val current = _pay.value.phone
        val next = when (key) {
            "del" -> current.dropLast(1)
            "clear" -> ""
            else -> if (current.length >= 10) current else current + key
        }
        _pay.value = _pay.value.copy(phone = next)
    }

    fun sendPhonePrompt() {
        if (_pay.value.phone.length < 10) return
        _screen.value = Screen.Approve
        _pay.value = _pay.value.copy(waiting = true, expiresSec = 120, error = null)
        payJob?.cancel()
        payJob = viewModelScope.launch {
            var left = 120
            while (left > 0 && _screen.value == Screen.Approve) {
                delay(1_000)
                left -= 1
                _pay.value = _pay.value.copy(expiresSec = left)
            }
            if (_screen.value == Screen.Approve) {
                _pay.value = _pay.value.copy(
                    waiting = false,
                    failReason = "The request expired before it was approved.",
                )
                _screen.value = Screen.PayFail
            }
        }
    }

    private suspend fun dispense(session: PaySession) {
        _screen.value = Screen.Vend
        val queued = _cart.value.flatMap { line ->
            List(line.qty) { VendLineUi(line.product.name, line.product.lane, 0, true, "", "Queued") }
        }.toMutableList()
        _vend.value = VendUi(lines = queued.toList(), running = true)
        var refundMinor = 0L
        var index = 0
        var stopped = false
        for (line in _cart.value) {
            var delivered = 0
            var failed = 0
            var detail = ""
            for (unit in 1..line.qty) {
                if (stopped) {
                    queued[index] = queued[index].copy(status = "Stopped", ok = false)
                    refundMinor += line.product.priceMinor
                    failed += 1
                    index += 1
                    continue
                }
                queued[index] = queued[index].copy(status = "Dispensing...")
                _vend.value = VendUi(lines = queued.toList(), running = true)
                val result = driver.dispense(
                    lane = line.product.lane,
                    devicePath = prefs.serialDevice,
                    baudRate = prefs.baudRate,
                    simulate = prefs.simulateVend,
                )
                detail = result.detail
                if (!result.ok) {
                    failed += 1
                    refundMinor += line.product.priceMinor
                    queued[index] = queued[index].copy(status = "Couldn't dispense", ok = false, detail = detail)
                    stopped = true
                } else {
                    delivered += 1
                    queued[index] = queued[index].copy(status = "Drop confirmed", ok = true, delivered = 1, detail = detail)
                }
                index += 1
                _vend.value = VendUi(lines = queued.toList(), running = true)
            }
            if (delivered > 0) report(session.reference, line, delivered, true, detail)
            if (failed > 0) report(session.reference, line, failed, false, detail)
        }
        val results = queued.toList()
        var refundNote: String? = null
        if (refundMinor > 0) {
            refundNote = try {
                val body = api.refund(session.reference, refundMinor, "Vend failed")
                body.optString("message").ifBlank { "Refund of the failed items was sent to Paystack." }
            } catch (error: Exception) {
                "Vend failed and the refund call failed: ${error.message}"
            }
        }
        _products.value = _products.value.map { product ->
            val delivered = results.filter { it.lane == product.lane }.sumOf { it.delivered }
            product.copy(stock = (product.stock - delivered).coerceAtLeast(0))
        }
        if (installReturn) {
            val lane = _cart.value.firstOrNull()?.product?.lane ?: 0
            val status = if (results.all { it.ok }) "success" else "vend_failed"
            _install.value = _install.value.copy(
                paymentLane = lane,
                paymentReference = session.reference,
                paymentStatus = status,
                message = if (status == "success") {
                    "Payment test finished. Save this install to the desk."
                } else {
                    "The payment went through and the vend failed."
                },
            )
            persistInstall(InstallNote("payment", lane = lane, result = status, detail = session.reference))
        }
        _cart.value = emptyList()
        _refundMinor.value = refundMinor
        _deliveredCount.value = results.count { it.status == "Drop confirmed" }
        _vend.value = VendUi(lines = results, running = false, refundNote = refundNote)
        if (installReturn) {
            _screen.value = Screen.Install
            installReturn = false
            return
        }
        if (results.any { !it.ok }) {
            _screen.value = Screen.Refund
        } else {
            _doneSeconds.value = 10
            _screen.value = Screen.Done
            repeat(10) {
                delay(1_000)
                if (_screen.value != Screen.Done) return
                _doneSeconds.value = 9 - it
            }
            if (_screen.value == Screen.Done) _screen.value = Screen.Attract
        }
    }

    private suspend fun report(
        reference: String,
        line: CartLine,
        qty: Int,
        ok: Boolean,
        detail: String,
    ) {
        try {
            api.reportDeliver(reference, line.product.lane, qty, ok, detail)
        } catch (_: Exception) {
        }
    }

    fun finishVend() {
        _screen.value = if (installReturn) Screen.Install else Screen.Attract
        installReturn = false
        reloadGoods()
    }

    fun cancelPay() {
        payJob?.cancel()
        _screen.value = if (installReturn) Screen.Install else Screen.Cart
        installReturn = false
    }

    fun onBack(): Boolean {
        when (_screen.value) {
            Screen.Home -> _screen.value = Screen.Attract
            is Screen.Detail, Screen.Cart -> _screen.value = Screen.Home
            Screen.Pay, Screen.Approve, Screen.PayFail -> cancelPay()
            Screen.Pin, Screen.Service, Screen.Settings, Screen.TempPause -> _screen.value = Screen.Attract
            Screen.Install -> closeInstall()
            Screen.Done, Screen.Refund -> finishVend()
            else -> Unit
        }
        return true
    }

    fun goHome() {
        _screen.value = Screen.Home
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            try {
                val release = api.latestRelease()
                if (release == null || !UpdateInstaller.isNewer(release)) {
                    _banner.value = "This machine is already on the latest build"
                    return@launch
                }
                _banner.value = "Downloading ${release.versionName}"
                UpdateInstaller.install(getApplication(), api, release, "settings")
                _banner.value = "Update downloaded. Confirm the install prompt if the board asks."
            } catch (error: Exception) {
                _banner.value = error.message ?: "Update check failed"
            }
        }
    }

    fun testVend(lane: Int) {
        viewModelScope.launch {
            val result = driver.dispense(lane, prefs.serialDevice, prefs.baudRate, prefs.simulateVend)
            _banner.value = result.detail
        }
    }

    private var installFromService = false

    fun openInstall(fromService: Boolean = false) {
        installFromService = fromService
        viewModelScope.launch {
            val stored = installStore.latest()
            val draft = stored ?: readInstallDraft()?.also {
                installStore.record(prefs.machineNo, it, null, finalize = false)
                prefs.installDraft = ""
            }
            _install.value = (draft ?: InstallUi(serial = prefs.serialDevice, baud = prefs.baudRate, simulate = prefs.simulateVend))
                .copy(ports = CabinetPorts.survey())
            _screen.value = Screen.Install
        }
    }

    fun closeInstall() {
        _screen.value = if (installFromService) Screen.Service else Screen.Settings
    }

    fun editInstallSetup() {
        _install.value = _install.value.copy(phase = "setup", ports = CabinetPorts.survey())
    }

    fun prepareCoils(rows: Int, coils: Int, board: String, serial: String, baud: Int, simulate: Boolean) {
        val safeRows = rows.coerceIn(1, 20)
        val safeCoils = coils.coerceIn(1, 20)
        val previous = _install.value.checks.associateBy { it.lane }
        val checks = LaneMap.addresses(safeRows, safeCoils).map { address ->
            previous[address.lane]?.copy(row = address.row, coil = address.coil) ?: CoilCheck(address.row, address.coil, address.lane)
        }
        prefs.serialDevice = serial.trim()
        prefs.baudRate = baud.takeIf { it > 0 } ?: 9600
        prefs.simulateVend = simulate || serial.isBlank()
        _install.value = _install.value.copy(
            rows = safeRows,
            coils = safeCoils,
            board = if (board == "vendele") "vendele" else "unknown",
            serial = prefs.serialDevice,
            baud = prefs.baudRate,
            simulate = prefs.simulateVend,
            checks = checks,
            index = 0,
            phase = "coils",
            message = null,
            ports = CabinetPorts.survey(),
        )
        persistInstall(InstallNote("walk_started", detail = prefs.serialDevice))
    }

    fun sendCoil() {
        val state = _install.value
        val check = state.checks.getOrNull(state.index) ?: return
        if (state.busy) return
        viewModelScope.launch {
            _install.value = _install.value.copy(busy = true, message = "Sending lane ${check.lane}")
            val result = driver.dispense(check.lane, prefs.serialDevice, prefs.baudRate, prefs.simulateVend)
            _install.value = _install.value.copy(busy = false, message = result.detail)
        }
    }

    fun markCoil(result: String) {
        val state = _install.value
        val check = state.checks.getOrNull(state.index) ?: return
        val updated = state.checks.toMutableList()
        updated[state.index] = check.copy(result = result, detail = state.message ?: check.detail)
        val next = if (state.index < updated.lastIndex) state.index + 1 else state.index
        _install.value = state.copy(checks = updated, index = next, message = null)
        val marked = updated[state.index]
        persistInstall(InstallNote("coil_marked", marked.lane, marked.row, marked.coil, result, marked.detail))
    }

    fun stepCoil(delta: Int) {
        val state = _install.value
        if (state.checks.isEmpty()) return
        _install.value = state.copy(index = (state.index + delta).coerceIn(0, state.checks.lastIndex), message = null)
        persistInstall(sync = false)
    }

    fun startPaymentTest() {
        val state = _install.value
        val turned = state.checks.firstOrNull { it.result == "turned" }
        if (turned == null) {
            _install.value = state.copy(message = "Mark a coil as turned before taking a payment.")
            return
        }
        val catalog = _products.value.firstOrNull { it.lane == turned.lane }
        val product = (catalog ?: Product(
            id = "LANE-${turned.lane}",
            name = "Lane ${turned.lane} test",
            priceMinor = 10_000,
            lane = turned.lane,
            stock = 1,
            currency = prefs.currency,
        )).copy(stock = maxOf(catalog?.stock ?: 1, 1))
        installReturn = true
        _cart.value = listOf(CartLine(product, 1))
        startPay("payments@example.com")
    }

    fun saveInstall() {
        if (_install.value.busy) return
        _install.value = _install.value.copy(busy = true, message = "Saving")
        val state = _install.value
        viewModelScope.launch {
            installStore.record(prefs.machineNo, state, InstallNote("visit_saved"), finalize = true)
        }
        scheduleSync(finalize = true)
    }

    private val syncMutex = Mutex()

    private fun scheduleSync(finalize: Boolean) {
        viewModelScope.launch {
            syncMutex.withLock {
                val state = _install.value
                if (!finalize && state.phase != "coils") return@withLock
                try {
                    api.submitInstall(installPayload(state, finalize))
                    if (finalize) {
                        _install.value = _install.value.copy(
                            busy = false,
                            message = "Saved. Open Install on the desk to read it back.",
                        )
                    }
                } catch (error: Exception) {
                    if (finalize) {
                        _install.value = _install.value.copy(busy = false, message = error.message ?: "Could not reach the desk")
                    }
                }
            }
        }
    }

    private fun installPayload(state: InstallUi, finalize: Boolean): JSONObject {
        val checks = JSONArray()
        state.checks.forEach { check ->
            checks.put(
                JSONObject()
                    .put("lane", check.lane)
                    .put("row", check.row)
                    .put("coil", check.coil)
                    .put("result", check.result)
                    .put("detail", check.detail),
            )
        }
        return JSONObject()
            .put("machine_no", prefs.machineNo)
            .put("name", prefs.machineNo)
            .put("currency", prefs.currency)
            .put("board", state.board)
            .put("serial_device", state.serial)
            .put("baud", state.baud)
            .put("rows", state.rows)
            .put("coils", state.coils)
            .put("simulate", state.simulate)
            .put("payment_lane", state.paymentLane)
            .put("payment_reference", state.paymentReference)
            .put("payment_status", state.paymentStatus)
            .put("finalize", finalize)
            .put("checks", checks)
    }

    private fun persistInstall(note: InstallNote? = null, sync: Boolean = true) {
        val state = _install.value
        viewModelScope.launch {
            installStore.record(prefs.machineNo, state, note, finalize = false)
        }
        if (sync) scheduleSync(finalize = false)
    }

    private fun readInstallDraft(): InstallUi? {
        val raw = prefs.installDraft
        if (raw.isBlank()) return null
        return try {
            val json = JSONObject(raw)
            val checksJson = json.optJSONArray("checks") ?: JSONArray()
            val checks = buildList {
                for (i in 0 until checksJson.length()) {
                    val item = checksJson.getJSONObject(i)
                    add(
                        CoilCheck(
                            row = item.optInt("row"),
                            coil = item.optInt("coil"),
                            lane = item.optInt("lane"),
                            result = item.optString("result", "untested"),
                            detail = item.optString("detail"),
                        ),
                    )
                }
            }
            InstallUi(
                rows = json.optInt("rows", 6),
                coils = json.optInt("coils", 8),
                board = json.optString("board", "vendele"),
                serial = json.optString("serial", prefs.serialDevice),
                baud = json.optInt("baud", prefs.baudRate),
                simulate = json.optBoolean("simulate", prefs.simulateVend),
                checks = checks,
                index = json.optInt("index"),
                phase = json.optString("phase", "setup"),
                paymentLane = json.optInt("paymentLane"),
                paymentReference = json.optString("paymentReference"),
                paymentStatus = json.optString("paymentStatus"),
            )
        } catch (_: Exception) {
            null
        }
    }

    fun clearBanner() {
        _banner.value = null
    }

    private fun onboardCatalog(): List<Product> {
        val currency = prefs.currency
        return listOf(
            Product("A1", "Still Water", 20_000, 1, 8, currency),
            Product("A2", "Coca-Cola", 35_000, 2, 6, currency),
            Product("A3", "Fanta", 35_000, 3, 8, currency),
            Product("A4", "Sprite", 35_000, 4, 8, currency),
            Product("B1", "Potato Chips", 25_000, 5, 10, currency),
            Product("B2", "Chocolate Bar", 15_000, 6, 12, currency),
        )
    }
}
