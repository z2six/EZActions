// Constants.java
package org.z2six.minemenuplus;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * // MainFile: Constants.java
 * Central constants for MineMenuPlus.
 * Keep logs here so every class can use the same logger.
 */
public final class Constants {
    public static final String MOD_ID = "minemenuplus";
    public static final String MOD_NAME = "MineMenuPlus";

    // Global logger for the mod
    public static final Logger LOG = LogUtils.getLogger();

    private Constants() {}
}
