package dev.mxr.verityinspired;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(VerityInspired.MOD_ID)
public final class VerityInspired {
    public static final String MOD_ID = "verityinspired";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public VerityInspired(IEventBus modBus, ModContainer container) {
        VerityEntities.ENTITY_TYPES.register(modBus);
        modBus.addListener(VerityAttributes::registerAttributes);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            modBus.addListener(dev.mxr.verityinspired.client.VerityClient::registerRenderers);
        }
        LOGGER.info("Verity-Inspired Horror loaded.");
    }
}