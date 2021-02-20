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
Array<generator>::~Array() {
    GraalVmGuard guard;
    freeGeneratorArray(guard.thread(), delegate_);
}

template<>
Array<load>::~Array() {
    GraalVmGuard guard;
    freeLoadArray(guard.thread(), delegate_);
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

template<>
Array<series>::~Array() {
    GraalVmGuard guard;
    freeNetworkElementsSeriesArray(guard.thread(), delegate_);
}

template<typename T>
class ToPtr {
public:
    ~ToPtr() {
        delete[] ptr_;
    }

    T* get() const {
        return ptr_;
    }

protected:
    explicit ToPtr(size_t size)
            : ptr_(new T[size])
    {}

    T* ptr_;
};

class ToCharPtrPtr : public ToPtr<char*> {
public:
    explicit ToCharPtrPtr(const std::vector<std::string>& strings)
            : ToPtr<char*>(strings.size())
    {
        for (int i = 0; i < strings.size(); i++) {
            ptr_[i] = (char *) strings[i].data();
        }
    }
};

class ToDoublePtr : public ToPtr<double> {
public:
    explicit ToDoublePtr(const std::vector<double>& doubles)
            : ToPtr<double>(doubles.size())
    {
        for (int i = 0; i < doubles.size(); i++) {
            ptr_[i] = doubles[i];
        }
    }
};

template<>
std::vector<std::string> toVector(array* arrayPtr) {
    std::vector<std::string> strings;
    strings.reserve(arrayPtr->length);
    for (int i = 0; i < arrayPtr->length; i++) {
        std::string str = *((char**) arrayPtr->ptr + i);
        strings.emplace_back(str);
    }
    return strings;
}

void checkException(graal_isolatethread_t* thread) {
    char* message = getExceptionMessage(thread);
    if (message) {
        // no need to free message as it is going to be managed on python side by a python str
        throw std::runtime_error(message);
    }
}

template<typename T>
T checkException(graal_isolatethread_t* thread, T result) {
    checkException(thread);
    return result;
}

void setDebugMode(bool debug) {
    GraalVmGuard guard;
    setDebugMode(guard.thread(), debug);
    checkException(guard.thread());
}

std::string getVersionTable() {
    GraalVmGuard guard;
    return checkException(guard.thread(), std::string(getVersionTable(guard.thread())));
}

void* createEmptyNetwork(const std::string& id) {
    GraalVmGuard guard;
    return checkException(guard.thread(), createEmptyNetwork(guard.thread(), (char*) id.data()));
}

void* createIeee14Network() {
    GraalVmGuard guard;
    return checkException(guard.thread(), createIeee14Network(guard.thread()));
}

void* createEurostagTutorialExample1Network() {
    GraalVmGuard guard;
    return checkException(guard.thread(), createEurostagTutorialExample1Network(guard.thread()));
}

void* loadNetwork(const std::string& file) {
    GraalVmGuard guard;
    return checkException(guard.thread(), loadNetwork(guard.thread(), (char*) file.data()));
}

void dumpNetwork(void* network, const std::string& file, const std::string& format) {
    GraalVmGuard guard;
    dumpNetwork(guard.thread(), network, (char*) file.data(), (char*) format.data());
    checkException(guard.thread());
}

bool updateSwitchPosition(void* network, const std::string& id, bool open) {
    GraalVmGuard guard;
    return checkException(guard.thread(), updateSwitchPosition(guard.thread(), network, (char*) id.data(), open));
}

bool updateConnectableStatus(void* network, const std::string& id, bool connected) {
    GraalVmGuard guard;
    return checkException(guard.thread(), updateConnectableStatus(guard.thread(), network, (char*) id.data(), connected));
}

std::vector<std::string> getNetworkElementsIds(void* network, element_type elementType, const std::vector<double>& nominalVoltages,
                                               const std::vector<std::string>& countries, bool mainCc, bool notConnectedToSameBusAtBothSides) {
    GraalVmGuard guard;
    ToDoublePtr nominalVoltagePtr(nominalVoltages);
    ToCharPtrPtr countryPtr(countries);
    array* elementsIdsArrayPtr = getNetworkElementsIds(guard.thread(), network, elementType, nominalVoltagePtr.get(), nominalVoltages.size(),
                                                       countryPtr.get(), countries.size(), mainCc, notConnectedToSameBusAtBothSides);
    std::vector<std::string> elementsIds = toVector<std::string>(elementsIdsArrayPtr);
    freeNetworkElementsIds(guard.thread(), elementsIdsArrayPtr);
    checkException(guard.thread());
    return elementsIds;
}

LoadFlowComponentResultArray* runLoadFlow(void* network, bool dc, load_flow_parameters& parameters) {
    GraalVmGuard guard;
    return checkException(guard.thread(), new LoadFlowComponentResultArray(runLoadFlow(guard.thread(), network, dc, &parameters)));
}

BusArray* getBusArray(void* network) {
    GraalVmGuard guard;
    return checkException(guard.thread(), new BusArray(getBusArray(guard.thread(), network)));
}

GeneratorArray* getGeneratorArray(void* network) {
    GraalVmGuard guard;
    return checkException(guard.thread(), new GeneratorArray(getGeneratorArray(guard.thread(), network)));
}

LoadArray* getLoadArray(void* network) {
    GraalVmGuard guard;
    return checkException(guard.thread(), new LoadArray(getLoadArray(guard.thread(), network)));
}

void writeSingleLineDiagramSvg(void* network, const std::string& containerId, const std::string& svgFile) {
    GraalVmGuard guard;
    writeSingleLineDiagramSvg(guard.thread(), network, (char*) containerId.data(), (char*) svgFile.data());
    checkException(guard.thread());
}

void* createSecurityAnalysis() {
    GraalVmGuard guard;
    return checkException(guard.thread(), createSecurityAnalysis(guard.thread()));
}

void addContingency(void* analysisContext, const std::string& contingencyId, const std::vector<std::string>& elementsIds) {
    GraalVmGuard guard;
    ToCharPtrPtr elementIdPtr(elementsIds);
    addContingency(guard.thread(), analysisContext, (char*) contingencyId.data(), elementIdPtr.get(), elementsIds.size());
    checkException(guard.thread());
}

ContingencyResultArray* runSecurityAnalysis(void* securityAnalysisContext, void* network, load_flow_parameters& parameters) {
    GraalVmGuard guard;
    return checkException(guard.thread(), new ContingencyResultArray(runSecurityAnalysis(guard.thread(), securityAnalysisContext, network, &parameters)));
}

void* createSensitivityAnalysis() {
    GraalVmGuard guard;
    return checkException(guard.thread(), createSensitivityAnalysis(guard.thread()));
}

void setFactorMatrix(void* sensitivityAnalysisContext, const std::vector<std::string>& branchesIds, const std::vector<std::string>& injectionsOrTransfosIds) {
    GraalVmGuard guard;
    ToCharPtrPtr branchIdPtr(branchesIds);
    ToCharPtrPtr injectionOrTransfoIdPtr(injectionsOrTransfosIds);
    setFactorMatrix(guard.thread(), sensitivityAnalysisContext, branchIdPtr.get(), branchesIds.size(),
                    injectionOrTransfoIdPtr.get(), injectionsOrTransfosIds.size());
    checkException(guard.thread());
}

void* runSensitivityAnalysis(void* sensitivityAnalysisContext, void* network, load_flow_parameters& parameters) {
    GraalVmGuard guard;
    return checkException(guard.thread(), runSensitivityAnalysis(guard.thread(), sensitivityAnalysisContext, network, &parameters));
}

matrix* getSensitivityMatrix(void* sensitivityAnalysisResultContext, const std::string& contingencyId) {
    GraalVmGuard guard;
    return checkException(guard.thread(), getSensitivityMatrix(guard.thread(), sensitivityAnalysisResultContext, (char*) contingencyId.c_str()));
}

matrix* getReferenceFlows(void* sensitivityAnalysisResultContext, const std::string& contingencyId) {
    GraalVmGuard guard;
    return checkException(guard.thread(), getReferenceFlows(guard.thread(), sensitivityAnalysisResultContext, (char*) contingencyId.c_str()));
}

SeriesArray* createNetworkElementsSeriesArray(void* network, element_type elementType) {
    GraalVmGuard guard;
    return checkException(guard.thread(), new SeriesArray(createNetworkElementsSeriesArray(guard.thread(), network, elementType)));
}

void destroyObjectHandle(void* objectHandle) {
    GraalVmGuard guard;
    destroyObjectHandle(guard.thread(), objectHandle);
    checkException(guard.thread());
}

}
