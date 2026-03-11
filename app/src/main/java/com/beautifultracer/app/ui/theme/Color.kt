package com.beautifultracer.app.ui.theme

import androidx.compose.ui.graphics.Color

// Primary palette — Deep indigo / electric blue
val PrimaryLight = Color(0xFF3D5AFE)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFDBE1FF)
val OnPrimaryContainerLight = Color(0xFF00105C)

val PrimaryDark = Color(0xFFBBC3FF)
val OnPrimaryDark = Color(0xFF08218A)
val PrimaryContainerDark = Color(0xFF2639C1)
val OnPrimaryContainerDark = Color(0xFFDBE1FF)

// Secondary palette — Teal accent
val SecondaryLight = Color(0xFF00BFA5)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFA7F5EC)
val OnSecondaryContainerLight = Color(0xFF00382F)

val SecondaryDark = Color(0xFF4CDBC4)
val OnSecondaryDark = Color(0xFF003830)
val SecondaryContainerDark = Color(0xFF005046)
val OnSecondaryContainerDark = Color(0xFFA7F5EC)

// Tertiary palette — Amber / warm tones
val TertiaryLight = Color(0xFFFF9100)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFFFDDB5)
val OnTertiaryContainerLight = Color(0xFF2C1600)

val TertiaryDark = Color(0xFFFFB95F)
val OnTertiaryDark = Color(0xFF462A00)
val TertiaryContainerDark = Color(0xFF653E00)
val OnTertiaryContainerDark = Color(0xFFFFDDB5)

// Error
val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

// Backgrounds & Surfaces — Light
val BackgroundLight = Color(0xFFFCFCFF)
val OnBackgroundLight = Color(0xFF1A1B2E)
val SurfaceLight = Color(0xFFFCFCFF)
val OnSurfaceLight = Color(0xFF1A1B2E)
val SurfaceVariantLight = Color(0xFFE2E1EC)
val OnSurfaceVariantLight = Color(0xFF45464F)
val OutlineLight = Color(0xFF767680)
val OutlineVariantLight = Color(0xFFC6C5D0)

// Backgrounds & Surfaces — Dark
val BackgroundDark = Color(0xFF121318)
val OnBackgroundDark = Color(0xFFE4E1E9)
val SurfaceDark = Color(0xFF121318)
val OnSurfaceDark = Color(0xFFE4E1E9)
val SurfaceVariantDark = Color(0xFF45464F)
val OnSurfaceVariantDark = Color(0xFFC6C5D0)
val OutlineDark = Color(0xFF90909A)
val OutlineVariantDark = Color(0xFF45464F)

// Utility colors for ping status
val PingExcellent = Color(0xFF00C853)   // < 20ms
val PingGood = Color(0xFF64DD17)        // < 50ms
val PingFair = Color(0xFFFFD600)        // < 100ms
val PingPoor = Color(0xFFFF9100)        // < 200ms
val PingBad = Color(0xFFFF1744)         // >= 200ms
val PingTimeout = Color(0xFF9E9E9E)     // Timeout / unreachable
