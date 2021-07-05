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

template<typename F, typename... ARGS>
void callJava(F f, ARGS... args) {
    GraalVmGuard guard;
    exception_handler exc;
    f(guard.thread(), args..., &exc);
    if (exc.message) {
        throw PyPowsyblError(exc.message);
    }
}

template<typename T, typename F, typename... ARGS>
T callJava(F f, ARGS... args) {
    GraalVmGuard guard;
    exception_handler exc;
    auto r = f(guard.thread(), args..., &exc);
    if (exc.message) {
        throw PyPowsyblError(exc.message);
    }
    return r;
}

//Destruction of java object when the shared_ptr has no more references
JavaHandle::JavaHandle(void* handle):
    handle_(handle, [](void* to_be_deleted) {
        callJava<>(::destroyObjectHandle, to_be_deleted);
    })
{
}

template<>
Array<load_flow_component_result>::~Array() {
    callJava<>(::freeLoadFlowComponentResultPointer, delegate_);
}

template<>
Array<contingency_result>::~Array() {
    callJava<>(::freeContingencyResultArrayPointer, delegate_);
}

template<>
Array<limit_violation>::~Array() {
    // already freed by contingency_result
}

template<>
Array<series>::~Array() {
    callJava<>(::freeSeriesArray, delegate_);
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
        char** ptr = (char**) arrayPtr->ptr + i;
        std::string str = *ptr ? *ptr : "";
        strings.emplace_back(str);
    }
    return strings;
}

class ToStringVector {
public:
    ToStringVector(array* arrayPtr)
        : arrayPtr_(arrayPtr) {
    }

    ~ToStringVector() {
        callJava<>(::freeStringArray, arrayPtr_);
    }

    std::vector<std::string> get() {
        return toVector<std::string>(arrayPtr_);
    }

private:
    array* arrayPtr_;
};

char* copyStringToCharPtr(const std::string& str) {
    char* c = new char[str.size() + 1];
    str.copy(c, str.size());
    c[str.size()] = '\0';
    return c;
}

char** copyVectorStringToCharPtrPtr(const std::vector<std::string>& strings) {
    char** charPtrPtr = new char*[strings.size()];
    for (int i = 0; i < strings.size(); i++) {
        charPtrPtr[i] = copyStringToCharPtr((char*) strings[i].c_str());
    }
    return charPtrPtr;
}

void deleteCharPtrPtr(char** charPtrPtr, int length) {
    for (int i = 0; i < length; i++) {
        delete charPtrPtr[i];
    }
    delete charPtrPtr;
}

void setDebugMode(bool debug) {
    callJava<>(::setDebugMode, debug);
}

std::string getVersionTable() {
    return std::string(callJava<char*>(::getVersionTable));
}

JavaHandle createEmptyNetwork(const std::string& id) {
    return callJava<JavaHandle>(::createEmptyNetwork, (char*) id.data());
}

JavaHandle createIeeeNetwork(int busCount) {
    return callJava<JavaHandle>(::createIeeeNetwork, busCount);
}

JavaHandle createEurostagTutorialExample1Network() {
    return callJava<JavaHandle>(::createEurostagTutorialExample1Network);
}

JavaHandle createFourSubstationsNodeBreakerNetwork() {
    return callJava<JavaHandle>(::createFourSubstationsNodeBreakerNetwork);
}

std::vector<std::string> getNetworkImportFormats() {
    auto formatsArrayPtr = callJava<array*>(::getNetworkImportFormats);
    ToStringVector formats(formatsArrayPtr);
    return formats.get();
}

std::vector<std::string> getNetworkExportFormats() {
    auto formatsArrayPtr = callJava<array*>(::getNetworkExportFormats);
    ToStringVector formats(formatsArrayPtr);
    return formats.get();
}

SeriesArray* createImporterParametersSeriesArray(const std::string& format) {
    return new SeriesArray(callJava<array*>(::createImporterParametersSeriesArray, (char*) format.data()));
}

