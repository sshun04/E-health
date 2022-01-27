package com.computing.eyeseoring

import android.media.Image
import androidx.camera.core.ImageProxy

interface PreviewListener {
    fun onImageInput(image: ImageProxy)
}