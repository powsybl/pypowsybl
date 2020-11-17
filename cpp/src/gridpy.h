/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
#ifndef GRIDPY_H
#define GRIDPY_H

#include <string>
#include "classes/gridpy-api.h"

namespace gridpy {

    struct LoadFlowResult {

        load_flow_result* ptr_;

        explicit LoadFlowResult(load_flow_result* ptr)
          : ptr_(ptr) {
        }

        bool isOk() const { return ptr_->ok; }

        ~LoadFlowResult();
    };

    void init();

    void printVersion();

    void* createEmptyNetwork(const std::string& id);

    void* createIeee14Network();

    void* loadNetwork(const std::string& file);

    LoadFlowResult* runLoadFlow(void* network, bool distributedSlack, bool dc);

    void destroyObjectHandle(void* objectHandle);
}

#endif //GRIDPY_H
