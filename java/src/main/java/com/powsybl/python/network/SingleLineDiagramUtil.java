/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.network;

import com.powsybl.iidm.network.Network;
import com.powsybl.sld.SingleLineDiagram;
import com.powsybl.sld.SldParameters;
import com.powsybl.sld.library.ComponentLibrary;

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

    static void writeSvg(Network network, String containerId, String svgFile, String metadataFile, SldParameters sldParameters) {
        try (Writer writer = Files.newBufferedWriter(Paths.get(svgFile));
             Writer metadataWriter = metadataFile == null || metadataFile.isEmpty() ? new StringWriter() : Files.newBufferedWriter(Paths.get(metadataFile))) {
            writeSvg(network, containerId, writer, metadataWriter, sldParameters);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void writeMatrixMultiSubstationSvg(Network network, String[][] matrixIds, String svgFile, String metadataFile, SldParameters sldParameters) {
        try (Writer writer = Files.newBufferedWriter(Paths.get(svgFile));
             Writer metadataWriter = metadataFile == null || metadataFile.isEmpty() ? new StringWriter() : Files.newBufferedWriter(Paths.get(metadataFile))) {
            writeMatrixMultiSubstationSvg(network, matrixIds, writer, metadataWriter, sldParameters);
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

    static List<String> getSvgAndMetadata(Network network, String containerId, SldParameters sldParameters) {
        try (StringWriter writer = new StringWriter(); StringWriter writerMeta = new StringWriter()) {
            writeSvg(network, containerId, writer, writerMeta, sldParameters);
            writer.flush();
            writerMeta.flush();
            return List.of(writer.toString(), writerMeta.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static SldParameters createSldParameters() {
        SldParameters sldParameters = new SldParameters();
        sldParameters.getSvgParameters().setSvgWidthAndHeightAdded(true);
        return sldParameters;
    }

    static void writeSvg(Network network, String containerId, Writer writer) {
        writeSvg(network, containerId, writer, new StringWriter(), createSldParameters());
    }

    static void writeSvg(Network network, String containerId, Writer writer, Writer metadataWriter, SldParameters sldParameters) {
        SingleLineDiagram.draw(network, containerId, writer, metadataWriter, sldParameters);
    }

    static void writeMatrixMultiSubstationSvg(Network network, String[][] matrixIds, Writer writer, Writer metadataWriter, SldParameters sldParameters) {
        sldParameters.setZoneLayoutFactory(new MatrixzoneLayoutFactory(matrixIds));
        SingleLineDiagram.drawMultiSubstations(network, substationIds, writer, metadataWriter, sldParameters);
    }

    static List<String> getComponentLibraryNames() {
        return ComponentLibrary.findAll()
                .stream()
                .map(ComponentLibrary::getName)
                .toList();
    }
}
