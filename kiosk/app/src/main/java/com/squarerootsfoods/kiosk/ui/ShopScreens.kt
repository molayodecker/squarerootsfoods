package com.squarerootsfoods.kiosk.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.squarerootsfoods.kiosk.ClimateUi
import com.squarerootsfoods.kiosk.KioskViewModel
import com.squarerootsfoods.kiosk.PayUi
import com.squarerootsfoods.kiosk.R
import com.squarerootsfoods.kiosk.Screen
import com.squarerootsfoods.kiosk.VendUi
import com.squarerootsfoods.kiosk.data.CartLine
import com.squarerootsfoods.kiosk.data.Product
import com.squarerootsfoods.kiosk.data.formatMoney
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

private val Md = RoundedCornerShape(8.dp)
private val imageClient = OkHttpClient()
private val Lg = RoundedCornerShape(14.dp)
private fun TextUnit.tight() = (value * -0.02f).sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AttractScreen(model: KioskViewModel) {
    val climate by model.climate.collectAsState()
    Column(Modifier.fillMaxSize().clickable { model.goHome() }) {
        Row(Modifier.fillMaxWidth().padding(start = 48.dp, end = 48.dp, top = 40.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.logo_reversed), "Square Roots Foods", Modifier.height(72.dp))
            Spacer(Modifier.weight(1f))
            ClimateReadout(climate)
        }
        val context = LocalContext.current
        val hero = remember {
            BitmapFactory.decodeResource(context.resources, R.drawable.hero_attract)?.asImageBitmap()
        }
        DewGlass(
            bitmap = hero,
            modifier = Modifier.fillMaxWidth().height(470.dp).padding(top = 36.dp),
        )
        Text(
            "Cold drinks and snacks.",
            color = Cream,
            fontSize = 72.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = InterFamily,
            lineHeight = 73.sp,
            letterSpacing = (-2.16).sp,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
            modifier = Modifier.padding(start = 48.dp, end = 48.dp, top = 64.dp).width(560.dp),
        )
        Text(
            "Pay with mobile money. Collect below.",
            color = Blue300,
            fontSize = 22.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = InterFamily,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
            modifier = Modifier.padding(start = 48.dp, top = 18.dp),
        )
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().padding(start = 48.dp, end = 48.dp, bottom = 40.dp), verticalAlignment = Alignment.CenterVertically) {
            val glow by rememberInfiniteTransition(label = "touch").animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "touchGlow",
            )
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .requiredSize(78.dp + (22f * glow).dp)
                        .blur(5.dp + (7f * glow).dp, BlurredEdgeTreatment.Unbounded)
                        .border(8.dp, Accent.copy(alpha = 0.9f), CircleShape),
                )
                Box(
                    Modifier
                        .requiredSize(68.dp + (8f * glow).dp)
                        .blur(2.dp + (3f * glow).dp, BlurredEdgeTreatment.Unbounded)
                        .border(4.dp, Accent, CircleShape),
                )
                Box(Modifier.size(64.dp).border(1.dp, Accent, CircleShape), contentAlignment = Alignment.Center) {
                    PhIcon(R.drawable.ic_hand_tap, Accent, 28.dp)
                }
            }
            Text(
                "Touch to start",
                color = Cream,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = InterFamily,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                modifier = Modifier.padding(start = 16.dp).weight(1f),
            )
            Box(
                Modifier.size(44.dp).clickable { model.requestPin() },
                contentAlignment = Alignment.Center,
            ) { PhIcon(R.drawable.ic_lock_simple, Blue600, 20.dp) }
        }
    }
}

private val catalogSlides = listOf(
    R.drawable.hero_slide_01,
    R.drawable.hero_slide_02,
    R.drawable.hero_slide_03,
    R.drawable.hero_slide_04,
    R.drawable.hero_slide_05,
    R.drawable.hero_slide_06,
    R.drawable.hero_slide_07,
    R.drawable.hero_slide_08,
    R.drawable.hero_slide_09,
    R.drawable.hero_slide_10,
    R.drawable.hero_slide_11,
    R.drawable.hero_slide_12,
)

@Composable
private fun CatalogHero() {
    var index by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(4_000)
            index = (index + 1) % catalogSlides.size
        }
    }
    Crossfade(
        targetState = index,
        animationSpec = tween(durationMillis = 700),
        label = "catalogHero",
        modifier = Modifier.fillMaxWidth().height(230.dp).clip(Lg),
    ) { slide ->
        Image(
            painterResource(catalogSlides[slide]),
            "Cold drinks and snacks",
            Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
fun CatalogScreen(model: KioskViewModel, products: List<Product>, cart: List<CartLine>) {
    var filter by remember { mutableStateOf("All") }
    var keypad by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    val soldOut by model.soldOut.collectAsState()
    val visible = products.filter { filter == "All" || productCategory(it) == filter }
    val count = cart.sumOf { it.qty }
    val total = cart.sumOf { it.totalMinor }
    val currency = cart.firstOrNull()?.product?.currency ?: products.firstOrNull()?.currency ?: model.currency
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ShopBar(model, onLogo = { model.toAttract() }, onOperator = { model.requestPin() })
            Column(Modifier.padding(horizontal = 32.dp).padding(top = 8.dp)) {
                CatalogHero()
                Row(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Drinks", "Snacks").forEach { label ->
                        ChoiceChip(label, filter == label) { filter = label }
                    }
                    Spacer(Modifier.weight(1f))
                    SecondaryButton("Slot number", onClick = { keypad = true; code = "" }, modifier = Modifier.height(44.dp), leading = R.drawable.ic_hash)
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 32.dp, end = 32.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(visible, key = { it.id }) { product ->
                    val qty = cart.find { it.product.id == product.id }?.qty ?: 0
                    CatalogCard(product, qty, onOpen = { model.openDetail(product.id) }, onAdd = { model.add(product) }, onChange = { model.changeQty(product.id, it) })
                }
            }
            Row(
                Modifier.fillMaxWidth().wrapContentHeight().padding(start = 32.dp, end = 32.dp, top = 12.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(if (count == 1) "1 item" else "$count items", color = Sky, fontSize = 13.sp)
                    Text(formatMoney(total, currency), color = Cream, fontSize = 26.sp, fontWeight = FontWeight.Medium)
                }
                PrimaryButton(
                    "Review basket",
                    onClick = { model.openCart() },
                    enabled = count > 0,
                    modifier = Modifier.wrapContentWidth().height(64.dp),
                    fontSize = 18.sp,
                    leading = R.drawable.ic_basket,
                )
            }
        }
        if (keypad) SlotKeypad(code, products, onKey = { key ->
            code = when (key) {
                "del" -> code.dropLast(1)
                "clear" -> ""
                else -> (code + key).take(2).trimStart('0').ifEmpty { if (key == "0") "" else key }
            }
        }, onClose = { keypad = false }, onAdd = {
            products.find { it.lane == code.toIntOrNull() }?.let { model.add(it) }
            keypad = false
        })
        soldOut?.let { name -> SoldOutDialog(name, onDrinks = { model.dismissSoldOut(); }, onOk = { model.dismissSoldOut() }) }
    }
}

