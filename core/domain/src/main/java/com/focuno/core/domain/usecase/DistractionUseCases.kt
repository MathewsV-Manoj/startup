package com.focuno.core.domain.usecase

import com.focuno.core.domain.classifier.DistractionClassifier
import com.focuno.core.model.DistractionEvent
import com.focuno.core.model.DistractionFeatures
import javax.inject.Inject

class DetectDistractionUseCase @Inject constructor(
    private val classifier: DistractionClassifier,
) {
    operator fun invoke(features: DistractionFeatures): DistractionEvent = classifier.classify(features)
}
