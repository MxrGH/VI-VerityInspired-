package com.mxr.fastergameplayv2;

import net.minecraft.client.Minecraft;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Lightweight client-side performance governor.
 *
 * This first optimization layer reduces rendering load and adapts chunk
 * distance when the measured frame rate remains low. It does not modify F3.
 * It is not a replacement for a renderer rewrite such as Sodium.
 */
@Mod(FasterGameplayV2.MOD_ID)
public final class FasterGameplayV2 {
    public static final String MOD_ID = "fastergameplayv2";

    private static final int SAMPLE_TICKS = 100;
    private static final int LOW_FPS_THRESHOLD = 24;
    private static final int HIGH_FPS_THRESHOLD = 55;
    private static final int MIN_RENDER_DISTANCE = 3;
    private static final int MAX_RENDER_DISTANCE = 8;

    private boolean presetApplied;
    private int tickCounter;
    private int sampledTicks;
    private int lastFrameCount;
    private int lowFpsSamples;
    private int highFpsSamples;
    private int currentRenderDistance = 6;

    public FasterGameplayV2() {
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null || minecraft.level == null) {
            // Reset the sampling window while in menus or between worlds.
            tickCounter = 0;
            sampledTicks = 0;
            lowFpsSamples = 0;
            highFpsSamples = 0;
            return;
        }

        if (!presetApplied) {
            applyLowEndPreset(minecraft.options);
            presetApplied = true;
            lastFrameCount = minecraft.getFrameTimeNs() > 0 ? minecraft.getFrameTimeNs() : 0;
            return;
        }

        // Sample periodically, not every tick, to keep this mod's overhead tiny.
        tickCounter++;
        sampledTicks++;
        if (tickCounter < SAMPLE_TICKS) return;
        tickCounter = 0;

        // Minecraft's FPS counter is available via the window on supported clients.
        int fps = minecraft.getFps();
        if (fps > 0 && fps < LOW_FPS_THRESHOLD) {
            lowFpsSamples++;
            highFpsSamples = 0;
        } else if (fps >= HIGH_FPS_THRESHOLD) {
            highFpsSamples++;
            lowFpsSamples = 0;
        } else {
            lowFpsSamples = Math.max(0, lowFpsSamples - 1);
            highFpsSamples = Math.max(0, highFpsSamples - 1);
        }

        // Require repeated bad/good samples to avoid flickering between values.
        if (lowFpsSamples >= 2 && currentRenderDistance > MIN_RENDER_DISTANCE) {
            currentRenderDistance--;
            setNumericOption(minecraft.options, "renderDistance", currentRenderDistance);
            lowFpsSamples = 0;
        } else if (highFpsSamples >= 4 && currentRenderDistance < MAX_RENDER_DISTANCE) {
            currentRenderDistance++;
            setNumericOption(minecraft.options, "renderDistance", currentRenderDistance);
            highFpsSamples = 0;
        }
    }

    private static void applyLowEndPreset(Object options) {
        setNumericOption(options, "renderDistance", 6);
        setNumericOption(options, "simulationDistance", 4);
        setNumericOption(options, "entityDistanceScaling", 0.50);
        setNumericOption(options, "mipmapLevels", 0);
        setNumericOption(options, "biomeBlendRadius", 0);
        setNumericOption(options, "maxFps", 120);

        setEnumOption(options, "graphicsMode", "FAST");
        setEnumOption(options, "cloudStatus", "OFF");
        setEnumOption(options, "particles", "MINIMAL");

        setBooleanOption(options, "ambientOcclusion", false);
        setBooleanOption(options, "entityShadows", false);
        setBooleanOption(options, "enableVsync", false);
    }

    private static void setNumericOption(Object owner, String fieldName, Number target) {
        try {
            Object option = findField(owner, fieldName);
            if (option == null) return;
            Method set = findOneArgMethod(option.getClass(), "set");
            if (set == null) return;
            Object value = convertNumber(target, set.getParameterTypes()[0]);
            if (value != null) set.invoke(option, value);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Skip options whose names or types differ in this game version.
        }
    }

    private static void setBooleanOption(Object owner, String fieldName, boolean target) {
        try {
            Object option = findField(owner, fieldName);
            if (option == null) return;
            Method set = findOneArgMethod(option.getClass(), "set");
            if (set == null || (set.getParameterTypes()[0] != boolean.class
                    && set.getParameterTypes()[0] != Boolean.class)) return;
            set.invoke(option, target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Safely skip unavailable options.
        }
    }

    private static void setEnumOption(Object owner, String fieldName, String targetName) {
        try {
            Object option = findField(owner, fieldName);
            if (option == null) return;
            Method set = findOneArgMethod(option.getClass(), "set");
            if (set == null) return;
            Class<?> type = set.getParameterTypes()[0];
            if (!type.isEnum()) return;
            for (Object constant : type.getEnumConstants()) {
                if (((Enum<?>) constant).name().equalsIgnoreCase(targetName)) {
                    set.invoke(option, constant);
                    return;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Safely skip unavailable options.
        }
    }

    private static Object findField(Object owner, String name) throws IllegalAccessException {
        for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getName().toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT))) {
                    field.setAccessible(true);
                    return field.get(owner);
                }
            }
        }
        return null;
    }

    private static Method findOneArgMethod(Class<?> type, String name) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 1) return method;
        }
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 1) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private static Object convertNumber(Number number, Class<?> type) {
        if (type == int.class || type == Integer.class) return number.intValue();
        if (type == double.class || type == Double.class) return number.doubleValue();
        if (type == float.class || type == Float.class) return number.floatValue();
        if (type == long.class || type == Long.class) return number.longValue();
        return null;
    }
}
