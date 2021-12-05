/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.commons.config.BaseVoltagesConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.nad.NetworkAreaDiagram;
import com.powsybl.nad.layout.LayoutParameters;
import com.powsybl.nad.svg.DefaultStyleProvider;
import com.powsybl.nad.svg.StyleProvider;
import com.powsybl.nad.svg.SvgParameters;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public final class NetworkAreaDiagramUtil {

    private NetworkAreaDiagramUtil() {
    }

    static String getSvg(Network network) {
        SvgParameters svgParameters = new SvgParameters()
                .setSvgWidthAndHeightAdded(true)
                .setFixedWidth(800)
                .setFixedHeight(600);
        LayoutParameters layoutParameters = new LayoutParameters();
        StyleProvider styleProvider = new DefaultStyleProvider(BaseVoltagesConfig.fromInputStream(PyPowsyblApiLib.class.getResourceAsStream("/nad-base-voltages.yml")));
        try (StringWriter writer = new StringWriter()) {
            new NetworkAreaDiagram(network)
                    .draw(writer, svgParameters, layoutParameters, styleProvider);
            return writer.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
