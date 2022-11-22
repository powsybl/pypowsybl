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
import com.powsybl.sld.library.ComponentLibrary;
import com.powsybl.sld.library.ConvergenceComponentLibrary;
import com.powsybl.sld.svg.DefaultDiagramLabelProvider;
import com.powsybl.sld.svg.DiagramStyleProvider;
import com.powsybl.sld.util.NominalVoltageDiagramStyleProvider;
import com.powsybl.sld.util.TopologicalStyleProvider;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

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

    static List<String> getSvgAndMetadata(Network network, String containerId, boolean useName, boolean centerName, boolean diagonalLabel, boolean topologicalColoring) {
        try (StringWriter writer = new StringWriter(); StringWriter writerMeta = new StringWriter()) {
            LayoutParameters layoutParameters = new LayoutParameters()
                    .setUseName(useName)
                    .setLabelCentered(centerName)
                    .setLabelDiagonal(diagonalLabel)
                    .setSvgWidthAndHeightAdded(true);
            DiagramStyleProvider styleProvider = topologicalColoring ? new TopologicalStyleProvider(network)
                    : new NominalVoltageDiagramStyleProvider(network);

            writeSvg(network, containerId, writer, writerMeta, layoutParameters, styleProvider);
            writer.flush();
            writerMeta.flush();
            return List.of(writer.toString(), writerMeta.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void writeSvg(Network network, String containerId, Writer writer) {
        writeSvg(network, containerId, writer, new StringWriter());
    }

    static void writeSvg(Network network, String containerId, Writer writer, Writer metadataWriter) {
        LayoutParameters layoutParameters = new LayoutParameters()
                .setSvgWidthAndHeightAdded(true);
        writeSvg(network, containerId, writer, metadataWriter, layoutParameters, new TopologicalStyleProvider(network));
    }

    static void writeSvg(Network network, String containerId, Writer writer, Writer metadataWriter, LayoutParameters layoutParameters, DiagramStyleProvider diagramStyleProvider) {
        ComponentLibrary componentLibrary = new ConvergenceComponentLibrary();
        SingleLineDiagram.draw(network, containerId, writer, metadataWriter, layoutParameters, componentLibrary,
                new DefaultDiagramLabelProvider(network, componentLibrary, layoutParameters), diagramStyleProvider, "");
    }

}
