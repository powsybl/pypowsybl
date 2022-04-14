package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.PhaseTapChangerAdder;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import gnu.trove.list.array.TIntArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public class PhaseTapChangerDataframeAdder implements NetworkElementAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.strings("regulation_mode"),
            SeriesMetadata.doubles("target_deadband"),
            SeriesMetadata.ints("low_tap"),
            SeriesMetadata.ints("tap")
    );

    private static final List<SeriesMetadata> STEPS_METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.doubles("g"),
            SeriesMetadata.doubles("b"),
            SeriesMetadata.doubles("r"),
            SeriesMetadata.doubles("x"),
            SeriesMetadata.doubles("rho"),
            SeriesMetadata.doubles("alpha")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return List.of(METADATA, STEPS_METADATA);
    }

    @Override
    public void addElements(Network network, List<UpdatingDataframe> dataframes) {
        if (dataframes.size() != 2) {
            throw new PowsyblException("Expected 2 dataframes: one for tap changes, one for steps.");
        }
        UpdatingDataframe tapChangersDf = dataframes.get(0);
        UpdatingDataframe stepsDf = dataframes.get(1);
        Map<String, TIntArrayList> stepsIndexes = getStepsIndexes(stepsDf);
        for (int index = 0; index < tapChangersDf.getLineCount(); index++) {
            createPhaseTapChanger(network, tapChangersDf, stepsDf, index, stepsIndexes);
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

    private static void createPhaseTapChanger(Network network,
                                              UpdatingDataframe tapChangersDataframe,
                                              UpdatingDataframe stepsDataframe,
                                              int transformerIndex,
                                              Map<String, TIntArrayList> stepIndexes) {
        String transformerId = tapChangersDataframe.getStringValue("id", transformerIndex)
                .orElseThrow(() -> new PowsyblException("id is missing"));
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(transformerId);
        PhaseTapChangerAdder adder = transformer.newPhaseTapChanger();
        tapChangersDataframe.getDoubleValue("target_deadband", transformerIndex).ifPresent(adder::setTargetDeadband);
        tapChangersDataframe.getStringValue("regulation_mode", transformerIndex)
                .ifPresent(rm -> adder.setRegulationMode(PhaseTapChanger.RegulationMode.valueOf(rm)));
        tapChangersDataframe.getIntValue("low_tap", transformerIndex).ifPresent(adder::setLowTapPosition);
        TIntArrayList steps = stepIndexes.get(transformerId);
        if (steps != null) {
            steps.forEach(i -> {
                PhaseTapChangerAdder.StepAdder stepAdder = adder.beginStep();
                stepsDataframe.getDoubleValue("b", transformerIndex).ifPresent(stepAdder::setB);
                stepsDataframe.getDoubleValue("g", transformerIndex).ifPresent(stepAdder::setG);
                stepsDataframe.getDoubleValue("r", transformerIndex).ifPresent(stepAdder::setR);
                stepsDataframe.getDoubleValue("x", transformerIndex).ifPresent(stepAdder::setX);
                stepsDataframe.getDoubleValue("rho", transformerIndex).ifPresent(stepAdder::setRho);
                stepsDataframe.getDoubleValue("alpha", transformerIndex).ifPresent(stepAdder::setAlpha);
                stepAdder.endStep();
                return true;
            });
        }
        tapChangersDataframe.getIntValue("tap", transformerIndex).ifPresent(adder::setTapPosition);
        adder.add();
    }
}
