package dev.mxr.verityinspired;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Set;

@EventBusSubscriber(modid = VerityInspired.MOD_ID)
public final class ApiKeyCommands {
    private static final String CONFIG_FILE = "verityinspired-secrets.properties";

    private ApiKeyCommands() {}

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("api")
                .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                .then(Commands.literal("tts")
                    .then(Commands.literal("api-key")
                        .then(Commands.argument("key", StringArgumentType.greedyString())
                            .executes(context -> saveKey(
                                context.getSource(),
                                "tts.api-key",
                                StringArgumentType.getString(context, "key"),
                                "TTS"
                            ))
                        )
                    )
                )
                .then(Commands.literal("ai")
                    .then(Commands.literal("api-key")
                        .then(Commands.argument("key", StringArgumentType.greedyString())
                            .executes(context -> saveKey(
                                context.getSource(),
                                "ai.api-key",
                                StringArgumentType.getString(context, "key"),
                                "AI"
                            ))
                        )
                    )
                )
        );
    }

    private static int saveKey(
        net.minecraft.commands.CommandSourceStack source,
        String propertyName,
        String key,
        String displayName
    ) {
        String trimmedKey = key.trim();
        if (trimmedKey.isEmpty() || trimmedKey.equalsIgnoreCase("yourkey")) {
            source.sendFailure(Component.literal("Provide your actual " + displayName + " API key."));
            return 0;
        }

        Path configDir = FMLPaths.CONFIGDIR.get();
        Path file = configDir.resolve(CONFIG_FILE);
        Properties properties = new Properties();

        try {
            Files.createDirectories(configDir);
            if (Files.exists(file)) {
                try (InputStream input = Files.newInputStream(file)) {
                    properties.load(input);
                }
            }

            properties.setProperty(propertyName, trimmedKey);
            try (OutputStream output = Files.newOutputStream(file)) {
                properties.store(output, "Verity-Inspired Horror API keys. Keep this file private.");
            }

            // Restrict permissions where the operating system supports POSIX permissions.
            try {
                Set<PosixFilePermission> permissions = EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE
                );
                Files.setPosixFilePermissions(file, permissions);
            } catch (UnsupportedOperationException ignored) {
                // Windows and some filesystems do not support POSIX permissions.
            }

            source.sendSuccess(
                () -> Component.literal(displayName + " API key saved locally. The key was not displayed."),
                false
            );
            VerityInspired.LOGGER.info("{} API key updated in local config (value hidden).", displayName);
            return 1;
        } catch (IOException exception) {
            VerityInspired.LOGGER.error("Could not save {} API key to local config.", displayName, exception);
            source.sendFailure(Component.literal("Could not save the API key. Check server file permissions."));
            return 0;
        }
    }
}
