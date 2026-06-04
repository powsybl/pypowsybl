/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.adders;

import java.util.Collections;
import java.util.List;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import static com.powsybl.dataframe.network.adders.SeriesUtils.getRequiredStrings;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.VoltageAngleLimit;
import com.powsybl.iidm.network.VoltageAngleLimitAdder;

/**
 * @author Nico Westerbeck {@literal <Nico.Westerbeck at 50hertz.com>}
 */
public class VoltageAngleLimitsDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.strings("from_element_id"),
            SeriesMetadata.strings("from_side"),
            SeriesMetadata.strings("to_element_id"),
            SeriesMetadata.strings("to_side"),
            SeriesMetadata.doubles("low_limit"),
            SeriesMetadata.doubles("high_limit")
    );

    private static final class VoltageAngleLimitsSeries {
        private final StringSeries ids;
        private final StringSeries fromElementIds;
        private final StringSeries fromSides;
        private final StringSeries toElementIds;
        private final StringSeries toSides;
        private final DoubleSeries lowLimits;
        private final DoubleSeries highLimits;

        private VoltageAngleLimitsSeries(UpdatingDataframe dataframe) {
            this.ids = getRequiredStrings(dataframe, "id");
            this.fromElementIds = getRequiredStrings(dataframe, "from_element_id");
            this.fromSides = dataframe.getStrings("from_side");
            this.toElementIds = getRequiredStrings(dataframe, "to_element_id");
            this.toSides = dataframe.getStrings("to_side");
            this.lowLimits = dataframe.getDoubles("low_limit");
            this.highLimits = dataframe.getDoubles("high_limit");
        }

        /**
         * Creates or replaces one voltage angle limit from a dataframe row.
         *
         * The row identifies the two endpoints through connectable ids and optional sides,
         * which are resolved to IIDM terminals. If another voltage angle limit already uses the
         * same limit id, it is removed first so the dataframe row behaves as a full replacement.
         * At least one of {@code low_limit} or {@code high_limit} must be defined.
         */
        private void create(Network network, int row) {
            Terminal terminalFrom = NetworkUtils.getTerminalOrThrow(network, fromElementIds.get(row), getSideValue(fromSides, row));
            Terminal terminalTo = NetworkUtils.getTerminalOrThrow(network, toElementIds.get(row), getSideValue(toSides, row));
            boolean hasLowLimit = hasValue(lowLimits, row);
            boolean hasHighLimit = hasValue(highLimits, row);

            if (!hasLowLimit && !hasHighLimit) {
                throw new PowsyblException("At least one of low_limit or high_limit must be provided.");
            }

            String id = ids.get(row);
            VoltageAngleLimit existing = network.getVoltageAngleLimit(id);
            if (existing != null) {
                existing.remove();
            }

            VoltageAngleLimitAdder adder = network.newVoltageAngleLimit()
                    .setId(id)
                    .from(terminalFrom)
                    .to(terminalTo);

            if (hasLowLimit) {
                adder.setLowLimit(lowLimits.get(row));
            }
            if (hasHighLimit) {
                adder.setHighLimit(highLimits.get(row));
            }

            adder.add();
        }

        private static String getSideValue(StringSeries sides, int row) {
            return sides == null ? null : sides.get(row);
        }

        private static boolean hasValue(DoubleSeries series, int row) {
            return series != null && !Double.isNaN(series.get(row));
        }
    }

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        VoltageAngleLimitsSeries series = new VoltageAngleLimitsSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.create(network, row);
        }
    }
}
