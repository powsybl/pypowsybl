/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
*/
package com.powsybl.dataframe.shortcircuit.adders;

import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.python.shortcircuit.ShortCircuitAnalysisContext;

import java.util.List;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@soft.it>
 */
public interface ShortCircuitContextFaultAdder {

    List<SeriesMetadata> getMetadata();

    void addElements(ShortCircuitAnalysisContext context, UpdatingDataframe dataframe);

}
