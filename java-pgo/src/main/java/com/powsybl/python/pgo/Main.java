/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.pgo;

import com.powsybl.commons.datasource.ResourceDataSource;
import com.powsybl.commons.datasource.ResourceSet;
import com.powsybl.iidm.network.Network;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Network network = Network.read(new ResourceDataSource("CGMES_v2.4.15_RealGridTestConfiguration",
                new ResourceSet("/data/realGrid", "CGMES_v2.4.15_RealGridTestConfiguration_EQ_V2.xml",
                                                  "CGMES_v2.4.15_RealGridTestConfiguration_SSH_V2.xml",
                                                  "CGMES_v2.4.15_RealGridTestConfiguration_SV_V2.xml",
                                                  "CGMES_v2.4.15_RealGridTestConfiguration_TP_V2.xml")));
    }
}
