/**
 * Copyright (c) 2021-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.extensions;

import com.google.auto.service.AutoService;
import com.powsybl.commons.PowsyblException;
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
                "[dataframe \"zones\"] index : id (str), target_v (float), bus_ids (str) / [dataframe \"units\"] index : id (str), participate (bool), zone_name (str)");
    }

    @Override
    public List<String> getExtensionTableNames() {
        return List.of("zones", "units");
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

    private ControlZone getControlZoneOrThrow(Network network, String name) {
        SecondaryVoltageControl ext = network.getExtension(SecondaryVoltageControl.class);
        if (ext == null) {
            throw new PowsyblException("Network " + network.getId() + " has no SecondaryVoltageControl extension.");
        }
        ControlZone zone = ext.getControlZones().stream()
                .filter(controlZone -> {
                    return controlZone.getName().equals(name);
                })
                .findAny()
                .orElse(null);
        if (zone == null) {
            throw new PowsyblException("No secondary voltage control zone named " + name + " found.");
        }
        return zone;
    }

    private ControlUnitWithZone getControlUnitWithZoneOrThrow(Network network, String id) {
        SecondaryVoltageControl ext = network.getExtension(SecondaryVoltageControl.class);
        if (ext == null) {
            throw new PowsyblException("Network " + network.getId() + " has no SecondaryVoltageControl extension.");
        }

        ControlZone zone = ext.getControlZones().stream()
                .filter(controlZone -> {
                    return  controlZone.getControlUnits().stream()
                        .filter(controlUnit -> {
                            return controlUnit.getId().equals(id);
                        })
                        .findAny()
                        .isPresent();
                })
                .findAny()
                .orElse(null);
        if (zone == null) {
            throw new PowsyblException("No secondary voltage control zone containing control unit " + id + " found.");
        }

        return new ControlUnitWithZone(zone.getControlUnits().stream().filter(controlUnit -> {
            return controlUnit.getId().equals(id);
        }).findAny().get(),
                zone.getName());
    }

    @Override
    public Map<String, NetworkDataframeMapper> createMappers() {
        Map<String, NetworkDataframeMapper> mappers = new HashMap<>();
        mappers.put("zones",
                NetworkDataframeMapperBuilder.ofStream(this::zonesStream, this::getControlZoneOrThrow)
                        .stringsIndex("id", ControlZone::getName)
                        .doubles("target_v", zone -> zone.getPilotPoint().getTargetV(), (zone, v) -> zone.getPilotPoint().setTargetV(v))
                        .strings("bus_ids", zone -> String.join(",", zone.getPilotPoint().getBusbarSectionsOrBusesIds()))
                        .build()
        );
        mappers.put("units",
                NetworkDataframeMapperBuilder.ofStream(this::unitsStream, this::getControlUnitWithZoneOrThrow)
                        .stringsIndex("id", unit -> unit.getUnit().getId())
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

    private static class ControlUnitWithZone {
        private final SecondaryVoltageControl.ControlUnit unit;
        private final String zoneName;

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
