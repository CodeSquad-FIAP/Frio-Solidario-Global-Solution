package br.com.fiap.friosolidario.core

import br.com.fiap.friosolidario.BuildConfig

/**
 * Único ponto do app que sabe onde a API mora. O valor vem de `frio.apiUrl`, em
 * `mobile/gradle.properties` — troque lá (ou com `-Pfrio.apiUrl=...`) e recompile.
 */
object ApiConfig {
    val BASE_URL: String = BuildConfig.API_URL

    /** Conexão, leitura e escrita. Passou disso, a tela mostra "sem conexão" com tentar de novo. */
    const val TIMEOUT_SEGUNDOS = 10L
}
