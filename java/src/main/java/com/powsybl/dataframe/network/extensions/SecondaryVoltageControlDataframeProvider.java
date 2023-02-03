/**
 * Copyright (c) 2021-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL-2.0/.
 */
package com.powsybl.dataframe.network.extensions;

import com.google.auto.service.AutoService;
import com.powsybl.dataframe.network.ExtensionInformation;
import com.powsybl.dataframe.network.NetworkDataframeMapper;
import com.powsybl.dataframe.network.NetworkDataframeMapperBuilder;
import com.powsybl.dataframe.network.adders.NetworkElementAdder;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.SecondaryVoltageControl;
import com.powsybl.iidm.network.extensions.SecondaryVoltageControl.ControlZone;

import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * @author Hugo Kulesza <hugo.kulesza@rte-france.com>
 */
@AutoService(NetworkExtensionDataframeProvider.class)
public class SecondaryVoltageControlDataframeProvider implements NetworkExtensionDataframeProvider {

    @Override
    public String getExtensionName() {
        return SecondaryVoltageControl.NAME;
    }

    @Override
    public ExtensionInformation getExtensionInformation() {
        return new ExtensionInformation(SecondaryVoltageControl.NAME,
                "Provides information about the secondary voltage control zones and units, in two distinct dataframes.",
                "[dataframe \"zones\"] index : name (str), target_v (float), bus_ids (str) / [dataframe \"units\"] index : unit_id (str), participate (bool), zone_name (str)");
    }

    @Override
    public List<Optional<String>> getExtensionTableNames() {
        return List.of(Optional.of("zones"), Optional.of("units"));
    }

    private Stream<ControlZone> zonesStream(Network network) {
        return network.getExtension(SecondaryVoltageControl.class)
                .getControlZones().stream();
    }

    private Stream<ControlUnitWithZone> unitsStream(Network network) {
        List<ControlUnitWithZone> units = new ArrayList<>();
        network.getExtension(SecondaryVoltageControl.class)
                .getControlZones()
                .forEach(zone -> {
                    units.addAll(zone.getControlUnits()
                            .stream()
                            .map(unit -> {
                                return new ControlUnitWithZone(unit, zone.getName());
                            })
                            .collect(toList())
                    );
                });
        return units.stream();
    }

    @Override
    public Map<Optional<String>, NetworkDataframeMapper> createMappers() {
        Map<Optional<String>, NetworkDataframeMapper> mappers = new HashMap<>();
        mappers.put(Optional.of("zones"),
                NetworkDataframeMapperBuilder.ofStream(this::zonesStream)
                        .stringsIndex("name", ControlZone::getName)
                        .doubles("target_v", zone -> zone.getPilotPoint().getTargetV(), (zone, v) -> zone.getPilotPoint().setTargetV(v))
                        .strings("bus_ids", zone -> zone.getPilotPoint().getBusbarSectionsOrBusesIds().get(0)) // TODO : parser liste ids
                        .build()
        );
        mappers.put(Optional.of("units"),
                NetworkDataframeMapperBuilder.ofStream(this::unitsStream)
                        .stringsIndex("unit_id", unit -> unit.getUnit().getId())
                        .booleans("participate", unit -> unit.getUnit().isParticipate(), (unit, b) -> unit.getUnit().setParticipate(b))
                        .strings("zone_name", ControlUnitWithZone::getZoneName)
                        .build()
        );
        return mappers;
    }

    @Override
    public void removeExtensions(Network network, List<String> ids) {
        network.removeExtension(SecondaryVoltageControl.class);
    }

    @Override
    public NetworkElementAdder createAdder() {
        return new SecondaryVoltageControlDataframeAdder();
    }

    private class ControlUnitWithZone {
        private SecondaryVoltageControl.ControlUnit unit;
        private String zoneName;

        public ControlUnitWithZone(SecondaryVoltageControl.ControlUnit unit, String zoneName) {
            this.unit = unit;
            this.zoneName = zoneName;
        }

        public SecondaryVoltageControl.ControlUnit getUnit() {
            return unit;
        }

        public String getZoneName() {
            return zoneName;
        }
    }
}
