package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.*;

import java.util.List;

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
    public void addElement(Network network, List<UpdatingDataframe> dfs, int indexElement) {
        if (dfs.size() != 2) {
            throw new PowsyblException("Expected 2 dataframes: one for tap changers, one for steps.");
        }
        createRatioTapChangers(network, dfs.get(0), dfs.get(1), indexElement);
    }

    private static void createRatioTapChangers(Network network, UpdatingDataframe ratiosDataframe, UpdatingDataframe stepsDataframe, int indexElement) {
        String transfomerId = ratiosDataframe.getStringValue("id", indexElement)
                .orElseThrow(() -> new PowsyblException("id is missing"));
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(transfomerId);
        RatioTapChangerAdder adder = transformer.newRatioTapChanger();
        ratiosDataframe.getDoubleValue("target_deadband", indexElement).ifPresent(adder::setTargetDeadband);
        ratiosDataframe.getDoubleValue("target_v", indexElement).ifPresent(adder::setTargetV);
        ratiosDataframe.getIntValue("on_load", indexElement).ifPresent(onLoad -> adder.setLoadTapChangingCapabilities(onLoad == 1));
        ratiosDataframe.getIntValue("low_tap", indexElement).ifPresent(adder::setLowTapPosition);
        ratiosDataframe.getIntValue("tap", indexElement).ifPresent(adder::setTapPosition);
        for (int sectionIndex = 0; sectionIndex < stepsDataframe.getLineCount(); sectionIndex++) {
            String transformerStepId = stepsDataframe.getStringValue("id", sectionIndex).orElse(null);
            if (transfomerId.equals(transformerStepId)) {
                RatioTapChangerAdder.StepAdder stepAdder = adder.beginStep();
                stepsDataframe.getDoubleValue("b", indexElement).ifPresent(stepAdder::setB);
                stepsDataframe.getDoubleValue("g", indexElement).ifPresent(stepAdder::setG);
                stepsDataframe.getDoubleValue("r", indexElement).ifPresent(stepAdder::setR);
                stepsDataframe.getDoubleValue("x", indexElement).ifPresent(stepAdder::setX);
                stepsDataframe.getDoubleValue("rho", indexElement).ifPresent(stepAdder::setRho);
                stepAdder.endStep();
            }
        }
        adder.add();
    }
}
