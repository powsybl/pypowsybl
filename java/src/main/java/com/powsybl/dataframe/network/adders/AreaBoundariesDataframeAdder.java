/**
 * Copyright (c) 2024, Coreso SA (https://www.coreso.eu/) and TSCNET Services GmbH (https://www.tscnet.eu/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.*;
import com.powsybl.python.commons.PyPowsyblApiHeader;

import java.util.*;

import static com.powsybl.dataframe.network.adders.SeriesUtils.getRequiredStrings;

/**
 * @author Damien Jeandemange <damien.jeandemange@artelys.com>
 */
public class AreaBoundariesDataframeAdder implements NetworkElementAdder {

    public enum AdderType {
        ADD,
        REMOVE
    }

    private final AdderType adderType;

    public AreaBoundariesDataframeAdder(AdderType adderType) {
        this.adderType = adderType;
    }

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.strings("boundary_type"),
            SeriesMetadata.strings("element"),
            SeriesMetadata.strings("side"),
            SeriesMetadata.booleans("ac")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    private static final class AreaBoundaries {

        private final StringSeries ids;
        private final StringSeries boundaryTypes;
        private final StringSeries elements;
        private final StringSeries sides;
        private final IntSeries acs;

        AreaBoundaries(UpdatingDataframe dataframe) {
            this.ids = getRequiredStrings(dataframe, "id");
            this.boundaryTypes = getRequiredStrings(dataframe, "boundary_type");
            this.elements = getRequiredStrings(dataframe, "element");
            this.sides = dataframe.getStrings("side");
            this.acs = dataframe.getInts("ac");
        }

        public StringSeries getIds() {
            return ids;
        }

        public StringSeries getBoundaryTypes() {
            return boundaryTypes;
        }

        public StringSeries getElements() {
            return elements;
        }

        public StringSeries getSides() {
            return sides;
        }

        public IntSeries getAcs() {
            return acs;
        }
    }

    @Override
    public void addElements(Network network, List<UpdatingDataframe> dataframes) {
        UpdatingDataframe primaryTable = dataframes.get(0);
        AreaBoundaries series = new AreaBoundaries(primaryTable);

        for (int i = 0; i < primaryTable.getRowCount(); i++) {
            String areaId = series.getIds().get(i);
            PyPowsyblApiHeader.ElementType boundaryType = PyPowsyblApiHeader.ElementType.valueOf(series.getBoundaryTypes().get(i));
            if (!Objects.equals(boundaryType, PyPowsyblApiHeader.ElementType.DANGLING_LINE) &&
                    !Objects.equals(boundaryType, PyPowsyblApiHeader.ElementType.TERMINAL)) {
                throw new PowsyblException("Area boundary boundary_type must be either DANGLING_LINE or TERMINAL");
            }
            String element = series.getElements().get(i);
            String side = series.getSides() == null ? "" : series.getSides().get(i);
            boolean ac = series.getAcs() == null || series.getAcs().get(i) == 1;
            Area area = network.getArea(areaId);
            if (area == null) {
                throw new PowsyblException("Area " + areaId + " not found");
            }
            Connectable<?> connectable = network.getConnectable(element);
            if (connectable == null) {
                throw new PowsyblException("Element " + element + " not found");
            }
            if (Objects.equals(boundaryType, PyPowsyblApiHeader.ElementType.DANGLING_LINE)) {
                // Boundary modeled by a dangling line
                if (connectable instanceof DanglingLine danglingLine) {
                    switch (adderType) {
                        case ADD -> area.newAreaBoundary().setBoundary(danglingLine.getBoundary()).setAc(ac).add();
                        case REMOVE -> area.removeAreaBoundary(danglingLine.getBoundary());
                    }
                } else {
                    throw new PowsyblException(element + " is not a dangling line");
                }
            } else if (Objects.equals(boundaryType, PyPowsyblApiHeader.ElementType.TERMINAL)) {
                // Boundary modeled by a terminal
                Terminal terminal;
                if (connectable instanceof Injection<?> injection) {
                    terminal = injection.getTerminal();
                } else if (connectable instanceof Branch<?> branch) {
                    terminal = branch.getTerminal(TwoSides.valueOf(side));
                } else if (connectable instanceof ThreeWindingsTransformer t3wt) {
                    terminal = t3wt.getTerminal(ThreeSides.valueOf(side));
                } else {
                    // Never supposed to happen
                    throw new PowsyblException("Element " + element + " is not an injection, branch, or three windings transformer");
                }
                switch (adderType) {
                    case ADD -> area.newAreaBoundary().setTerminal(terminal).setAc(ac).add();
                    case REMOVE -> area.removeAreaBoundary(terminal);
                }
            }
        }
    }
}