@Composable
private fun CatalogCard(product: Product, qty: Int, onOpen: () -> Unit, onAdd: () -> Unit, onChange: (Int) -> Unit) {
    val sold = product.stock <= 0
    val inCart = qty > 0 && !sold
    Box(Modifier.wrapContentHeight(Alignment.Top).alpha(if (sold) 0.55f else 1f)) {
        if (inCart) {
            Box(Modifier.matchParentSize().blur(12.dp, BlurredEdgeTreatment.Unbounded).border(2.dp, Accent, Md))
        }
        Column(
            Modifier
                .fillMaxWidth()
                .border(1.dp, if (inCart) Accent else Color.Transparent, Md)
                .clip(Md)
                .background(Surface)
                .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(Md).background(Brush.radialGradient(listOf(Blue800, Blue900))).clickable(onClick = onOpen),
        ) {
            ProductImage(product, Modifier.fillMaxSize(), ContentScale.Crop)
            Text(
                "${product.lane}",
                color = Accent100,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = InterFamily,
                letterSpacing = 0.24.sp,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                modifier = Modifier.padding(8.dp).clip(RoundedCornerShape(6.dp)).background(Accent800).padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }
        Column(Modifier.clickable(onClick = onOpen).padding(horizontal = 4.dp, vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(product.name, color = Cream, fontSize = 16.sp, fontWeight = FontWeight.Medium, fontFamily = InterFamily, maxLines = 1)
            Text(if (sold) "Sold out" else "${product.stock} left", color = Sky, fontSize = 12.sp, fontFamily = InterFamily)
            Text(formatMoney(product.priceMinor, product.currency), color = Apricot, fontSize = 18.sp, fontWeight = FontWeight.Medium, fontFamily = InterFamily, modifier = Modifier.padding(top = 4.dp))
        }
        if (qty > 0 && !sold) {
            Row(Modifier.fillMaxWidth().height(44.dp).border(1.dp, Accent, Md), verticalAlignment = Alignment.CenterVertically) {
                StepKey(R.drawable.ic_minus) { onChange(-1) }
                Text("$qty", color = Apricot, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                StepKey(R.drawable.ic_plus) { onChange(1) }
            }
        } else {
            PrimaryButton(if (sold) "Sold out" else "Add", onClick = onAdd, enabled = !sold, modifier = Modifier.fillMaxWidth().height(44.dp), fontSize = 15.sp, leading = if (sold) null else R.drawable.ic_plus)
        }
        }
    }
}

@Composable
fun DetailScreen(model: KioskViewModel, product: Product?) {
    if (product == null) return
    var qty by remember(product.id) { mutableStateOf(1) }
    Column(Modifier.fillMaxSize()) {
        ShopBar(model, onLogo = { model.toAttract() }, onOperator = { model.requestPin() })
        Column(Modifier.weight(1f).padding(horizontal = 32.dp).padding(top = 8.dp)) {
            GhostLink("All products") { model.goHome() }
            Box(Modifier.fillMaxWidth().height(540.dp).padding(top = 16.dp).clip(Lg).background(Blue900)) {
                ProductImage(product, Modifier.fillMaxSize(), ContentScale.Crop)
            }
            Row(Modifier.padding(top = 28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Slot ${product.lane}",
                    color = Accent100,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = InterFamily,
                    letterSpacing = 0.28.sp,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Accent800).padding(horizontal = 10.dp, vertical = 3.dp),
                )
                Text(
                    if (product.stock <= 0) "Sold out" else "${product.stock} left",
                    color = Sky,
                    fontSize = 15.sp,
                    fontFamily = InterFamily,
                )
            }
            Text(
                product.name,
                color = Cream,
                fontSize = 48.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = InterFamily,
                letterSpacing = 48.sp.tight(),
                lineHeight = 52.sp,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                formatMoney(product.priceMinor, product.currency),
                color = Accent300,
                fontSize = 30.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = InterFamily,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Row(Modifier.padding(start = 32.dp, end = 32.dp, top = 24.dp, bottom = 28.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.height(64.dp).border(1.dp, Divider, Md).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                StepKey(R.drawable.ic_minus, 52.dp) { if (qty > 1) qty -= 1 }
                Text("$qty", color = Cream, fontSize = 20.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                StepKey(R.drawable.ic_plus, 52.dp) { if (qty < product.stock) qty += 1 }
            }
            Spacer(Modifier.width(12.dp))
            PrimaryButton(
                "Add to basket  ·  ${formatMoney(product.priceMinor * qty, product.currency)}",
                onClick = { repeat(qty) { model.add(product) }; model.goHome() },
                enabled = product.stock > 0,
                modifier = Modifier.weight(1f).height(64.dp),
                fontSize = 18.sp,
                leading = R.drawable.ic_basket,
            )
        }
    }
}

@Composable
fun BasketScreen(model: KioskViewModel, cart: List<CartLine>) {
    val soldOut by model.soldOut.collectAsState()
    val cashEnabled by model.cashEnabled.collectAsState()
    val count = cart.sumOf { it.qty }
    val total = cart.sumOf { it.totalMinor }
    val currency = cart.firstOrNull()?.product?.currency ?: model.currency
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ShopBar(model, onLogo = { model.toAttract() }, onOperator = { model.requestPin() })
            Column(Modifier.weight(1f).padding(horizontal = 32.dp).padding(top = 8.dp)) {
                GhostLink("Continue shopping") { model.goHome() }
                Text("Your basket", color = Cream, fontSize = 44.sp, fontWeight = FontWeight.Medium, letterSpacing = 44.sp.tight(), modifier = Modifier.padding(top = 20.dp, bottom = 28.dp))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    cart.forEach { line ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${line.product.lane}", color = Cream, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.width(44.dp).clip(RoundedCornerShape(4.dp)).background(Accent700).padding(vertical = 6.dp))
                            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                                Text(line.product.name, color = Cream, fontSize = 19.sp, fontWeight = FontWeight.Medium)
                                Text("${formatMoney(line.product.priceMinor, line.product.currency)} each", color = Sky, fontSize = 13.sp)
                            }
                            Row(Modifier.height(52.dp).border(1.dp, Divider, Md), verticalAlignment = Alignment.CenterVertically) {
                                StepKey(R.drawable.ic_minus) { model.changeQty(line.product.id, -1) }
                                Text("${line.qty}", color = Cream, fontSize = 17.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                                StepKey(R.drawable.ic_plus) { model.changeQty(line.product.id, 1) }
                            }
                            Text(formatMoney(line.totalMinor, line.product.currency), color = Cream, fontSize = 19.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, modifier = Modifier.width(130.dp))
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Divider, Divider, Color.Transparent))))
                    }
                }
            }
            Column(Modifier.padding(start = 32.dp, end = 32.dp, top = 24.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Text("Subtotal  ·  $count items", color = Blue400, fontSize = 17.sp, modifier = Modifier.weight(1f))
                    Text(formatMoney(total, currency), color = Cream, fontSize = 34.sp, fontWeight = FontWeight.Medium)
                }
                PrimaryButton("Pay with mobile money", onClick = { model.startPay("payments@example.com") }, enabled = cart.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(64.dp), fontSize = 18.sp, trailing = R.drawable.ic_arrow_right)
                if (cashEnabled) {
                    SecondaryButton("Pay with cash", onClick = { model.startCash() }, modifier = Modifier.fillMaxWidth().height(64.dp), fontSize = 18.sp)
                }
            }
        }
        soldOut?.let { SoldOutDialog(it, onDrinks = { model.dismissSoldOut(); model.goHome() }, onOk = { model.dismissSoldOut() }) }
    }
}

