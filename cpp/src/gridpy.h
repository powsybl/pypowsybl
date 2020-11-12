/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
#ifndef GRIDPY_H
#define GRIDPY_H

namespace gridpy {

    void init();

    void printVersion();

    void* createEmptyNetwork(const std::string& id);

    void* createIeee14Network();

    void runLoadFlow(void* network);
}

#endif //GRIDPY_H
