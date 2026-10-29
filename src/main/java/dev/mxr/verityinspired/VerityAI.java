package dev.mxr.verityinspired;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.fml.loading.FMLPaths;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class VerityAI {
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build();

    private VerityAI() {}

    private static String apiKey(String name) throws IOException {
        Path file = FMLPaths.CONFIGDIR.get().resolve("verityinspired-secrets.properties");
        if (!Files.exists(file)) return "";
        Properties properties = new Properties();
        try (var input = Files.newInputStream(file)) {
            properties.load(input);
        }
        return properties.getProperty(name, "").trim();
    }

    public static CompletableFuture<String> transcribe(byte[] wav) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String key = apiKey("ai.api-key");
                if (key.isBlank()) return localTranscribe(wav);
                String boundary = "----VerityVoiceBoundaryA7B9";
                String head = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"voice.wav\"\r\nContent-Type: audio/wav\r\n\r\n";
                String tail = "\r\n--" + boundary + "\r\nContent-Disposition: form-data; name=\"model\"\r\n\r\ngpt-4o-mini-transcribe\r\n--" + boundary + "--\r\n";
                ByteArrayOutputStream body = new ByteArrayOutputStream();
                body.write(head.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                body.write(wav);
                body.write(tail.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/audio/transcriptions"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
                HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300)
                    throw new IOException("Speech transcription returned HTTP " + response.statusCode() + ".");
                JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
                return result.has("text") ? result.get("text").getAsString().trim() : "";
            } catch (Exception exception) {
                throw new CompletionException(exception);
            }
        });
    }

    public static CompletableFuture<String> ask(String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String key = apiKey("ai.api-key");
                if (key.isBlank()) return localReply(prompt);
                JsonObject body = new JsonObject();
                body.addProperty("model", "gpt-4.1-mini");
                body.addProperty("instructions",
                    "You are Verity, a fictional eerie Minecraft horror entity. Speak in short, unsettling but non-graphic lines. Never claim to be a real person.");
                body.addProperty("input", prompt);
                HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/responses"))
                    .timeout(Duration.ofSeconds(45))
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
                HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IOException("AI provider returned HTTP " + response.statusCode() + ".");
                }
                JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray output = root.has("output") ? root.getAsJsonArray("output") : new JsonArray();
                StringBuilder text = new StringBuilder();
                for (JsonElement item : output) {
                    if (!item.isJsonObject()) continue;
                    JsonObject obj = item.getAsJsonObject();
                    if (!obj.has("content") || !obj.get("content").isJsonArray()) continue;
                    for (JsonElement part : obj.getAsJsonArray("content")) {
                        if (part.isJsonObject() && part.getAsJsonObject().has("text")) {
                            if (!text.isEmpty()) text.append(' ');
                            text.append(part.getAsJsonObject().get("text").getAsString());
                        }
                    }
                }
                if (text.isEmpty()) throw new IOException("AI provider returned no text.");
                return text.toString().trim();
            } catch (Exception exception) {
                throw new CompletionException(exception);
            }
        });
    }

    public static CompletableFuture<byte[]> speak(String text) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String key = apiKey("tts.api-key");
                if (key.isBlank()) return localSpeak(text);
                JsonObject body = new JsonObject();
                body.addProperty("model", "gpt-4o-mini-tts");
                body.addProperty("voice", "onyx");
                body.addProperty("input", text);
                body.addProperty("response_format", "wav");
                HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/audio/speech"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
                HttpResponse<byte[]> response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IOException("TTS provider returned HTTP " + response.statusCode() + ".");
                }
                return response.body();
            } catch (Exception exception) {
                throw new CompletionException(exception);
            }
        });
    }


    private static String localReply(String prompt) {
        String input = prompt == null ? "" : prompt.toLowerCase(java.util.Locale.ROOT);
        if (input.contains("hello") || input.contains("hi") || input.contains("hey")) return "You finally decided to speak to me.";
        if (input.contains("who are you") || input.contains("your name")) return "They call me Verity. Names are the easiest things to steal.";
        if (input.contains("where") || input.contains("here")) return "Closer than you think. Keep your eyes open.";
        if (input.contains("what are you doing")) return "Listening. You make more noise than you realize.";
        if (input.contains("go away") || input.contains("leave me")) return "I could. But then who would watch you?";
        if (input.contains("help")) return "Help yourself first. I want to see what you do.";
        if (input.contains("thank")) return "Do not thank me yet.";
        String[] replies = {"I heard you. I am still deciding what that means.","Interesting. Keep talking. I am learning your patterns.","You sound certain. That is usually when people make mistakes.","I am here. I have been here longer than you think.","Say that again. I want to remember your voice.","There is something behind you. No, do not turn around. I am only testing you.","You ask questions as if every answer will make you safer."};
        return replies[Math.floorMod(prompt == null ? 0 : prompt.hashCode(), replies.length)];
    }

    private static String localTranscribe(byte[] wav) throws Exception {
        if (!System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win"))
            throw new IOException("No AI key is set. Offline speech recognition requires Windows built-in speech recognition; configure ai.api-key on other systems.");
        Path input = Files.createTempFile("verity-input-", ".wav");
        try {
            Files.write(input, wav);
            String path = input.toAbsolutePath().toString().replace("'", "''");
            String script = "$ErrorActionPreference='Stop'; Add-Type -AssemblyName System.Speech; " +
                "$r=New-Object System.Speech.Recognition.SpeechRecognitionEngine; " +
                "$r.LoadGrammar((New-Object System.Speech.Recognition.DictationGrammar)); " +
                "$r.SetInputToWaveFile('" + path + "'); " +
                "$result=$r.Recognize([TimeSpan]::FromSeconds(12)); " +
                "if ($null -ne $result) { [Console]::Out.Write($result.Text) }; $r.Dispose()";
            Process process = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-Command", script).redirectErrorStream(true).start();
            byte[] output = process.getInputStream().readAllBytes();
            if (!process.waitFor(20, java.util.concurrent.TimeUnit.SECONDS)) { process.destroyForcibly(); throw new IOException("Offline speech recognition timed out."); }
            if (process.exitValue() != 0) throw new IOException("Windows offline speech recognition unavailable. Configure ai.api-key for online transcription.");
            return new String(output, java.nio.charset.StandardCharsets.UTF_8).trim();
        } finally { Files.deleteIfExists(input); }
    }

    private static byte[] localSpeak(String text) throws Exception {
        if (!System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win"))
            throw new IOException("No TTS key is set. Built-in Verity-style speech is available on Windows; configure tts.api-key on other systems.");
        Path output = Files.createTempFile("verity-voice-", ".wav");
        try {
            String path = output.toAbsolutePath().toString().replace("'", "''");
            String safeText = (text == null ? "" : text).replace("'", "''");
            String script = "$ErrorActionPreference='Stop'; Add-Type -AssemblyName System.Speech; " +
                "$s=New-Object System.Speech.Synthesis.SpeechSynthesizer; $s.Rate=-3; $s.Volume=90; " +
                "$s.SetOutputToWaveFile('" + path + "'); $s.Speak('" + safeText + "'); $s.Dispose()";
            Process process = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-Command", script).redirectErrorStream(true).start();
            byte[] log = process.getInputStream().readAllBytes();
            if (!process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)) { process.destroyForcibly(); throw new IOException("Local Verity speech synthesis timed out."); }
            if (process.exitValue() != 0 || !Files.exists(output) || Files.size(output) <= 44)
                throw new IOException("Windows speech synthesis failed: " + new String(log, java.nio.charset.StandardCharsets.UTF_8));
            return Files.readAllBytes(output);
        } finally { Files.deleteIfExists(output); }
    }

    public static short[] wavToPcm48kMono(byte[] wav) throws Exception {
        try (AudioInputStream original = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wav))) {
            AudioFormat target = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000.0F, 16, 1, 2, 48000.0F, false);
            try (AudioInputStream converted = AudioSystem.getAudioInputStream(target, original);
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                converted.transferTo(output);
                byte[] bytes = output.toByteArray();
                short[] samples = new short[bytes.length / 2];
                for (int i = 0; i < samples.length; i++) {
                    samples[i] = (short) ((bytes[i * 2] & 0xff) | (bytes[i * 2 + 1] << 8));
                }
                return samples;
            }
        }
    }
}