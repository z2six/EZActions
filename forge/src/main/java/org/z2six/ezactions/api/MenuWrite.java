package org.z2six.ezactions.api;

import java.util.Optional;
import java.util.function.Predicate;
import org.z2six.ezactions.data.click.IClickAction;
import org.z2six.ezactions.data.icon.IconSpec;

/** Mutating operations (persisted immediately). */
public interface MenuWrite {

    /** Add an action into the addressed bundle (root if path is empty). Returns created id. */
    default Optional<String> addAction(MenuPath path, String title, String noteOrNull, IClickAction action, boolean locked) {
        return addAction(path, title, noteOrNull, null, action, locked);
    }

    /** Add an action into the addressed bundle (root if path is empty). Returns created id. */
    Optional<String> addAction(MenuPath path, String title, String noteOrNull, IconSpec iconOrNull, IClickAction action, boolean locked);

    /** Add a bundle into the addressed bundle (root if path is empty). Returns created id. */
    default Optional<String> addBundle(MenuPath path, String title, String noteOrNull,
                                       boolean hideFromMainRadial, boolean bundleKeybindEnabled, boolean locked) {
        return addBundle(path, title, noteOrNull, null, hideFromMainRadial, bundleKeybindEnabled, locked);
    }

    /** Add a bundle into the addressed bundle (root if path is empty). Returns created id. */
    Optional<String> addBundle(MenuPath path, String title, String noteOrNull, IconSpec iconOrNull,
                               boolean hideFromMainRadial, boolean bundleKeybindEnabled, boolean locked);

    /** Move item within the same bundle: content indices (0..n). */
    boolean moveWithin(MenuPath path, int fromIndex, int toIndex);

    /** Move item by id into another bundle (append at end). */
    boolean moveTo(String itemId, MenuPath targetBundle);

    /** Remove first item matching predicate in the addressed bundle. */
    boolean removeFirst(MenuPath path, Predicate<ApiMenuItem> predicate);

    /** Remove by id anywhere. */
    boolean removeById(String id);

    /** Update title/note/icon for an existing item by id (bundle or action). */
    boolean updateMeta(String id, String titleOrNull, String noteOrNull, IconSpec iconOrNull);

    /** Replace action payload for an existing action item by id. */
    boolean replaceAction(String id, IClickAction action);

    /** Update bundle-only flags by id. */
    boolean setBundleFlags(String id, boolean hideFromMainRadial, boolean bundleKeybindEnabled);

    /** Update lock flag by id. */
    boolean setLocked(String id, boolean locked);

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
