package com.powsybl.dataframe;

/**
 * @author Naledi El Cheikh {@literal <naledi.elcheikh at rte-france.com>}
 */
public enum DataframeDistributionMode {
    PROPORTIONAL_TO_TARGETP,
    PROPORTIONAL_TO_PMAX,
    PROPORTIONAL_TO_DIFF_PMAX_TARGETP,
    PROPORTIONAL_TO_DIFF_TARGETP_PMIN,
    PROPORTIONAL_TO_P0,
    UNIFORM_DISTRIBUTION
}
