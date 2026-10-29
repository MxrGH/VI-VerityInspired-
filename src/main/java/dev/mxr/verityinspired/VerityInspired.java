package dev.mxr.verityinspired;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/** Initial entry point; gameplay and voice integration remain to be implemented. */
@Mod(VerityInspired.MOD_ID)
public final class VerityInspired {
    public static final String MOD_ID = "verityinspired";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public VerityInspired(IEventBus modBus, ModContainer container) {
        LOGGER.info("Verity-Inspired Horror initialising.");
        // Next: register the entity, sounds, saved story state, and networking.
    }
}
