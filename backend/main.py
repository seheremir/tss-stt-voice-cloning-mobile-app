from fastapi import FastAPI, UploadFile, File, Form
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, JSONResponse, Response
import os
import requests
from uuid import uuid4
from dotenv import load_dotenv
import traceback
import wave
import json
import vosk
from pydub import AudioSegment  
from uuid import uuid4
load_dotenv()


ELEVENLABS_API_KEY = os.getenv("ELEVENLABS_API_KEY")
ELEVENLABS_VOICE_ID = "uYXf8XasLslADfZ2MB4u"
  # default, klonlanan ses için güncellenecek


app = FastAPI()

# CORS (Android ve Web için)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
def home():
    return {"message": "ElevenLabs TTS/STT/Clone API çalışıyor 🚀"}

# Text to Speech
@app.post("/tts")
async def text_to_speech(text: str = Form(...), voice: str = Form(None)):
    voice_id = voice if voice else ELEVENLABS_VOICE_ID
    tts_url = f"https://api.elevenlabs.io/v1/text-to-speech/{voice_id}"

    headers = {
        "xi-api-key": ELEVENLABS_API_KEY,
        "Content-Type": "application/json"
    }
    json_data = {"text": text, "voice_settings": {"stability": 0.75, "similarity_boost": 0.75}}

    response = requests.post(tts_url, headers=headers, json=json_data)

    if response.status_code == 200:
        file_path = f"temp_{uuid4().hex}.mp3"
        with open(file_path, "wb") as f:
            f.write(response.content)
        return FileResponse(file_path, media_type="audio/mpeg", filename="tts_output.mp3")
    else:
        return JSONResponse(status_code=response.status_code, content={"error": response.text})

@app.post("/clone")
async def clone_voice(audio: UploadFile = File(...), voice_name: str = Form(...)):
    try:
        # 1️⃣ Save uploaded audio
        original_file_location = f"uploads/{uuid4().hex}_{audio.filename}"
        os.makedirs("uploads", exist_ok=True)
        with open(original_file_location, "wb") as f:
            f.write(await audio.read())

        # 2️⃣ Convert audio to WAV for Vosk
        wav_file_location = original_file_location.rsplit(".", 1)[0] + ".wav"
        audio_segment = AudioSegment.from_file(original_file_location)  # format otomatik algılanır
        audio_segment = audio_segment.set_channels(1).set_frame_rate(16000)  # Vosk için mono 16kHz
        audio_segment.export(wav_file_location, format="wav")

        # 3️⃣ Convert audio to text (Vosk)
        model_path = "vosk-model-small-en-us-0.15"
        if not os.path.exists(model_path):
            return {"error": f"Vosk model not found: {model_path}"}

        model = vosk.Model(model_path)
        wf = wave.open(wav_file_location, "rb")
        rec = vosk.KaldiRecognizer(model, wf.getframerate())

        recognized_text = ""
        while True:
            data = wf.readframes(4000)
            if len(data) == 0:
                break
            if rec.AcceptWaveform(data):
                res = json.loads(rec.Result())
                recognized_text += " " + res.get("text", "")
        res = json.loads(rec.FinalResult())
        recognized_text += " " + res.get("text", "")
        recognized_text = recognized_text.strip()

        # 4️⃣ Generate speech (ElevenLabs)
        tts_url = f"https://api.elevenlabs.io/v1/text-to-speech/{ELEVENLABS_VOICE_ID}"
        headers = {
            "xi-api-key": ELEVENLABS_API_KEY,
            "Content-Type": "application/json"
        }
        json_data = {"text": recognized_text, "voice_settings": {"stability": 0.75, "similarity_boost": 0.75}}
        tts_response = requests.post(tts_url, headers=headers, json=json_data)
        if tts_response.status_code != 200:
            return {"error": f"TTS error: {tts_response.text}"}

        # 5️⃣ Return audio bytes directly
        return Response(content=tts_response.content, media_type="audio/mpeg")

    except Exception as e:
        import traceback
        return {"error": str(e), "traceback": traceback.format_exc()}

# 📝 SPEECH → TEXT (Vosk Offline)
@app.post("/transcribe")
async def transcribe(audio: UploadFile = File(...)):
    try:
        os.makedirs("uploads", exist_ok=True)
        file_location = f"uploads/{uuid4().hex}_{audio.filename}"

        # Ses dosyasını kaydet
        with open(file_location, "wb") as f:
            f.write(await audio.read())

        # MP3 / MP4 ise WAV’a çevir
        ext = os.path.splitext(file_location)[1].lower()
        if ext in [".mp3", ".m4a", ".aac"]:
            wav_path = file_location.replace(ext, ".wav")
            audio_segment = AudioSegment.from_file(file_location, format=ext.replace(".", ""))
            audio_segment = audio_segment.set_channels(1).set_frame_rate(16000)  # mono ve 16kHz Vosk için
            audio_segment.export(wav_path, format="wav")
        else:
            wav_path = file_location

        # Vosk Model yükle
        model_path = "vosk-model-small-en-us-0.15"
        if not os.path.exists(model_path):
            return {"error": f"Vosk model dizini bulunamadı: {model_path}"}

        model = vosk.Model(model_path)
        wf = wave.open(wav_path, "rb")
        rec = vosk.KaldiRecognizer(model, wf.getframerate())

        result_text = ""
        while True:
            data = wf.readframes(4000)
            if len(data) == 0:
                break
            if rec.AcceptWaveform(data):
                res = json.loads(rec.Result())
                result_text += " " + res.get("text", "")
        res = json.loads(rec.FinalResult())
        result_text += " " + res.get("text", "")

        return {"text": result_text.strip(), "status": "success"}

    except Exception as e:
        return {"error": str(e), "traceback": traceback.format_exc()}