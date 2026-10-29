package dev.mxr.verityinspired;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@ForgeVoicechatPlugin
public final class VerityVoicePlugin implements VoicechatPlugin {
    private static volatile VoicechatServerApi serverApi;

    @Override
    public String getPluginId() {
        return "verityinspired";
    }

    @Override
    public void initialize(VoicechatApi api) {
        if (api instanceof VoicechatServerApi server) {
            serverApi = server;
            VerityInspired.LOGGER.info("Simple Voice Chat API connected.");
        }
    }

    public static boolean isAvailable() {
        return serverApi != null;
    }

    public static void playWav(ServerLevel level, double x, double y, double z, byte[] wav) throws Exception {
        VoicechatServerApi api = serverApi;
        if (api == null) throw new IllegalStateException("Simple Voice Chat is not ready. Check that it is installed and enabled.");
        short[] pcm = VerityAI.wavToPcm48kMono(wav);
        if (pcm.length == 0) throw new IllegalArgumentException("TTS returned empty audio.");

        LocationalAudioChannel channel = api.createLocationalAudioChannel(
            UUID.randomUUID(),
            api.fromServerLevel(level),
            api.createPosition(x, y, z)
        );
        if (channel == null) throw new IllegalStateException("Could not create a voice chat audio channel.");
        channel.setDistance(28.0F);

        OpusEncoder encoder = api.createEncoder();
        AtomicInteger cursor = new AtomicInteger();
        AudioPlayer player = api.createAudioPlayer(channel, encoder, () -> {
            int start = cursor.getAndAdd(960);
            if (start >= pcm.length) return null;
            short[] frame = new short[960];
            int count = Math.min(frame.length, pcm.length - start);
            System.arraycopy(pcm, start, frame, 0, count);
            return frame;
        });
        player.setOnStopped(() -> {
            try { encoder.close(); } catch (Exception ignored) {}
        });
        player.startPlaying();
    }
}