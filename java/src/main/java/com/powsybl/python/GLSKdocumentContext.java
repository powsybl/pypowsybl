/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.commons.PowsyblException;
import com.powsybl.glsk.ucte.UcteGlskDocument;
import com.powsybl.iidm.network.Network;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Bertrand Rix {@literal <bertrand.rix at artelys.com>}
 */
public class GLSKdocumentContext {

    private UcteGlskDocument document;

    public void load(String filename) {
        try (FileInputStream fis = new FileInputStream(filename)) {
            document = UcteGlskDocument.importGlsk(fis);
        } catch (IOException e) {
            throw new PowsyblException(e.getMessage());
        }
    }

    public List<String> getInjectionIdForCountry(Network n, String country, Instant instant) {
        return new ArrayList<>(document.getZonalGlsks(n, instant).getData(country).getVariablesById().keySet());
    }

    public Instant getInjectionFactorStart() {
        return document.getGSKTimeInterval().getStart();
    }

    public Instant getInjectionFactorEnd() {
        return document.getGSKTimeInterval().getEnd();
    }

    public List<Double> getInjectionFactorForCountryTimeinterval(Network n, String country, Instant instant) {
        return document.getZonalGlsks(n, instant).getData(country).getVariablesById()
                .values().stream().map(e -> e.getWeight()).collect(Collectors.toList());
    }

    public List<String> getCountries() {
        return document.getZones();
    }
}
