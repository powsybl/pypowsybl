/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

/**
 * Provides mapping between java classes and dataframe (=table) based views.
 *
 * Mapping is defined by instances of {@link com.powsybl.dataframe.DataframeMapper}.
 *
 * {@link com.powsybl.dataframe.DataframeHandler} interface allows to define what to do with the data,
 * and {@link com.powsybl.dataframe.IndexedSeries} and its primitive variants allow to provide
 * some input series to update the data.
 *
 * @author Sylvain Leclerc {@literal <sylvain.leclerc at rte-france.com>}
 */
package com.powsybl.dataframe;
