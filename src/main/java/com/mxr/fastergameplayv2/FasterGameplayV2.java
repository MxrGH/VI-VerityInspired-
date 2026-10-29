package com.mxr.fastergameplayv2;

import net.minecraft.client.Minecraft;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Conservative client-side options tuner.
 * It intentionally never reads or writes debug/F3 state.
 */
@Mod(FasterGameplayV2.MOD_ID)
public final class FasterGameplayV2 {
    public static final String MOD_ID = "fastergameplayv2";
    private boolean applied;

    public FasterGameplayV2() {
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        if (applied) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null || minecraft.level == null) return;
        applied = true;
        Object options = minecraft.options;

        setNumericOption(options, "renderDistance", 8);
        setNumericOption(options, "simulationDistance", 5);
        setNumericOption(options, "entityDistanceScaling", 0.75);
        setNumericOption(options, "mipmapLevels", 2);
        setNumericOption(options, "biomeBlendRadius", 0);
        setEnumOption(options, "graphicsMode", "FAST");
        setEnumOption(options, "cloudStatus", "OFF");
        setEnumOption(options, "particles", "MINIMAL");
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
            // Safely skip options unavailable in this version.
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
