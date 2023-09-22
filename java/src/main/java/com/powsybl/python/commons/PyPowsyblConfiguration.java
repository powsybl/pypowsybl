package com.powsybl.python.commons;

/**
 * Library level configurations.
 *
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public final class PyPowsyblConfiguration {

    private static boolean readConfig = true;
    private static String defaultLoadflowProvider = "OpenLoadFlow";
    private static String defaultSecurityAnalysisProvider = "OpenLoadFlow";
    private static String defaultSensitivityAnalysisProvider = "OpenLoadFlow";
    private static String defaultShortCircuitAnalysisProvider = "";

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

    public static String getDefaultLoadFlowProvider() {
        return defaultLoadflowProvider;
    }

    public static void setDefaultLoadFlowProvider(String loadflowProvider) {
        defaultLoadflowProvider = loadflowProvider;
    }

    public static String getDefaultSecurityAnalysisProvider() {
        return defaultSecurityAnalysisProvider;
    }

    public static void setDefaultSecurityAnalysisProvider(String securityAnalysisProvider) {
        defaultSecurityAnalysisProvider = securityAnalysisProvider;
    }

    public static String getDefaultSensitivityAnalysisProvider() {
        return defaultSensitivityAnalysisProvider;
    }

    public static void setDefaultSensitivityAnalysisProvider(String sensitivityAnalysisProvider) {
        defaultSensitivityAnalysisProvider = sensitivityAnalysisProvider;
    }

    public static String getDefaultShortCircuitAnalysisProvider() {
        return defaultShortCircuitAnalysisProvider;
    }

    public static void setDefaultShortCircuitAnalysisProvider(String shortCircuitAnalysisProvider) {
        defaultShortCircuitAnalysisProvider = shortCircuitAnalysisProvider;
    }
}
