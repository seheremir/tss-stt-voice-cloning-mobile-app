package com.example.voicemate.model

import com.google.gson.annotations.SerializedName

/**
 * Transcribe API'den dönen yanıt modeli
 */
data class TranscribeResponse(
    @SerializedName("text")
    val text: String,

    @SerializedName("language")
    val language: String? = null,

    @SerializedName("confidence")
    val confidence: Double? = null
)