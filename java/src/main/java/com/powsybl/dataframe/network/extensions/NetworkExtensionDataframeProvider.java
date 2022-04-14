/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network.extensions;

import com.powsybl.dataframe.network.NetworkDataframeMapper;

/**
 * SPI for defining dataframes of network extensions.
 *
 * @author Christian Biasuzzi <christian.biasuzzi@soft.it>
 */
public interface NetworkExtensionDataframeProvider {

    /**
     * The extension name. Should match the IIDM extension name as defined in the extension class.
     */
    String getExtensionName();

    /**
     * Defines the mapping between the network and the extension dataframe.
     */
    NetworkDataframeMapper createMapper();
}
