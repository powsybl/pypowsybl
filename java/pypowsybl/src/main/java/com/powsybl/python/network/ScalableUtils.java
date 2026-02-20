package com.powsybl.python.network;

import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.modification.scalable.ScalingParameters;
import com.powsybl.python.commons.PyPowsyblApiHeader;

import static com.powsybl.python.commons.Util.freeCharPtrPtr;

public final class ScalableUtils {

    private ScalableUtils() {
    }

    public static ScalingParameters.Priority convert(PyPowsyblApiHeader.Priority type) {
        return switch (type) {
            case RESPECT_OF_VOLUME_ASKED -> ScalingParameters.Priority.RESPECT_OF_VOLUME_ASKED;
            case RESPECT_OF_DISTRIBUTION -> ScalingParameters.Priority.RESPECT_OF_DISTRIBUTION;
            case ONESHOT -> ScalingParameters.Priority.ONESHOT;
        };
    }

    public static ScalingParameters.ScalingType convert(PyPowsyblApiHeader.ScalingType type) {
        return switch (type) {
            case DELTA_P -> ScalingParameters.ScalingType.DELTA_P;
            case TARGET_P -> ScalingParameters.ScalingType.TARGET_P;
        };
    }

    public static Scalable.ScalingConvention convert(PyPowsyblApiHeader.ScalingConvention type) {
        return switch (type) {
            case GENERATOR -> Scalable.ScalingConvention.GENERATOR;
            case LOAD -> Scalable.ScalingConvention.LOAD;
        };
    }

    /**
     * Frees inner memory, but not the pointer itself.
     */
    public static void freeScalingParametersContent(PyPowsyblApiHeader.ScalingParametersPointer parameters) {
        freeCharPtrPtr(parameters.getIgnoredInjectionIds(), parameters.getIgnoredInjectionIdsCount());
    }
}
