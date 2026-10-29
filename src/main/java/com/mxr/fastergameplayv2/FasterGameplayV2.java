package com.mxr.fastergameplayv2;

import net.minecraft.client.Minecraft;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * MxrMC's small settings helper.
 *
 * No continuous FPS polling or automatic render-distance changes. Real
 * rendering and memory optimizations must be implemented and profiled as
 * separate, targeted patches.
 */
@Mod(FasterGameplayV2.MOD_ID)
public final class FasterGameplayV2 {
    public static final String MOD_ID = "fastergameplayv2";
    private boolean applied;

    public FasterGameplayV2() {
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null) return;

        if (minecraft.level == null) {
            applied = false;
            return;
        }
        if (applied) return;

        // Conservative, fixed preset: never automatically raises render distance.
        setOption(minecraft.options, "renderDistance", 6);
        setOption(minecraft.options, "simulationDistance", 4);
        setOption(minecraft.options, "entityDistanceScaling", 0.50);
        setOption(minecraft.options, "mipmapLevels", 0);
        setOption(minecraft.options, "biomeBlendRadius", 0);
        setOption(minecraft.options, "maxFps", 120);
        setEnumOption(minecraft.options, "graphicsMode", "FAST");
        setEnumOption(minecraft.options, "cloudStatus", "OFF");
        setEnumOption(minecraft.options, "particles", "MINIMAL");
        setOption(minecraft.options, "ambientOcclusion", false);
        setOption(minecraft.options, "entityShadows", false);
        setOption(minecraft.options, "enableVsync", false);
        applied = true;
    }

    private static void setOption(Object owner, String name, Object value) {
        try {
            Object option = findField(owner, name);
            if (option == null) return;
            Method setter = findSetter(option.getClass());
            if (setter == null) return;
            Class<?> parameter = setter.getParameterTypes()[0];
            Object converted = value;
            if (value instanceof Number n) {
                if (parameter == int.class || parameter == Integer.class) converted = n.intValue();
                else if (parameter == double.class || parameter == Double.class) converted = n.doubleValue();
                else if (parameter == float.class || parameter == Float.class) converted = n.floatValue();
                else if (parameter == long.class || parameter == Long.class) converted = n.longValue();
                else return;
            } else if (value instanceof Boolean
                    && parameter != boolean.class && parameter != Boolean.class) {
                return;
            } else if (!(value instanceof Boolean)) {
                return;
            }
            setter.invoke(option, converted);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Unsupported options are skipped safely.
        }
    }

    private static void setEnumOption(Object owner, String name, String target) {
        try {
            Object option = findField(owner, name);
            if (option == null) return;
            Method setter = findSetter(option.getClass());
            if (setter == null || !setter.getParameterTypes()[0].isEnum()) return;
            for (Object constant : setter.getParameterTypes()[0].getEnumConstants()) {
                if (((Enum<?>) constant).name().equalsIgnoreCase(target)) {
                    setter.invoke(option, constant);
                    return;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Unsupported options are skipped safely.
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

    private static Method findSetter(Class<?> type) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals("set") && method.getParameterCount() == 1) return method;
        }
        return null;
    }
}
