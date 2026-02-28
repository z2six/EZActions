// MainFile: src/main/java/org/z2six/ezactions/ezactions.java
package org.z2six.ezactions;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.z2six.ezactions.config.DesignClientConfig;
import org.z2six.ezactions.handler.KeyboardHandler;
import org.z2six.ezactions.util.CustomIconManager;
import org.z2six.ezactions.util.EZActionsKeybinds;

/**
 * Main mod entry. We register MOD-bus and GAME-bus listeners programmatically.
 */
@Mod(Constants.MOD_ID)
public final class ezactions {

    public ezactions(IEventBus modBus, ModContainer modContainer) {
        Constants.LOG.info("[{}] Initializing … (client? {})", Constants.MOD_NAME, FMLEnvironment.dist == Dist.CLIENT);

        // Register CLIENT configs
        try {
            modContainer.registerConfig(
                    ModConfig.Type.CLIENT,
                    org.z2six.ezactions.config.RadialAnimConfig.SPEC,
                    "ezactions/anim-client.toml"
            );
            modContainer.registerConfig(
                    ModConfig.Type.CLIENT,
                    org.z2six.ezactions.config.GeneralClientConfig.SPEC,
                    "ezactions/general-client.toml"
            );

            // NEW: design-client.toml (replaces legacy radial.json; shows in Configured)
            modContainer.registerConfig(
                    ModConfig.Type.CLIENT,
                    DesignClientConfig.SPEC,
                    "ezactions/design-client.toml"
            );
            // Hook: migrate existing file-based TOML (or legacy radial.json) into the spec on first load
            modBus.addListener(DesignClientConfig::onConfigLoad);

            Constants.LOG.debug("[{}] Registered CLIENT config specs (anim-client.toml, general-client.toml, design-client.toml).", Constants.MOD_NAME);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Failed to register CLIENT configs: {}", Constants.MOD_NAME, t.toString());
        }

        try {
            // MOD bus: key mapping registration
            modBus.addListener(EZActionsKeybinds::onRegisterKeyMappings);
            Constants.LOG.debug("[{}] Registered MOD-bus listeners.", Constants.MOD_NAME);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Failed to register MOD-bus listeners: {}", Constants.MOD_NAME, t.toString());
        }

        try {
            // GAME bus (global): client tick listeners
            if (FMLEnvironment.dist == Dist.CLIENT) {
                try { CustomIconManager.ensureFolderReady(); } catch (Throwable ignored) {}
                NeoForge.EVENT_BUS.addListener(KeyboardHandler::onClientTickPre);
                NeoForge.EVENT_BUS.addListener(KeyboardHandler::onClientTickPost);
                Constants.LOG.debug("[{}] Registered GAME-bus listeners (Pre & Post).", Constants.MOD_NAME);
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Failed to register GAME-bus listeners: {}", Constants.MOD_NAME, t.toString());
        }
    }
}
