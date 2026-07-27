/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.extensions;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.adders.AbstractSimpleAdder;
import com.powsybl.dataframe.network.adders.SeriesUtils;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.RemoteReactivePowerControlAdder;

import java.util.Collections;
import java.util.List;

public class RemoteReactivePowerControlDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.doubles("target_q"),
            SeriesMetadata.strings("regulated_element_id"),
            SeriesMetadata.strings("regulated_side"),
            SeriesMetadata.booleans("enabled")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    private static class RemoteReactivePowerControlSerie {
        private final StringSeries generatorId;
        private final DoubleSeries targetQ;
        private final StringSeries regulatedElement;
        private final StringSeries regulatedSide;
        private final IntSeries enabled;

        RemoteReactivePowerControlSerie(UpdatingDataframe dataframe) {
            this.generatorId = dataframe.getStrings("id");
            this.targetQ = dataframe.getDoubles("target_q");
            this.regulatedElement = dataframe.getStrings("regulated_element_id");
            this.regulatedSide = dataframe.getStrings("regulated_side");
            this.enabled = dataframe.getInts("enabled");
        }

        void create(Network network, int row) {
            String id = this.generatorId.get(row);
            Generator generator = network.getGenerator(id);
            if (generator == null) {
                throw new PowsyblException("Invalid generator id : could not find " + id);
            }
            RemoteReactivePowerControlAdder adder = generator.newExtension(RemoteReactivePowerControlAdder.class);
            SeriesUtils.applyIfPresent(targetQ, row, adder::withTargetQ);
            SeriesUtils.applyIfPresent(enabled, row, value -> adder.withEnabled(value != 0));
            String elementId = regulatedElement != null ? regulatedElement.get(row) : null;
            String side = regulatedSide != null ? regulatedSide.get(row) : null;
            adder.withRegulatingTerminal(getTerminal(network, elementId, side, id));
            adder.add();
        }
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        RemoteReactivePowerControlSerie series = new RemoteReactivePowerControlSerie(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.create(network, row);
        }
    }

    /**
     * Resolves the regulating terminal from a regulated element id and an optional side.
     * The regulated element defaults to the generator itself when not provided.
     */
    protected static Terminal getTerminal(Network network, String regulatedElement, String side, String generatorId) {
        String targetElement = regulatedElement == null || regulatedElement.isEmpty() ? generatorId : regulatedElement;
        Identifiable<?> identifiable = network.getIdentifiable(targetElement);
        if (identifiable == null) {
            throw new PowsyblException("Regulated element '" + targetElement + "' not found");
        }
        if (identifiable instanceof Injection<?> injection) {
            return injection.getTerminal();
        } else if (identifiable instanceof Branch<?> branch) {
            return branch.getTerminal(TwoSides.valueOf(requireSide(side, targetElement)));
        } else if (identifiable instanceof ThreeWindingsTransformer t3wt) {
            return t3wt.getTerminal(ThreeSides.valueOf(requireSide(side, targetElement)));
        } else {
            throw new UnsupportedOperationException("Cannot set regulated element to " + targetElement +
                    ": the regulated element may only be an injection, a branch or a three windings transformer.");
        }
    }

    private static String requireSide(String side, String elementId) {
        if (side == null || side.isEmpty()) {
            throw new PowsyblException("A regulated_side must be provided when the regulated element '"
                    + elementId + "' is a branch or a three windings transformer");
        }
        return side;
    }

    /**
     * Returns the side ("ONE", "TWO", "THREE" or empty for injections) of a terminal within its connectable.
     */
    protected static String getTerminalSideStr(Terminal terminal) {
        if (terminal == null) {
            return "";
        }
        Connectable<?> connectable = terminal.getConnectable();
        if (connectable instanceof Branch<?> branch) {
            if (terminal == branch.getTerminal1()) {
                return TwoSides.ONE.name();
            } else if (terminal == branch.getTerminal2()) {
                return TwoSides.TWO.name();
            }
        } else if (connectable instanceof ThreeWindingsTransformer t3wt) {
            for (ThreeSides threeSides : ThreeSides.values()) {
                if (terminal == t3wt.getTerminal(threeSides)) {
                    return threeSides.name();
                }
            }
        }
        return "";
    }
}