JavaHandle loadNetwork(const std::string& file, const std::map<std::string, std::string>& parameters) {
    std::vector<std::string> parameterNames;
    std::vector<std::string> parameterValues;
    parameterNames.reserve(parameters.size());
    parameterValues.reserve(parameters.size());
    for (std::pair<std::string, std::string> p : parameters) {
        parameterNames.push_back(p.first);
        parameterValues.push_back(p.second);
    }
    ToCharPtrPtr parameterNamesPtr(parameterNames);
    ToCharPtrPtr parameterValuesPtr(parameterValues);
    return callJava<JavaHandle>(::loadNetwork, (char*) file.data(), parameterNamesPtr.get(), parameterNames.size(),
                              parameterValuesPtr.get(), parameterValues.size());
}

JavaHandle loadNetworkFromString(const std::string& fileName, const std::string& fileContent, const std::map<std::string, std::string>& parameters) {
    std::vector<std::string> parameterNames;
    std::vector<std::string> parameterValues;
    parameterNames.reserve(parameters.size());
    parameterValues.reserve(parameters.size());
    for (std::pair<std::string, std::string> p : parameters) {
        parameterNames.push_back(p.first);
        parameterValues.push_back(p.second);
    }
    ToCharPtrPtr parameterNamesPtr(parameterNames);
    ToCharPtrPtr parameterValuesPtr(parameterValues);
    return callJava<JavaHandle>(::loadNetworkFromString, (char*) fileName.data(), (char*) fileContent.data(),
                           parameterNamesPtr.get(), parameterNames.size(),
                           parameterValuesPtr.get(), parameterValues.size());
}

void dumpNetwork(const JavaHandle& network, const std::string& file, const std::string& format, const std::map<std::string, std::string>& parameters) {
    std::vector<std::string> parameterNames;
    std::vector<std::string> parameterValues;
    parameterNames.reserve(parameters.size());
    parameterValues.reserve(parameters.size());
    for (std::pair<std::string, std::string> p : parameters) {
        parameterNames.push_back(p.first);
        parameterValues.push_back(p.second);
    }
    ToCharPtrPtr parameterNamesPtr(parameterNames);
    ToCharPtrPtr parameterValuesPtr(parameterValues);
    callJava(::dumpNetwork, network, (char*) file.data(), (char*) format.data(), parameterNamesPtr.get(), parameterNames.size(),
                parameterValuesPtr.get(), parameterValues.size());
}

std::string dumpNetworkToString(const JavaHandle& network, const std::string& format, const std::map<std::string, std::string>& parameters) {
    std::vector<std::string> parameterNames;
    std::vector<std::string> parameterValues;
    parameterNames.reserve(parameters.size());
    parameterValues.reserve(parameters.size());
    for (std::pair<std::string, std::string> p : parameters) {
        parameterNames.push_back(p.first);
        parameterValues.push_back(p.second);
    }
    ToCharPtrPtr parameterNamesPtr(parameterNames);
    ToCharPtrPtr parameterValuesPtr(parameterValues);
    return std::string(callJava<char*>(::dumpNetworkToString, network, (char*) format.data(), parameterNamesPtr.get(), parameterNames.size(),
             parameterValuesPtr.get(), parameterValues.size()));
}

void reduceNetwork(const JavaHandle& network, double v_min, double v_max, const std::vector<std::string>& ids,
                   const std::vector<std::string>& vls, const std::vector<int>& depths, bool withDangLingLines) {
    ToCharPtrPtr elementIdPtr(ids);
    ToCharPtrPtr vlsPtr(vls);
    ToIntPtr depthsPtr(depths);
    callJava(::reduceNetwork, network, v_min, v_max, elementIdPtr.get(), ids.size(), vlsPtr.get(), vls.size(), depthsPtr.get(), depths.size(), withDangLingLines);
}

bool updateSwitchPosition(const JavaHandle& network, const std::string& id, bool open) {
    return callJava<bool>(::updateSwitchPosition, network, (char*) id.data(), open);
}

bool updateConnectableStatus(const JavaHandle& network, const std::string& id, bool connected) {
    return callJava<bool>(::updateConnectableStatus, network, (char*) id.data(), connected);
}

