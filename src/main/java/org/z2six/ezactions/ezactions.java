// MainFile: src/main/java/org/z2six/ezactions/ezactions.java
package org.z2six.ezactions;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.z2six.ezactions.handler.KeyboardHandler;
import org.z2six.ezactions.util.EZActionsKeybinds;

/**
 * Main mod entry. We register MOD-bus and GAME-bus listeners programmatically
 * to avoid deprecated @EventBusSubscriber(bus=...) usage.
 */
@Mod(Constants.MOD_ID)
public final class ezactions {

    public ezactions(IEventBus modBus) {
        Constants.LOG.info("[{}] Initializing … (client? {})", Constants.MOD_NAME, FMLEnvironment.dist == Dist.CLIENT);

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
