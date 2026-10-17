package com.focuno.core.ml

import android.content.Context
import com.focuno.core.domain.classifier.DistractionClassifier
import com.focuno.core.domain.classifier.HeuristicDistractionClassifier
import com.focuno.core.model.DistractionEvent
import com.focuno.core.model.DistractionFeatures
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TensorFlow Lite-backed classifier. Drop a `distraction_v1.tflite` model into
 * `core/ml/src/main/assets/` and [isModelAvailable] becomes true. Until then it
 * transparently falls back to the heuristic classifier so the app always works.
 *
 * Input:  float32 [1, DistractionFeatures.VECTOR_SIZE]
 * Output: float32 [1, 1] distraction probability
 */
@Singleton
class TfliteDistractionClassifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : DistractionClassifier {

    private val fallback = HeuristicDistractionClassifier()

    private val interpreter: Interpreter? by lazy {
        runCatching { loadInterpreter() }.getOrNull()
    }

    val isModelAvailable: Boolean get() = interpreter != null

    override fun classify(features: DistractionFeatures): DistractionEvent {
        val model = interpreter ?: return fallback.classify(features)

        val input = ByteBuffer
            .allocateDirect(DistractionFeatures.VECTOR_SIZE * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .apply {
                features.toVector().forEach { putFloat(it) }
                rewind()
            }

        val output = Array(1) { FloatArray(1) }
        model.run(input, output)

        val probability = output[0][0].coerceIn(0f, 1f)
        return DistractionEvent(
            at = java.time.Instant.now(),
            probability = probability,
            reason = "Model prediction",
        )
    }

    private fun loadInterpreter(): Interpreter {
        val assetFileDescriptor = context.assets.openFd(MODEL_ASSET)
        assetFileDescriptor.use { afd ->
            val modelBuffer = afd.createInputStream().channel.map(
                FileChannel.MapMode.READ_ONLY,
                afd.startOffset,
                afd.declaredLength,
            )
            return Interpreter(modelBuffer)
        }
    }

    private companion object {
        const val MODEL_ASSET = "distraction_v1.tflite"
    }
}
