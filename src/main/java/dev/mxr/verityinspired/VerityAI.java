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

    public static CompletableFuture<String> ask(String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String key = apiKey("ai.api-key");
                if (key.isBlank()) throw new IOException("AI API key missing. Set it with /api ai api-key <key>.");
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
                if (key.isBlank()) throw new IOException("TTS API key missing. Set it with /api tts api-key <key>.");
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