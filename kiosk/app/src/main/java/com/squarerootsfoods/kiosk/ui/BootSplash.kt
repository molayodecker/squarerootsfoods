package com.squarerootsfoods.kiosk.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

private const val LAST_FRAME = 84
private const val FRAME_MS = 33L

@Composable
fun BootSplash(onFinished: () -> Unit) {
    val context = LocalContext.current
    var frame by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(Unit) {
        coroutineScope {
            var pending = async(Dispatchers.IO) { decodeFrame(context, 0) }
            for (index in 0..LAST_FRAME) {
                val decoded = pending.await()
                if (decoded != null) frame = decoded.asImageBitmap()
                val next = index + 1
                if (next <= LAST_FRAME) {
                    pending = async(Dispatchers.IO) { decodeFrame(context, next) }
                }
                delay(FRAME_MS)
            }
        }
        delay(180)
        onFinished()
    }
    Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
        frame?.let {
            Image(it, "Square Roots", Modifier.size(420.dp), contentScale = ContentScale.Fit)
        }
    }
}

private fun decodeFrame(context: android.content.Context, index: Int) = runCatching {
    context.assets.open("splash/frame_%03d.png".format(index)).use { stream ->
        BitmapFactory.decodeStream(stream)
    }
}.getOrNull()
