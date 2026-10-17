package com.focuno.core.ml

import com.focuno.core.domain.classifier.DistractionClassifier
import com.focuno.core.domain.classifier.HeuristicDistractionClassifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Selects the classifier implementation. Today: the explainable heuristic.
 * To ship the trained model, flip [provideDistractionClassifier] to prefer
 * [TfliteDistractionClassifier] when [TfliteDistractionClassifier.isModelAvailable].
 */
@Module
@InstallIn(SingletonComponent::class)
object ClassifierModule {

    @Provides
    @Singleton
    fun provideDistractionClassifier(): DistractionClassifier = HeuristicDistractionClassifier()
}
