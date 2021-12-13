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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public final class NetworkAreaDiagramUtil {

    private NetworkAreaDiagramUtil() {
    }

    static void writeSvg(Network network, Writer writer) {
        SvgParameters svgParameters = new SvgParameters()
                .setSvgWidthAndHeightAdded(true)
                .setFixedWidth(800)
                .setFixedHeight(600);
        LayoutParameters layoutParameters = new LayoutParameters();
        InputStream is = Objects.requireNonNull(PyPowsyblApiLib.class.getResourceAsStream("/nad-base-voltages.yml"));
        StyleProvider styleProvider = new DefaultStyleProvider(BaseVoltagesConfig.fromInputStream(is));
        new NetworkAreaDiagram(network)
                .draw(writer, svgParameters, layoutParameters, styleProvider);
    }

    static String getSvg(Network network) {
        try (StringWriter writer = new StringWriter()) {
            writeSvg(network, writer);
            return writer.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void writeSvg(Network network, String svgFile) {
        try (Writer writer = Files.newBufferedWriter(Paths.get(svgFile), StandardCharsets.UTF_8)) {
            writeSvg(network, writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
