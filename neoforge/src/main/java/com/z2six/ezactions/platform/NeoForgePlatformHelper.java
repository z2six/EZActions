package com.z2six.ezactions.platform;

import com.z2six.ezactions.platform.services.IPlatformHelper;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeoForgePlatformHelper implements IPlatformHelper {

    private static final Logger LOG = LoggerFactory.getLogger(NeoForgePlatformHelper.class);

    @Override
    public String getPlatformName() {
        LOG.debug("[EZActions] getPlatformName() -> NeoForge");
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        try {
            boolean loaded = ModList.get().isLoaded(modId);
            LOG.debug("[EZActions] isModLoaded('{}') -> {}", modId, loaded);
            return loaded;
        } catch (Throwable t) {
            LOG.warn("[EZActions] isModLoaded('{}') failed; assuming not loaded. Reason: {}", modId, t.toString());
            return false;
        }
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        try {
            // In this NeoForge version, 'production' is a public static boolean field.
            boolean production = FMLEnvironment.production;
            boolean dev = !production;
            LOG.debug("[EZActions] isDevelopmentEnvironment() -> {} (production={})", dev, production);
            return dev;
        } catch (Throwable t) {
            // Safer default: treat unknown as production to avoid enabling dev-only paths.
            LOG.warn("[EZActions] isDevelopmentEnvironment() detection failed; defaulting to production. Reason: {}", t.toString());
            return false;
        }
    }
}
