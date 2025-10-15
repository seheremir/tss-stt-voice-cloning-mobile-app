package com.example.voicemate.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException

class AudioRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    var outputFilePath: String? = null
        private set

    private var isRecording = false

    /**
     * Ses kaydını başlatır
     * @param filename Kaydedilecek dosyanın adı (uzantısız)
     * @return Başarılı olursa true, aksi halde false
     */
    fun startRecording(filename: String): Boolean {
        if (isRecording) {
            return false
        }

        try {
            // Eski kayıt dosyasını sil
            outputFilePath?.let { path ->
                File(path).let { file ->
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }

            // Yeni dosya yolu oluştur
            val outputFile = File(context.cacheDir, "$filename.m4a")
            outputFilePath = outputFile.absolutePath

            // MediaRecorder'ı başlat
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFilePath)

                prepare()
                start()
            }

            isRecording = true
            return true

        } catch (e: IOException) {
            e.printStackTrace()
            releaseRecorder()
            return false
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            releaseRecorder()
            return false
        } catch (e: Exception) {
            e.printStackTrace()
            releaseRecorder()
            return false
        }
    }

    /**
     * Ses kaydını durdurur
     * @return Kaydedilen dosyanın yolu, hata durumunda null
     */
    fun stopRecording(): String? {
        if (!isRecording) {
            return null
        }

        return try {
            mediaRecorder?.apply {
                stop()
                reset()
            }
            isRecording = false
            val path = outputFilePath
            releaseRecorder()
            path
        } catch (e: RuntimeException) {
            e.printStackTrace()
            releaseRecorder()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            releaseRecorder()
            null
        }
    }

    /**
     * MediaRecorder kaynaklarını serbest bırakır
     */
    private fun releaseRecorder() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            isRecording = false
        }
    }

    /**
     * Kayıt durumunu kontrol eder
     */
    fun isRecording(): Boolean = isRecording

    /**
     * Kaynakları temizler
     */
    fun release() {
        stopRecording()
        releaseRecorder()

        // Önbellek dosyasını temizle
        outputFilePath?.let { path ->
            try {
                File(path).delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        outputFilePath = null
    }
}