std::vector<std::string> getNetworkElementsIds(const JavaHandle& network, element_type elementType, const std::vector<double>& nominalVoltages,
                                               const std::vector<std::string>& countries, bool mainCc, bool mainSc,
                                               bool notConnectedToSameBusAtBothSides) {
    ToDoublePtr nominalVoltagePtr(nominalVoltages);
    ToCharPtrPtr countryPtr(countries);
    auto elementsIdsArrayPtr = callJava<array*>(::getNetworkElementsIds, network, elementType,
                                                       nominalVoltagePtr.get(), nominalVoltages.size(),
                                                       countryPtr.get(), countries.size(), mainCc, mainSc,
                                                       notConnectedToSameBusAtBothSides);
    ToStringVector elementsIds(elementsIdsArrayPtr);
    return elementsIds.get();
}

LoadFlowComponentResultArray* runLoadFlow(const JavaHandle& network, bool dc, const std::shared_ptr<load_flow_parameters>& parameters,
                                          const std::string& provider) {
    return new LoadFlowComponentResultArray(
            callJava<array*>(::runLoadFlow, network, dc, parameters.get(), (char *) provider.data()));
}

void writeSingleLineDiagramSvg(const JavaHandle& network, const std::string& containerId, const std::string& svgFile) {
    callJava(::writeSingleLineDiagramSvg, network, (char*) containerId.data(), (char*) svgFile.data());
}

std::string getSingleLineDiagramSvg(const JavaHandle& network, const std::string& containerId) {
    return std::string(callJava<char*>(::getSingleLineDiagramSvg, network, (char*) containerId.data()));
}

JavaHandle createSecurityAnalysis() {
    return callJava<JavaHandle>(::createSecurityAnalysis);
}

void addContingency(const JavaHandle& analysisContext, const std::string& contingencyId, const std::vector<std::string>& elementsIds) {
    ToCharPtrPtr elementIdPtr(elementsIds);
    callJava(::addContingency, analysisContext, (char*) contingencyId.data(), elementIdPtr.get(), elementsIds.size());
}

JavaHandle runSecurityAnalysis(const JavaHandle& securityAnalysisContext, const JavaHandle& network, load_flow_parameters& parameters,
                                            const std::string& provider) {
    return callJava<JavaHandle>(::runSecurityAnalysis, securityAnalysisContext, network, &parameters, (char *) provider.data());
}

JavaHandle createSensitivityAnalysis() {
    return callJava<JavaHandle>(::createSensitivityAnalysis);
}

::zone* createZone(const std::string& id, const std::vector<std::string>& injectionsIds, const std::vector<double>& injectionsShiftKeys) {
    auto z = new ::zone;
    z->id = copyStringToCharPtr(id);
    z->length = injectionsIds.size();
    z->injections_ids = new char*[injectionsIds.size()];
    for (int i = 0; i < injectionsIds.size(); i++) {
        z->injections_ids[i] = copyStringToCharPtr(injectionsIds[i]);
    }
    z->injections_shift_keys = new double[injectionsShiftKeys.size()];
    for (int i = 0; i < injectionsIds.size(); i++) {
        z->injections_shift_keys[i] = injectionsShiftKeys[i];
    }
    return z;
}

void deleteZone(::zone* z) {
    delete[] z->id;
    for (int i = 0; i < z->length; i++) {
        delete[] z->injections_ids[i];
    }
    delete[] z->injections_ids;
    delete[] z->injections_shift_keys;
}

class ZonesPtr {
public:
    ZonesPtr(const std::vector<zone*>& vector)
        : vector_(vector) {
    }

    ~ZonesPtr() {
        for (auto z : vector_) {
            deleteZone(z);
        }
    }

    ::zone** get() const {
        return (::zone**) &vector_[0];
    }

private:
    const std::vector<::zone*>& vector_;
};

void setZones(const JavaHandle& sensitivityAnalysisContext, const std::vector<::zone*>& zones) {
    ZonesPtr zonesPtr(zones);
    callJava(::setZones, sensitivityAnalysisContext, zonesPtr.get(), zones.size());
}

