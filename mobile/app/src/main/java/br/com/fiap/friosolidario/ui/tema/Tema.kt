package br.com.fiap.friosolidario.ui.tema

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.friosolidario.R

// Direção "linha-sinal": papel claro, tinta preta, fios grossos e quase nenhuma decoração. A cor
// fica reservada para o estado: a linha do equipamento vira o próprio semáforo, verde claro quando
// está em ordem e cheia quando pede ação.

private val Tinta = Color(0xFF0C0C0C)
private val Papel = Color(0xFFFBFBF8)
private val Vermelho = Color(0xFFC9281C)
private val Amarelo = Color(0xFFF2B705)

private val Claro = lightColorScheme(
    primary = Tinta,
    onPrimary = Papel,
    primaryContainer = Tinta,
    onPrimaryContainer = Papel,
    secondary = Tinta,
    onSecondary = Papel,
    secondaryContainer = Tinta,
    onSecondaryContainer = Papel,
    tertiary = Tinta,
    onTertiary = Papel,
    background = Papel,
    onBackground = Tinta,
    surface = Papel,
    onSurface = Tinta,
    surfaceVariant = Color(0xFFECECE6),
    onSurfaceVariant = Color(0xFF55554F),
    surfaceContainerLowest = Papel,
    surfaceContainerLow = Papel,
    surfaceContainer = Color(0xFFF3F3EE),
    surfaceContainerHigh = Color(0xFFF3F3EE),
    surfaceContainerHighest = Color(0xFFECECE6),
    outline = Tinta,
    outlineVariant = Color(0xFFC8C8C1),
    error = Vermelho,
    onError = Color.White,
    errorContainer = Vermelho,
    onErrorContainer = Color.White,
    inverseSurface = Tinta,
    inverseOnSurface = Papel,
    inversePrimary = Amarelo,
)

private val TintaClara = Color(0xFFF3F3EE)
private val Noite = Color(0xFF0D0D0C)

private val Escuro = darkColorScheme(
    primary = TintaClara,
    onPrimary = Noite,
    primaryContainer = TintaClara,
    onPrimaryContainer = Noite,
    secondary = TintaClara,
    onSecondary = Noite,
    secondaryContainer = TintaClara,
    onSecondaryContainer = Noite,
    tertiary = TintaClara,
    onTertiary = Noite,
    background = Noite,
    onBackground = TintaClara,
    surface = Noite,
    onSurface = TintaClara,
    surfaceVariant = Color(0xFF262624),
    onSurfaceVariant = Color(0xFFB0B0A8),
    surfaceContainerLowest = Noite,
    surfaceContainerLow = Noite,
    surfaceContainer = Color(0xFF1A1A18),
    surfaceContainerHigh = Color(0xFF1A1A18),
    surfaceContainerHighest = Color(0xFF262624),
    outline = TintaClara,
    outlineVariant = Color(0xFF3D3D39),
    error = Color(0xFFFF7566),
    onError = Noite,
    errorContainer = Vermelho,
    onErrorContainer = Color.White,
    inverseSurface = TintaClara,
    inverseOnSurface = Noite,
    inversePrimary = Color(0xFF8A6400),
)

/**
 * Cor de um estado (semáforo, severidade). [forte] é o tom para texto e ícone sobre o fundo da tela;
 * [fundo] preenche a linha inteira quando o estado pede ação, com [sobreFundo] e [suave] por cima.
 */
@Immutable
data class CorDeEstado(val forte: Color, val fundo: Color, val sobreFundo: Color, val suave: Color)

@Immutable
data class CoresDeEstado(
    val ok: CorDeEstado,
    val atencao: CorDeEstado,
    val critico: CorDeEstado,
    val neutro: CorDeEstado,
)

private val SobreAmarelo = Color(0xFF1C1500)

