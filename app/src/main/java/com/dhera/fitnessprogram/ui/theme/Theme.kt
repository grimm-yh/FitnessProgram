package com.dhera.fitnessprogram.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppTheme(val label: String, val primary: Color) {
    DEFAULT("默认紫", Purple40),
    ORANGE("活力橙", FitnessOrange),
    BLUE("稳重蓝", FitnessBlue),
    GREEN("生机绿", FitnessGreen),
    RED("热血红", FitnessRed),
    TEAL("时尚青", FitnessTeal)
}

@Composable
fun FitnessProgramTheme(
    appTheme: AppTheme = AppTheme.DEFAULT,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = appTheme.primary,
            secondary = Color.DarkGray,
            tertiary = appTheme.primary
        )
    } else {
        lightColorScheme(
            primary = appTheme.primary,
            secondary = Color.Gray,
            tertiary = appTheme.primary
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
