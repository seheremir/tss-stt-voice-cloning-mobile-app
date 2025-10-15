package com.example.voicemate.network

import com.example.voicemate.model.TranscribeResponse
import com.example.voicemate.model.CloneResponse
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface VoiceMateApi {

    // --- STT (Speech-to-Text) ---
    @Multipart
    @POST("transcribe")
    suspend fun transcribe(
        @Part audio: MultipartBody.Part
    ): Response<TranscribeResponse>

    // --- TTS (Text-to-Speech) ---
    @FormUrlEncoded
    @POST("tts")
    suspend fun textToSpeech(
        @Field("text") text: String,
        @Field("voice") voice: String = "uYXf8XasLslADfZ2MB4u" // default voiceId
    ): Response<ResponseBody>

    // --- Voice Cloning ---
    @Multipart
    @POST("clone")
    suspend fun cloneVoice(
        @Part audio: MultipartBody.Part,
        @Part("voice_name") voiceName: String
    ): Response<ResponseBody> // artık JSON değil, doğrudan audio bytes

}
