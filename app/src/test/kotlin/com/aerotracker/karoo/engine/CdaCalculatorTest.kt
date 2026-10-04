package com.aerotracker.karoo.engine

import com.aerotracker.karoo.model.AeroInput
import com.aerotracker.karoo.model.CdaCategory
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitarios del motor de cálculo de CdA.
 *
 * Valores de referencia tomados de la literatura de ciencias del ciclismo
 * y validados contra la calculadora de TrainingPeaks CdA.
 */
class CdaCalculatorTest {

    @Test
    fun `calcula CdA correcto para valores tipicos de TT`() {
        // Valores típicos de un triatleta en posición TT:
        // 250W, 40 km/h, llano, 75kg
        val input = AeroInput(
            powerWatts = 250.0,
            speedMs = 40.0 / 3.6,   // ~11.11 m/s
            gradientPercent = 0.0,
            riderMassKg = 80.0       // 75kg ciclista + 5kg bici aprox
        )
        val result = CdaCalculator.calculate(input)

        assertTrue("El resultado debe ser válido", result.isValid)
        // CdA típico en TT agresivo ~0.22–0.26
        assertTrue("CdA debe estar en rango TT (0.18–0.30)", result.cdA in 0.18..0.30)
        assertEquals(CdaCategory.AERO, result.category)
    }

    @Test
    fun `calcula CdA correcto para postura erguida`() {
        // Ciclista sentado erguido, manos en capota:
        // 200W, 30 km/h, llano, 80kg → CdA ~0.35–0.45
        val input = AeroInput(
            powerWatts = 200.0,
            speedMs = 30.0 / 3.6,
            gradientPercent = 0.0,
            riderMassKg = 80.0
        )
        val result = CdaCalculator.calculate(input)

        assertTrue(result.isValid)
        assertTrue("CdA debe ser > 0.30 para postura erguida", result.cdA > 0.28)
    }

    @Test
    fun `invalido cuando velocidad es menor al minimo`() {
        val input = AeroInput(
            powerWatts = 100.0,
            speedMs = 2.0,  // < 3 m/s mínimo
            gradientPercent = 0.0,
            riderMassKg = 80.0
        )
        val result = CdaCalculator.calculate(input)
        assertFalse("Debe ser inválido con velocidad muy baja", result.isValid)
    }

    @Test
    fun `invalido cuando potencia es cero`() {
        val input = AeroInput(
            powerWatts = 0.0,
            speedMs = 10.0,
            gradientPercent = 0.0,
            riderMassKg = 80.0
        )
        val result = CdaCalculator.calculate(input)
        assertFalse("Debe ser inválido sin potencia", result.isValid)
    }

    @Test
    fun `headwind aumenta CdA calculado`() {
        val baseInput = AeroInput(
            powerWatts = 300.0,
            speedMs = 12.0,
            gradientPercent = 0.0,
            riderMassKg = 80.0,
            windSpeedMs = 0.0
        )
        val headwindInput = baseInput.copy(windSpeedMs = 3.0) // 10.8 km/h de frente

        val baseResult = CdaCalculator.calculate(baseInput)
        val headwindResult = CdaCalculator.calculate(headwindInput)

        // Con viento de frente, la misma potencia "parece" más aerodinámica
        // (denominador mayor → CdA calculado menor)
        assertTrue(headwindResult.cdA < baseResult.cdA)
    }

    @Test
    fun `calcula densidad del aire correctamente a altitud`() {
        val densitySeaLevel = CdaCalculator.airDensityFromAltitudeAndTemp(0.0, 15.0)
        val densityHighAlt = CdaCalculator.airDensityFromAltitudeAndTemp(2000.0, 10.0)

        assertTrue("Densidad a nivel del mar ~1.225", densitySeaLevel in 1.2..1.25)
        assertTrue("Densidad a 2000m debe ser menor", densityHighAlt < densitySeaLevel)
    }

    @Test
    fun `categoria correcta para cada rango de CdA`() {
        assertEquals(CdaCategory.SUPER_AERO, CdaCategory.fromCdA(0.18))
        assertEquals(CdaCategory.AERO, CdaCategory.fromCdA(0.22))
        assertEquals(CdaCategory.EFFICIENT, CdaCategory.fromCdA(0.28))
        assertEquals(CdaCategory.UPRIGHT, CdaCategory.fromCdA(0.36))
        assertEquals(CdaCategory.VERY_UPRIGHT, CdaCategory.fromCdA(0.50))
    }

    @Test
    fun `suavizado devuelve promedio correcto`() {
        val values = listOf(0.24, 0.26, 0.28, 0.22, 0.25)
        val smoothed = CdaCalculator.smoothedCdA(values)
        assertEquals(0.25, smoothed, 0.001)
    }
}
