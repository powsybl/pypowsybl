/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.network;

import com.powsybl.iidm.network.*;
import com.powsybl.nad.NadParameters;
import com.powsybl.nad.NetworkAreaDiagram;
import com.powsybl.nad.build.iidm.VoltageLevelFilter;
import com.powsybl.nad.svg.SvgParameters;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public final class NetworkAreaDiagramUtil {

    private NetworkAreaDiagramUtil() {
    }

    static void writeSvg(Network network, List<String> voltageLevelIds, int depth, Writer writer,
                         double nominalVoltageUpperBound, double nominalVoltageLowerBound, NadParameters nadParameters) {

        Predicate<VoltageLevel> filter = !voltageLevelIds.isEmpty()
                ? getNominalVoltageFilter(network, voltageLevelIds, nominalVoltageLowerBound, nominalVoltageUpperBound, depth)
                : getNominalVoltageFilter(network, nominalVoltageLowerBound, nominalVoltageUpperBound);
        NetworkAreaDiagram.draw(network, writer, nadParameters, filter);
    }

    static String getSvg(Network network, List<String> voltageLevelIds, NadParameters nadParameters) {
        return getSvg(network, voltageLevelIds, 0, -1, -1, nadParameters);
    }

    static String getSvg(Network network, List<String> voltageLevelIds, int depth,
                         double nominalVoltageUpperBound, double nominalVoltageLowerBound, NadParameters nadParameters) {
        try (StringWriter writer = new StringWriter()) {
            writeSvg(network, voltageLevelIds, depth, writer, nominalVoltageUpperBound, nominalVoltageLowerBound, nadParameters);
            return writer.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void writeSvg(Network network, List<String> voltageLevelIds, int depth, String svgFile,
                         Double nominalVoltageUpperBound, Double nominalVoltageLowerBound, NadParameters nadParameters) {
        try (Writer writer = Files.newBufferedWriter(Paths.get(svgFile), StandardCharsets.UTF_8)) {
            writeSvg(network, voltageLevelIds, depth, writer, nominalVoltageUpperBound, nominalVoltageLowerBound, nadParameters);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static NadParameters createNadParameters() {
        SvgParameters svgParameters = new SvgParameters()
                .setSvgWidthAndHeightAdded(true)
                .setFixedWidth(800)
                .setFixedHeight(600);
        return new NadParameters()
                .setSvgParameters(svgParameters);
    }

    static VoltageLevelFilter getNominalVoltageFilter(Network network, List<String> voltageLevelIds, double nominalVoltageLowerBound, double nominalVoltageUpperBound, int depth) {
        if (nominalVoltageLowerBound >= 0 && nominalVoltageUpperBound >= 0) {
            return VoltageLevelFilter.createNominalVoltageFilter(network, voltageLevelIds, nominalVoltageLowerBound, nominalVoltageUpperBound, depth);
        } else if (nominalVoltageLowerBound < 0 && nominalVoltageUpperBound >= 0) {
            return VoltageLevelFilter.createNominalVoltageUpperBoundFilter(network, voltageLevelIds, nominalVoltageUpperBound, depth);
        } else if (nominalVoltageLowerBound >= 0 && nominalVoltageUpperBound < 0) {
            return VoltageLevelFilter.createNominalVoltageLowerBoundFilter(network, voltageLevelIds, nominalVoltageLowerBound, depth);
        } else {
            return VoltageLevelFilter.createVoltageLevelsDepthFilter(network, voltageLevelIds, depth);
        }
    }

    static VoltageLevelFilter getNominalVoltageFilter(Network network, double nominalVoltageLowerBound, double nominalVoltageUpperBound) {
        if (nominalVoltageLowerBound >= 0 && nominalVoltageUpperBound >= 0) {
            return VoltageLevelFilter.createNominalVoltageFilter(network, nominalVoltageLowerBound, nominalVoltageUpperBound);
        } else if (nominalVoltageLowerBound < 0 && nominalVoltageUpperBound >= 0) {
            return VoltageLevelFilter.createNominalVoltageUpperBoundFilter(network, nominalVoltageUpperBound);
        } else if (nominalVoltageLowerBound >= 0 && nominalVoltageUpperBound < 0) {
            return VoltageLevelFilter.createNominalVoltageLowerBoundFilter(network, nominalVoltageLowerBound);
        } else {
            return VoltageLevelFilter.createNominalVoltageFilterWithPredicate(network, VoltageLevelFilter.NO_FILTER);
        }
    }

    public static List<String> getDisplayedVoltageLevels(Network network, List<String> voltageLevelIds, int depth) {
        return NetworkAreaDiagram.getDisplayedVoltageLevels(network, voltageLevelIds, depth);
    }
}
