package org.z2six.ezactions.api;

import org.z2six.ezactions.api.model.ApiMenuItem;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** Mutating operations (persisted immediately). */
public interface MenuWrite {

    /** Move item within the same bundle: content indices (0..n). */
    boolean moveWithin(MenuPath path, int fromIndex, int toIndex);

    /** Move item by id into another bundle (append at end). */
    boolean moveTo(String itemId, MenuPath targetBundle);

    /** Remove first item matching predicate in the addressed bundle. */
    boolean removeFirst(MenuPath path, Predicate<ApiMenuItem> predicate);

    /** Remove by id anywhere. */
    boolean removeById(String id);

    /** Create (or ensure) a chain of bundles by titles. Returns true if created any. */
    boolean ensureBundles(MenuPath path);

    /**
     * Add or replace an item by importing a minimal JSON snippet into the bundle.
     * This accepts the same schema as ImportExport#exportJson for a single item.
     *
     * Returns the resolved id of the added/replaced item.
     */
    Optional<String> upsertFromJson(MenuPath path, String jsonItemOrArray);
}
