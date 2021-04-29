/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
interface SensitivityAnalysisResultContext {

    PyPowsyblApiHeader.MatrixPointer createBranchFlowsSensitivityMatrix(String contingencyId);

    PyPowsyblApiHeader.MatrixPointer createBusVoltagesSensitivityMatrix(String contingencyId);

    PyPowsyblApiHeader.MatrixPointer createReferenceFlows(String contingencyId);

    PyPowsyblApiHeader.MatrixPointer createReferenceVoltages(String contingencyId);
}
