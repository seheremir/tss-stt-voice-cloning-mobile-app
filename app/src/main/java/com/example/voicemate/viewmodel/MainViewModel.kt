package com.example.voicemate.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.voicemate.audio.AudioRecorder
import com.example.voicemate.network.RetrofitInstance
import com.example.voicemate.model.TranscribeResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import okhttp3.RequestBody.Companion.toRequestBody


class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val audioRecorder = AudioRecorder(application)

    // State
    private val _recording = MutableStateFlow(false)
    val recording: StateFlow<Boolean> = _recording

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _recordingType = MutableStateFlow<RecordingType?>(null)

    private var exoPlayer: ExoPlayer? = null

    enum class RecordingType { TRANSCRIBE, CLONE }

    init {
        exoPlayer = ExoPlayer.Builder(application).build()
    }

    // --- Recording functions ---
    fun startRecording(type: RecordingType) {
        if (_recording.value) return
        val filename = if (type == RecordingType.TRANSCRIBE) "transcribe_audio" else "clone_audio"
        if (audioRecorder.startRecording(filename)) {
            _recording.value = true
            _recordingType.value = type
            _errorMessage.value = null
        } else _errorMessage.value = "Ses kaydı başlatılamadı"
    }

    fun stopRecordingForTranscribe() {
        if (!_recording.value || _recordingType.value != RecordingType.TRANSCRIBE) return
        val filePath = audioRecorder.stopRecording()
        _recording.value = false
        _recordingType.value = null

        if (filePath == null) { _errorMessage.value = "Ses kaydı durdurulamadı"; return }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val file = File(filePath)
                val requestFile = file.asRequestBody("audio/m4a".toMediaTypeOrNull())
                val audioPart = MultipartBody.Part.createFormData("audio", file.name, requestFile)
                val response = withContext(Dispatchers.IO) { RetrofitInstance.api.transcribe(audioPart) }
                if (response.isSuccessful) {
                    val transcribeResponse: TranscribeResponse? = response.body()
                    _recognizedText.value = transcribeResponse?.text ?: ""
                } else _errorMessage.value = "Transkripsiyon hatası: ${response.code()}"
            } catch (e: Exception) {
                _errorMessage.value = "Hata: ${e.localizedMessage}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // --- TTS ---
    fun requestTtsAndPlay(text: String) {
        if (text.isBlank()) { _errorMessage.value = "Lütfen metin girin"; return }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = withContext(Dispatchers.IO) { RetrofitInstance.api.textToSpeech(text) }
                if (response.isSuccessful) {
                    val audioBytes = response.body()?.bytes()
                    if (audioBytes != null) playAudio(audioBytes)
                    else _errorMessage.value = "Ses verisi alınamadı"
                } else _errorMessage.value = "TTS hatası: ${response.code()}"
            } catch (e: Exception) {
                _errorMessage.value = "Hata: ${e.localizedMessage}"
                e.printStackTrace()
            } finally { _isLoading.value = false }
        }
    }

    // --- Voice Cloning ---
    fun stopRecordingAndClone(voiceName: String) {
        if (!_recording.value || _recordingType.value != RecordingType.CLONE) return
        val filePath = audioRecorder.stopRecording()
        _recording.value = false
        _recordingType.value = null

        if (filePath == null) {
            _errorMessage.value = "Recording could not stop"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val file = File(filePath)
                val requestFile = file.asRequestBody("audio/m4a".toMediaTypeOrNull())
                val audioPart = MultipartBody.Part.createFormData("audio", file.name, requestFile)

                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.cloneVoice(audioPart, voiceName)
                }

                if (response.isSuccessful) {
                    val audioBytes = response.body()?.bytes() // artık ResponseBody'den bytes alıyoruz
                    if (audioBytes != null) playAudio(audioBytes)
                    else _errorMessage.value = "No audio data returned"
                } else {
                    _errorMessage.value = "Clone error: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.localizedMessage}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }




    // --- Playback ---
    private suspend fun playAudio(audioBytes: ByteArray) {
        withContext(Dispatchers.IO) {
            try {
                val tempFile = File(getApplication<Application>().cacheDir, "tts_output.mp3")
                FileOutputStream(tempFile).use { it.write(audioBytes) }
                withContext(Dispatchers.Main) {
                    exoPlayer?.apply {
                        setMediaItem(MediaItem.fromUri(tempFile.absolutePath))
                        prepare()
                        playWhenReady = true
                        addListener(object : Player.Listener {
                            override fun onPlaybackStateChanged(playbackState: Int) {
                                if (playbackState == Player.STATE_ENDED) tempFile.delete()
                            }
                        })
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { _errorMessage.value = "Ses çalma hatası: ${e.localizedMessage}" }
                e.printStackTrace()
            }
        }
    }

    fun updateRecognizedText(text: String) { _recognizedText.value = text }
    fun clearError() { _errorMessage.value = null }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.release()
        exoPlayer?.release()
        exoPlayer = null
    }
}
