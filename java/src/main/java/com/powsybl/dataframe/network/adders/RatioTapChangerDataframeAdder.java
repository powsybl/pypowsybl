package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.*;
import gnu.trove.list.array.TIntArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.powsybl.dataframe.network.adders.SeriesUtils.applyBooleanIfPresent;
import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

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
            SeriesMetadata.doubles("target_deadband"),
            SeriesMetadata.booleans("regulating"),
            SeriesMetadata.strings("regulated_side"),
            SeriesMetadata.strings("side")
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

    private static class RatioTapChangerSeries {
        private final StringSeries ids;
        private final IntSeries taps;
        private final IntSeries lowTaps;
        private final IntSeries onLoad;
        private final DoubleSeries targetV;
        private final DoubleSeries targetDeadband;
        private final IntSeries regulating;
        private final StringSeries regulatedSide;
        private final StringSeries sides;

        private final Map<String, TIntArrayList> stepsIndexes;
        private final DoubleSeries g;
        private final DoubleSeries b;
        private final DoubleSeries r;
        private final DoubleSeries x;
        private final DoubleSeries rho;

        RatioTapChangerSeries(UpdatingDataframe tapChangersDf, UpdatingDataframe stepsDf) {
            this.ids = tapChangersDf.getStrings("id");
            this.taps = tapChangersDf.getInts("tap");
            this.lowTaps = tapChangersDf.getInts("low_tap");
            this.onLoad = tapChangersDf.getInts("on_load");
            this.targetV = tapChangersDf.getDoubles("target_v");
            this.targetDeadband = tapChangersDf.getDoubles("target_deadband");
            this.regulating = tapChangersDf.getInts("regulating");
            this.regulatedSide = tapChangersDf.getStrings("regulated_side");
            this.sides = tapChangersDf.getStrings("side");

            this.stepsIndexes = getStepsIndexes(stepsDf);
            this.g = stepsDf.getDoubles("g");
            this.b = stepsDf.getDoubles("b");
            this.r = stepsDf.getDoubles("r");
            this.x = stepsDf.getDoubles("x");
            this.rho = stepsDf.getDoubles("rho");
        }

        void create(Network network, int row) {
            String transformerId = ids.get(row);
            RatioTapChangerAdder adder;
            TwoWindingsTransformer twoWindingsTransformer = network.getTwoWindingsTransformer(transformerId);
            if (twoWindingsTransformer == null) {
                ThreeWindingsTransformer threeWindingsTransformer = network.getThreeWindingsTransformer(transformerId);
                if (threeWindingsTransformer == null) {
                    throw new PowsyblException("Transformer " + transformerId + " does not exist.");
                }
                if (sides == null) {
                    throw new PowsyblException("Side is missing: cannot add a ratio tap changer on a three-winding transformer.");
                }
                ThreeSides side = ThreeSides.valueOf(sides.get(row));
                adder = threeWindingsTransformer.getLeg(side).newRatioTapChanger();
                if (regulatedSide != null) {
                    setRegulatedSide(threeWindingsTransformer, adder, regulatedSide.get(row));
                }
            } else {
                adder = twoWindingsTransformer.newRatioTapChanger();
                if (regulatedSide != null) {
                    setRegulatedSide(twoWindingsTransformer, adder, regulatedSide.get(row));
                }
            }
            applyIfPresent(targetDeadband, row, adder::setTargetDeadband);
            applyIfPresent(targetV, row, adder::setTargetV);
            applyBooleanIfPresent(onLoad, row, adder::setLoadTapChangingCapabilities);
            applyIfPresent(lowTaps, row, adder::setLowTapPosition);
            applyIfPresent(taps, row, adder::setTapPosition);
            applyBooleanIfPresent(regulating, row, adder::setRegulating);

            TIntArrayList steps = stepsIndexes.get(transformerId);
            if (steps != null) {
                steps.forEach(i -> {
                    RatioTapChangerAdder.StepAdder stepAdder = adder.beginStep();
                    applyIfPresent(b, i, stepAdder::setB);
                    applyIfPresent(g, i, stepAdder::setG);
                    applyIfPresent(r, i, stepAdder::setR);
                    applyIfPresent(x, i, stepAdder::setX);
                    applyIfPresent(rho, i, stepAdder::setRho);
                    stepAdder.endStep();
                    return true;
                });
            }
            adder.add();
        }

        private static void setRegulatedSide(TwoWindingsTransformer transformer, RatioTapChangerAdder adder, String regulatedSideStr) {
            if (!regulatedSideStr.isEmpty()) {
                TwoSides regulatedSide = TwoSides.valueOf(regulatedSideStr);
                adder.setRegulationTerminal(transformer.getTerminal(regulatedSide));
            }
        }

        private static void setRegulatedSide(ThreeWindingsTransformer transformer, RatioTapChangerAdder adder, String regulatedSideStr) {
            if (!regulatedSideStr.isEmpty()) {
                ThreeSides regulatedSide = ThreeSides.valueOf(regulatedSideStr);
                adder.setRegulationTerminal(transformer.getTerminal(regulatedSide));
            }
        }
    }

    @Override
    public void addElements(Network network, List<UpdatingDataframe> dataframes) {
        if (dataframes.size() != 2) {
            throw new PowsyblException("Expected 2 dataframes: one for tap changers, one for steps.");
        }
        UpdatingDataframe tapChangersDf = dataframes.get(0);
        UpdatingDataframe stepsDf = dataframes.get(1);
        RatioTapChangerSeries series = new RatioTapChangerSeries(tapChangersDf, stepsDf);
        IntStream.range(0, tapChangersDf.getRowCount())
                .forEach(row -> series.create(network, row));
    }

    /**
     * Mapping transfo ID --> index of steps in the steps dataframe
     */
    private static Map<String, TIntArrayList> getStepsIndexes(UpdatingDataframe stepsDataframe) {
        StringSeries ids = stepsDataframe.getStrings("id");
        if (ids == null) {
            throw new PowsyblException("Steps dataframe: id is not set");
        }
        Map<String, TIntArrayList> stepIndexes = new HashMap<>();
        for (int stepIndex = 0; stepIndex < stepsDataframe.getRowCount(); stepIndex++) {
            String transformerId = ids.get(stepIndex);
            stepIndexes.computeIfAbsent(transformerId, k -> new TIntArrayList())
                    .add(stepIndex);
        }
        return stepIndexes;
    }
}
