// MainFile: src/main/java/org/z2six/ezactions/ezactions.java
package org.z2six.ezactions;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.z2six.ezactions.config.DesignClientConfig;
import org.z2six.ezactions.handler.KeyboardHandler;
import org.z2six.ezactions.util.EZActionsKeybinds;

/*
 * Main mod entry. We register MOD-bus and GAME-bus listeners programmatically.
 * Forge 1.20.1 port: swapped NeoForge APIs for Forge equivalents (no behavior change).
 *
 * Note on deprecations:
 * - On Forge 1.20.1, FMLJavaModLoadingContext.get() and ModLoadingContext.get() are the
 *   correct accessors even though they’re marked for removal in newer MC lines.
 *   We locally suppress those warnings without changing behavior.
 */
@Mod(Constants.MOD_ID)
public final class ezactions {

    public ezactions() {
        final boolean isClient = (FMLEnvironment.dist == Dist.CLIENT);
        try {
            Constants.LOG.info("[{}] Initializing … (client? {})", Constants.MOD_NAME, isClient);
        } catch (Throwable ignored) {}

        // Acquire Forge mod event bus + mod loading context (1.20.1-correct; suppress removal warnings here only)
        final IEventBus modBus = getModEventBus();
        final ModLoadingContext mlc = getModLoadingContext();

        // ----------------------------
        // Register CLIENT configs
        // ----------------------------
        try {
            // anim-client.toml
            mlc.registerConfig(
                    ModConfig.Type.CLIENT,
                    org.z2six.ezactions.config.RadialAnimConfig.SPEC,
                    "ezactions/anim-client.toml"
            );

            // general-client.toml
            mlc.registerConfig(
                    ModConfig.Type.CLIENT,
                    org.z2six.ezactions.config.GeneralClientConfig.SPEC,
                    "ezactions/general-client.toml"
            );

            // design-client.toml (Configured-visible) + migration hook on first load
            mlc.registerConfig(
                    ModConfig.Type.CLIENT,
                    DesignClientConfig.SPEC,
                    "ezactions/design-client.toml"
            );
            modBus.addListener(DesignClientConfig::onConfigLoad);

            Constants.LOG.debug("[{}] Registered CLIENT config specs (anim-client.toml, general-client.toml, design-client.toml).",
                    Constants.MOD_NAME);
        } catch (Throwable t) {
            try {
                Constants.LOG.warn("[{}] Failed to register CLIENT configs: {}", Constants.MOD_NAME, t.toString());
            } catch (Throwable ignored) {}
        }

        // ----------------------------
        // MOD bus listeners (registrations, etc.)
        // ----------------------------
        try {
            modBus.addListener(EZActionsKeybinds::onRegisterKeyMappings);
            Constants.LOG.debug("[{}] Registered MOD-bus listeners.", Constants.MOD_NAME);
        } catch (Throwable t) {
            try {
                Constants.LOG.warn("[{}] Failed to register MOD-bus listeners: {}", Constants.MOD_NAME, t.toString());
            } catch (Throwable ignored) {}
        }

        // ----------------------------
        // GAME bus listeners (runtime events)
        // ----------------------------
        try {
            if (isClient) {
                // Client tick handlers (pre/post). These methods must match Forge's event signatures.
                MinecraftForge.EVENT_BUS.addListener(KeyboardHandler::onClientTickPre);
                MinecraftForge.EVENT_BUS.addListener(KeyboardHandler::onClientTickPost);
                Constants.LOG.debug("[{}] Registered GAME-bus listeners (Pre & Post).", Constants.MOD_NAME);
            }
        } catch (Throwable t) {
            try {
                Constants.LOG.warn("[{}] Failed to register GAME-bus listeners: {}", Constants.MOD_NAME, t.toString());
            } catch (Throwable ignored) {}
        }
    }

    // -------------------------------------------------
    // Local helpers with narrowly-scoped suppressions
    // -------------------------------------------------

    @SuppressWarnings({"removal", "deprecation"})
    private static IEventBus getModEventBus() {
        // Forge 1.20.1 canonical access
        return FMLJavaModLoadingContext.get().getModEventBus();
    }

    @SuppressWarnings({"removal", "deprecation"})
    private static ModLoadingContext getModLoadingContext() {
        // Forge 1.20.1 canonical access
        return ModLoadingContext.get();
    }
}
