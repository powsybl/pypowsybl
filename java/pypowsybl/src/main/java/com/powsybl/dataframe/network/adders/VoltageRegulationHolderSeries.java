/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.regulation.RegulationMode;
import com.powsybl.iidm.network.regulation.VoltageRegulationAdder;
import com.powsybl.iidm.network.regulation.VoltageRegulationHolderAdder;
import com.powsybl.python.network.NetworkUtil;

import static com.powsybl.dataframe.network.adders.SeriesUtils.applyBooleanIfPresent;
import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Hugo Kulesza <hugo.kulesza at rte-france.com>
 */
public class VoltageRegulationHolderSeries extends InjectionSeries {

    protected final StringSeries regulationMode;
    protected final IntSeries regulating;
    protected final DoubleSeries regulationValue;
    protected final DoubleSeries targetDeadband;
    protected final DoubleSeries slope;
    protected final StringSeries regulatingElements;
    protected final DoubleSeries localTargetQ;
    protected final DoubleSeries localTargetV;

    VoltageRegulationHolderSeries(UpdatingDataframe dataframe) {
        super(dataframe);
        this.regulationMode = dataframe.getStrings("regulation_mode");
        this.regulating = dataframe.getInts("regulating");
        this.regulationValue = dataframe.getDoubles("regulation_value");
        this.targetDeadband = dataframe.getDoubles("target_deadband");
        this.slope = dataframe.getDoubles("slope");
        this.regulatingElements = dataframe.getStrings("regulating_element_id");
        this.localTargetQ = dataframe.getDoubles("local_target_q");
        this.localTargetV = dataframe.getDoubles("local_target_v");
    }

    protected void setVoltageRegulationAttributes(VoltageRegulationHolderAdder<?> adder, Network network, int row) {
        applyIfPresent(localTargetQ, row, adder::setLocalTargetQ);
        applyIfPresent(localTargetV, row, adder::setLocalTargetV);
        if (newColumnsUsed()) {
            VoltageRegulationAdder<?> regulationAdder = adder.newVoltageRegulation();
            applyBooleanIfPresent(regulating, row, regulationAdder::withRegulating);
            applyIfPresent(regulationMode, row, RegulationMode.class, regulationAdder::withMode);
            applyIfPresent(regulationValue, row, regulationAdder::withTargetValue);
            applyIfPresent(targetDeadband, row, regulationAdder::withTargetDeadband);
            applyIfPresent(slope, row, regulationAdder::withSlope);
            if (regulatingElements != null) {
                String id = ids.get(row);
                String elementId = regulatingElements.get(row);
                if (!elementId.equals(id)) {
                    NetworkUtil.setRegulatingTerminal(regulationAdder::withTerminal, network, elementId);
                }
            }
            regulationAdder.add();
        }
    }

    protected boolean newColumnsUsed() {
        return regulating != null || regulatingElements != null || regulationMode != null || regulationValue != null;
    }
}
