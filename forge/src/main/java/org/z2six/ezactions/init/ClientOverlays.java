package org.z2six.ezactions.init;

/**
 * Placeholder for client overlay registration.
 * We’ll wire this up during the "modify" step by subscribing to the
 * NeoForge client GUI render event and calling RadialLingerOverlay.render(...).
 */
public final class ClientOverlays {
    private ClientOverlays() {}

    /** Call during client setup once we hook into the event bus (next step). */
    public static void init() {
        // Intentionally empty for now.
        // Example (next step):
        // NeoForge.EVENT_BUS.addListener(ClientOverlays::onRenderGui);
    }
}
