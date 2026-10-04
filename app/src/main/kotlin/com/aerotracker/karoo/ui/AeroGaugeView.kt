package com.aerotracker.karoo.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.aerotracker.karoo.model.CdaCategory
import kotlin.math.*

/**
 * Vista personalizada del medidor de CdA tipo "túnel de viento".
 *
 * Dibuja un medidor de aguja semicircular con:
 *  - Arco de colores (verde = aero, rojo = no aero)
 *  - Aguja apuntando al valor actual
 *  - Valor numérico de CdA en el centro
 *  - Etiquetas de categoría
 *  - Indicador de vatios ahorrados
 */
class AeroGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    // Configuración del medidor
    private val startAngle = 150f      // Ángulo de inicio (grados desde eje X positivo)
    private val sweepAngle = 240f      // Ángulo total del arco
    private val cdaMin = 0.15f
    private val cdaMax = 0.55f

    // Estado
    var cdaValue: Float = 0.28f
        set(value) { field = value.coerceIn(cdaMin, cdaMax); invalidate() }

    var isValid: Boolean = true
        set(value) { field = value; invalidate() }

    var wattsSaved: Float = 0f
        set(value) { field = value; invalidate() }

    var category: CdaCategory = CdaCategory.EFFICIENT
        set(value) { field = value; invalidate() }

    // Paints
    private val arcPaintGreen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 32f
        strokeCap = Paint.Cap.ROUND
    }

    private val arcPaintBackground = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 32f
        color = Color.parseColor("#2A2A2A")
        strokeCap = Paint.Cap.ROUND
    }

    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.WHITE
        strokeCap = Paint.Cap.ROUND
    }

    private val needleBasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val cdaTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }

    private val labelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AAAAAA")
        textAlign = Paint.Align.CENTER
    }

    private val categoryTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val wattsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
    }

    private val invalidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#888888")
        textAlign = Paint.Align.CENTER
    }

    // Shader de color para el arco (verde → amarillo → rojo)
    private var arcShader: SweepGradient? = null
    private var arcRect = RectF()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val padding = arcPaintGreen.strokeWidth * 2
        val size = min(w.toFloat(), h * 1.5f) - padding * 2
        val cx = w / 2f
        val cy = h * 0.6f
        val r = size / 2f

        arcRect.set(cx - r, cy - r, cx + r, cy + r)

        // Crear shader de colores en el arco
        arcShader = SweepGradient(
            cx, cy,
            intArrayOf(
                Color.parseColor("#00E676"),  // Verde vibrante (super aero)
                Color.parseColor("#69F0AE"),  // Verde claro
                Color.parseColor("#FFEB3B"),  // Amarillo
                Color.parseColor("#FF9800"),  // Naranja
                Color.parseColor("#F44336"),  // Rojo (muy erguido)
                Color.parseColor("#F44336"),
            ),
            floatArrayOf(0f, 0.2f, 0.4f, 0.7f, 0.9f, 1.0f)
        )
        arcPaintGreen.shader = arcShader
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height * 0.6f

        // Fondo del arco (gris oscuro)
        canvas.drawArc(arcRect, startAngle, sweepAngle, false, arcPaintBackground)

        if (isValid) {
            // Calcular qué porcentaje del arco está lleno
            val fraction = (cdaValue - cdaMin) / (cdaMax - cdaMin)
            val activeSweep = sweepAngle * fraction.coerceIn(0f, 1f)

            // Guardar y rotar canvas para alinear shader con el arco
            canvas.save()
            canvas.rotate(startAngle, cx, cy)
            canvas.drawArc(arcRect, 0f, activeSweep, false, arcPaintGreen)
            canvas.restore()

            // Dibujar aguja
            drawNeedle(canvas, cx, cy, fraction)

            // Valor de CdA
            cdaTextPaint.textSize = arcRect.width() * 0.18f
            canvas.drawText(
                String.format("%.3f", cdaValue),
                cx, cy + arcRect.height() * 0.15f,
                cdaTextPaint
            )

            // Unidad
            labelTextPaint.textSize = arcRect.width() * 0.08f
            canvas.drawText("CdA m²", cx, cy + arcRect.height() * 0.25f, labelTextPaint)

            // Categoría
            val catColor = categoryColor(category)
            categoryTextPaint.color = catColor
            categoryTextPaint.textSize = arcRect.width() * 0.09f
            
            // Usar el contexto para resolver el recurso de string y así soportar internacionalización
            val categoryLabel = try {
                context.getString(category.labelResId)
            } catch (e: Exception) {
                "" // Fallback de seguridad
            }
            
            canvas.drawText(categoryLabel, cx, cy + arcRect.height() * 0.38f, categoryTextPaint)

            // Vatios ahorrados
            if (wattsSaved != 0f) {
                val sign = if (wattsSaved >= 0) "−" else "+"
                val absW = abs(wattsSaved).roundToInt()
                wattsPaint.color = if (wattsSaved >= 0) Color.parseColor("#00E676") else Color.parseColor("#FF5252")
                wattsPaint.textSize = arcRect.width() * 0.075f
                canvas.drawText("$sign${absW}W vs base", cx, cy + arcRect.height() * 0.49f, wattsPaint)
            }

            // Etiquetas min/max
            drawMinMaxLabels(canvas, cx, cy)

        } else {
            // Pantalla de "sin datos"
            invalidPaint.textSize = arcRect.width() * 0.1f
            canvas.drawText("Esperando datos...", cx, cy, invalidPaint)
            invalidPaint.textSize = arcRect.width() * 0.07f
            canvas.drawText("v > 10 km/h & potenciómetro", cx, cy + invalidPaint.textSize * 1.5f, invalidPaint)
        }
    }

    private fun drawNeedle(canvas: Canvas, cx: Float, cy: Float, fraction: Float) {
        val r = arcRect.width() / 2f
        val angleRad = Math.toRadians((startAngle + sweepAngle * fraction).toDouble())
        val needleLen = r * 0.82f
        val tipX = cx + needleLen * cos(angleRad).toFloat()
        val tipY = cy + needleLen * sin(angleRad).toFloat()

        // Sombra de la aguja
        needlePaint.color = Color.parseColor("#333333")
        needlePaint.strokeWidth = 8f
        canvas.drawLine(cx + 2, cy + 2, tipX + 2, tipY + 2, needlePaint)

        // Aguja blanca
        needlePaint.color = Color.WHITE
        needlePaint.strokeWidth = 5f
        canvas.drawLine(cx, cy, tipX, tipY, needlePaint)

        // Centro de la aguja (círculo)
        canvas.drawCircle(cx, cy, 12f, needleBasePaint)
    }

    private fun drawMinMaxLabels(canvas: Canvas, cx: Float, cy: Float) {
        labelTextPaint.textSize = arcRect.width() * 0.065f
        val r = arcRect.width() / 2f * 1.1f

        // Etiqueta min (izquierda)
        val minAngle = Math.toRadians(startAngle.toDouble())
        val minX = cx + r * cos(minAngle).toFloat()
        val minY = cy + r * sin(minAngle).toFloat()
        canvas.drawText("0.15", minX, minY, labelTextPaint)

        // Etiqueta max (derecha)
        val maxAngle = Math.toRadians((startAngle + sweepAngle).toDouble())
        val maxX = cx + r * cos(maxAngle).toFloat()
        val maxY = cy + r * sin(maxAngle).toFloat()
        canvas.drawText("0.55", maxX, maxY, labelTextPaint)
    }

    private fun categoryColor(cat: CdaCategory): Int = when (cat) {
        CdaCategory.SUPER_AERO  -> Color.parseColor("#00E676")
        CdaCategory.AERO        -> Color.parseColor("#69F0AE")
        CdaCategory.EFFICIENT   -> Color.parseColor("#FFEB3B")
        CdaCategory.UPRIGHT     -> Color.parseColor("#FF9800")
        CdaCategory.VERY_UPRIGHT -> Color.parseColor("#F44336")
    }
}
