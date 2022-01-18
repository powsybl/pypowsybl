package com.powsybl.python;

/**
 * Library level configurations.
 *
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public final class PyPowsyblConfiguration {

    private static boolean readConfig = true;

    private PyPowsyblConfiguration() {
    }

    /**
     * Should we read parameters from {@link com.powsybl.commons.config.PlatformConfig} or not ?
     */
    public static boolean isReadConfig() {
        return readConfig;
    }

    /**
     * Should we read parameters from {@link com.powsybl.commons.config.PlatformConfig} or not ?
     */
    public static void setReadConfig(boolean value) {
        readConfig = value;
    }
}
