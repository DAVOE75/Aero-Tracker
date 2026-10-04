package com.aerotracker.karoo.extension

import android.util.Log
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.models.*
import com.aerotracker.karoo.engine.CdaCalculator
import com.aerotracker.karoo.model.AeroInput
import com.aerotracker.karoo.model.UserPreferences
import kotlinx.coroutines.*

/**
 * Servicio principal de la extensión Aero Tracker para Karoo 2/3 (API 1.1.7).
 */
class AeroTrackerExtension : KarooExtension("aero-tracker", "1.0.0") {

    companion object {
        private const val TAG = "AeroTracker"

        const val DATA_TYPE_CDA         = "aerotracker-cda"
        const val DATA_TYPE_CDA_SMOOTH  = "aerotracker-cda-smooth"
        const val DATA_TYPE_WATTS_SAVED = "aerotracker-watts-saved"
        const val DATA_TYPE_POWER_AERO  = "aerotracker-power-aero"
        const val DATA_TYPE_CATEGORY    = "aerotracker-category"
    }

    private val extensionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val prefs = UserPreferences()
    private val cdaHistory = ArrayDeque<Double>(60)

    private var latestPower: Double = 0.0
    private var latestSpeed: Double = 0.0
    private var latestGradient: Double = 0.0
    private var latestAltitude: Double = 0.0

