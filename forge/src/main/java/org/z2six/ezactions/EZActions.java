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
import org.z2six.ezactions.util.CustomIconManager;
import org.z2six.ezactions.util.EZActionsKeybinds;

@Mod(Constants.MOD_ID)
public class EZActions {

    public EZActions() {
        Constants.LOG.info("[{}] Initializing... (client? {})", Constants.MOD_NAME, FMLEnvironment.dist == Dist.CLIENT);

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        try {
            ModLoadingContext.get().registerConfig(
                    ModConfig.Type.CLIENT,
                    org.z2six.ezactions.config.RadialAnimConfig.SPEC,
                    "ezactions/anim-client.toml"
            );
            ModLoadingContext.get().registerConfig(
                    ModConfig.Type.CLIENT,
                    org.z2six.ezactions.config.GeneralClientConfig.SPEC,
                    "ezactions/general-client.toml"
            );
            ModLoadingContext.get().registerConfig(
                    ModConfig.Type.CLIENT,
                    DesignClientConfig.SPEC,
                    "ezactions/design-client.toml"
            );
            modBus.addListener(DesignClientConfig::onConfigLoad);
            Constants.LOG.debug("[{}] Registered CLIENT configs.", Constants.MOD_NAME);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Failed to register CLIENT configs: {}", Constants.MOD_NAME, t.toString());
        }

        try {
            modBus.addListener(EZActionsKeybinds::onRegisterKeyMappings);
            Constants.LOG.debug("[{}] Registered MOD-bus listeners.", Constants.MOD_NAME);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Failed to register MOD-bus listeners: {}", Constants.MOD_NAME, t.toString());
        }

        try {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                try {
                    CustomIconManager.ensureFolderReady();
                } catch (Throwable ignored) {
                }
                MinecraftForge.EVENT_BUS.addListener(KeyboardHandler::onClientTick);
                Constants.LOG.debug("[{}] Registered GAME-bus listeners.", Constants.MOD_NAME);
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Failed to register GAME-bus listeners: {}", Constants.MOD_NAME, t.toString());
        }
    }
}
