package com.powsybl.dataframe.network.extensions;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.adders.AbstractSimpleAdder;
import com.powsybl.dataframe.network.adders.SeriesUtils;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.extensions.ThreeWindingsTransformerPhaseAngleClockAdder;

import java.util.Collections;
import java.util.List;

public class ThreeWindingsTransformerPhaseAngleClockDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.ints("phase_angle_clock_leg2"),
            SeriesMetadata.ints("phase_angle_clock_leg3")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    private static final class PhaseAngleClockSeries {

        private final StringSeries id;
        private final IntSeries phaseAngleClockLeg2;
        private final IntSeries phaseAngleClockLeg3;

        private PhaseAngleClockSeries(UpdatingDataframe dataframe) {
            this.id = SeriesUtils.getRequiredStrings(dataframe, "id");
            this.phaseAngleClockLeg2 = dataframe.getInts("phase_angle_clock_leg2");
            this.phaseAngleClockLeg3 = dataframe.getInts("phase_angle_clock_leg3");
            if (phaseAngleClockLeg2 == null && phaseAngleClockLeg3 == null) {
                throw new PowsyblException("At least one phase angle clock leg column is required.");
            }
        }

        private void create(Network network, int row) {
            String transformerId = id.get(row);
            ThreeWindingsTransformer transformer = network.getThreeWindingsTransformer(transformerId);
            if (transformer == null) {
                throw new PowsyblException("Three windings transformer '" + transformerId + "' does not exist.");
            }
            ThreeWindingsTransformerPhaseAngleClockAdder adder = transformer.newExtension(ThreeWindingsTransformerPhaseAngleClockAdder.class);
            adder.withPhaseAngleClockLeg2(phaseAngleClockLeg2 == null ? 0 : phaseAngleClockLeg2.get(row));
            adder.withPhaseAngleClockLeg3(phaseAngleClockLeg3 == null ? 0 : phaseAngleClockLeg3.get(row));
            adder.add();
        }
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        PhaseAngleClockSeries series = new PhaseAngleClockSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.create(network, row);
        }
    }
}
