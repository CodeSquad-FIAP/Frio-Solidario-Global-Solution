package br.com.fiap.friosolidario.ui.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val Claro = lightColorScheme(
    primary = Color(0xFF0B5C7A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC9E8F5),
    onPrimaryContainer = Color(0xFF001F2A),
    secondary = Color(0xFF4D616C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0E6F2),
    onSecondaryContainer = Color(0xFF081E27),
    background = Color(0xFFF4F8FB),
    onBackground = Color(0xFF171C1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171C1F),
    surfaceVariant = Color(0xFFE3EBF0),
    onSurfaceVariant = Color(0xFF41484D),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFEDF3F7),
    surfaceContainerHigh = Color(0xFFE6EEF3),
    surfaceContainerHighest = Color(0xFFDFE8EE),
    outline = Color(0xFF71787D),
    outlineVariant = Color(0xFFCFD8DE),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFFCE4E1),
    onErrorContainer = Color(0xFF5C1410),
)

private val Escuro = darkColorScheme(
    primary = Color(0xFF86CFEB),
    onPrimary = Color(0xFF003546),
    primaryContainer = Color(0xFF004D64),
    onPrimaryContainer = Color(0xFFC9E8F5),
    secondary = Color(0xFFB4CAD6),
    onSecondary = Color(0xFF1F333D),
    secondaryContainer = Color(0xFF354A54),
    onSecondaryContainer = Color(0xFFD0E6F2),
    background = Color(0xFF0F1417),
    onBackground = Color(0xFFDEE3E7),
    surface = Color(0xFF171C1F),
    onSurface = Color(0xFFDEE3E7),
    surfaceVariant = Color(0xFF2A3136),
    onSurfaceVariant = Color(0xFFC0C8CD),
    surfaceContainerLowest = Color(0xFF0A0F12),
    surfaceContainerLow = Color(0xFF171C1F),
    surfaceContainer = Color(0xFF1B2024),
    surfaceContainerHigh = Color(0xFF252B2E),
    surfaceContainerHighest = Color(0xFF303639),
    outline = Color(0xFF8A9297),
    outlineVariant = Color(0xFF41484D),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

/** Cor de um estado (semáforo, severidade): o tom forte e o fundo suave do mesmo matiz. */
@Immutable
data class CorDeEstado(val forte: Color, val fundo: Color, val sobreFundo: Color)

@Immutable
data class CoresDeEstado(
    val ok: CorDeEstado,
    val atencao: CorDeEstado,
    val critico: CorDeEstado,
    val neutro: CorDeEstado,
)

private val EstadosClaro = CoresDeEstado(
    ok = CorDeEstado(Color(0xFF1E8E3E), Color(0xFFE2F4E7), Color(0xFF0D4A1F)),
    atencao = CorDeEstado(Color(0xFFE8A200), Color(0xFFFFF2CC), Color(0xFF5C4100)),
    critico = CorDeEstado(Color(0xFFD12D22), Color(0xFFFCE4E1), Color(0xFF6E130D)),
    neutro = CorDeEstado(Color(0xFF8A9297), Color(0xFFE9EEF1), Color(0xFF3A4247)),
)

private val EstadosEscuro = CoresDeEstado(
    ok = CorDeEstado(Color(0xFF5BD07A), Color(0xFF12381D), Color(0xFFBDEFCA)),
    atencao = CorDeEstado(Color(0xFFFFC53D), Color(0xFF453200), Color(0xFFFFE6A3)),
    critico = CorDeEstado(Color(0xFFFF6B5E), Color(0xFF5A130E), Color(0xFFFFD2CD)),
    neutro = CorDeEstado(Color(0xFF8A9297), Color(0xFF2A3136), Color(0xFFC0C8CD)),
)

val LocalCoresDeEstado = staticCompositionLocalOf { EstadosClaro }

@Composable
fun FrioSolidarioTema(escuro: Boolean = isSystemInDarkTheme(), conteudo: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalCoresDeEstado provides if (escuro) EstadosEscuro else EstadosClaro,
    ) {
        MaterialTheme(colorScheme = if (escuro) Escuro else Claro, content = conteudo)
    }
}
