package br.com.fiap.friosolidario

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.fiap.friosolidario.ui.Navegacao
import br.com.fiap.friosolidario.ui.tema.FrioSolidarioTema

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FrioSolidarioTema {
                Navegacao()
            }
        }
    }
}
