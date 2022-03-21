/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.DataframeElementType;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;
import com.powsybl.python.LimitsDataframeAdderKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.powsybl.dataframe.DataframeElementType.*;

/**
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public final class NetworkElementAdders {

    private static final Map<DataframeElementType, NetworkElementAdder> ADDERS = Map.ofEntries(
            Map.entry(BUS, new BusDataframeAdder()),
            Map.entry(GENERATOR, new GeneratorDataframeAdder()),
            Map.entry(LINE, new LineDataframeAdder()),
            Map.entry(STATIC_VAR_COMPENSATOR, new SvcDataframeAdder()),
            Map.entry(TWO_WINDINGS_TRANSFORMER, new TwtDataframeAdder()),
            Map.entry(LOAD, new LoadDataframeAdder()),
            Map.entry(VSC_CONVERTER_STATION, new VscDataframeAdder()),
            Map.entry(LCC_CONVERTER_STATION, new LccDataframeAdder()),
            Map.entry(BUSBAR_SECTION, new BusBarDataframeAdder()),
            Map.entry(DANGLING_LINE, new DanglingLineDataframeAdder()),
            Map.entry(VOLTAGE_LEVEL, new VoltageLevelDataframeAdder()),
            Map.entry(SUBSTATION, new SubstationDataframeAdder()),
            Map.entry(HVDC_LINE, new HvdcDataframeAdder()),
            Map.entry(BATTERY, new BatteryDataframeAdder()),
            Map.entry(SWITCH, new SwitchDataframeAdder()),
            Map.entry(SHUNT_COMPENSATOR, new ShuntDataframeAdder()),
            Map.entry(PHASE_TAP_CHANGER, new PhaseTapChangerDataframeAdder()),
            Map.entry(RATIO_TAP_CHANGER, new RatioTapChangerDataframeAdder()),
            Map.entry(OPERATIONAL_LIMITS, new LimitsDataframeAdder())
    );

    private NetworkElementAdders() {
    }

    public static NetworkElementAdder getAdder(DataframeElementType type) {
        return ADDERS.get(type);
    }

    public static List<NetworkElementAdder> getAdders() {
        return List.copyOf(ADDERS.values());
    }

    public static void addElements(DataframeElementType type, Network network, List<UpdatingDataframe> dfs) {
        NetworkElementAdder adder = ADDERS.get(type);
        if (adder == null) {
            throw new PowsyblException("Creation not implemented for type " + type.name());
        }
        if (type == OPERATIONAL_LIMITS) {
            UpdatingDataframe primaryTable = dfs.get(0);
            Map<LimitsDataframeAdderKey, List<Integer>> indexMap = new HashMap<>();
            for (int i = 0; i < primaryTable.getLineCount(); i++) {
                String elementId = primaryTable.getStringValue("element_id", i)
                        .orElseThrow(() -> new PowsyblException("element id is missing"));
                String side = primaryTable.getStringValue("side", i)
                        .orElseThrow(() -> new PowsyblException("side is missing"));
                String limitType = primaryTable.getStringValue("type", i)
                        .orElseThrow(() -> new PowsyblException("type is missing"));
                LimitsDataframeAdderKey key = new LimitsDataframeAdderKey(elementId, side, limitType);
                if (!indexMap.containsKey(key)) {
                    indexMap.put(key, new ArrayList<>());
                }
                indexMap.get(key).add(i);
            }

            ((LimitsDataframeAdder) adder).addElements(network, primaryTable, indexMap);

        } else {
            UpdatingDataframe primaryTable = dfs.get(0);
            for (int i = 0; i < primaryTable.getLineCount(); i++) {
                adder.addElement(network, dfs, i);
            }
        }
    }
}
