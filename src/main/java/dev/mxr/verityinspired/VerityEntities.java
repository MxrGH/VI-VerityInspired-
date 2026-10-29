package dev.mxr.verityinspired;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class VerityEntities {
    public static final DeferredRegister.Entities ENTITY_TYPES =
        DeferredRegister.createEntities(VerityInspired.MOD_ID);

    public static final Supplier<EntityType<VerityEntity>> VERITY = ENTITY_TYPES.registerEntityType(
        "verity", VerityEntity::new, MobCategory.MONSTER,
        builder -> builder.sized(0.8F, 2.7F).clientTrackingRange(10).updateInterval(2)
    );

    private VerityEntities() {}
}