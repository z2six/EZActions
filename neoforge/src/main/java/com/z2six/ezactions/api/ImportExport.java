package com.z2six.ezactions.api;

import java.util.Optional;

/** Import/export full menus or bundle slices as JSON strings. */
public interface ImportExport {

    /** Export the entire tree as JSON. */
    String exportAllJson();

    /** Export a bundle addressed by path as JSON array (items only). */
    String exportBundleJson(MenuPath path);

    /**
     * Import a JSON string (object or array) into the addressed bundle.
     * Behavior: append by default. Returns number of items successfully added.
     */
    int importInto(MenuPath path, String json);

    /** Replace entire tree with a JSON string (array or root object). Returns number of items at root after import. */
    int replaceAll(String json);

    /** Validate JSON against the schema we support. Returns error message if invalid. */
    Optional<String> validate(String json);
}
