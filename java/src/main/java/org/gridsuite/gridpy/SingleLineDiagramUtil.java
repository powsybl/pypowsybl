/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.gridpy;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Network;
import com.powsybl.sld.NetworkGraphBuilder;
import com.powsybl.sld.VoltageLevelDiagram;
import com.powsybl.sld.layout.LayoutParameters;
import com.powsybl.sld.layout.SmartVoltageLevelLayoutFactory;
import com.powsybl.sld.layout.VoltageLevelLayoutFactory;
import com.powsybl.sld.library.ComponentLibrary;
import com.powsybl.sld.library.ResourcesComponentLibrary;
import com.powsybl.sld.svg.DefaultDiagramLabelProvider;
import com.powsybl.sld.svg.DefaultSVGWriter;
import com.powsybl.sld.util.TopologicalStyleProvider;

import java.nio.file.Paths;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public final class SingleLineDiagramUtil {

    private SingleLineDiagramUtil() {
    }

    static void writeSvg(Network network, String containerId, String svgFile) {
        ComponentLibrary componentLibrary = new ResourcesComponentLibrary("/ConvergenceLibrary");
        if (network.getVoltageLevel(containerId) != null) {
            VoltageLevelLayoutFactory voltageLevelLayoutFactory = new SmartVoltageLevelLayoutFactory(network);
            VoltageLevelDiagram voltageLevelDiagram = VoltageLevelDiagram.build(new NetworkGraphBuilder(network), containerId, voltageLevelLayoutFactory, false);
            LayoutParameters layoutParameters = new LayoutParameters()
                    .setCssInternal(true)
                    .setAdaptCellHeightToContent(true);
            voltageLevelDiagram.writeSvg("",
                    new DefaultSVGWriter(componentLibrary, layoutParameters),
                    new DefaultDiagramLabelProvider(network, componentLibrary, layoutParameters),
                    new TopologicalStyleProvider(network),
                    Paths.get(svgFile));
        } else {
            throw new PowsyblException("Container '" + containerId + "' not found");
        }
    }
}
