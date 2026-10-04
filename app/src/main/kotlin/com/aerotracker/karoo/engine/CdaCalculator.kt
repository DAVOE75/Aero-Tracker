package com.aerotracker.karoo.engine

import com.aerotracker.karoo.model.AeroInput
import com.aerotracker.karoo.model.AeroResult
import com.aerotracker.karoo.model.CdaCategory
import kotlin.math.abs
import kotlin.math.sign

/**
 * Motor de cálculo de CdA (Coeficiente de Arrastre Aerodinámico).
 *
 * Fórmula base (modelo de potencia en ciclismo):
 *
 *   P_total = P_aero + P_rolling + P_gravity
 *
 * Donde:
 *   P_aero   = 0.5 * ρ * CdA * (v + v_wind)² * v
 *   P_rolling = Crr * m * g * v * cos(arctan(gradient/100))  ≈ Crr * m * g * v
 *   P_gravity = m * g * gradient/100 * v
 *   ρ         = densidad del aire (kg/m³)
 *   v         = velocidad del ciclista (m/s)
 *   v_wind    = velocidad del viento de frente (m/s), positivo = headwind
 *   m         = masa total (ciclista + bici) kg
 *   g         = 9.81 m/s²
 *
 * Despejando CdA:
 *   CdA = (P_total - P_rolling - P_gravity) / (0.5 * ρ * (v + v_wind)² * v)
 *
 * Restricciones:
 *   - Se requiere velocidad mínima de 3 m/s (~11 km/h) para evitar división por cero
 *   - CdA resultado debe estar en rango [0.05, 1.0] para ser válido
 */
object CdaCalculator {

    private const val G = 9.81          // m/s²
    private const val MIN_SPEED_MS = 3.0    // 10.8 km/h mínimo
    private const val MIN_POWER_W = 10.0    // Mínimo de potencia
    private const val CDA_MIN = 0.05
    private const val CDA_MAX = 1.0

    /**
     * Calcula el CdA a partir de los datos de entrada.
     * @return [AeroResult] con el resultado del cálculo
     */
    fun calculate(input: AeroInput, baselineCdA: Double = 0.32): AeroResult {
        val v = input.speedMs
        val p = input.powerWatts
        val g = input.gradientPercent / 100.0
        val m = input.riderMassKg
        val rho = input.airDensityKgM3
        val crr = input.crrRolling
        val vWind = input.windSpeedMs

        // Validación de entrada
        if (v < MIN_SPEED_MS || p < MIN_POWER_W) {
            return AeroResult(
                cdA = 0.0,
                powerAero = 0.0,
                powerRolling = 0.0,
                powerGravity = 0.0,
                wattsSavedVsBaseline = 0.0,
                category = CdaCategory.UPRIGHT,
                isValid = false
            )
        }

        // Velocidad aparente del viento respecto al ciclista
        // vWind positivo = viento de frente (headwind)
        val vAirRelative = v + vWind

        // Potencias componentes
        val pRolling = crr * m * G * v
        val pGravity = m * G * g * v

        // Potencia disponible para aerodinámica
        val pAero = p - pRolling - pGravity

        // Si la potencia aerodinámica es negativa (bajando o muy lento), no calculamos CdA válido
        if (pAero <= 0) {
            return AeroResult(
                cdA = 0.0,
                powerAero = 0.0,
                powerRolling = pRolling,
                powerGravity = pGravity,
                wattsSavedVsBaseline = 0.0,
                category = CdaCategory.SUPER_AERO,
                isValid = false
            )
        }

        // Denominador: 0.5 * ρ * v_rel² * v
        val denominator = 0.5 * rho * vAirRelative * vAirRelative * v

        if (denominator <= 0) {
            return AeroResult(
                cdA = 0.0,
                powerAero = pAero,
                powerRolling = pRolling,
                powerGravity = pGravity,
                isValid = false,
                category = CdaCategory.UPRIGHT
            )
        }

        val cdA = pAero / denominator

        // Validar rango físico
        val isValid = cdA in CDA_MIN..CDA_MAX

        // Calcular vatios ahorrados respecto a la postura base
        val pAeroBaseline = 0.5 * rho * baselineCdA * vAirRelative * vAirRelative * v
        val wattsSaved = pAeroBaseline - pAero

        return AeroResult(
            cdA = if (isValid) cdA else cdA.coerceIn(CDA_MIN, CDA_MAX),
            powerAero = pAero,
            powerRolling = pRolling,
            powerGravity = pGravity,
            wattsSavedVsBaseline = wattsSaved,
            category = CdaCategory.fromCdA(cdA),
            isValid = isValid
        )
    }

    /**
     * Calcula el promedio suavizado de CdA usando una ventana deslizante.
     */
    fun smoothedCdA(history: List<Double>): Double {
        if (history.isEmpty()) return 0.0
        return history.average()
    }

    /**
     * Convierte densidad del aire basándose en altitud (metros) y temperatura (°C).
     * Fórmula aproximada ISA (Atmósfera Estándar Internacional).
     */
    fun airDensityFromAltitudeAndTemp(altitudeM: Double, tempC: Double): Double {
        val tempK = tempC + 273.15
        val pressure = 101325.0 * Math.pow((1.0 - 0.0000226 * altitudeM), 5.256)
        return pressure / (287.05 * tempK)
    }
}
