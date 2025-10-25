package org.z2six.ezactions.api;

import java.util.List;
import java.util.Optional;

/** Read-only operations. */
public interface MenuRead {

    /** List items in the bundle addressed by path (root if empty). */
    List<ApiMenuItem> list(MenuPath path);

    /** Find first item by id anywhere in the tree (depth-first). */
    Optional<ApiMenuItem> findById(String id);

    /** Get the breadcrumb titles of the current editor/radial context. */
    List<String> currentPath();

    /** True if there is at least one bundle/title path matching exactly. */
    boolean existsPath(MenuPath path);
}
