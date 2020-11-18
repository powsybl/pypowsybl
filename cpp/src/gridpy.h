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

class LoadFlowResult {
public:
    explicit LoadFlowResult(load_flow_result* ptr)
      : ptr_(ptr) {
    }

    bool isOk() const { return ptr_->ok; }

    ~LoadFlowResult();

private:
    load_flow_result* ptr_;
};

class BusArray {
public:
    explicit BusArray(bus_array* delegate)
      : delegate_(delegate) {
    }

    int length() const { return delegate_->length; }

    bus* begin() const { return delegate_->ptr; }

    bus* end() const { return delegate_->ptr + delegate_->length; }

    ~BusArray();

private:
    bus_array* delegate_;
};

void init();

void printVersion();

void* createEmptyNetwork(const std::string& id);

void* createIeee14Network();

void* loadNetwork(const std::string& file);

LoadFlowResult* runLoadFlow(void* network, bool distributedSlack, bool dc);

BusArray* getBusArray(void* network, bool busBreakerView);

void destroyObjectHandle(void* objectHandle);

}

#endif //GRIDPY_H
