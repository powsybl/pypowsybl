/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Network;
import com.powsybl.sld.NetworkGraphBuilder;
import com.powsybl.sld.SubstationDiagram;
import com.powsybl.sld.VoltageLevelDiagram;
import com.powsybl.sld.layout.*;
import com.powsybl.sld.library.ComponentLibrary;
import com.powsybl.sld.library.ConvergenceComponentLibrary;
import com.powsybl.sld.svg.DefaultDiagramLabelProvider;
import com.powsybl.sld.svg.DefaultSVGWriter;
import com.powsybl.sld.util.TopologicalStyleProvider;

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
        ComponentLibrary componentLibrary = new ConvergenceComponentLibrary();
        LayoutParameters layoutParameters = new LayoutParameters()
                .setCssLocation(LayoutParameters.CssLocation.INSERTED_IN_SVG)
                .setAdaptCellHeightToContent(true)
                .setSvgWidthAndHeightAdded(true);
        if (network.getVoltageLevel(containerId) != null) {
            VoltageLevelLayoutFactory voltageLevelLayoutFactory = new SmartVoltageLevelLayoutFactory(network);
            VoltageLevelDiagram voltageLevelDiagram = VoltageLevelDiagram.build(new NetworkGraphBuilder(network), containerId, voltageLevelLayoutFactory, false);
            voltageLevelDiagram.writeSvg("",
                    new DefaultSVGWriter(componentLibrary, layoutParameters),
                    new DefaultDiagramLabelProvider(network, componentLibrary, layoutParameters),
                    new TopologicalStyleProvider(network),
                    writer, new StringWriter());
        } else if (network.getSubstation(containerId) != null) {
            SubstationLayoutFactory substationLayoutFactory = new HorizontalSubstationLayoutFactory();
            VoltageLevelLayoutFactory voltageLevelLayoutFactory = new SmartVoltageLevelLayoutFactory(network);
            SubstationDiagram substationDiagram = SubstationDiagram.build(new NetworkGraphBuilder(network), containerId, substationLayoutFactory, voltageLevelLayoutFactory, false);
            substationDiagram.writeSvg("",
                    new DefaultSVGWriter(componentLibrary, layoutParameters),
                    new DefaultDiagramLabelProvider(network, componentLibrary, layoutParameters),
                    new TopologicalStyleProvider(network),
                    writer, new StringWriter());
        } else {
            throw new PowsyblException("Container '" + containerId + "' not found");
        }
    }
}