@Composable
fun PayScreen(model: KioskViewModel, pay: PayUi, totalLabel: String) {
    Column(Modifier.fillMaxSize()) {
        ShopBar(model, onLogo = { model.toAttract() }, onOperator = {})
        Column(Modifier.weight(1f).padding(horizontal = 32.dp).padding(top = 8.dp)) {
            GhostLink("Back to basket") { model.cancelPay() }
            Text("Amount due", color = Blue300, fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = InterFamily, modifier = Modifier.padding(top = 20.dp))
            Text(totalLabel, color = Cream, fontSize = 56.sp, fontWeight = FontWeight.Medium, letterSpacing = 56.sp.tight(), modifier = Modifier.padding(bottom = 24.dp))
            if (pay.mode != "cash") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 20.dp)) {
                    ChoiceChip("Scan QR code", pay.mode == "qr", tall = true, icon = R.drawable.ic_qr_code) { model.setPayMode("qr") }
                    ChoiceChip("Use phone number", pay.mode == "phone", tall = true, icon = R.drawable.ic_device_mobile) { model.setPayMode("phone") }
                }
            }
            if (pay.mode == "cash") {
                var operatorEmail by remember { mutableStateOf("") }
                var operatorPassword by remember { mutableStateOf("") }
                Column(Modifier.weight(1f).fillMaxWidth().clip(Lg).background(Surface).border(1.dp, CardEdge, Lg).padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Operator confirmation required", color = Cream, fontSize = 24.sp, fontWeight = FontWeight.Medium, fontFamily = InterFamily)
                    Text("Hand the cash to an operator. The operator signs in after counting it.", color = Blue200, fontSize = 16.sp, fontWeight = FontWeight.Medium, fontFamily = InterFamily)
                    pay.error?.let { Text(it, color = Pressed, fontSize = 15.sp) }
                    OutlinedTextField(
                        value = operatorEmail,
                        onValueChange = { operatorEmail = it },
                        label = { Text("Operator email") },
                        singleLine = true,
                        enabled = !pay.waiting,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = operatorPassword,
                        onValueChange = { operatorPassword = it },
                        label = { Text("Operator password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !pay.waiting,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.weight(1f))
                    PrimaryButton(
                        "Confirm cash received",
                        onClick = {
                            model.confirmCash(operatorEmail.trim(), operatorPassword)
                            operatorPassword = ""
                        },
                        enabled = !pay.waiting && operatorEmail.isNotBlank() && operatorPassword.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        fontSize = 18.sp,
                    )
                }
            } else if (pay.mode == "qr") {
                Column(Modifier.weight(1f).fillMaxWidth().clip(Lg).background(Surface).border(1.dp, CardEdge, Lg).padding(28.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(240.dp).clip(Md).background(Cream), contentAlignment = Alignment.Center) {
                            val url = pay.session?.authorizationUrl
                            if (url != null) Image(rememberQrBitmap(url), "Paystack QR", Modifier.fillMaxSize().padding(8.dp))
                            else Text(pay.error ?: "Paystack QR", color = Blue900, fontSize = 13.sp)
                        }
                        Column(Modifier.padding(start = 28.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Scan with your phone", color = Cream, fontSize = 24.sp, fontWeight = FontWeight.Medium, fontFamily = InterFamily)
                            Text("Open your camera or MoMo app and approve the payment. Your items vend once Square Roots confirms it.", color = Blue200, fontSize = 16.sp, fontWeight = FontWeight.Medium, fontFamily = InterFamily)
                            if (pay.waiting) Row(verticalAlignment = Alignment.CenterVertically) {
                                SpinIcon(R.drawable.ic_circle_notch, Accent, 18.dp)
                                Text("Waiting for payment", color = Accent, fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = InterFamily, modifier = Modifier.padding(start = 8.dp))
                            }
                            pay.error?.let { Text(it, color = Pressed, fontSize = 15.sp) }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("MTN MoMo", "Telecel Cash", "AirtelTigo Money").forEach {
                            Text(it, color = Cream, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = InterFamily, modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Blue700).padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }
                }
            } else {
                PhonePad(pay, model)
            }
        }
        Row(Modifier.padding(start = 32.dp, end = 32.dp, top = 20.dp, bottom = 28.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton("Cancel", onClick = { model.cancelPay() }, modifier = Modifier.height(64.dp).width(160.dp), fontSize = 17.sp)
            if (pay.mode == "phone") {
                PrimaryButton("Send payment request", onClick = { model.sendPhonePrompt() }, enabled = pay.phone.length >= 10, modifier = Modifier.weight(1f).height(64.dp), fontSize = 18.sp, trailing = R.drawable.ic_arrow_right)
            }
        }
    }
}

@Composable
private fun PhonePad(pay: PayUi, model: KioskViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth().height(76.dp).clip(Md).background(Surface).padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            PhIcon(R.drawable.ic_device_mobile, Sky, 24.dp)
            Text(formatPhone(pay.phone).ifBlank { "024" }, color = Cream, fontSize = 32.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp, modifier = Modifier.padding(start = 14.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("MTN MoMo", "Telecel Cash", "AirtelTigo Money").forEach { name ->
                ChoiceChip(name, pay.network == name, tall = true, modifier = Modifier.weight(1f)) { model.setNetwork(name) }
            }
        }
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "Clear", "0", "⌫")
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    SecondaryButton(key, onClick = {
                        when (key) {
                            "Clear" -> model.phoneKey("clear")
                            "⌫" -> model.phoneKey("del")
                            else -> model.phoneKey(key)
                        }
                    }, modifier = Modifier.weight(1f).height(60.dp), fontSize = 24.sp)
                }
            }
        }
    }
}

