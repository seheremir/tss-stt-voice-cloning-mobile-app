package com.example.voicemate

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voicemate.ui.MainScreen
import com.example.voicemate.ui.theme.VoiceMateTheme
import com.example.voicemate.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Mikrofon izni verildi", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                this,
                "Mikrofon izni gerekli. Lütfen ayarlardan izin verin.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mikrofon iznini kontrol et ve iste
        checkAndRequestAudioPermission()

        setContent {
            VoiceMateTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: MainViewModel = viewModel()
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }

    private fun checkAndRequestAudioPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.RECORD_AUDIO
        } else {
            Manifest.permission.RECORD_AUDIO
        }

        when {
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                // İzin zaten verilmiş
            }
            shouldShowRequestPermissionRationale(permission) -> {
                // Kullanıcıya neden izin gerektiğini açıkla
                Toast.makeText(
                    this,
                    "Ses kaydı için mikrofon izni gereklidir",
                    Toast.LENGTH_LONG
                ).show()
                requestPermissionLauncher.launch(permission)
            }
            else -> {
                // İzni doğrudan iste
                requestPermissionLauncher.launch(permission)
            }
        }
    }
}