package org.z2six.ezactions.api;

/** UI helpers. */
public interface EditorOps {
    /** Open the in-game editor screen. */
    void openEditor();

    /** Open the in-game config screen. */
    void openConfig();

    /** Open the radial at root. */
    void openRadial();

    /** Open the radial at a bundle id (falls back to root if not found). */
    void openRadialAtBundle(String bundleId);

    /** Open a temporary radial defined by JSON (same schema as menu JSON root array/item). */
    boolean openTemporaryRadial(String jsonItemOrArray, DynamicRadialStyle styleOrNull);
}
