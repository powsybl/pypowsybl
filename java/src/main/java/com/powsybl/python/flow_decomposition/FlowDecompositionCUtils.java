/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.flow_decomposition;

import com.powsybl.flow_decomposition.FlowDecompositionParameters;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.commons.PyPowsyblConfiguration;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public final class FlowDecompositionCUtils {

    private FlowDecompositionCUtils() {
    }

    public static FlowDecompositionParameters createFlowDecompositionParameters() {
        return PyPowsyblConfiguration.isReadConfig() ? FlowDecompositionParameters.load() : new FlowDecompositionParameters();
    }

    public static FlowDecompositionParameters createFlowDecompositionParameters(PyPowsyblApiHeader.FlowDecompositionParametersPointer loadFlowParametersPtr) {
        return createFlowDecompositionParameters()
            .setEnableLossesCompensation(loadFlowParametersPtr.isLossesCompensationEnabled())
            .setLossesCompensationEpsilon(loadFlowParametersPtr.getLossesCompensationEpsilon())
            .setSensitivityEpsilon(loadFlowParametersPtr.getSensitivityEpsilon())
            .setsetRescaleEnabled(loadFlowParametersPtr.isRescaleEnabled())
            .setDcFallbackEnabledAfterAcDivergence(loadFlowParametersPtr.isDcFallbackEnabledAfterAcDivergence())
            .setSensitivityVariableBatchSize(loadFlowParametersPtr.getSensitivityVariableBatchSize());
    }
}
