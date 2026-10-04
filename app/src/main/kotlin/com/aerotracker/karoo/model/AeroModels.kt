package com.aerotracker.karoo.model

import androidx.annotation.StringRes
import com.aerotracker.karoo.R

/**
 * Datos de entrada para el cálculo de CdA en tiempo real.
 *
 * @param powerWatts           Potencia en vatios
 * @param speedMs              Velocidad en m/s
 * @param gradientPercent      Desnivel en porcentaje (positivo = subida)
 * @param windSpeedMs          Velocidad del viento en m/s (positivo = de frente, negativo = a favor)
 * @param riderMassKg          Masa del ciclista + bici en kg
 * @param airDensityKgM3       Densidad del aire en kg/m³ (por defecto 1.225 a nivel del mar)
 * @param crrRolling           Coeficiente de resistencia a la rodadura (por defecto 0.004)
 */
data class AeroInput(
    val powerWatts: Double,
    val speedMs: Double,
    val gradientPercent: Double,
    val windSpeedMs: Double = 0.0,
    val riderMassKg: Double = 75.0,
    val airDensityKgM3: Double = 1.225,
    val crrRolling: Double = 0.004
)

/**
 * Resultado del cálculo de CdA.
 *
 * @param cdA                   Coeficiente de arrastre aerodinámico calculado (m²)
 * @param powerAero             Potencia consumida por aerodinámica (W)
 * @param powerRolling          Potencia consumida por rodadura (W)
 * @param powerGravity          Potencia consumida por gravedad (W)
 * @param wattsSavedVsBaseline  Vatios ahorrados respecto a la postura base
 * @param category              Categoría descriptiva del CdA
 * @param isValid               Indica si el cálculo es válido (velocidad suficiente, etc.)
 */
data class AeroResult(
    val cdA: Double,
    val powerAero: Double,
    val powerRolling: Double,
    val powerGravity: Double,
    val wattsSavedVsBaseline: Double = 0.0,
    val category: CdaCategory,
    val isValid: Boolean
)

/**
 * Categoría descriptiva del coeficiente CdA.
 * Rangos aproximados para ciclistas en bicicleta de carretera.
 */
enum class CdaCategory(@StringRes val labelResId: Int, @StringRes val descriptionResId: Int) {
    SUPER_AERO(R.string.cat_super_aero_label, R.string.cat_super_aero_desc),
    AERO(R.string.cat_aero_label, R.string.cat_aero_desc),
    EFFICIENT(R.string.cat_efficient_label, R.string.cat_efficient_desc),
    UPRIGHT(R.string.cat_upright_label, R.string.cat_upright_desc),
    VERY_UPRIGHT(R.string.cat_very_upright_label, R.string.cat_very_upright_desc);

    companion object {
        fun fromCdA(cda: Double): CdaCategory = when {
            cda < 0.20 -> SUPER_AERO
            cda < 0.25 -> AERO
            cda < 0.32 -> EFFICIENT
            cda < 0.40 -> UPRIGHT
            else       -> VERY_UPRIGHT
        }
    }
}

/**
 * Preferencias de usuario para el cálculo.
 */
data class UserPreferences(
    val riderMassKg: Double = 75.0,
    val bikeMassKg: Double = 8.0,
    val crrRolling: Double = 0.004,
    val baselineCdA: Double = 0.32,
    val useWeatherApi: Boolean = false,
    val openWeatherApiKey: String = "",
    val showWattsSaved: Boolean = true,
    val smoothingWindowSec: Int = 5
) {
    val totalMassKg: Double get() = riderMassKg + bikeMassKg
}
