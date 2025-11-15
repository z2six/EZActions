// neoforge/src/main/java/org/z2six/ezactions/client/pack/ClientPacks.java
package org.z2six.ezactions.client.pack;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;        // Position enum
import net.minecraft.server.packs.repository.PackSource;  // <-- correct PackSource package
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.z2six.ezactions.Constants;

@EventBusSubscriber(modid = Constants.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ClientPacks {
    private ClientPacks() {}

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent e) {
        if (e.getPackType() != PackType.CLIENT_RESOURCES) return;

        // Points at: common/src/main/resources/resourcepacks/ezactions_no_menu_blur
        ResourceLocation packLoc = ResourceLocation.fromNamespaceAndPath(
                Constants.MOD_ID, "resourcepacks/ezactions_no_menu_blur"
        );

        e.addPackFinders(
                packLoc,
                PackType.CLIENT_RESOURCES,
                Component.literal("EZActions • No Menu Blur"),
                PackSource.BUILT_IN,   // required by this overload
                true,                  // alwaysActive (forced ON)
                Pack.Position.TOP      // highest priority
        );
    }
}
