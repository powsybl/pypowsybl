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

template<>
Array<load_flow_component_result>::~Array() {
    GraalVmGuard guard;
    freeLoadFlowComponentResultPointer(guard.thread(), delegate_);
}

template<>
Array<bus>::~Array() {
    GraalVmGuard guard;
    freeBusArray(guard.thread(), delegate_);
}

template<>
Array<security_analysis_result>::~Array() {
    GraalVmGuard guard;
    freeSecurityAnalysisResultArrayPointer(guard.thread(), delegate_);
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

void dumpNetwork(void* network, const std::string& file, const std::string& format) {
    GraalVmGuard guard;
    dumpNetwork(guard.thread(), network, (char*) file.data(), (char*) format.data());
}

bool updateSwitchPosition(void* network, const std::string& id, bool open) {
    GraalVmGuard guard;
    return updateSwitchPosition(guard.thread(), network, (char*) id.data(), open);
}

bool updateConnectableStatus(void* network, const std::string& id, bool connected) {
    GraalVmGuard guard;
    return updateConnectableStatus(guard.thread(), network, (char*) id.data(), connected);
}

LoadFlowComponentResultArray* runLoadFlow(void* network, bool distributedSlack, bool dc) {
    GraalVmGuard guard;
    return new LoadFlowComponentResultArray(runLoadFlow(guard.thread(), network, distributedSlack, dc));
}

BusArray* getBusArray(void* network, bool busBreakerView) {
    GraalVmGuard guard;
    return new BusArray(getBusArray(guard.thread(), network, busBreakerView));
}

void writeSingleLineDiagramSvg(void* network, const std::string& containerId, const std::string& svgFile) {
    GraalVmGuard guard;
    writeSingleLineDiagramSvg(guard.thread(), network, (char*) containerId.data(), (char*) svgFile.data());
}

void* createSecurityAnalysis() {
    GraalVmGuard guard;
    return createSecurityAnalysis(guard.thread());
}

void addContingencyToSecurityAnalysis(void* securityAnalysisContext, const std::string& contingencyId, const std::vector<std::string>& elementsIds) {
    GraalVmGuard guard;
    char** elementIdPtr = new char*[elementsIds.size()];
    for (int i = 0; i < elementsIds.size(); i++) {
        elementIdPtr[i] = (char *) elementsIds[i].data();
    }
    addContingencyToSecurityAnalysis(guard.thread(), securityAnalysisContext, (char*) contingencyId.data(), elementIdPtr, elementsIds.size());
    delete[] elementIdPtr;
}

SecurityAnalysisResultArray * runSecurityAnalysis(void* securityAnalysisContext, void* network) {
    GraalVmGuard guard;
    return new SecurityAnalysisResultArray(runSecurityAnalysis(guard.thread(), securityAnalysisContext, network));
}

void destroyObjectHandle(void* objectHandle) {
    GraalVmGuard guard;
    destroyObjectHandle(guard.thread(), objectHandle);
}

}
