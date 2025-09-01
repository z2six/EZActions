// Constants.java
package org.z2six.ezactions;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * // MainFile: Constants.java
 * Central constants for ezactions.
 * Keep logs here so every class can use the same logger.
 */
public final class Constants {
    public static final String MOD_ID = "ezactions";
    public static final String MOD_NAME = "ezactions";

    // Global logger for the mod
    public static final Logger LOG = LogUtils.getLogger();

    private Constants() {}
}
