package dev.mxr.verityinspired;

import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

public final class VerityAttributes {
    private VerityAttributes() {}

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(VerityEntities.VERITY.get(), VerityEntity.createAttributes().build());
    }
}