@Composable
fun ApproveScreen(model: KioskViewModel, pay: PayUi, totalLabel: String) {
    val left = pay.expiresSec.coerceAtLeast(0)
    val label = "${left / 60}:${(left % 60).toString().padStart(2, '0')}"
    Column(Modifier.fillMaxSize()) {
        ShopBar(model, onLogo = {}, onOperator = {})
        Column(Modifier.weight(1f).padding(horizontal = 32.dp).padding(top = 60.dp)) {
            Box(Modifier.size(72.dp).shadow(40.dp, Lg, ambientColor = ButtonGlow, spotColor = ButtonGlow).border(1.dp, Accent, Lg), contentAlignment = Alignment.Center) {
                PhIcon(R.drawable.ic_device_mobile, Accent, 34.dp)
            }
            Text(
                "Approve $totalLabel on your phone",
                color = Cream,
                fontSize = 48.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = InterFamily,
                letterSpacing = 48.sp.tight(),
                lineHeight = 54.sp,
                modifier = Modifier.padding(top = 36.dp).width(560.dp),
            )
            Text(
                "We sent a request to ${maskPhone(pay.phone)} (${pay.network}). Enter your MoMo PIN on your phone to approve it.",
                color = Blue300,
                fontSize = 19.sp,
                fontFamily = InterFamily,
                lineHeight = 28.sp,
                modifier = Modifier.padding(top = 14.dp).width(600.dp),
            )
            Text("Don't see it? Dial your network's MoMo menu to check pending approvals.", color = Sky, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp, bottom = 40.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SpinIcon(R.drawable.ic_circle_notch, Accent, 20.dp)
                Text("Waiting for approval  ·  expires in $label", color = Accent, fontSize = 17.sp, fontWeight = FontWeight.Medium, fontFamily = InterFamily, modifier = Modifier.padding(start = 10.dp))
            }
            Box(Modifier.fillMaxWidth().height(4.dp).padding(top = 16.dp).clip(RoundedCornerShape(2.dp)).background(Blue800)) {
                Box(Modifier.fillMaxWidth(left / 120f).height(4.dp).background(Accent))
            }
        }
        Row(Modifier.padding(start = 32.dp, end = 32.dp, bottom = 28.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton("Change number", onClick = { model.setPayMode("phone") }, modifier = Modifier.height(64.dp), fontSize = 17.sp)
            SecondaryButton("Cancel payment", onClick = { model.cancelPay() }, modifier = Modifier.weight(1f).height(64.dp), fontSize = 17.sp)
        }
    }
}

@Composable
fun PayFailScreen(model: KioskViewModel, pay: PayUi) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        ShopBar(model, onLogo = {}, onOperator = {})
        Column(Modifier.weight(1f).padding(horizontal = 32.dp), verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(72.dp).border(1.dp, Sky, CircleShape), contentAlignment = Alignment.Center) { PhIcon(R.drawable.ic_x, Blue200, 34.dp) }
            Text("Payment didn't go through", color = Cream, fontSize = 48.sp, fontWeight = FontWeight.Medium, fontFamily = InterFamily, letterSpacing = 48.sp.tight(), lineHeight = 54.sp, modifier = Modifier.padding(top = 32.dp))
            Text(pay.failReason.ifBlank { pay.error ?: "The payment was declined." }, color = Blue300, fontSize = 19.sp, modifier = Modifier.padding(top = 12.dp))
            Text("You haven't been charged. Your basket is saved.", color = Sky, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp, bottom = 40.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton("Try again", onClick = { model.startPay("payments@example.com") }, modifier = Modifier.height(60.dp), fontSize = 17.sp, leading = R.drawable.ic_arrow_clockwise)
                SecondaryButton("Cancel order", onClick = { model.goHome() }, modifier = Modifier.height(60.dp), fontSize = 17.sp)
            }
        }
    }
}

