# VoiceMate

VoiceMate is an Android app for **voice recording, speech-to-text transcription**, and **voice cloning**. It combines offline speech recognition with cloud-based text-to-speech for a seamless experience.

## Features

- **Record Voice** – Capture audio directly from the app.  
- **Offline Speech-to-Text** – Powered by **Vosk**, works without internet.  
- **Voice Cloning / TTS** – Generate speech in the recorded voice using **ElevenLabs API**.  
- **Playback** – Listen to generated audio with **ExoPlayer**.  
- **Modern UI** – Built with **Jetpack Compose** for smooth interaction.  

## Technologies

- **Kotlin & Android** – Core development  
- **Jetpack Compose** – UI toolkit  
- **Retrofit & OkHttp** – Networking  
- **Vosk** – Offline STT engine  
- **ElevenLabs API** – Text-to-speech & voice cloning  
- **ExoPlayer (Media3)** – Audio playback  
- **Coroutines** – Async tasks  
- **Python & FastAPI** – Backend for audio processing  

## Architecture

1. **Frontend** – Android app handles recording, playback, and API requests.  
2. **Backend** – FastAPI server converts audio to text and generates cloned speech.  
3. **Hybrid** – Offline transcription + online TTS ensures speed and quality.  

## Demo Video

[![Watch the demo](https://img.youtube.com/vi/H7To5necp9g/hqdefault.jpg)](https://youtube.com/shorts/H7To5necp9g?feature=share)

> Click the image above to play the video.

## Getting Started

1. Clone the repository.  
2. Set up **ElevenLabs API key** in the backend.  
3. Start backend: `uvicorn main:app --reload`.  
4. Open app in Android Studio and run on an emulator or device.  
