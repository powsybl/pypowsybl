package com.powsybl.python.network;

import com.powsybl.iidm.modification.scalable.ProportionalScalable;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.modification.scalable.ScalingParameters;
import com.powsybl.iidm.network.Connectable;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import org.graalvm.nativeimage.c.type.CCharPointer;

import java.util.List;
import java.util.stream.Collectors;

import static com.powsybl.python.commons.Util.freeCharPtrPtr;

public final class ScalableUtils {

    public enum ScalableType {
        ELEMENT,
        STACK,
        PROPORTIONAL,
        UPDOWN;
    }

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

    public static ProportionalScalable.DistributionMode convert(PyPowsyblApiHeader.DistributionMode type) {
        return switch (type) {
            case PROPORTIONAL_TO_TARGETP -> ProportionalScalable.DistributionMode.PROPORTIONAL_TO_TARGETP;
            case PROPORTIONAL_TO_PMAX -> ProportionalScalable.DistributionMode.PROPORTIONAL_TO_PMAX;
            case PROPORTIONAL_TO_DIFF_PMAX_TARGETP -> ProportionalScalable.DistributionMode.PROPORTIONAL_TO_DIFF_PMAX_TARGETP;
            case PROPORTIONAL_TO_DIFF_TARGETP_PMIN -> ProportionalScalable.DistributionMode.PROPORTIONAL_TO_DIFF_TARGETP_PMIN;
            case PROPORTIONAL_TO_P0 -> ProportionalScalable.DistributionMode.PROPORTIONAL_TO_P0;
            case UNIFORM_DISTRIBUTION -> ProportionalScalable.DistributionMode.UNIFORM_DISTRIBUTION;
        };
    }

    public static List<Injection<?>> getInjections(Network network, List<String> injectionIds) {
        return injectionIds.stream().map(id -> {
            Connectable<?> connectable = network.getConnectable(id);
            if (!(connectable instanceof Injection)) {
                throw new IllegalArgumentException("Injection with ID '" + id + "' not found in the network");
            }
            return (Injection<?>) connectable;
        }).collect(Collectors.toList());
    }

    public static String checkInjectionId(CCharPointer injectionIdPtr, ScalableType scalableType) {
        if (scalableType != ScalableType.ELEMENT) {
            throw new IllegalArgumentException("No injection ids should be specified for " + scalableType + " scalable.");
        }
        return CTypeUtil.toString(injectionIdPtr);
    }

    /**
     * Frees inner memory, but not the pointer itself.
     */
    public static void freeScalingParametersContent(PyPowsyblApiHeader.ScalingParametersPointer parameters) {
        freeCharPtrPtr(parameters.getIgnoredInjectionIds(), parameters.getIgnoredInjectionIdsCount());
    }

}
