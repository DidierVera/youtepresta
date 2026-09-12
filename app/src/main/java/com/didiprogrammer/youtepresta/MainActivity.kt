package com.didiprogrammer.youtepresta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.didiprogrammer.youtepresta.data.model.Friend
import com.didiprogrammer.youtepresta.data.remote.SupabaseClientProvider
import com.didiprogrammer.youtepresta.ui.theme.YouTePrestaTheme
import io.github.jan.supabase.postgrest.from

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YouTePrestaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SupabaseConnectionStatus(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun SupabaseConnectionStatus(modifier: Modifier = Modifier) {
    var statusMessage by remember { mutableStateOf("Conectando a Supabase...") }

    LaunchedEffect(Unit) {
        statusMessage = try {
            val friends = SupabaseClientProvider.client
                .from("friends")
                .select()
                .decodeList<Friend>()
            "Conectado a Supabase (${friends.size} amigos)"
        } catch (e: Exception) {
            "Error al conectar con Supabase: ${e.message}"
        }
    }

    Text(text = statusMessage, modifier = modifier)
}
