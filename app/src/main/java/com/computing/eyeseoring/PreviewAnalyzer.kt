package com.computing.eyeseoring

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class PreviewAnalyzer(private val previewListener: PreviewListener) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        previewListener.onImageInput(image)
    }
}