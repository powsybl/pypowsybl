/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
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
import java.util.function.Predicate;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public final class NetworkAreaDiagramUtil {

    private NetworkAreaDiagramUtil() {
    }

    static void writeSvg(Network network, String voltageLevelId, int depth, Writer writer) {
        SvgParameters svgParameters = new SvgParameters()
                .setSvgWidthAndHeightAdded(true)
                .setFixedWidth(800)
                .setFixedHeight(600);
        Predicate<VoltageLevel> filter = voltageLevelId != null && voltageLevelId.length() > 0
                ? VoltageLevelFilter.createVoltageLevelDepthFilter(network, voltageLevelId, depth)
                : VoltageLevelFilter.NO_FILTER;
        new NetworkAreaDiagram(network, filter)
                .draw(writer, svgParameters);
    }

    static String getSvg(Network network, String voltageLevelId, int depth) {
        try (StringWriter writer = new StringWriter()) {
            writeSvg(network, voltageLevelId, depth, writer);
            return writer.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void writeSvg(Network network, String voltageLevelId, int depth, String svgFile) {
        try (Writer writer = Files.newBufferedWriter(Paths.get(svgFile), StandardCharsets.UTF_8)) {
            writeSvg(network, voltageLevelId, depth, writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
