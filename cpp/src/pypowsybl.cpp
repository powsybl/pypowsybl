/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
#include "pypowsybl.h"
#include "pypowsybl-java.h"
#include <iostream>

namespace pypowsybl {

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
            ptr_[i] = (char*) strings[i].data();
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

class ToIntPtr : public ToPtr<int> {
public:
    explicit ToIntPtr(const std::vector<int>& ints)
            : ToPtr<int>(ints.size())
    {
        for (int i = 0; i < ints.size(); i++) {
            ptr_[i] = ints[i];
        }
    }
};

template<>
std::vector<std::string> toVector(array* arrayPtr) {
    std::vector<std::string> strings;
    strings.reserve(arrayPtr->length);
    for (int i = 0; i < arrayPtr->length; i++) {
        char** ptr = (char**) arrayPtr->ptr + i;
        std::string str = *ptr ? *ptr : "";
        strings.emplace_back(str);
    }
    return strings;
}

template<typename F, typename... ARGS>
void executeJava(F f, ARGS... args) {
   exception_handler exc;
   f(args..., &exc);
   if (exc.message) {
       throw PyPowsyblError(exc.message);
   }
}

template<typename T, typename F, typename... ARGS>
T executeJava(F f, ARGS... args) {
   exception_handler exc;
   auto r = f(args..., &exc);
   if (exc.message) {
       throw PyPowsyblError(exc.message);
   }
   return r;
}

void setDebugMode(bool debug) {
    GraalVmGuard guard;
    setDebugMode(guard.thread(), debug);
}

std::string getVersionTable() {
    GraalVmGuard guard;
    return std::string(executeJava<char*>(::getVersionTable, guard.thread()));
}

void* createEmptyNetwork(const std::string& id) {
    GraalVmGuard guard;
    return executeJava<void*>(::createEmptyNetwork, guard.thread(), (char*) id.data());
}

void* createIeee14Network() {
    GraalVmGuard guard;
    return executeJava<void*>(::createIeee14Network, guard.thread());
}

void* createEurostagTutorialExample1Network() {
    GraalVmGuard guard;
    return executeJava<void*>(::createEurostagTutorialExample1Network, guard.thread());
}

void* loadNetwork(const std::string& file) {
    GraalVmGuard guard;
    return executeJava<void*>(::loadNetwork, guard.thread(), (char*) file.data());
}

void dumpNetwork(void* network, const std::string& file, const std::string& format) {
    GraalVmGuard guard;
    executeJava(::dumpNetwork, guard.thread(), network, (char*) file.data(), (char*) format.data());
}

void reduceNetwork(void* network, double v_min, double v_max, const std::vector<std::string>& ids,
                   const std::vector<std::string>& vls, const std::vector<int>& depths, bool withDangLingLines) {
    GraalVmGuard guard;
    ToCharPtrPtr elementIdPtr(ids);
    ToCharPtrPtr vlsPtr(vls);
    ToIntPtr depthsPtr(depths);
    executeJava(::reduceNetwork, guard.thread(), network, v_min, v_max, elementIdPtr.get(), ids.size(), vlsPtr.get(), vls.size(), depthsPtr.get(), depths.size(), withDangLingLines);
}

bool updateSwitchPosition(void* network, const std::string& id, bool open) {
    GraalVmGuard guard;
    return executeJava<bool>(::updateSwitchPosition, guard.thread(), network, (char*) id.data(), open);
}

bool updateConnectableStatus(void* network, const std::string& id, bool connected) {
    GraalVmGuard guard;
    return executeJava<bool>(::updateConnectableStatus, guard.thread(), network, (char*) id.data(), connected);
}

std::vector<std::string> getNetworkElementsIds(void* network, element_type elementType, const std::vector<double>& nominalVoltages,
                                               const std::vector<std::string>& countries, bool mainCc, bool mainSc,
                                               bool notConnectedToSameBusAtBothSides) {
    GraalVmGuard guard;
    ToDoublePtr nominalVoltagePtr(nominalVoltages);
    ToCharPtrPtr countryPtr(countries);
    auto elementsIdsArrayPtr = executeJava<array*>(::getNetworkElementsIds, guard.thread(), network, elementType,
                                                       nominalVoltagePtr.get(), nominalVoltages.size(),
                                                       countryPtr.get(), countries.size(), mainCc, mainSc,
                                                       notConnectedToSameBusAtBothSides);
    std::vector<std::string> elementsIds = toVector<std::string>(elementsIdsArrayPtr);
    freeNetworkElementsIds(guard.thread(), elementsIdsArrayPtr);
    return elementsIds;
}

LoadFlowComponentResultArray* runLoadFlow(void* network, bool dc, load_flow_parameters& parameters, const std::string& provider) {
    GraalVmGuard guard;
    return new LoadFlowComponentResultArray(
            executeJava<array*>(::runLoadFlow, guard.thread(), network, dc, &parameters, (char *) provider.data()));
}

BusArray* getBusArray(void* network) {
    GraalVmGuard guard;
    return new BusArray(executeJava<array*>(::getBusArray, guard.thread(), network));
}

GeneratorArray* getGeneratorArray(void* network) {
    GraalVmGuard guard;
    return new GeneratorArray(executeJava<array*>(::getGeneratorArray, guard.thread(), network));
}

LoadArray* getLoadArray(void* network) {
    GraalVmGuard guard;
    return new LoadArray(executeJava<array*>(::getLoadArray, guard.thread(), network));
}

void writeSingleLineDiagramSvg(void* network, const std::string& containerId, const std::string& svgFile) {
    GraalVmGuard guard;
    executeJava(::writeSingleLineDiagramSvg, guard.thread(), network, (char*) containerId.data(),
                (char*) svgFile.data());
}

void* createSecurityAnalysis() {
    GraalVmGuard guard;
    return createSecurityAnalysis(guard.thread());
}

void addContingency(void* analysisContext, const std::string& contingencyId, const std::vector<std::string>& elementsIds) {
    GraalVmGuard guard;
    ToCharPtrPtr elementIdPtr(elementsIds);
    executeJava(::addContingency, guard.thread(), analysisContext, (char*) contingencyId.data(), elementIdPtr.get(),
                elementsIds.size());
}

ContingencyResultArray* runSecurityAnalysis(void* securityAnalysisContext, void* network, load_flow_parameters& parameters) {
    GraalVmGuard guard;
    return new ContingencyResultArray(executeJava<array*>(::runSecurityAnalysis, guard.thread(), securityAnalysisContext,
                                                          network, &parameters));
}

void* createSensitivityAnalysis() {
    GraalVmGuard guard;
    return createSensitivityAnalysis(guard.thread());
}

void setFactorMatrix(void* sensitivityAnalysisContext, const std::vector<std::string>& branchesIds,
                     const std::vector<std::string>& injectionsOrTransfosIds) {
    GraalVmGuard guard;
    ToCharPtrPtr branchIdPtr(branchesIds);
    ToCharPtrPtr injectionOrTransfoIdPtr(injectionsOrTransfosIds);
    executeJava(::setFactorMatrix, guard.thread(), sensitivityAnalysisContext, branchIdPtr.get(), branchesIds.size(),
                injectionOrTransfoIdPtr.get(), injectionsOrTransfosIds.size());
}

void* runSensitivityAnalysis(void* sensitivityAnalysisContext, void* network, load_flow_parameters& parameters) {
    GraalVmGuard guard;
    return executeJava<void*>(::runSensitivityAnalysis, guard.thread(), sensitivityAnalysisContext, network, &parameters);
}

matrix* getSensitivityMatrix(void* sensitivityAnalysisResultContext, const std::string& contingencyId) {
    GraalVmGuard guard;
    return executeJava<matrix*>(::getSensitivityMatrix, guard.thread(), sensitivityAnalysisResultContext,
                                (char*) contingencyId.c_str());
}

matrix* getReferenceFlows(void* sensitivityAnalysisResultContext, const std::string& contingencyId) {
    GraalVmGuard guard;
    return executeJava<matrix*>(::getReferenceFlows, guard.thread(), sensitivityAnalysisResultContext,
                                (char*) contingencyId.c_str());
}

SeriesArray* createNetworkElementsSeriesArray(void* network, element_type elementType) {
    GraalVmGuard guard;
    return new SeriesArray(executeJava<array*>(::createNetworkElementsSeriesArray, guard.thread(), network, elementType));
}

void destroyObjectHandle(void* objectHandle) {
    GraalVmGuard guard;
    destroyObjectHandle(guard.thread(), objectHandle);
}

}