    private var karooSystem: KarooSystemService? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AeroTrackerExtension created")
        karooSystem = KarooSystemService(this)
        karooSystem?.connect { connected ->
            if (connected) {
                startMonitoring()
            }
        }
    }

    private fun startMonitoring() {
        karooSystem?.let { ks ->
            // Escuchar cambios de potencia
            ks.addConsumer<OnStreamState>(OnStreamState.StartStreaming(DataType.Type.POWER)) { event ->
                val state = event.state
                if (state is StreamState.Streaming) {
                    latestPower = state.dataPoint.values[DataType.Field.POWER] ?: latestPower
                }
            }

            // Escuchar cambios de velocidad (km/h a m/s)
            ks.addConsumer<OnStreamState>(OnStreamState.StartStreaming(DataType.Type.SPEED)) { event ->
                val state = event.state
                if (state is StreamState.Streaming) {
                    val speedKph = state.dataPoint.values[DataType.Field.SPEED] ?: return@addConsumer
                    latestSpeed = speedKph / 3.6
                }
            }

            // Escuchar cambios de pendiente (%)
            ks.addConsumer<OnStreamState>(OnStreamState.StartStreaming(DataType.Type.ELEVATION_GRADE)) { event ->
                val state = event.state
                if (state is StreamState.Streaming) {
                    latestGradient = state.dataPoint.values[DataType.Field.ELEVATION_GRADE] ?: latestGradient
                }
            }

            // Escuchar cambios de elevación (m)
            ks.addConsumer<OnStreamState>(OnStreamState.StartStreaming(DataType.Type.PRESSURE_ELEVATION_CORRECTION)) { event ->
                val state = event.state
                if (state is StreamState.Streaming) {
                    latestAltitude = state.dataPoint.values[DataType.Field.PRESSURE_ELEVATION] ?: latestAltitude
                }
            }
        }
    }

    private fun getCalculationResult(): com.aerotracker.karoo.model.AeroResult {
        val airDensity = CdaCalculator.airDensityFromAltitudeAndTemp(latestAltitude, 20.0)

        val input = AeroInput(
            powerWatts = latestPower,
            speedMs = latestSpeed,
            gradientPercent = latestGradient,
            windSpeedMs = 0.0,
            riderMassKg = prefs.totalMassKg,
            airDensityKgM3 = airDensity,
            crrRolling = prefs.crrRolling
        )

        return CdaCalculator.calculate(input, prefs.baselineCdA)
    }

    override val types: List<DataTypeImpl> = listOf(
        object : DataTypeImpl("aero-tracker", "aerotracker-cda") {
            override fun startStream(emitter: Emitter<StreamState>) {
                val job = extensionScope.launch {
                    while (isActive) {
                        val result = getCalculationResult()
                        if (result.isValid) {
                            if (cdaHistory.size >= 60) cdaHistory.removeFirst()
                            cdaHistory.addLast(result.cdA)
                            
                            val dataPoint = DataPoint(dataTypeId, mapOf(dataTypeId to result.cdA))
                            emitter.onNext(StreamState.Streaming(dataPoint))
                        } else {
                            emitter.onNext(StreamState.Idle)
                        }
                        delay(1000L)
                    }
                }
                emitter.setCancellable { job.cancel() }
            }

            override fun startView(context: android.content.Context, config: ViewConfig, emitter: io.hammerhead.karooext.internal.ViewEmitter) {
                val view = com.aerotracker.karoo.ui.AeroGaugeView(context)
                
                val viewJob = extensionScope.launch {
                    while (isActive) {
                        val result = getCalculationResult()
                        
                        view.cdaValue = result.cdA.toFloat()
                        view.isValid = result.isValid
                        view.wattsSaved = result.wattsSavedVsBaseline.toFloat()
                        view.category = result.category
                        
                        val width = config.viewSize.first
                        val height = config.viewSize.second
                        
                        if (width > 0 && height > 0) {
                            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bitmap)
                            view.measure(
                                android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
                                android.view.View.MeasureSpec.makeMeasureSpec(height, android.view.View.MeasureSpec.EXACTLY)
                            )
                            view.layout(0, 0, width, height)
                            view.draw(canvas)

                            val rv = android.widget.RemoteViews(context.packageName, com.aerotracker.karoo.R.layout.widget_aero_gauge)
                            rv.setImageViewBitmap(com.aerotracker.karoo.R.id.gauge_image, bitmap)
                            emitter.updateView(rv)
                        }
                        delay(1000L)
                    }
                }
                emitter.setCancellable { viewJob.cancel() }
            }
        },
        object : DataTypeImpl("aero-tracker", "aerotracker-cda-smooth") {
            override fun startStream(emitter: Emitter<StreamState>) {
                val job = extensionScope.launch {
                    while (isActive) {
                        if (cdaHistory.isNotEmpty()) {
                            val smoothedCdA = CdaCalculator.smoothedCdA(cdaHistory.takeLast(prefs.smoothingWindowSec))
                            val dataPoint = DataPoint(dataTypeId, mapOf(dataTypeId to smoothedCdA))
                            emitter.onNext(StreamState.Streaming(dataPoint))
                        } else {
                            emitter.onNext(StreamState.Idle)
                        }
                        delay(1000L)
                    }
                }
                emitter.setCancellable { job.cancel() }
            }
        },
        object : DataTypeImpl("aero-tracker", "aerotracker-watts-saved") {
            override fun startStream(emitter: Emitter<StreamState>) {
                val job = extensionScope.launch {
                    while (isActive) {
                        val result = getCalculationResult()
                        if (result.isValid) {
                            val dataPoint = DataPoint(dataTypeId, mapOf(dataTypeId to result.wattsSavedVsBaseline))
                            emitter.onNext(StreamState.Streaming(dataPoint))
                        } else {
                            emitter.onNext(StreamState.Idle)
                        }
                        delay(1000L)
                    }
                }
                emitter.setCancellable { job.cancel() }
            }
        },
        object : DataTypeImpl("aero-tracker", "aerotracker-power-aero") {
            override fun startStream(emitter: Emitter<StreamState>) {
                val job = extensionScope.launch {
                    while (isActive) {
                        val result = getCalculationResult()
                        if (result.isValid) {
                            val dataPoint = DataPoint(dataTypeId, mapOf(dataTypeId to result.powerAero))
                            emitter.onNext(StreamState.Streaming(dataPoint))
                        } else {
                            emitter.onNext(StreamState.Idle)
                        }
                        delay(1000L)
                    }
                }
                emitter.setCancellable { job.cancel() }
            }
        },
        object : DataTypeImpl("aero-tracker", "aerotracker-category") {
            override fun startStream(emitter: Emitter<StreamState>) {
                val job = extensionScope.launch {
                    while (isActive) {
                        val result = getCalculationResult()
                        if (result.isValid) {
                            val dataPoint = DataPoint(dataTypeId, mapOf(dataTypeId to result.category.ordinal.toDouble()))
                            emitter.onNext(StreamState.Streaming(dataPoint))
                        } else {
                            emitter.onNext(StreamState.Idle)
                        }
                        delay(1000L)
                    }
                }
                emitter.setCancellable { job.cancel() }
            }
        }
    )

    override fun onDestroy() {
        karooSystem?.disconnect()
        extensionScope.cancel()
        super.onDestroy()
    }
}
