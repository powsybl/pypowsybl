/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.contingency;

import com.powsybl.iidm.network.Network;

import java.nio.file.Path;
import java.util.List;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public interface ContingencyContainer {

    void addContingency(String contingencyId, List<String> elementIds);

    void readJsonContingency(Path pathToJsonFile, Network network);
}
