// MainFile: src/main/java/org/z2six/ezactions/gui/noblur/NoMenuBlurScreen.java
package ezactions.gui.noblur;

/**
 * Marker interface for screens that should NOT apply the vanilla menu blur.
 * Our mixin checks for this interface and skips the blur shader.
 */
public interface NoMenuBlurScreen {}
