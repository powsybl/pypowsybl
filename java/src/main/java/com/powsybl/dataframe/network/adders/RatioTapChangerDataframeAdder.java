package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.*;
import gnu.trove.list.array.TIntArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public class RatioTapChangerDataframeAdder implements NetworkElementAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.ints("tap"),
            SeriesMetadata.ints("low_tap"),
            SeriesMetadata.booleans("on_load"),
            SeriesMetadata.doubles("target_v"),
            SeriesMetadata.doubles("target_deadband")
    );

    private static final List<SeriesMetadata> STEPS_METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.doubles("g"),
            SeriesMetadata.doubles("b"),
            SeriesMetadata.doubles("r"),
            SeriesMetadata.doubles("x"),
            SeriesMetadata.doubles("rho")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return List.of(METADATA, STEPS_METADATA);
    }

    @Override
    public void addElements(Network network, List<UpdatingDataframe> dataframes) {
        if (dataframes.size() != 2) {
            throw new PowsyblException("Expected 2 dataframes: one for tap changers, one for steps.");
        }
        UpdatingDataframe tapChangersDf = dataframes.get(0);
        UpdatingDataframe stepsDf = dataframes.get(1);
        Map<String, TIntArrayList> stepsIndexes = getStepsIndexes(stepsDf);
        for (int index = 0; index < tapChangersDf.getLineCount(); index++) {
            createRatioTapChanger(network, tapChangersDf, stepsDf, index, stepsIndexes);
        }
    }

    /**
     * Mapping transfo ID --> index of steps in the steps dataframe
     */
    private static Map<String, TIntArrayList> getStepsIndexes(UpdatingDataframe stepsDataframe) {
        Map<String, TIntArrayList> stepIndexes = new HashMap<>();
        for (int stepIndex = 0; stepIndex < stepsDataframe.getLineCount(); stepIndex++) {
            String transformerId = stepsDataframe.getStringValue("id", stepIndex)
                    .orElseThrow(() -> new PowsyblException("Steps dataframe: id is not set"));
            stepIndexes.computeIfAbsent(transformerId, k -> new TIntArrayList())
                    .add(stepIndex);
        }
        return stepIndexes;
    }

    private static void createRatioTapChanger(Network network,
                                              UpdatingDataframe tapChangersDataframe,
                                              UpdatingDataframe stepsDataframe,
                                              int transformerIndex,
                                              Map<String, TIntArrayList> stepIndexes) {
        String transformerId = tapChangersDataframe.getStringValue("id", transformerIndex)
                .orElseThrow(() -> new PowsyblException("id is missing"));
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(transformerId);
        RatioTapChangerAdder adder = transformer.newRatioTapChanger();
        tapChangersDataframe.getDoubleValue("target_deadband", transformerIndex).ifPresent(adder::setTargetDeadband);
        tapChangersDataframe.getDoubleValue("target_v", transformerIndex).ifPresent(adder::setTargetV);
        tapChangersDataframe.getIntValue("on_load", transformerIndex).ifPresent(onLoad -> adder.setLoadTapChangingCapabilities(onLoad == 1));
        tapChangersDataframe.getIntValue("low_tap", transformerIndex).ifPresent(adder::setLowTapPosition);
        tapChangersDataframe.getIntValue("tap", transformerIndex).ifPresent(adder::setTapPosition);
        TIntArrayList steps = stepIndexes.get(transformerId);
        if (steps != null) {
            steps.forEach(i -> {
                RatioTapChangerAdder.StepAdder stepAdder = adder.beginStep();
                stepsDataframe.getDoubleValue("b", transformerIndex).ifPresent(stepAdder::setB);
                stepsDataframe.getDoubleValue("g", transformerIndex).ifPresent(stepAdder::setG);
                stepsDataframe.getDoubleValue("r", transformerIndex).ifPresent(stepAdder::setR);
                stepsDataframe.getDoubleValue("x", transformerIndex).ifPresent(stepAdder::setX);
                stepsDataframe.getDoubleValue("rho", transformerIndex).ifPresent(stepAdder::setRho);
                stepAdder.endStep();
                return true;
            });
        }
        adder.add();
    }
}
