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
Array<contingency_result>::~Array() {
    GraalVmGuard guard;
    freeContingencyResultArrayPointer(guard.thread(), delegate_);
}

template<>
Array<limit_violation>::~Array() {
    // already freed by contingency_result
}

void setDebugMode(bool debug) {
    GraalVmGuard guard;
    setDebugMode(guard.thread(), debug);
}

std::string getVersionTable() {
    GraalVmGuard guard;
    return std::string(getVersionTable(guard.thread()));
}

void* createEmptyNetwork(const std::string& id) {
    GraalVmGuard guard;
    return createEmptyNetwork(guard.thread(), (char*) id.data());
}

void* createIeee14Network() {
    GraalVmGuard guard;
    return createIeee14Network(guard.thread());
}

void* createEurostagTutorialExample1Network() {
    GraalVmGuard guard;
    return createEurostagTutorialExample1Network(guard.thread());
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

std::vector<std::string> getNetworkElementsIds(void* network, element_type elementType, double nominalVoltage, bool mainCc) {
    GraalVmGuard guard;
    array* elementsIdsArrayPtr = getNetworkElementsIds(guard.thread(), network, elementType, nominalVoltage, mainCc);
    std::vector<std::string> elementsIds;
    elementsIds.reserve(elementsIdsArrayPtr->length);
    for (int i = 0; i < elementsIdsArrayPtr->length; i++) {
        std::string elementId = *((char**) elementsIdsArrayPtr->ptr + i);
        elementsIds.emplace_back(elementId);
    }
    freeNetworkElementsIds(guard.thread(), elementsIdsArrayPtr);
    return elementsIds;
}

LoadFlowComponentResultArray* runLoadFlow(void* network, bool dc, load_flow_parameters& parameters) {
    GraalVmGuard guard;
    return new LoadFlowComponentResultArray(runLoadFlow(guard.thread(), network, dc, &parameters));
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

class ToCharPtrPtr {
public:
    explicit ToCharPtrPtr(const std::vector<std::string>& strings)
        : charPtrPtr_(new char*[strings.size()])
    {
        for (int i = 0; i < strings.size(); i++) {
            charPtrPtr_[i] = (char *) strings[i].data();
        }
    }

    ~ToCharPtrPtr() {
        delete[] charPtrPtr_;
    }

    char** get() const {
        return charPtrPtr_;
    }

private:
    char** charPtrPtr_;
};

void addContingency(void* analysisContext, const std::string& contingencyId, const std::vector<std::string>& elementsIds) {
    GraalVmGuard guard;
    ToCharPtrPtr elementIdPtr(elementsIds);
    addContingency(guard.thread(), analysisContext, (char*) contingencyId.data(), elementIdPtr.get(), elementsIds.size());
}

ContingencyResultArray* runSecurityAnalysis(void* securityAnalysisContext, void* network, load_flow_parameters& parameters) {
    GraalVmGuard guard;
    return new ContingencyResultArray(runSecurityAnalysis(guard.thread(), securityAnalysisContext, network, &parameters));
}

void* createSensitivityAnalysis() {
    GraalVmGuard guard;
    return createSensitivityAnalysis(guard.thread());
}

void setFactorMatrix(void* sensitivityAnalysisContext, const std::vector<std::string>& branchesIds, const std::vector<std::string>& injectionsOrTransfosIds) {
    GraalVmGuard guard;
    ToCharPtrPtr branchIdPtr(branchesIds);
    ToCharPtrPtr injectionOrTransfoIdPtr(injectionsOrTransfosIds);
    setFactorMatrix(guard.thread(), sensitivityAnalysisContext, branchIdPtr.get(), branchesIds.size(),
                    injectionOrTransfoIdPtr.get(), injectionsOrTransfosIds.size());
}

void* runSensitivityAnalysis(void* sensitivityAnalysisContext, void* network, load_flow_parameters& parameters) {
    GraalVmGuard guard;
    return runSensitivityAnalysis(guard.thread(), sensitivityAnalysisContext, network, &parameters);
}

matrix* getSensitivityMatrix(void* sensitivityAnalysisResultContext, const std::string& contingencyId) {
    GraalVmGuard guard;
    return getSensitivityMatrix(guard.thread(), sensitivityAnalysisResultContext, (char*) contingencyId.c_str());
}

void destroyObjectHandle(void* objectHandle) {
    GraalVmGuard guard;
    destroyObjectHandle(guard.thread(), objectHandle);
}

}
