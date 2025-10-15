package com.example.voicemate.model

import com.google.gson.annotations.SerializedName

/**
 * Clone Voice API'den dönen yanıt modeli
 */
data class CloneResponse(
    @SerializedName("voiceId")
    val voiceId: String,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("voice_name")
    val voiceName: String? = null
)