@Composable
fun VendScreen(model: KioskViewModel, vend: VendUi) {
    Column(Modifier.fillMaxSize()) {
        ShopBar(model, onLogo = {}, onOperator = {})
        Column(Modifier.weight(1f).padding(horizontal = 32.dp).padding(top = 60.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PhIcon(R.drawable.ic_check_circle, Apricot, 16.dp)
                Text("Payment confirmed", color = Apricot, fontSize = 15.sp, modifier = Modifier.padding(start = 8.dp))
            }
            Text("Vending your items", color = Cream, fontSize = 44.sp, fontWeight = FontWeight.Medium, letterSpacing = 44.sp.tight(), modifier = Modifier.padding(top = 12.dp))
            Text("One slot at a time. Please wait before opening the chute.", color = Blue400, fontSize = 17.sp, modifier = Modifier.padding(top = 8.dp, bottom = 40.dp))
            vend.lines.forEach { line ->
                val failed = line.status == "Couldn't dispense"
                val stopped = line.status == "Stopped"
                Row(Modifier.fillMaxWidth().padding(vertical = 18.dp).alpha(if (stopped) 0.45f else 1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).border(1.dp, if (failed) Sky else Accent, Md), contentAlignment = Alignment.Center) {
                        Text("${line.lane}", color = Cream, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    Text(line.name, color = Cream, fontSize = 19.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f).padding(start = 18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val tint = if (stopped) Sky else if (line.status == "Dispensing...") Accent else Apricot
                        if (line.status == "Dispensing...") SpinIcon(vendIcon(line.status), tint, 16.dp) else PhIcon(vendIcon(line.status), tint, 16.dp)
                        Text(line.status, color = tint, fontSize = 14.sp, modifier = Modifier.padding(start = 6.dp))
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Divider, Divider, Color.Transparent))))
            }
        }
        Text("Each item is confirmed by the drop sensor before the next slot runs.", color = Sky, fontSize = 13.sp, modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 24.dp, bottom = 36.dp))
    }
}

@Composable
fun DoneScreen(model: KioskViewModel) {
    val seconds by model.doneSeconds.collectAsState()
    val count by model.deliveredCount.collectAsState()
    Column(Modifier.fillMaxSize()) {
        ShopBar(model, onLogo = {}, onOperator = {})
        Column(Modifier.weight(1f).padding(horizontal = 32.dp), verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(72.dp).shadow(40.dp, CircleShape, ambientColor = ButtonGlow, spotColor = ButtonGlow).border(1.dp, Accent, CircleShape), contentAlignment = Alignment.Center) {
                PhIcon(R.drawable.ic_check, Accent, 34.dp)
            }
            Text("Enjoy.", color = Cream, fontSize = 52.sp, fontWeight = FontWeight.Medium, letterSpacing = 52.sp.tight(), modifier = Modifier.padding(top = 32.dp))
            Text("Collect $count items from the chute below.", color = Blue300, fontSize = 20.sp, modifier = Modifier.padding(top = 10.dp))
            Text("A receipt has been sent to your phone.", color = Sky, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp, bottom = 40.dp))
            Row(Modifier.fillMaxWidth().clip(Md).background(Surface).border(1.dp, CardEdge, Md).padding(horizontal = 22.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                PhIcon(R.drawable.ic_arrow_down, Accent, 24.dp)
                Text("Push the flap at the bottom of the machine", color = Cream, fontSize = 17.sp, modifier = Modifier.padding(start = 14.dp))
            }
            Spacer(Modifier.height(40.dp))
            PrimaryButton("Done  ·  ${seconds}s", onClick = { model.finishVend() }, modifier = Modifier.height(60.dp), fontSize = 17.sp)
        }
    }
}