private val EstadosClaro = CoresDeEstado(
    // O que está em ordem também se lê de relance: verde claro na linha, sem competir com o
    // vermelho e o amarelo no topo da ordem.
    ok = CorDeEstado(Color(0xFF17703A), Color(0xFFE3F1E8), Tinta, Color(0xFF3B6B4E)),
    atencao = CorDeEstado(Color(0xFF7A5800), Amarelo, SobreAmarelo, SobreAmarelo),
    critico = CorDeEstado(Vermelho, Vermelho, Color.White, Color.White),
    neutro = CorDeEstado(Color(0xFF55554F), Color.Transparent, Tinta, Color(0xFF55554F)),
)

private val EstadosEscuro = CoresDeEstado(
    ok = CorDeEstado(Color(0xFF5BD07A), Color(0xFF172A1E), TintaClara, Color(0xFF9DBEAA)),
    atencao = CorDeEstado(Amarelo, Amarelo, SobreAmarelo, SobreAmarelo),
    critico = CorDeEstado(Color(0xFFFF7566), Vermelho, Color.White, Color.White),
    neutro = CorDeEstado(Color(0xFFB0B0A8), Color.Transparent, TintaClara, Color(0xFFB0B0A8)),
)

val LocalCoresDeEstado = staticCompositionLocalOf { EstadosClaro }

/** Archivo (SIL OFL 1.1, ver licencas/Archivo-OFL.txt) em três papéis: texto, título e número. */
private val Texto = FontFamily(
    Font(R.font.archivo_regular, FontWeight.Normal),
    Font(R.font.archivo_medium, FontWeight.Medium),
    Font(R.font.archivo_bold, FontWeight.Bold),
)

/** Corte largo e pesado, para o nome da tela. */
private val Titulo = FontFamily(Font(R.font.archivo_display, FontWeight.ExtraBold))

/** Corte estreito e pesado: temperaturas e pesos cabem grandes sem quebrar a linha. */
val FonteDeNumeros = FontFamily(Font(R.font.archivo_numeros, FontWeight.ExtraBold))

private fun titulo(tamanho: Int, linha: Int) =
    TextStyle(fontFamily = Titulo, fontWeight = FontWeight.ExtraBold, fontSize = tamanho.sp, lineHeight = linha.sp)

private fun texto(tamanho: Int, linha: Int, peso: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = Texto, fontWeight = peso, fontSize = tamanho.sp, lineHeight = linha.sp)

private val Tipografia = Typography(
    displayLarge = titulo(48, 52),
    displayMedium = titulo(40, 44),
    displaySmall = titulo(34, 38),
    headlineLarge = titulo(30, 34),
    headlineMedium = titulo(26, 30),
    headlineSmall = titulo(22, 26),
    titleLarge = titulo(21, 26),
    titleMedium = texto(17, 22, FontWeight.Bold),
    titleSmall = texto(15, 20, FontWeight.Bold),
    bodyLarge = texto(16, 23),
    bodyMedium = texto(14, 20),
    bodySmall = texto(13, 18),
    labelLarge = texto(15, 20, FontWeight.Bold),
    labelMedium = texto(13, 18, FontWeight.Medium),
    labelSmall = texto(12, 16, FontWeight.Medium),
)

/** Cantos quase retos em tudo: botão, campo, diálogo. Nada de pílula. */
private val Formas = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(6.dp),
    extraLarge = RoundedCornerShape(8.dp),
)

@Composable
// O app é sempre claro, mesmo com o sistema em modo noturno (decisão do Kevin em 15/11/2026): a
// tela é lida em ambiente iluminado e o preenchimento vermelho e amarelo foi pensado sobre papel.
// A paleta escura fica definida para o caso de a decisão mudar; hoje ninguém a seleciona.
fun FrioSolidarioTema(escuro: Boolean = false, conteudo: @Composable () -> Unit) {
    CompositionLocalProvider(LocalCoresDeEstado provides if (escuro) EstadosEscuro else EstadosClaro) {
        MaterialTheme(
            colorScheme = if (escuro) Escuro else Claro,
            typography = Tipografia,
            shapes = Formas,
            content = conteudo,
        )
    }
}

/** Botões do Material nascem em pílula; aqui seguem o canto quase reto do resto da tela. */
val FormaDoBotao = RoundedCornerShape(4.dp)
