/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.gridpy;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
class NetworkUtilTest {

    @Test
    void test() {
        Network network = EurostagTutorialExample1Factory.create();
        List<String> elementsIds = NetworkUtil.getElementsIds(network, GridPyApiHeader.ElementType.TWO_WINDINGS_TRANSFORMER, Collections.singleton(24.0), Collections.singleton("FR"), true, true, false);
        assertEquals(Collections.singletonList("NGEN_NHV1"), elementsIds);
    }
}
