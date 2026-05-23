package com.campusdigitalfp.proyecto_v2.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary              = Color(0xFF97C459),
    onPrimary            = Color(0xFF173404),
    primaryContainer     = Color(0xFF27500A),  // fondo de cards "assigned" en dark
    onPrimaryContainer   = Color(0xFFC0DD97),
    secondary            = Color(0xFFFAC775),
    onSecondary          = Color(0xFF412402),
    secondaryContainer   = Color(0xFF633806),  // fondo de cards "done" en dark
    onSecondaryContainer = Color(0xFFFAEEDA),
    tertiary             = Color(0xFFEF9F27),
    onTertiary           = Color(0xFF2C2C2A),
    background           = Color(0xFF2C2C2A),
    surface              = Color(0xFF2C2C2A),
    onBackground         = Color(0xFFF1EFE8),
    onSurface            = Color(0xFFF1EFE8),
    onSurfaceVariant     = Color(0xFFB4B2A9),  // texto secundario (partner, ciudad...)
    outline              = Color(0xFF888780),
    outlineVariant       = Color(0xFF444441),  // divisores HorizontalDivider
    surfaceVariant       = Color(0xFF444441),  // fondo botón filtro no seleccionado
)

private val LightColorScheme = lightColorScheme(
    primary              = Color(0xFF3B6D11),
    onPrimary            = Color(0xFFC0DD97),
    primaryContainer     = Color(0xFFC0DD97),  // fondo de cards "assigned" en light
    onPrimaryContainer   = Color(0xFF173404),
    secondary            = Color(0xFFBA7517),
    onSecondary          = Color(0xFFFAEEDA),
    secondaryContainer   = Color(0xFFFAC775),  // fondo de cards "done" en light
    onSecondaryContainer = Color(0xFF412402),
    tertiary             = Color(0xFFEF9F27),
    onTertiary           = Color(0xFF2C2C2A),
    background           = Color(0xFFF1EFE8),
    surface              = Color(0xFFF1EFE8),
    onBackground         = Color(0xFF2C2C2A),
    onSurface            = Color(0xFF2C2C2A),
    onSurfaceVariant     = Color(0xFF5F5E5A),  // texto secundario (partner, ciudad...)
    outline              = Color(0xFF888780),
    outlineVariant       = Color(0xFFD3D1C7),  // divisores HorizontalDivider
    surfaceVariant       = Color(0xFFD3D1C7),  // fondo botón filtro no seleccionado
)

@Composable
fun Proyecto_v2Theme(
    darkTheme: Boolean = isSystemInDarkTheme() ,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false ,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme ,
        typography = Typography ,
        content = content
    )
}