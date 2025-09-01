// MainFile: src/main/java/org/z2six/minemenuplus/gui/noblur/NoMenuBlurScreen.java
package org.z2six.minemenuplus.gui.noblur;

/**
 * Marker interface for screens that should NOT apply the vanilla menu blur.
 * Our mixin checks for this interface and skips the blur shader.
 */
public interface NoMenuBlurScreen {}