@Composable
fun RefundScreen(model: KioskViewModel, vend: VendUi, pay: PayUi) {
    val amount by model.refundMinor.collectAsState()
    val currency = model.currency
    val failed = vend.lines.count { !it.ok }
    Column(Modifier.fillMaxSize()) {
        ShopBar(model, onLogo = {}, onOperator = {})
        Column(Modifier.weight(1f).padding(horizontal = 32.dp).padding(top = 60.dp).verticalScroll(rememberScrollState())) {
            Box(Modifier.size(72.dp).border(1.dp, Sky, CircleShape), contentAlignment = Alignment.Center) { PhIcon(R.drawable.ic_arrow_u_up_left, Blue200, 32.dp) }
            Text("$failed items couldn't be dispensed", color = Cream, fontSize = 46.sp, fontWeight = FontWeight.Medium, fontFamily = InterFamily, letterSpacing = 46.sp.tight(), lineHeight = 52.sp, modifier = Modifier.padding(top = 32.dp).width(600.dp))
            Text("Your other items are in the chute. We stopped the machine and are refunding the rest.", color = Blue300, fontSize = 18.sp, modifier = Modifier.padding(top = 12.dp, bottom = 32.dp).width(600.dp))
            vend.lines.forEach { line ->
                Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${line.lane}", color = Cream, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.width(40.dp).clip(RoundedCornerShape(4.dp)).background(Blue800).padding(vertical = 4.dp))
                    Text(line.name, color = Cream, fontSize = 17.sp, modifier = Modifier.weight(1f).padding(start = 16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PhIcon(if (line.ok) R.drawable.ic_check_circle else R.drawable.ic_arrow_u_up_left, if (line.ok) Apricot else Blue300, 16.dp)
                        Text(if (line.ok) "In the chute" else "Refunding", color = if (line.ok) Apricot else Blue300, fontSize = 14.sp, modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(top = 28.dp).clip(Lg).background(Surface).border(1.dp, Accent700, Lg).padding(22.dp)) {
                Text("Refund", color = Blue400, fontSize = 14.sp)
                Text(formatMoney(amount, currency), color = Apricot, fontSize = 34.sp, fontWeight = FontWeight.Medium)
                Text("To ${maskPhone(pay.phone)}  ·  ${pay.network}  ·  usually within 24 hours", color = Blue300, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp))
                Text(vend.refundNote ?: "Slot reported to the operator", color = Sky, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
        PrimaryButton("Done", onClick = { model.finishVend() }, modifier = Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 24.dp, bottom = 28.dp).height(64.dp), fontSize = 18.sp)
    }
}

@Composable
fun ServicePinScreen(model: KioskViewModel) {
    val error by model.pinError.collectAsState()
    var pin by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(start = 48.dp, end = 48.dp, top = 60.dp, bottom = 40.dp)) {
        Box(Modifier.size(56.dp).border(1.dp, Divider, Md), contentAlignment = Alignment.Center) { PhIcon(R.drawable.ic_wrench, Blue300, 26.dp) }
        Text("Service mode", color = Cream, fontSize = 44.sp, fontWeight = FontWeight.Medium, letterSpacing = 44.sp.tight(), modifier = Modifier.padding(top = 28.dp))
        Text(if (error) "Wrong PIN. Try again." else "Enter your operator PIN.", color = Blue400, fontSize = 17.sp, modifier = Modifier.padding(top = 8.dp, bottom = 40.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 48.dp)) {
            repeat(6) { index ->
                val filled = index < pin.length
                Box(Modifier.size(22.dp).clip(CircleShape).background(if (filled) Accent else Color.Transparent).border(1.5.dp, if (filled) Accent else Blue600, CircleShape))
            }
        }
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "Clear", "0", "⌫")
        Column(Modifier.width(460.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            keys.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { key ->
                        SecondaryButton(key, onClick = {
                            pin = when (key) {
                                "Clear" -> ""
                                "⌫" -> pin.dropLast(1)
                                else -> (pin + key).take(8)
                            }
                            if (pin.length >= 6 && key != "Clear" && key != "⌫") model.submitPin(pin)
                        }, modifier = Modifier.weight(1f).height(76.dp), fontSize = 26.sp)
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        GhostLink("Back") { model.toAttract() }
    }
}

@Composable
fun ServiceScreen(model: KioskViewModel, products: List<Product>) {
    val climate by model.climate.collectAsState()
    var selected by remember { mutableStateOf(products.firstOrNull()?.lane ?: 1) }
    val stock = products.associate { it.lane to it.stock }
    val names = products.associate { it.lane to it.name }
    Column(Modifier.fillMaxSize().padding(start = 32.dp, end = 32.dp, top = 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Service mode", color = Cream, fontSize = 34.sp, fontWeight = FontWeight.Medium, letterSpacing = 34.sp.tight())
            Text("Operator", color = Cream, fontSize = 12.sp, modifier = Modifier.padding(start = 12.dp).clip(RoundedCornerShape(4.dp)).background(Accent700).padding(horizontal = 8.dp, vertical = 3.dp))
            Spacer(Modifier.weight(1f))
            SecondaryButton("Install tests", onClick = { model.openInstall(fromService = true) }, modifier = Modifier.height(44.dp).padding(end = 8.dp))
            SecondaryButton("Exit", onClick = { model.toAttract() }, modifier = Modifier.height(44.dp), leading = R.drawable.ic_sign_out)
        }
        Text("${model.machineNo}  ·  App 3.2.0  ·  Dispenser: simulated", color = Sky, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp, bottom = 20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 24.dp)) {
            StatusCard(
                "Cabinet",
                climate.temperatureText() ?: "Off",
                climateDetail(climate),
                R.drawable.ic_thermometer_cold,
                Modifier.weight(1f),
            )
            StatusCard("Door", "Closed", "Locked", R.drawable.ic_door, Modifier.weight(1f))
            StatusCard("Network", "Online", "Heartbeat 12 s ago", R.drawable.ic_wifi_high, Modifier.weight(1f))
            StatusCard("Sales", "Active", "Tap to pause", R.drawable.ic_storefront, Modifier.weight(1f))
        }
        Text("Slots", color = Cream, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        LazyVerticalGrid(
            columns = GridCells.Fixed(10),
            modifier = Modifier.weight(1f).padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items((1..60).toList()) { lane ->
                val on = lane == selected
                Column(
                    Modifier.clip(Md).background(Surface).border(1.dp, if (on) Accent else CardEdge, Md).clickable { selected = lane }.padding(6.dp),
                ) {
                    Text("$lane", color = Cream, fontSize = 12.sp)
                    Text("${stock[lane] ?: 0}", color = Apricot, fontSize = 11.sp)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("$selected", color = Cream, modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Accent700).padding(horizontal = 8.dp, vertical = 4.dp))
            Text(names[selected] ?: "Empty", color = Cream, fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 12.dp).weight(1f))
            Text("${stock[selected] ?: 0} in spiral", color = Sky, fontSize = 13.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 20.dp)) {
            SecondaryButton("", onClick = {}, modifier = Modifier.height(52.dp).width(60.dp), leading = R.drawable.ic_minus)
            SecondaryButton("", onClick = {}, modifier = Modifier.height(52.dp).width(60.dp), leading = R.drawable.ic_plus)
            PrimaryButton("Fill to capacity", onClick = {}, modifier = Modifier.height(52.dp), leading = R.drawable.ic_package)
            SecondaryButton("Disable slot", onClick = {}, modifier = Modifier.height(52.dp), leading = R.drawable.ic_prohibit)
            SecondaryButton("Test vend", onClick = { model.testVend(selected) }, modifier = Modifier.height(52.dp))
        }
    }
}

@Composable
fun TempPauseScreen(model: KioskViewModel) {
    val climate by model.climate.collectAsState()
    Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 40.dp)) {
        Image(painterResource(R.drawable.logo_reversed), "Square Roots Foods", Modifier.height(72.dp).alpha(0.55f))
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PhIcon(R.drawable.ic_thermometer_hot, Accent, 44.dp)
            Text(climate.temperatureText() ?: "—", color = Cream, fontSize = 96.sp, fontWeight = FontWeight.Medium, letterSpacing = 96.sp.tight(), modifier = Modifier.padding(start = 12.dp))
            Text("limit 8°C", color = Sky, fontSize = 16.sp, modifier = Modifier.padding(start = 14.dp))
        }
        Text("Sales are paused", color = Cream, fontSize = 48.sp, fontWeight = FontWeight.Medium, letterSpacing = 48.sp.tight(), modifier = Modifier.padding(top = 36.dp))
        Text("The cabinet is warmer than it should be. We'll reopen once it's back under 8°C so everything you buy is properly cold.", color = Blue300, fontSize = 18.sp, modifier = Modifier.padding(top = 16.dp).width(560.dp))
        Text("No payments are taken. The operator has been notified.", color = Sky, fontSize = 15.sp, modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${model.machineNo}  ·  ${climate.readingAge()}", color = Blue600, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Box(Modifier.size(44.dp).clickable { model.requestPin() }, contentAlignment = Alignment.Center) { PhIcon(R.drawable.ic_lock_simple, Blue600, 20.dp) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShopBar(model: KioskViewModel, onLogo: () -> Unit, onOperator: () -> Unit) {
    val climate by model.climate.collectAsState()
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = onLogo, onLongClick = onOperator).padding(horizontal = 32.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.mark), "Square Roots", Modifier.size(32.dp))
        Text("Square Roots", color = Cream, fontSize = 18.sp, fontWeight = FontWeight.Medium, fontFamily = InterFamily, letterSpacing = 0.18.sp, modifier = Modifier.padding(start = 10.dp))
        Text("DEMO", color = Cream, fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = InterFamily, letterSpacing = 0.22.sp, modifier = Modifier.padding(start = 10.dp).clip(RoundedCornerShape(6.dp)).background(Blue800).padding(horizontal = 10.dp, vertical = 3.dp))
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PhIcon(R.drawable.ic_device_mobile, Blue400, 16.dp)
            Text(
                "Mobile money",
                color = Blue400,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = InterFamily,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        ClimateReadout(climate, compact = true)
    }
}

@Composable
private fun ClimateReadout(climate: ClimateUi, compact: Boolean = false) {
    val temperature = climate.temperatureText()
    val humidity = climate.humidityText()
    if (temperature == null && humidity == null) return
    Row(verticalAlignment = Alignment.CenterVertically, modifier = if (compact) Modifier.padding(start = 16.dp) else Modifier) {
        if (temperature != null) {
            PhIcon(R.drawable.ic_thermometer_cold, Apricot, if (compact) 16.dp else 18.dp)
            Text(
                if (compact && climate.temperatureC != null) "$temperature chilled" else temperature,
                color = Apricot,
                fontSize = if (compact) 13.sp else 15.sp,
                fontWeight = if (compact) FontWeight.Medium else FontWeight.Normal,
                fontFamily = if (compact) InterFamily else null,
                style = if (compact) TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)) else TextStyle.Default,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        if (humidity != null) {
            Text(
                if (temperature != null) "  $humidity" else humidity,
                color = Sky,
                fontSize = if (compact) 13.sp else 15.sp,
                fontWeight = if (compact) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

private fun climateDetail(climate: ClimateUi): String {
    val humidity = climate.humidityText()
    return when {
        !climate.humidityOn -> "Humidity off"
        humidity == null -> "Humidity off"
        humidity == "No reading" -> "Humidity has no reading"
        else -> "$humidity humidity"
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, tall: Boolean = false, modifier: Modifier = Modifier, icon: Int? = null, onClick: () -> Unit) {
    val color = if (selected) Accent else Cream
    Row(
        modifier
            .height(if (tall) 48.dp else 44.dp)
            .clip(Md)
            .border(1.dp, if (selected) Accent else Divider, Md)
            .clickable(onClick = onClick)
            .padding(horizontal = if (tall) 20.dp else 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            PhIcon(icon, color, if (tall) 18.dp else 17.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text(label, color = color, fontSize = if (tall) 16.sp else 15.sp, fontWeight = FontWeight.Medium, fontFamily = InterFamily)
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, fontSize: TextUnit = 15.sp, leading: Int? = null, trailing: Int? = null) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val tint = if (!enabled) Accent.copy(alpha = 0.45f) else if (pressed) Pressed else Accent
    val glow = enabled && fontSize.value >= 18f
    val face: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                PhIcon(leading, tint, if (fontSize.value >= 18f) 22.dp else 20.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(label, color = tint, fontSize = fontSize, fontWeight = FontWeight.Medium, fontFamily = InterFamily)
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                PhIcon(trailing, tint, 20.dp)
            }
        }
    }
    val colors = ButtonDefaults.buttonColors(
        containerColor = if (pressed) Pressed.copy(alpha = 0.22f) else Color.Transparent,
        contentColor = tint,
        disabledContainerColor = Color.Transparent,
        disabledContentColor = Accent.copy(alpha = 0.45f),
    )
    val border = BorderStroke(1.dp, if (!enabled) Accent.copy(alpha = 0.45f) else if (pressed) Pressed else Accent)
    if (glow) {
        Box {
            Box(Modifier.matchParentSize().blur(16.dp, BlurredEdgeTreatment.Unbounded).border(2.dp, Accent, Md))
            Button(onClick, modifier, enabled, Md, colors, border = border, contentPadding = PaddingValues(horizontal = 32.dp, vertical = 0.dp), interactionSource = interaction) { face() }
        }
    } else {
        Button(onClick, modifier, enabled, Md, colors, border = border, contentPadding = PaddingValues(horizontal = 32.dp, vertical = 0.dp), interactionSource = interaction) { face() }
    }
}

@Composable
private fun SecondaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, fontSize: TextUnit = 15.sp, leading: Int? = null, trailing: Int? = null) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = Md,
        border = BorderStroke(1.dp, Divider),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Cream),
        contentPadding = if (label.isEmpty()) PaddingValues(0.dp) else PaddingValues(horizontal = 16.dp, vertical = 0.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) PhIcon(leading, LocalContentColor.current, 18.dp)
            if (label.isNotEmpty()) {
                if (leading != null) Spacer(Modifier.width(8.dp))
                Text(label, fontSize = fontSize, fontWeight = FontWeight.Medium, fontFamily = InterFamily)
            }
            if (trailing != null) {
                if (label.isNotEmpty()) Spacer(Modifier.width(8.dp))
                PhIcon(trailing, LocalContentColor.current, 18.dp)
            }
        }
    }
}

@Composable
private fun StepKey(icon: Int, size: androidx.compose.ui.unit.Dp = 40.dp, onClick: () -> Unit) {
    Box(Modifier.size(size).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        PhIcon(icon, Accent, 18.dp)
    }
}

@Composable
private fun GhostLink(label: String, onClick: () -> Unit) {
    Row(Modifier.clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        PhIcon(R.drawable.ic_arrow_left, Accent, 18.dp)
        Text(label, color = Accent, fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun StatusCard(title: String, value: String, note: String, icon: Int, modifier: Modifier) {
    Column(modifier.clip(Md).background(Surface).border(1.dp, CardEdge, Md).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PhIcon(icon, Sky, 14.dp)
            Text(title, color = Sky, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
        }
        Text(value, color = Cream, fontSize = 22.sp, fontWeight = FontWeight.Medium)
        Text(note, color = Sky, fontSize = 12.sp)
    }
}

@Composable
private fun SoldOutDialog(name: String, onDrinks: () -> Unit, onOk: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
        Column(Modifier.width(460.dp).clip(Lg).background(Surface).border(1.dp, CardEdge, Lg).padding(28.dp)) {
            PhIcon(R.drawable.ic_warning_circle, Accent, 32.dp)
            Text("$name just sold out", color = Cream, fontSize = 22.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 12.dp))
            Text("We couldn't reserve it for you, so it's not in your basket. You haven't been charged.", color = Blue300, fontSize = 16.sp, modifier = Modifier.padding(top = 12.dp))
            Row(Modifier.padding(top = 20.dp).align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("See drinks", onClick = onDrinks, modifier = Modifier.height(44.dp))
                PrimaryButton("OK", onClick = onOk, modifier = Modifier.height(44.dp))
            }
        }
    }
}

@Composable
private fun SlotKeypad(code: String, products: List<Product>, onKey: (String) -> Unit, onClose: () -> Unit, onAdd: () -> Unit) {
    val match = products.find { it.lane == code.toIntOrNull() }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
        Column(Modifier.width(420.dp).clip(Lg).background(Background).border(1.dp, CardEdge, Lg).padding(24.dp).clickable {}) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enter slot number", color = Cream, fontSize = 20.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Box(Modifier.size(44.dp).clickable(onClick = onClose), contentAlignment = Alignment.Center) { PhIcon(R.drawable.ic_x, Cream, 20.dp) }
            }
            Text(code.ifBlank { "—" }, color = Cream, fontSize = 40.sp, fontWeight = FontWeight.Medium)
            Text(match?.let { "${it.name}  ·  ${formatMoney(it.priceMinor, it.currency)}" } ?: "Number on the shelf tag", color = Blue300, fontSize = 15.sp, modifier = Modifier.padding(bottom = 16.dp))
            listOf("123", "456", "789", "C0⌫").forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    row.forEach { ch ->
                        val key = when (ch) { 'C' -> "clear"; '⌫' -> "del"; else -> ch.toString() }
                        SecondaryButton(ch.toString(), onClick = { onKey(key) }, modifier = Modifier.weight(1f).height(60.dp), fontSize = 22.sp)
                    }
                }
            }
            PrimaryButton("Add to basket", onClick = onAdd, enabled = match != null && (match.stock > 0), modifier = Modifier.fillMaxWidth().height(56.dp), leading = R.drawable.ic_plus)
        }
    }
}

