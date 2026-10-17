package com.focuno.core.domain.classifier

import com.focuno.core.model.DistractionEvent
import com.focuno.core.model.DistractionFeatures

/**
 * Predicts attention loss from a feature vector. V1 ships
 * [HeuristicDistractionClassifier]; V2 can swap in a TFLite-backed implementation
 * without any upstream change.
 */
fun interface DistractionClassifier {
    fun classify(features: DistractionFeatures): DistractionEvent
}
