/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.network;

import com.powsybl.iidm.network.Network;
import com.powsybl.sld.SingleLineDiagram;
import com.powsybl.sld.layout.LayoutParameters;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public final class SingleLineDiagramUtil {

    private SingleLineDiagramUtil() {
    }

    static void writeSvg(Network network, String containerId, String svgFile) {
        try (Writer writer = Files.newBufferedWriter(Paths.get(svgFile))) {
            writeSvg(network, containerId, writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String getSvg(Network network, String containerId) {
        try (StringWriter writer = new StringWriter()) {
            writeSvg(network, containerId, writer);
            writer.flush();
            return writer.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void writeSvg(Network network, String containerId, Writer writer) {
        LayoutParameters layoutParameters = new LayoutParameters()
                .setSvgWidthAndHeightAdded(true);
        SingleLineDiagram.draw(network, containerId, writer, new StringWriter(), layoutParameters);
    }
}
