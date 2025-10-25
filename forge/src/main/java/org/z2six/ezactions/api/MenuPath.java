package com.z2six.ezactions.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable path of bundle titles from root -> ... -> target bundle.
 * Titles are compared literally (case-sensitive) to match the editor UI.
 */
public final class MenuPath {
    private final List<String> titles;

    private MenuPath(List<String> titles) {
        this.titles = Collections.unmodifiableList(new ArrayList<>(titles));
    }

    public static MenuPath of(List<String> titles) {
        Objects.requireNonNull(titles, "titles");
        return new MenuPath(titles);
    }

    public static MenuPath root() {
        return new MenuPath(List.of());
    }

    /** Return a new MenuPath with one more title appended. */
    public MenuPath child(String title) {
        List<String> copy = new ArrayList<>(titles);
        copy.add(Objects.requireNonNull(title));
        return new MenuPath(copy);
    }

    public List<String> titles() {
        return titles;
    }

    @Override public String toString() { return String.join("/", titles); }
}