@Composable
private fun PhIcon(res: Int, tint: Color, size: androidx.compose.ui.unit.Dp) {
    Icon(painterResource(res), contentDescription = null, modifier = Modifier.size(size), tint = tint)
}

@Composable
private fun SpinIcon(res: Int, tint: Color, size: androidx.compose.ui.unit.Dp) {
    val turn by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 900, easing = LinearEasing)),
        label = "spinTurn",
    )
    Icon(
        painterResource(res),
        contentDescription = null,
        modifier = Modifier.size(size).graphicsLayer { rotationZ = turn },
        tint = tint,
    )
}

private fun vendIcon(status: String): Int = when (status) {
    "Couldn't dispense" -> R.drawable.ic_warning
    "Stopped" -> R.drawable.ic_minus_circle
    "Drop confirmed" -> R.drawable.ic_check_circle
    "Dispensing..." -> R.drawable.ic_circle_notch
    else -> R.drawable.ic_clock
}

private fun formatPhone(digits: String): String = digits.chunked(3).joinToString(" ")
private fun maskPhone(digits: String): String = if (digits.length < 6) digits else "${digits.take(3)}  ····  ${digits.takeLast(4)}"

@Composable
private fun ProductImage(product: Product, modifier: Modifier, scale: ContentScale) {
    if (product.imageUrl.isNotBlank()) {
        RemoteProductImage(product.imageUrl, product.name, modifier, scale)
    } else {
        productPhoto(product.name)?.let { Image(painterResource(it), product.name, modifier, contentScale = scale) }
    }
}

