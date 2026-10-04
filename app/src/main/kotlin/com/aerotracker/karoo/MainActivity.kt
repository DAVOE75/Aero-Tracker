package com.aerotracker.karoo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aerotracker.karoo.engine.CdaCalculator
import com.aerotracker.karoo.model.AeroInput
import com.aerotracker.karoo.model.CdaCategory
import com.aerotracker.karoo.ui.theme.AeroTrackerTheme

/**
 * Actividad principal de configuración de Aero Tracker.
 *
 * Sirve como simulador de CdA en el teléfono/tablet para verificar
 * que los cálculos son correctos antes de usar en el Karoo.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AeroTrackerTheme {
                AeroTrackerConfigScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AeroTrackerConfigScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("AeroPrefs", android.content.Context.MODE_PRIVATE) }

    var power by remember { mutableStateOf(200f) }
    var speedKph by remember { mutableStateOf(35f) }
    var gradient by remember { mutableStateOf(0f) }
    var windKph by remember { mutableStateOf(0f) }
    
    // Variables persistentes reales
    var riderMass by remember { mutableStateOf(sharedPrefs.getFloat("RIDER_MASS", 75f)) }
    var bikeMass by remember { mutableStateOf(sharedPrefs.getFloat("BIKE_MASS", 8f)) }
    var crr by remember { mutableStateOf(sharedPrefs.getFloat("CRR", 0.004f)) }

    val result = remember(power, speedKph, gradient, riderMass, bikeMass, windKph, crr) {
        CdaCalculator.calculate(
            AeroInput(
                powerWatts = power.toDouble(),
                speedMs = speedKph / 3.6,
                gradientPercent = gradient.toDouble(),
                riderMassKg = (riderMass + bikeMass).toDouble(),
                windSpeedMs = windKph / 3.6,
                crrRolling = crr.toDouble()
            )
        )
    }

    val bgColor = Color(0xFF0D0D0D)
    val cardColor = Color(0xFF1A1A1A)
    val accentColor = Color(0xFF00E676)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "🌬️ Aero Tracker",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            "Simulador de CdA — Karoo 2/3",
                            color = Color(0xFF888888),
                            fontSize = 12.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF111111)
                )
            )
        },
        containerColor = bgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CdA Result Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("CdA Estimado", color = Color(0xFF888888), fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))

                    if (result.isValid) {
                        Text(
                            String.format("%.4f", result.cdA),
                            color = accentColor,
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("m²", color = Color(0xFF666666), fontSize = 14.sp)
                        Spacer(Modifier.height(12.dp))

                        val catColor = categoryColor(result.category)
                        Chip(
                            label = result.category.label,
                            description = result.category.description,
                            color = catColor
                        )

                        Spacer(Modifier.height(12.dp))

                        // Desglose de potencias
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            PowerBreakdownItem(
                                label = "Aero",
                                value = "${result.powerAero.toInt()}W",
                                color = Color(0xFF2196F3)
                            )
                            PowerBreakdownItem(
                                label = "Rodadura",
                                value = "${result.powerRolling.toInt()}W",
                                color = Color(0xFFFF9800)
                            )
                            PowerBreakdownItem(
                                label = "Gravedad",
                                value = "${result.powerGravity.toInt()}W",
                                color = Color(0xFFE91E63)
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        val wSaved = result.wattsSavedVsBaseline.toInt()
                        val sign = if (wSaved >= 0) "−" else "+"
                        Text(
                            "$sign${Math.abs(wSaved)}W vs postura base (CdA 0.32)",
                            color = if (wSaved >= 0) accentColor else Color(0xFFFF5252),
                            fontSize = 13.sp
                        )
                    } else {
                        Text(
                            "—",
                            color = Color(0xFF555555),
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Velocidad insuficiente o sin potencia",
                            color = Color(0xFF666666),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Instrucciones y Explicación
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("💡 ¿Cómo funciona Aero Tracker?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Esta extensión calcula en tiempo real tu Coeficiente de Arrastre Aerodinámico (CdA). " +
                        "Aplica las leyes de la física tomando la potencia total que aplicas a los pedales y restando la resistencia a la rodadura y a la gravedad (desnivel).\n\n" +
                        "Lo que sobra, es la potencia necesaria para vencer la resistencia del viento. Cuanto más bajo sea el CdA, más rápido irás con los mismos vatios.",
                        color = Color(0xFFCCCCCC),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text("⚙️ Configuración Importante (Guarda en la extensión)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Para que los cálculos en carretera sean exactos, DEBES ajustar estos valores. " +
                        "La extensión utilizará estos datos automáticamente en el Karoo:\n" +
                        "• Masa del Ciclista: Tu peso corporal con ropa.\n" +
                        "• Masa Bicicleta: Peso de la bici + bidones + equipamiento.\n" +
                        "• Crr: Tipo de asfalto (0.003 liso, 0.004 normal, 0.006 rugoso/mojado).",
                        color = Color(0xFFCCCCCC),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // Datos de la Extensión (Persistentes)
            Text("Ajustes del Ciclista (Para el Karoo)", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)

            NumberInputCard(
                label = "Masa Ciclista",
                value = riderMass,
                onValueChange = { 
                    riderMass = it
                    sharedPrefs.edit().putFloat("RIDER_MASS", it).apply()
                },
                unit = "kg",
                color = Color(0xFFE91E63),
                cardColor = cardColor
            )

            NumberInputCard(
                label = "Masa Bici + Equipaje",
                value = bikeMass,
                onValueChange = { 
                    bikeMass = it
                    sharedPrefs.edit().putFloat("BIKE_MASS", it).apply()
                },
                unit = "kg",
                color = Color(0xFFE91E63),
                cardColor = cardColor
            )

            SliderCard(
                label = "Coef. Rodadura (Crr)",
                value = crr,
                onValueChange = { 
                    crr = it
                    sharedPrefs.edit().putFloat("CRR", it).apply()
                },
                range = 0.002f..0.008f,
                unit = "",
                color = Color(0xFFFF9800),
                cardColor = cardColor,
                format = "%.4f"
            )

            Spacer(Modifier.height(8.dp))
            
            // Sliders de entrada del simulador
            Text("Parámetros de Simulación (Pruebas)", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)

            SliderCard(
                label = "Potencia",
                value = power,
                onValueChange = { power = it },
                range = 0f..600f,
                unit = "W",
                color = Color(0xFF2196F3),
                cardColor = cardColor
            )

            SliderCard(
                label = "Velocidad",
                value = speedKph,
                onValueChange = { speedKph = it },
                range = 0f..70f,
                unit = "km/h",
                color = accentColor,
                cardColor = cardColor
            )

            SliderCard(
                label = "Desnivel",
                value = gradient,
                onValueChange = { gradient = it },
                range = -10f..10f,
                unit = "%",
                color = Color(0xFFFF9800),
                cardColor = cardColor
            )

            SliderCard(
                label = "Viento de frente",
                value = windKph,
                onValueChange = { windKph = it },
                range = -30f..30f,
                unit = "km/h",
                color = Color(0xFF9C27B0),
                cardColor = cardColor
            )

            // Nota informativa
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2818))
            ) {
                Row(modifier = Modifier.padding(12.dp)) {
                    Text("ℹ️ ", fontSize = 14.sp)
                    Text(
                        "Esta pantalla es el simulador. En el Karoo 2/3 los datos de potencia, velocidad y desnivel se leen automáticamente de los sensores. Añade los campos de datos de Aero Tracker a tu pantalla de actividad.",
                        color = Color(0xFF80CBC4),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun Chip(label: String, description: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(description, color = color.copy(alpha = 0.7f), fontSize = 11.sp)
        }
    }
}

@Composable
fun PowerBreakdownItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, color = Color(0xFF888888), fontSize = 11.sp)
    }
}

@Composable
fun NumberInputCard(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    unit: String,
    color: Color,
    cardColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(label, color = Color(0xFFAAAAAA), fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { onValueChange((value - 0.5f).coerceAtLeast(0f)) },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF333333))
                ) {
                    Text("-", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                }
                
                Text(
                    String.format("%.1f %s", value, unit),
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                IconButton(
                    onClick = { onValueChange((value + 0.5f).coerceAtMost(200f)) },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF333333))
                ) {
                    Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                }
            }
        }
    }
}

@Composable
fun SliderCard(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    color: Color,
    cardColor: Color,
    format: String = "%.1f %s"
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, color = Color(0xFFAAAAAA), fontSize = 13.sp)
                Text(
                    if (unit.isEmpty()) String.format(format, value) else String.format(format, value, unit),
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                colors = SliderDefaults.colors(
                    thumbColor = color,
                    activeTrackColor = color,
                    inactiveTrackColor = color.copy(alpha = 0.2f)
                )
            )
        }
    }
}

fun categoryColor(cat: CdaCategory): Color = when (cat) {
    CdaCategory.SUPER_AERO   -> Color(0xFF00E676)
    CdaCategory.AERO         -> Color(0xFF69F0AE)
    CdaCategory.EFFICIENT    -> Color(0xFFFFEB3B)
    CdaCategory.UPRIGHT      -> Color(0xFFFF9800)
    CdaCategory.VERY_UPRIGHT -> Color(0xFFF44336)
}
