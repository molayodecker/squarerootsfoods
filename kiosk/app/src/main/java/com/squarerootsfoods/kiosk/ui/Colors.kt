package com.squarerootsfoods.kiosk.ui

import androidx.compose.ui.graphics.Color

// Palette
val Navy = Color(0xFF003366)
val Sky = Color(0xFF6699CC)
val Orange = Color(0xFFFF6600)
val Apricot = Color(0xFFFFB366)
val Slate = Color(0xFF334455)
val CreamWash = Color(0xFFFFE5CC)

// Core UI
val Background = Color(0xFF002346)
val BackgroundTop = Navy
val Surface = Color(0xFF0B3D6E)
val Cream = Color(0xFFFFF4E8)
val Accent = Orange
val Divider = Cream.copy(alpha = 0.16f)

// Accent ramp, lightest to darkest
val Accent100 = Color(0xFFFFF0E5)
val Accent200 = CreamWash
val Accent300 = Apricot
val Accent400 = Color(0xFFFF8A33)
val Accent500 = Orange
val Accent600 = Color(0xFFE05500)
val Accent700 = Color(0xFFB34400)
val Accent800 = Color(0xFF803100)
val Accent900 = Color(0xFF4D1D00)

// Blue ramp, lightest to darkest
val Blue100 = Cream
val Blue200 = Color(0xFFDDE8F4)
val Blue300 = Color(0xFFB8CFE8)
val Blue400 = Color(0xFF8FB3DA)
val Blue500 = Sky
val Blue600 = Color(0xFF4D7AA8)
val Blue700 = Color(0xFF335D87)
val Blue800 = Color(0xFF1F4A78)
val Blue900 = Color(0xFF123459)

val SelectedGlow = Accent.copy(alpha = 0.22f)
val ButtonGlow = Accent.copy(alpha = 0.25f)
val CardEdge = Blue800
val Muted = Blue300
val Pressed = Accent400

val NavyTop = BackgroundTop
val Paper = Background
val Ink = Cream
val Gold = Apricot
val Pine = Accent
val PineDark = Surface
val CardWhite = Surface
val Danger = Pressed
