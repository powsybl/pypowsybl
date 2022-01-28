package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.PhaseTapChangerAdder;
import com.powsybl.iidm.network.TwoWindingsTransformer;

import java.util.List;

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
    public void addElement(Network network, List<UpdatingDataframe> dfs, int indexElement) {
        if (dfs.size() != 2) {
            throw new PowsyblException("Expected 2 dataframes: one for tap changes, one for steps.");
        }
        createPhaseTapChangers(network, dfs.get(0), dfs.get(1), indexElement);
    }

    private static void createPhaseTapChangers(Network network, UpdatingDataframe phasesDataframe, UpdatingDataframe stepsDataframe, int indexElement) {
        String transfomerId = phasesDataframe.getStringValue("id", indexElement)
                .orElseThrow(() -> new PowsyblException("id is missing"));
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(transfomerId);
        PhaseTapChangerAdder adder = transformer.newPhaseTapChanger();
        phasesDataframe.getDoubleValue("target_deadband", indexElement).ifPresent(adder::setTargetDeadband);
        phasesDataframe.getStringValue("regulation_mode", indexElement)
                .ifPresent(rm -> adder.setRegulationMode(PhaseTapChanger.RegulationMode.valueOf(rm)));
        phasesDataframe.getIntValue("low_tap", indexElement).ifPresent(adder::setLowTapPosition);
        for (int sectionIndex = 0; sectionIndex < stepsDataframe.getLineCount(); sectionIndex++) {
            String transformerStepId = stepsDataframe.getStringValue("id", sectionIndex).orElse(null);
            if (transfomerId.equals(transformerStepId)) {
                PhaseTapChangerAdder.StepAdder stepAdder = adder.beginStep();
                stepsDataframe.getDoubleValue("b", indexElement).ifPresent(stepAdder::setB);
                stepsDataframe.getDoubleValue("g", indexElement).ifPresent(stepAdder::setG);
                stepsDataframe.getDoubleValue("r", indexElement).ifPresent(stepAdder::setR);
                stepsDataframe.getDoubleValue("x", indexElement).ifPresent(stepAdder::setX);
                stepsDataframe.getDoubleValue("rho", indexElement).ifPresent(stepAdder::setRho);
                stepsDataframe.getDoubleValue("alpha", indexElement).ifPresent(stepAdder::setAlpha);
                stepAdder.endStep();
            }
        }
        phasesDataframe.getIntValue("tap", indexElement).ifPresent(adder::setTapPosition);
        adder.add();
    }
}
