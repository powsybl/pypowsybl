/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.grid2op;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
class BackendTest {

    @Test
    void busNumTest() {
        // 3 voltage levels with 2 buses
        assertEquals(0, Backend.localToGlobalBusNum(3, 0, 1));
        assertEquals(1, Backend.localToGlobalBusNum(3, 1, 1));
        assertEquals(3, Backend.localToGlobalBusNum(3, 0, 2));
        assertEquals(4, Backend.localToGlobalBusNum(3, 1, 2));

        assertEquals(1, Backend.globalToLocalBusNum(3, 0));
        assertEquals(1, Backend.globalToLocalBusNum(3, 1));
        assertEquals(2, Backend.globalToLocalBusNum(3, 3));
        assertEquals(2, Backend.globalToLocalBusNum(3, 4));
    }
}
