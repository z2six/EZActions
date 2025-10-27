package org.z2six.ezactions.api.internal;

import org.z2six.ezactions.api.MenuPath;
import org.z2six.ezactions.api.MenuRead;
import org.z2six.ezactions.api.model.ApiMenuItem;
import org.z2six.ezactions.data.menu.MenuItem;
import org.z2six.ezactions.data.menu.RadialMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class MenuReadImpl implements MenuRead {

    @Override
    public List<ApiMenuItem> list(MenuPath path) {
        List<MenuItem> root = RadialMenu.rootMutable();
        List<MenuItem> at = (path == null || path.titles().isEmpty())
                ? root
                : TreeOps.findBundleByTitles(root, path.titles());
        List<ApiMenuItem> out = new ArrayList<>();
        if (at == null) return out;
        for (MenuItem mi : at) out.add(snap(mi));
        return out;
    }

    @Override
    public Optional<ApiMenuItem> findById(String id) {
        MenuItem mi = TreeOps.findFirstById(RadialMenu.rootMutable(), id);
        return mi == null ? Optional.empty() : Optional.of(snap(mi));
    }

    @Override
    public List<String> currentPath() {
        return TreeOps.currentPathTitles();
    }

    @Override
    public boolean existsPath(MenuPath path) {
        return TreeOps.existsPath(RadialMenu.rootMutable(), path == null ? List.of() : path.titles());
    }

    private static ApiMenuItem snap(MenuItem mi) {
        String type = mi.isCategory() ? "BUNDLE" : "ACTION";
        return new ApiMenuItem(
                safe(mi.id()),
                safe(mi.title()),
                mi.isCategory(),
                type,
                nullToNull(mi.note()),
                null // icon reserved
        );
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static String nullToNull(String s) { return s; }
}
