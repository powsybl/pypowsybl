/**
 * Copyright (c) 2020-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

/**
 * @author Nicolas Pierre <nicolas.pierre@artelys.com>
 */
public class GeneratorSynchronousThreeWindingsProportionalRegulationsAdder extends AbstractBlackBoxAdder {

    @Override
    protected AddBlackBoxToModelMapping getAddBlackBoxToModelMapping() {
        return (modelMapping, staticId, parameterSetId) -> modelMapping
                .addGeneratorSynchronousThreeWindingsProportionalRegulations(staticId, parameterSetId);
    }

}
