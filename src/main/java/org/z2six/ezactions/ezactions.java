// MainFile: src/main/java/org/z2six/ezactions/ezactions.java
package org.z2six.ezactions;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

// NEW imports for config registration
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

import org.z2six.ezactions.handler.KeyboardHandler;
import org.z2six.ezactions.util.EZActionsKeybinds;

/**
 * Main mod entry. We register MOD-bus and GAME-bus listeners programmatically
 * to avoid deprecated @EventBusSubscriber(bus=...) usage.
 */
@Mod(Constants.MOD_ID)
public final class ezactions {

    // NOTE: Added ModContainer so we can register the TOML config spec.
    public ezactions(IEventBus modBus, ModContainer modContainer) {
        Constants.LOG.info("[{}] Initializing … (client? {})", Constants.MOD_NAME, FMLEnvironment.dist == Dist.CLIENT);

        // --- Register CLIENT config (generates config/ezactions/anim-client.toml) ---
        try {
            modContainer.registerConfig(
                    ModConfig.Type.CLIENT,
                    org.z2six.ezactions.config.RadialAnimConfig.SPEC,
                    "ezactions/anim-client.toml"
            );
            Constants.LOG.debug("[{}] Registered CLIENT config spec at config/ezactions/anim-client.toml", Constants.MOD_NAME);
        } catch (Throwable t) {
            // Fail safe: config not critical for boot; log and continue
            Constants.LOG.warn("[{}] Failed to register CLIENT config: {}", Constants.MOD_NAME, t.toString());
        }

        try {
            // MOD bus: key mapping registration
            modBus.addListener(EZActionsKeybinds::onRegisterKeyMappings);
            Constants.LOG.debug("[{}] Registered MOD-bus listeners.", Constants.MOD_NAME);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Failed to register MOD-bus listeners: {}", Constants.MOD_NAME, t.toString());
        }

        try {
            // GAME bus (global): client tick for our tick-tap helper
            if (FMLEnvironment.dist == Dist.CLIENT) {
                NeoForge.EVENT_BUS.addListener(KeyboardHandler::onClientTickPre);
                Constants.LOG.debug("[{}] Registered GAME-bus listeners.", Constants.MOD_NAME);
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Failed to register GAME-bus listeners: {}", Constants.MOD_NAME, t.toString());
        }
    }
}