void setBranchFlowFactorMatrix(const JavaHandle& sensitivityAnalysisContext, const std::vector<std::string>& branchesIds,
                               const std::vector<std::string>& variablesIds) {
    ToCharPtrPtr branchIdPtr(branchesIds);
    ToCharPtrPtr variableIdPtr(variablesIds);
    callJava(::setBranchFlowFactorMatrix, sensitivityAnalysisContext, branchIdPtr.get(), branchesIds.size(),
                variableIdPtr.get(), variablesIds.size());
}

void setBusVoltageFactorMatrix(const JavaHandle& sensitivityAnalysisContext, const std::vector<std::string>& busIds,
                               const std::vector<std::string>& targetVoltageIds) {
    ToCharPtrPtr busVoltageIdPtr(busIds);
    ToCharPtrPtr targetVoltageIdPtr(targetVoltageIds);
    callJava(::setBusVoltageFactorMatrix, sensitivityAnalysisContext, busVoltageIdPtr.get(),
                busIds.size(), targetVoltageIdPtr.get(), targetVoltageIds.size());
}

JavaHandle runSensitivityAnalysis(const JavaHandle& sensitivityAnalysisContext, const JavaHandle& network, bool dc, load_flow_parameters& parameters, const std::string& provider) {
    return callJava<JavaHandle>(::runSensitivityAnalysis, sensitivityAnalysisContext, network, dc, &parameters, (char *) provider.data());
}

matrix* getBranchFlowsSensitivityMatrix(const JavaHandle& sensitivityAnalysisResultContext, const std::string& contingencyId) {
    return callJava<matrix*>(::getBranchFlowsSensitivityMatrix, sensitivityAnalysisResultContext,
                                (char*) contingencyId.c_str());
}

matrix* getBusVoltagesSensitivityMatrix(const JavaHandle& sensitivityAnalysisResultContext, const std::string& contingencyId) {
    return callJava<matrix*>(::getBusVoltagesSensitivityMatrix, sensitivityAnalysisResultContext,
                                (char*) contingencyId.c_str());
}

matrix* getReferenceFlows(const JavaHandle& sensitivityAnalysisResultContext, const std::string& contingencyId) {
    return callJava<matrix*>(::getReferenceFlows, sensitivityAnalysisResultContext,
                                (char*) contingencyId.c_str());
}

matrix* getReferenceVoltages(const JavaHandle& sensitivityAnalysisResultContext, const std::string& contingencyId) {
    return callJava<matrix*>(::getReferenceVoltages, sensitivityAnalysisResultContext,
                                (char*) contingencyId.c_str());
}

SeriesArray* createNetworkElementsSeriesArray(const JavaHandle& network, element_type elementType) {
    return new SeriesArray(callJava<array*>(::createNetworkElementsSeriesArray, network, elementType));
}

int getSeriesType(element_type elementType, const std::string& seriesName) {
    return callJava<int>(::getSeriesType, elementType, (char *) seriesName.c_str());
}

void updateNetworkElementsWithIntSeries(const JavaHandle& network, element_type elementType, const std::string& seriesName, const std::vector<std::string>& ids,
                                        const std::vector<int>& values, int elementCount) {
    ToCharPtrPtr idPtr(ids);
    ToIntPtr valuePtr(values);
    callJava(::updateNetworkElementsWithIntSeries, network, elementType, (char *) seriesName.c_str(),
                    idPtr.get(), valuePtr.get(), elementCount);
}

void updateNetworkElementsWithDoubleSeries(const JavaHandle& network, element_type elementType, const std::string& seriesName, const std::vector<std::string>& ids,
                                           const std::vector<double>& values, int elementCount) {
    ToCharPtrPtr idPtr(ids);
    ToDoublePtr valuePtr(values);
    callJava(::updateNetworkElementsWithDoubleSeries, network, elementType, (char *) seriesName.c_str(),
                    idPtr.get(), valuePtr.get(), elementCount);
}

void updateNetworkElementsWithStringSeries(const JavaHandle& network, element_type elementType, const std::string& seriesName, const std::vector<std::string>& ids,
                                           const std::vector<std::string>& values, int elementCount) {
    ToCharPtrPtr idPtr(ids);
    ToCharPtrPtr valuePtr(values);
    callJava<>(::updateNetworkElementsWithStringSeries, network, elementType, (char *) seriesName.c_str(),
                idPtr.get(), valuePtr.get(), elementCount);
}

