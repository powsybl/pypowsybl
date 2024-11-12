package com.powsybl.python.commons;

/**
 * Library level configurations.
 *
 * @author Sylvain Leclerc {@literal <sylvain.leclerc@rte-france.com>}
 */
public final class PyPowsyblConfiguration {

    private static final String OPEN_LOAD_FLOW = "OpenLoadFlow";

    private static boolean readConfig = true;
    private static String defaultLoadFlowProvider = OPEN_LOAD_FLOW;
    private static String defaultSecurityAnalysisProvider = OPEN_LOAD_FLOW;
    private static String defaultSensitivityAnalysisProvider = OPEN_LOAD_FLOW;
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
        return defaultLoadFlowProvider;
    }

    public static void setDefaultLoadFlowProvider(String loadflowProvider) {
        defaultLoadFlowProvider = loadflowProvider;
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
