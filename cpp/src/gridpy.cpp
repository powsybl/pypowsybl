/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
#include "gridpy.h"
#include "gridpy-java.h"
#include <iostream>

namespace gridpy {

graal_isolate_t* isolate = nullptr;

void init() {
    graal_isolatethread_t* thread = nullptr;

    if (graal_create_isolate(nullptr, &isolate, &thread) != 0) {
        throw std::runtime_error("graal_create_isolate error");
    }
}

class GraalVmGuard {
public:
    GraalVmGuard() {
        if (!isolate) {
            throw std::runtime_error("isolate has not been created");
        }
        if (graal_attach_thread(isolate, &thread_) != 0) {
            throw std::runtime_error("graal_create_isolate error");
        }
    }

    ~GraalVmGuard() noexcept(false) {
        if (graal_detach_thread(thread_) != 0) {
            throw std::runtime_error("graal_detach_thread error");
        }
    }

    graal_isolatethread_t * thread() const {
        return thread_;
    }

private:
    graal_isolatethread_t* thread_ = nullptr;
};

LoadFlowResult::~LoadFlowResult() {
    GraalVmGuard guard;
    freeLoadFlowResultPointer(guard.thread(), ptr_);
}

BusArray::~BusArray() {
    GraalVmGuard guard;
    freeBusArray(guard.thread(), delegate_);
}

void printVersion() {
    GraalVmGuard guard;
    printVersion(guard.thread());
}

void* createEmptyNetwork(const std::string& id) {
    GraalVmGuard guard;
    return createEmptyNetwork(guard.thread(), (char*) id.data());
}

void* createIeee14Network() {
    GraalVmGuard guard;
    return createIeee14Network(guard.thread());
}

void* loadNetwork(const std::string& file) {
    GraalVmGuard guard;
    return loadNetwork(guard.thread(), (char*) file.data());
}

void updateSwitchPosition(void* network, const std::string& id, bool open) {
    GraalVmGuard guard;
    updateSwitchPosition(guard.thread(), network, (char*) id.data(), open);
}

void updateConnectableStatus(void* network, const std::string& id, bool connected) {
    GraalVmGuard guard;
    updateConnectableStatus(guard.thread(), network, (char*) id.data(), connected);
}

LoadFlowResult* runLoadFlow(void* network, bool distributedSlack, bool dc) {
    GraalVmGuard guard;
    return new LoadFlowResult(runLoadFlow(guard.thread(), network, distributedSlack, dc));
}

BusArray* getBusArray(void* network, bool busBreakerView) {
    GraalVmGuard guard;
    return new BusArray(getBusArray(guard.thread(), network, busBreakerView));
}

void destroyObjectHandle(void* objectHandle) {
    GraalVmGuard guard;
    destroyObjectHandle(guard.thread(), objectHandle);
}

}
