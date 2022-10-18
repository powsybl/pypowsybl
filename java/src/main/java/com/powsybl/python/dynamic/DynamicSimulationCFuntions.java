package com.powsybl.python.dynamic;

import static com.powsybl.python.commons.Util.doCatch;

import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.dynamicsimulation.CurvesSupplier;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.DynamicSimulationResult;
import com.powsybl.dynamicsimulation.EventModelsSupplier;
import com.powsybl.iidm.network.Network;
import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader;

@CContext(Directives.class)
public final class DynamicSimulationCFuntions {

    private DynamicSimulationCFuntions() {
    }

    private static Logger logger() {
        return LoggerFactory.getLogger(DynamicSimulationCFuntions.class);
    }

    @CEntryPoint(name = "runDynamicModel")
    public static ObjectHandle runSecurityAnalysis(IsolateThread thread,
            ObjectHandle dynamicContextHandle,
            ObjectHandle networkHandle,
            ObjectHandle dynamicMappingHandle,
            ObjectHandle eventModelsSupplierHandle,
            ObjectHandle curvesSupplierHandle,
            ObjectHandle dynamicSimulationParametersHandle,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            DynamicSimulationContext dynamicContext = ObjectHandles.getGlobal().get(dynamicContextHandle);
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            DynamicModelMapper dynamicMapping = ObjectHandles.getGlobal().get(dynamicMappingHandle);
            EventModelsSupplier eventModelsSupplier = ObjectHandles.getGlobal().get(eventModelsSupplierHandle);
            CurvesSupplier curvesSupplier = ObjectHandles.getGlobal().get(curvesSupplierHandle);
            DynamicSimulationParameters dynamicSimulationParameters = ObjectHandles.getGlobal()
                    .get(dynamicSimulationParametersHandle);
            logger().info("Dynamic simulation run by Dynawaltz");
            DynamicSimulationResult result = dynamicContext.run(network,
                    dynamicMapping,
                    eventModelsSupplier,
                    curvesSupplier,
                    dynamicSimulationParameters);
            return ObjectHandles.getGlobal().create(result);
        });
    }

    @CEntryPoint(name = "addAlphaBetaLoad")
    public static void addAlphaBetaLoad(ObjectHandle dynamicMappingHandle, String staticId, String dynamicParam) {
        DynamicModelMapper dynamicMapping = ObjectHandles.getGlobal().get(dynamicMappingHandle);
        dynamicMapping.addAlphaBetaLoad(staticId, dynamicParam);
    }

    @CEntryPoint(name = "addOneTransformerLoad")
    public static void addOneTransformerLoad(ObjectHandle dynamicMappingHandle, String staticId, String dynamicParam) {
        DynamicModelMapper dynamicMapping = ObjectHandles.getGlobal().get(dynamicMappingHandle);
        dynamicMapping.addOneTransformerLoad(staticId, dynamicParam);
    }

    @CEntryPoint(name = "addOmegaRef")
    public static void addOmegaRef(ObjectHandle dynamicMappingHandle, String generatorId) {
        DynamicModelMapper dynamicMapping = ObjectHandles.getGlobal().get(dynamicMappingHandle);
        dynamicMapping.addOmegaRef(generatorId);
    }

    @CEntryPoint(name = "addGeneratorSynchronousThreeWindings")
    public static void addGeneratorSynchronousThreeWindings(ObjectHandle dynamicMappingHandle, String staticId,
            String dynamicParam) {
        DynamicModelMapper dynamicMapping = ObjectHandles.getGlobal().get(dynamicMappingHandle);
        dynamicMapping.addGeneratorSynchronousThreeWindings(staticId, dynamicParam);
    }

    @CEntryPoint(name = "addGeneratorSynchronousThreeWindingsProportionalRegulations")
    public static void addGeneratorSynchronousThreeWindingsProportionalRegulations(ObjectHandle dynamicMappingHandle,
            String staticId,
            String dynamicParam) {
        DynamicModelMapper dynamicMapping = ObjectHandles.getGlobal().get(dynamicMappingHandle);
        dynamicMapping.addGeneratorSynchronousThreeWindingsProportionalRegulations(staticId, dynamicParam);
    }

    @CEntryPoint(name = "addGeneratorSynchronousFourWindings")
    public static void addGeneratorSynchronousFourWindings(ObjectHandle dynamicMappingHandle, String staticId,
            String dynamicParam) {
        DynamicModelMapper dynamicMapping = ObjectHandles.getGlobal().get(dynamicMappingHandle);
        dynamicMapping.addGeneratorSynchronousFourWindings(staticId, dynamicParam);
    }

    @CEntryPoint(name = "addGeneratorSynchronousFourWindingsProportionalRegulations")
    public static void addGeneratorSynchronousFourWindingsProportionalRegulations(ObjectHandle dynamicMappingHandle,
            String staticId,
            String dynamicParam) {
        DynamicModelMapper dynamicMapping = ObjectHandles.getGlobal().get(dynamicMappingHandle);
        dynamicMapping.addGeneratorSynchronousFourWindingsProportionalRegulations(staticId, dynamicParam);
    }
}
