package br.com.fiap.friosolidario

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import br.com.fiap.friosolidario.ui.Navegacao
import br.com.fiap.friosolidario.ui.tema.FrioSolidarioTema

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Barras do sistema sempre no estilo claro (ícones escuros): o app não acompanha o modo noturno.
        val barraClara = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = barraClara, navigationBarStyle = barraClara)
        setContent {
            FrioSolidarioTema {
                // O Surface define a cor padrão do texto. Sem ele, um Text sem cor explícita fora de
                // um Scaffold sai preto e some no tema escuro (o título do login, por exemplo).
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Navegacao()
                }
            }
        }
    }
}
