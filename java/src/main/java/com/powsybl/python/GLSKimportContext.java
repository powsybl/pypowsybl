/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.commons.PowsyblException;
import com.powsybl.glsk.api.AbstractGlskRegisteredResource;
import com.powsybl.glsk.ucte.UcteGlskDocument;
import com.powsybl.glsk.ucte.UcteGlskPoint;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Bertrand Rix {@literal <bertrand.rix at artelys.com>}
 */
public class GLSKimportContext {

    private UcteGlskDocument document;

    public void load(String filename) {
        try {
            document = UcteGlskDocument.importGlsk(new FileInputStream(filename));
        } catch (FileNotFoundException e) {
            throw new PowsyblException(e.getMessage());
        }
    }

    public List<String> getInjectionIdForCountry(String country, Instant instant) {
        UcteGlskPoint point = document.getGlskPointsForInstant(instant).get(country);
        List<String> generatorIds = new ArrayList<>();
        for (AbstractGlskRegisteredResource resource : point.getGlskShiftKeys().get(0).getRegisteredResourceArrayList()) {
            generatorIds.add(resource.getName());
        }
        return generatorIds;
    }

    public Instant getInjectionFactorStart() {
        return document.getGSKTimeInterval().getStart();
    }

    public Instant getInjectionFactorEnd() {
        return document.getGSKTimeInterval().getEnd();
    }

    public List<Double> getInjectionFactorForCountryTimeinterval(String country, Instant instant) {
        UcteGlskPoint point = document.getGlskPointsForInstant(instant).get(country);
        List<Double> factors = new ArrayList<>();
        for (AbstractGlskRegisteredResource resource : point.getGlskShiftKeys().get(0).getRegisteredResourceArrayList()) {
            factors.add(resource.getParticipationFactor());
        }
        return factors;
    }

    public List<String> getCountries() {
        return document.getZones();
    }
}
