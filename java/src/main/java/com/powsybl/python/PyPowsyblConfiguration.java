package com.powsybl.python;

/**
 * Library level configurations.
 *
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public final class PyPowsyblConfiguration {

    private static boolean readConfig = true;
    private static String defaultLoadflowProvider = "OpenLoadFlow";
    private static String defaultSecurityAnalysisProvider = "OpenSecurityAnalysis";
    private static String defaultSensitivityAnalysisProvider = "OpenSensitivityAnalysis";

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
        PyPowsyblConfiguration.defaultLoadflowProvider = loadflowProvider;
    }

    public static String getDefaultSecurityAnalysisProvider() {
        return defaultSecurityAnalysisProvider;
    }

    public static void setDefaultSecurityAnalysisProvider(String securityAnalysisProvider) {
        PyPowsyblConfiguration.defaultSecurityAnalysisProvider = securityAnalysisProvider;
    }

    public static String getDefaultSensitivityAnalysisProvider() {
        return defaultSensitivityAnalysisProvider;
    }

    public static void setDefaultSensitivityAnalysisProvider(String sensitivityAnalysisProvider) {
        PyPowsyblConfiguration.defaultSensitivityAnalysisProvider = sensitivityAnalysisProvider;
    }
}
