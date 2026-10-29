package dev.mxr.verityinspired;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = VerityInspired.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class VerityAttributes {
    private VerityAttributes() {}

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(VerityEntities.VERITY.get(), VerityEntity.createAttributes().build());
    }
}