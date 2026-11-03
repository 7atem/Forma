// Text/status/download pattern adapted from Nemory MlKitNanoRuntime, Apache-2.0.
// See docs/NEMORY_REUSE_AUDIT.md. No image, cloud, JNI or vault dependencies.
package com.formacoach.app

import com.formacoach.coach.LocalTextModel
import com.formacoach.coach.ModelAvailability
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.TextPart
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map

@Singleton
class LocalNanoModel @Inject constructor():LocalTextModel {
    private val model by lazy {Generation.getClient()}
    override suspend fun status()=when(model.checkStatus()) {
        FeatureStatus.AVAILABLE -> ModelAvailability.READY
        FeatureStatus.DOWNLOADABLE -> ModelAvailability.DOWNLOADABLE
        FeatureStatus.DOWNLOADING -> ModelAvailability.DOWNLOADING
        FeatureStatus.UNAVAILABLE -> ModelAvailability.UNAVAILABLE
        else -> ModelAvailability.ERROR
    }
    override suspend fun generate(prompt:String):String? {
        val request=GenerateContentRequest.Builder(TextPart(prompt)).apply {maxOutputTokens=64;temperature=0.0f}.build()
        return model.generateContent(request).candidates.firstOrNull()?.text
    }
    override fun download()=model.download().map {event ->when(event) {
        is DownloadStatus.DownloadCompleted -> ModelAvailability.READY
        is DownloadStatus.DownloadFailed -> ModelAvailability.ERROR
        else -> ModelAvailability.DOWNLOADING
    }}
}