@Composable
private fun RemoteProductImage(url: String, name: String, modifier: Modifier, scale: ContentScale) {
    var bitmap by remember(url) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(url) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val response = imageClient.newCall(Request.Builder().url(url).build()).execute()
                response.use { body ->
                    body.body?.bytes()?.let { bytes ->
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    }
                }
            }.getOrNull()
        }
    }
    bitmap?.let { Image(it, name, modifier, contentScale = scale) }
}

private fun productCategory(product: Product): String {
    return if (product.category.equals("Snacks", true) || product.category.equals("Drinks", true)) {
        product.category.replaceFirstChar { it.titlecase() }
    } else {
        shopCategory(product.name)
    }
}

private fun shopCategory(name: String): String {
    val lower = name.lowercase()
    return if (lower.contains("chip") || lower.contains("chocolate") || lower.contains("snack") || lower.contains("bar")) "Snacks" else "Drinks"
}

private fun productPhoto(name: String): Int? = when {
    name.equals("Still Water", true) -> R.drawable.product_still_water
    name.contains("coca", true) || name.equals("Cola", true) -> R.drawable.product_coca_cola
    name.contains("fanta", true) -> R.drawable.product_fanta
    name.contains("sprite", true) -> R.drawable.product_sprite
    else -> null
}

@Composable
private fun rememberQrBitmap(contents: String) = remember(contents) {
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val matrix = QRCodeWriter().encode(contents, BarcodeFormat.QR_CODE, 480, 480, hints)
    Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888).apply {
        for (x in 0 until matrix.width) for (y in 0 until matrix.height) {
            setPixel(x, y, if (matrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
        }
    }.asImageBitmap()
}

