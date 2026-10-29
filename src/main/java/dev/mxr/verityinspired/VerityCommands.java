package dev.mxr.verityinspired;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.concurrent.CompletionException;

@EventBusSubscriber(modid = VerityInspired.MOD_ID)
public final class VerityCommands {
    private VerityCommands() {}

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("verity")
            .then(Commands.literal("voice")
                .then(Commands.literal("on").executes(ctx -> {
                    var player = ctx.getSource().getPlayerOrException();
                    VerityVoiceConversation.setEnabled(player.getUUID(), true);
                    player.sendSystemMessage(Component.literal("Voice conversation enabled. While you are within 18 blocks of Verity, your voice audio is sent to the configured AI service for transcription. Use /verity voice off to disable it."));
                    return 1;
                }))
                .then(Commands.literal("off").executes(ctx -> {
                    var player = ctx.getSource().getPlayerOrException();
                    VerityVoiceConversation.setEnabled(player.getUUID(), false);
                    player.sendSystemMessage(Component.literal("Voice conversation disabled."));
                    return 1;
                }))
            )
            .then(Commands.literal("ai").requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                .then(Commands.argument("prompt", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        CommandSourceStack source = ctx.getSource();
                        String prompt = StringArgumentType.getString(ctx, "prompt");
                        source.sendSuccess(() -> Component.literal("Verity is thinking..."), false);
                        VerityAI.ask(prompt).whenComplete((reply, error) -> source.getServer().execute(() -> {
                            if (error != null) {
                                source.sendFailure(Component.literal("Verity AI failed: " + safeMessage(error)));
                            } else {
                                source.sendSuccess(() -> Component.literal("[Verity] " + reply), false);
                            }
                        }));
                        return 1;
                    })
                )
            )
            .then(Commands.literal("speak").requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                .then(Commands.argument("text", StringArgumentType.greedyString())
                    .executes(ctx -> speak(ctx.getSource(), StringArgumentType.getString(ctx, "text")))
                )
            )
            .then(Commands.literal("ask").requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                .then(Commands.argument("prompt", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        CommandSourceStack source = ctx.getSource();
                        String prompt = StringArgumentType.getString(ctx, "prompt");
                        source.sendSuccess(() -> Component.literal("Verity is thinking and preparing speech..."), false);
                        ServerLevel level = source.getLevel();
                        Vec3 pos = source.getPosition();
                        VerityAI.ask(prompt).thenCompose(reply ->
                            VerityAI.speak(reply).thenApply(wav -> new SpokenReply(reply, wav))
                        ).whenComplete((result, error) -> source.getServer().execute(() -> {
                            if (error != null) {
                                source.sendFailure(Component.literal("Verity AI/TTS failed: " + safeMessage(error)));
                                return;
                            }
                            source.sendSuccess(() -> Component.literal("[Verity] " + result.text()), false);
                            try {
                                VerityVoicePlugin.playWav(level, pos.x, pos.y, pos.z, result.wav());
                                source.sendSuccess(() -> Component.literal("Verity's voice is playing through Simple Voice Chat nearby."), false);
                            } catch (Exception exception) {
                                source.sendFailure(Component.literal("Speech generated, but voice playback failed: " + safeMessage(exception)));
                            }
                        }));
                        return 1;
                    })
                )
            )
        );
    }

    private static int speak(CommandSourceStack source, String text) {
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();
        source.sendSuccess(() -> Component.literal("Generating Verity's voice..."), false);
        VerityAI.speak(text).whenComplete((wav, error) -> source.getServer().execute(() -> {
            if (error != null) {
                source.sendFailure(Component.literal("Verity TTS failed: " + safeMessage(error)));
                return;
            }
            try {
                VerityVoicePlugin.playWav(level, pos.x, pos.y, pos.z, wav);
                source.sendSuccess(() -> Component.literal("Verity's voice is playing through Simple Voice Chat nearby."), false);
            } catch (Exception exception) {
                source.sendFailure(Component.literal("Speech generated, but voice playback failed: " + safeMessage(exception)));
            }
        }));
        return 1;
    }

    private static String safeMessage(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof CompletionException || current.getCause() != null) && current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) return current.getClass().getSimpleName();
        return message.length() > 180 ? message.substring(0, 180) : message;
    }

    private record SpokenReply(String text, byte[] wav) {}
}