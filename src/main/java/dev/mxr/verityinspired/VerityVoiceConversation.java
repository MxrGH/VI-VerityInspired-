package dev.mxr.verityinspired;

import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class VerityVoiceConversation {
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();
    private static final Set<UUID> ENABLED = ConcurrentHashMap.newKeySet();
    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "Verity-voice-timeout");
        thread.setDaemon(true);
        return thread;
    });

    private VerityVoiceConversation() {}

    static void initialize() {
        TIMER.scheduleAtFixedRate(VerityVoiceConversation::flushFinishedSpeech, 300, 300, TimeUnit.MILLISECONDS);
    }

    public static void setEnabled(UUID id, boolean enabled) {
        if (enabled) {
            ENABLED.add(id);
        } else {
            ENABLED.remove(id);
            Session session = SESSIONS.remove(id);
            if (session != null) session.close();
        }
    }

    public static boolean isEnabled(UUID id) { return ENABLED.contains(id); }

    public static void onMicrophonePacket(MicrophonePacketEvent event) {
        ServerPlayer player = VerityVoicePlugin.actualPlayer(event);
        if (player == null || !ENABLED.contains(player.getUUID()) || player.isRemoved()) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        if (level.getEntitiesOfClass(VerityEntity.class, player.getBoundingBox().inflate(18.0D),
            entity -> entity.isAlive()).isEmpty()) return;

        byte[] opus = event.getPacket().getOpusEncodedData();
        if (opus == null || opus.length == 0) return;
        Session session = SESSIONS.computeIfAbsent(player.getUUID(), id -> new Session());
        if (session.decoder == null) return;
        try {
            short[] pcm = session.decoder.decode(opus);
            synchronized (session) {
                for (short sample : pcm) {
                    session.audio.write(sample & 0xff);
                    session.audio.write((sample >>> 8) & 0xff);
                }
                session.lastPacketNanos = System.nanoTime();
                session.player = player;
                session.level = level;
            }
        } catch (Exception e) {
            VerityInspired.LOGGER.debug("Could not decode incoming voice packet", e);
        }
    }

    private static void flushFinishedSpeech() {
        long now = System.nanoTime();
        for (Map.Entry<UUID, Session> entry : SESSIONS.entrySet()) {
            Session session = entry.getValue();
            byte[] wav;
            ServerPlayer player;
            synchronized (session) {
                if (session.audio.size() < 24000 || session.lastPacketNanos == 0
                    || now - session.lastPacketNanos < TimeUnit.MILLISECONDS.toNanos(1100)
                    || session.processing) continue;
                wav = makeWav(session.audio.toByteArray());
                session.audio.reset();
                session.processing = true;
                player = session.player;
            }
            if (player == null || !player.isAlive() || !ENABLED.contains(entry.getKey())) {
                synchronized (session) { session.processing = false; }
                continue;
            }

            VerityAI.transcribe(wav)
                .thenCompose(transcript -> {
                    if (transcript == null || transcript.isBlank()) return CompletableFuture.completedFuture(null);
                    return VerityAI.ask("The player just said: " + transcript
                        + "\nReply naturally as Verity in a short, eerie, non-graphic way.");
                })
                .thenCompose(reply -> {
                    if (reply == null || reply.isBlank()) return CompletableFuture.completedFuture(null);
                    return VerityAI.speak(reply).thenApply(audio -> new Reply(reply, audio));
                })
                .whenComplete((reply, error) -> {
                    if (session.level == null || session.level.getServer() == null) {
                        synchronized (session) { session.processing = false; }
                        return;
                    }
                    session.level.getServer().execute(() -> {
                        try {
                            if (error != null) {
                                player.sendSystemMessage(Component.literal("[Verity voice] " + safeMessage(error)));
                                return;
                            }
                            if (reply == null || !player.isAlive() || !ENABLED.contains(player.getUUID())) return;
                            var nearby = player.level().getEntitiesOfClass(VerityEntity.class,
                                player.getBoundingBox().inflate(24.0D), entity -> entity.isAlive());
                            VerityEntity nearest = nearby.stream()
                                .min(java.util.Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
                            if (nearest == null || !(nearest.level() instanceof ServerLevel serverLevel)) return;
                            try {
                                VerityVoicePlugin.playWav(serverLevel, nearest.getX(),
                                    nearest.getY() + 1.0D, nearest.getZ(), reply.audio());
                                player.sendSystemMessage(Component.literal("[Verity] " + reply.text()));
                            } catch (Exception e) {
                                player.sendSystemMessage(Component.literal("[Verity voice] Playback failed: " + safeMessage(e)));
                            }
                        } finally {
                            synchronized (session) { session.processing = false; }
                        }
                    });
                });
        }
    }

    private static String safeMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        String message = current.getMessage();
        if (message == null || message.isBlank()) return current.getClass().getSimpleName();
        return message.length() > 140 ? message.substring(0, 140) : message;
    }

    private static byte[] makeWav(byte[] pcm) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(pcm.length + 44);
        out.write(new byte[]{'R','I','F','F'});
        writeInt(out, 36 + pcm.length);
        out.write(new byte[]{'W','A','V','E','f','m','t',' '});
        writeInt(out, 16);
        writeShort(out, 1);
        writeShort(out, 1);
        writeInt(out, 48000);
        writeInt(out, 96000);
        writeShort(out, 2);
        writeShort(out, 16);
        out.write(new byte[]{'d','a','t','a'});
        writeInt(out, pcm.length);
        out.write(pcm);
        return out.toByteArray();
    }

    private static void writeInt(ByteArrayOutputStream out, int value) {
        out.write(value & 255); out.write((value >>> 8) & 255);
        out.write((value >>> 16) & 255); out.write((value >>> 24) & 255);
    }

    private static void writeShort(ByteArrayOutputStream out, int value) {
        out.write(value & 255); out.write((value >>> 8) & 255);
    }

    private static final class Session {
        final OpusDecoder decoder = VerityVoicePlugin.createDecoder();
        final ByteArrayOutputStream audio = new ByteArrayOutputStream();
        volatile long lastPacketNanos;
        volatile boolean processing;
        volatile ServerPlayer player;
        volatile ServerLevel level;
        void close() {
            if (decoder != null && !decoder.isClosed()) decoder.close();
            synchronized (this) { audio.reset(); }
        }
    }

    private record Reply(String text, byte[] audio) {}
}