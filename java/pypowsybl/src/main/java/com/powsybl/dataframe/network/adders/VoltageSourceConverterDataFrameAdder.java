/**
 * Copyright (c) 2025, SuperGrid Institute (https://www.supergrid-institute.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.AcDcConverter;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageSourceConverterAdder;
import com.powsybl.python.network.NetworkUtil;

import java.util.Collections;
import java.util.List;

import static com.powsybl.dataframe.network.adders.NetworkUtils.getVoltageLevelOrThrow;
import static com.powsybl.dataframe.network.adders.SeriesUtils.applyBooleanIfPresent;
import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Denis BONNAND {@literal <denis.bonnand at supergrid-institute.com>}
 */
public class VoltageSourceConverterDataFrameAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.strings("name"),
            SeriesMetadata.strings("voltage_level_id"),
            SeriesMetadata.strings("bus1_id"),
            SeriesMetadata.strings("bus2_id"),
            SeriesMetadata.strings("dc_node1_id"),
            SeriesMetadata.strings("dc_node2_id"),
            SeriesMetadata.strings("pcc_terminal_id"),
            SeriesMetadata.strings("connectable_bus1_id"),
            SeriesMetadata.strings("connectable_bus2_id"),
            SeriesMetadata.ints("dc_connected1"),
            SeriesMetadata.ints("dc_connected2"),
            SeriesMetadata.deprecatedBooleans("voltage_regulator_on"),
            SeriesMetadata.strings("control_mode"),
            SeriesMetadata.doubles("target_p"),
            SeriesMetadata.deprecatedDoubles("target_q"),
            SeriesMetadata.doubles("target_v_dc"),
            SeriesMetadata.deprecatedDoubles("target_v_ac"),
            SeriesMetadata.doubles("local_target_q"),
            SeriesMetadata.booleans("regulating"),
            SeriesMetadata.strings("regulation_mode"),
            SeriesMetadata.doubles("regulation_value"),
            SeriesMetadata.strings("regulated_element_id"),
            SeriesMetadata.doubles("local_target_v_ac"),
            SeriesMetadata.doubles("idle_loss"),
            SeriesMetadata.doubles("switching_loss"),
            SeriesMetadata.doubles("resistive_loss"),
            SeriesMetadata.doubles("min_p"),
            SeriesMetadata.doubles("max_p")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        VoltageSourceConverterDataFrameAdder.VoltageSourceConverterSeries series = new VoltageSourceConverterDataFrameAdder.VoltageSourceConverterSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.createVoltageSourceConverter(network, row);
        }
    }

    private static class VoltageSourceConverterSeries extends VoltageRegulationHolderSeries {

        protected final StringSeries voltageLevels;
        protected final StringSeries buses1;
        protected final StringSeries buses2;
        protected final StringSeries dcNodes1;
        protected final StringSeries dcNodes2;
        private final StringSeries connectableBuses1;
        private final StringSeries connectableBuses2;
        private final IntSeries dcConnected1;
        private final IntSeries dcConnected2;
        private final StringSeries pccTerminals;
        protected final IntSeries voltageRegulatorOn;
        protected final StringSeries controlMode;
        protected final DoubleSeries targetP;
        protected final DoubleSeries targetQ;
        protected final DoubleSeries targetVdc;
        protected final DoubleSeries targetVac;
        protected final DoubleSeries idleLoss;
        protected final DoubleSeries switchingLoss;
        protected final DoubleSeries resistiveLoss;
        protected final DoubleSeries minP;
        protected final DoubleSeries maxP;

        VoltageSourceConverterSeries(UpdatingDataframe dataframe) {
            super(dataframe);
            this.voltageLevels = dataframe.getStrings("voltage_level_id");
            if (voltageLevels == null) {
                throw new PowsyblException("voltage_level_id is missing");
            }
            this.buses1 = dataframe.getStrings("bus1_id");
            this.buses2 = dataframe.getStrings("bus2_id");
            this.dcNodes1 = dataframe.getStrings("dc_node1_id");
            this.dcNodes2 = dataframe.getStrings("dc_node2_id");
            this.connectableBuses1 = dataframe.getStrings("connectable_bus1_id");
            this.connectableBuses2 = dataframe.getStrings("connectable_bus2_id");
            this.dcConnected1 = dataframe.getInts("dc_connected1");
            this.dcConnected2 = dataframe.getInts("dc_connected2");
            this.pccTerminals = dataframe.getStrings("pcc_terminal_id");
            this.voltageRegulatorOn = dataframe.getInts("voltage_regulator_on");
            this.controlMode = dataframe.getStrings("control_mode");
            this.targetP = dataframe.getDoubles("target_p");
            this.targetQ = dataframe.getDoubles("target_q");
            this.targetVdc = dataframe.getDoubles("target_v_dc");
            this.targetVac = dataframe.getDoubles("target_v_ac");
            this.idleLoss = dataframe.getDoubles("idle_loss");
            this.switchingLoss = dataframe.getDoubles("switching_loss");
            this.resistiveLoss = dataframe.getDoubles("resistive_loss");
            this.minP = dataframe.getDoubles("min_p");
            this.maxP = dataframe.getDoubles("max_p");

            this.localTargetV = dataframe.getDoubles("local_target_v_ac");
        }

        void setVoltageSourceConverterAttributes(VoltageSourceConverterAdder adder, int row, Network network) {
            setIdentifiableAttributes(adder, row);
            applyIfPresent(connectableBuses1, row, connectableBusId1 -> {
                if (!connectableBusId1.isEmpty()) {
                    adder.setConnectableBus1(connectableBusId1);
                }
            });
            applyIfPresent(buses1, row, bus1 -> {
                if (!bus1.isEmpty()) {
                    adder.setBus1(bus1);
                }
            });
            applyIfPresent(connectableBuses2, row, connectableBusId2 -> {
                if (!connectableBusId2.isEmpty()) {
                    adder.setConnectableBus2(connectableBusId2);
                }
            });
            applyIfPresent(buses2, row, bus2 -> {
                if (!bus2.isEmpty()) {
                    adder.setBus2(bus2);
                }
            });
            applyIfPresent(dcNodes1, row, dcNode1 -> {
                if (!dcNode1.isEmpty()) {
                    adder.setDcNode1(dcNode1);
                    applyBooleanIfPresent(dcConnected1, row, adder::setDcConnected1);
                }
            });
            applyIfPresent(dcNodes2, row, dcNode2 -> {
                if (!dcNode2.isEmpty()) {
                    adder.setDcNode2(dcNode2);
                    applyBooleanIfPresent(dcConnected2, row, adder::setDcConnected2);
                }
            });
            applyIfPresent(pccTerminals, row, elementId -> NetworkUtil
                    .setTerminal(adder::setPccTerminal, network, elementId));
            applyIfPresent(controlMode, row, AcDcConverter.ControlMode.class, adder::setControlMode);
            applyIfPresent(targetP, row, adder::setTargetP);
            applyIfPresent(targetVdc, row, adder::setTargetVdc);
            applyIfPresent(idleLoss, row, adder::setIdleLoss);
            applyIfPresent(switchingLoss, row, adder::setSwitchingLoss);
            applyIfPresent(resistiveLoss, row, adder::setResistiveLoss);
            // Symmetric with the update path (NetworkDataframes): expose the "unbounded" state as -/+ inf,
            // mapping it back to the core's -/+ Double.MAX_VALUE sentinel. Finite values pass through unchanged
            // (creation dataframes are in SI, hence no per-unit conversion here).
            applyIfPresent(minP, row, value -> adder.setMinP(value == Double.NEGATIVE_INFINITY ? -Double.MAX_VALUE : value));
            applyIfPresent(maxP, row, value -> adder.setMaxP(value == Double.POSITIVE_INFINITY ? Double.MAX_VALUE : value));

            setVoltageRegulationAttributes(adder, network, row);

            // For compatibility with former regulation
            applyBooleanIfPresent(voltageRegulatorOn, row, adder::setVoltageRegulatorOn);
            applyIfPresent(targetVac, row, adder::setVoltageSetpoint);
            applyIfPresent(targetQ, row, adder::setReactivePowerSetpoint);
        }

        void createVoltageSourceConverter(Network network, int row) {
            VoltageSourceConverterAdder adder = getVoltageLevelOrThrow(network, voltageLevels.get(row))
                    .newVoltageSourceConverter();
            setIdentifiableAttributes(adder, row);
            setVoltageSourceConverterAttributes(adder, row, network);
            adder.add();
        }
    }
}