void createElement(const JavaHandle& network, element_type elementType,
                   const std::vector<std::string> &doubleKeys, const std::vector<double> &doubleVals,
                   const std::vector<std::string> &strKeys, const std::vector<std::string> &strVals,
                   const std::vector<std::string> &intKeys, const std::vector<int> &intVals) {
    ToCharPtrPtr doubleKeysPtr(doubleKeys);
    ToDoublePtr doubleValsPtr(doubleVals);
    ToCharPtrPtr strKeysPtr(strKeys);
    ToCharPtrPtr strValsPtr(strVals);
    ToCharPtrPtr intKeysPtr(intKeys);
    ToIntPtr intValsPtr(intVals);

    callJava<>(::createElement, network, elementType,
            doubleKeysPtr.get(), doubleValsPtr.get(), doubleVals.size(),
            strKeysPtr.get(), strValsPtr.get(), strKeys.size(),
            intKeysPtr.get(), intValsPtr.get(), intKeys.size());
}

std::string getWorkingVariantId(const JavaHandle& network) {
    return std::string(callJava<char*>(::getWorkingVariantId, network));
}

void setWorkingVariant(const JavaHandle& network, std::string& variant) {
    callJava<>(::setWorkingVariant, network, (char*) variant.c_str());
}

void removeVariant(const JavaHandle& network, std::string& variant) {
    callJava<>(::removeVariant, network, (char*) variant.c_str());
}

void cloneVariant(const JavaHandle& network, std::string& src, std::string& variant, bool mayOverwrite) {
    callJava<>(::cloneVariant, network, (char*) src.c_str(), (char*) variant.c_str(), mayOverwrite);
}

std::vector<std::string> getVariantsIds(const JavaHandle& network) {
    auto formatsArrayPtr = callJava<array*>(::getVariantsIds, network);
    ToStringVector formats(formatsArrayPtr);
    return formats.get();
}

void addMonitoredElements(const JavaHandle& securityAnalysisContext, contingency_context_type contingencyContextType, const std::vector<std::string>& branchIds,
                      const std::vector<std::string>& voltageLevelIds, const std::vector<std::string>& threeWindingsTransformerIds,
                      const std::vector<std::string>& contingencyIds) {
    ToCharPtrPtr branchIdsPtr(branchIds);
    ToCharPtrPtr voltageLevelIdsPtr(voltageLevelIds);
    ToCharPtrPtr threeWindingsTransformerIdsPtr(threeWindingsTransformerIds);
    ToCharPtrPtr contingencyIdsPtr(contingencyIds);
    callJava<>(::addMonitoredElements, securityAnalysisContext, contingencyContextType, branchIdsPtr.get(), branchIds.size(),
    voltageLevelIdsPtr.get(), voltageLevelIds.size(), threeWindingsTransformerIdsPtr.get(),
    threeWindingsTransformerIds.size(), contingencyIdsPtr.get(), contingencyIds.size());
}

ContingencyResultArray* getSecurityAnalysisResult(const JavaHandle& securityAnalysisResult) {
    return new ContingencyResultArray(callJava<array*>(::getSecurityAnalysisResult, securityAnalysisResult));
}

SeriesArray* getLimitViolations(const JavaHandle& securityAnalysisResult) {
    return new SeriesArray(callJava<array*>(::getLimitViolations, securityAnalysisResult));
}

SeriesArray* getBranchResults(const JavaHandle& securityAnalysisResult) {
    return new SeriesArray(callJava<array*>(::getBranchResults, securityAnalysisResult));
}

SeriesArray* getBusResults(const JavaHandle& securityAnalysisResult) {
    return new SeriesArray(callJava<array*>(::getBusResults, securityAnalysisResult));
}

SeriesArray* getThreeWindingsTransformerResults(const JavaHandle& securityAnalysisResult) {
    return new SeriesArray(callJava<array*>(::getThreeWindingsTransformerResults, securityAnalysisResult));
}
}
