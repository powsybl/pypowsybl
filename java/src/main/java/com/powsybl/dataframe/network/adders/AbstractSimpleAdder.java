/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.reporter.Reporter;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.modification.topology.CreateFeederBay;
import com.powsybl.iidm.modification.topology.CreateFeederBayBuilder;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.ConnectablePosition;

import java.util.List;

/**
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public abstract class AbstractSimpleAdder implements NetworkElementAdder {

    protected UpdatingDataframe getPrimaryDataframe(List<UpdatingDataframe> dataframes) {
        if (dataframes.size() != 1) {
            throw new IllegalArgumentException("Expected only one input dataframe");
        }
        return dataframes.get(0);
    }

    @FunctionalInterface
    public interface AdditionStrategy {
        void add(Network network, UpdatingDataframe df, InjectionAdder<?> adder, int row, boolean throwException, Reporter reporter);
    }

    public void addElementsWithBay(Network network, UpdatingDataframe dataframe, boolean throwException, Reporter reporter) {
        addElements(network, dataframe, this::addWithBay, throwException, reporter);
    }

    public void addElements(Network network, UpdatingDataframe dataframe) {
        addElements(network, dataframe, (n, df, adder, row, throwException, reporter) -> add(adder), false, Reporter.NO_OP);
    }

    void addElements(Network network, UpdatingDataframe dataframe, AbstractSimpleAdder.AdditionStrategy addition, boolean throwException, Reporter reporter) {
        //do nothing
    }

    private void add(InjectionAdder<?> injectionAdder) {
        if (injectionAdder instanceof LoadAdder) {
            ((LoadAdder) injectionAdder).add();
        } else if (injectionAdder instanceof BatteryAdder) {
            ((BatteryAdder) injectionAdder).add();
        } else if (injectionAdder instanceof DanglingLineAdder) {
            ((DanglingLineAdder) injectionAdder).add();
        } else if (injectionAdder instanceof GeneratorAdder) {
            ((GeneratorAdder) injectionAdder).add();
        } else if (injectionAdder instanceof ShuntCompensatorAdder) {
            ((ShuntCompensatorAdder) injectionAdder).add();
        } else if (injectionAdder instanceof StaticVarCompensatorAdder) {
            ((StaticVarCompensatorAdder) injectionAdder).add();
        } else if (injectionAdder instanceof LccConverterStationAdder) {
            ((LccConverterStationAdder) injectionAdder).add();
        } else if (injectionAdder instanceof VscConverterStationAdder) {
            ((VscConverterStationAdder) injectionAdder).add();
        } else {
            throw new AssertionError("Given InjectionAdder not supported: " + injectionAdder.getClass().getName());
        }
    }

    private void addWithBay(Network network, UpdatingDataframe dataframe, InjectionAdder<?> injectionAdder, int row, boolean throwException, Reporter reporter) {
        String busbarSectionId = dataframe.getStrings("busbar_section_id").get(row);
        int injectionPositionOrder = dataframe.getInts("position_order").get(row);
        ConnectablePosition.Direction direction = ConnectablePosition.Direction.valueOf(dataframe.getStringValue("direction", row).orElse("BOTTOM"));
        CreateFeederBay modification = new CreateFeederBayBuilder()
                .withInjectionAdder(injectionAdder)
                .withBbsId(busbarSectionId)
                .withInjectionPositionOrder(injectionPositionOrder)
                .withInjectionDirection(direction)
                .build();
        modification.apply(network, throwException, reporter == null ? Reporter.NO_OP : reporter);
    }

    @Override
    public void addElements(Network network, List<UpdatingDataframe> dataframes) {
        UpdatingDataframe primaryDf = getPrimaryDataframe(dataframes);
        addElements(network, primaryDf);
    }

    @Override
    public void addElementsWithBay(Network network, List<UpdatingDataframe> dataframes, boolean throwException, Reporter reporter) {
        UpdatingDataframe primaryDf = getPrimaryDataframe(dataframes);
        addElementsWithBay(network, primaryDf, throwException, reporter);
    }

}
