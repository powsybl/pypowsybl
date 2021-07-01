/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
#ifndef PYPOWSYBL_H
#define PYPOWSYBL_H

#include <string>
#include <vector>
#include <map>
#include <memory>
#include <stdexcept>
#include "pypowsybl-api.h"

namespace pypowsybl {

class PyPowsyblError : public std::runtime_error {
public:
    PyPowsyblError(const char* msg)
        : runtime_error(msg) {
    }

    PyPowsyblError(const std::string&  msg)
        : runtime_error(msg) {
    }
};

/**
 * Wraps a void* corresponding to a graalvm ObjectHandle
 * in order to handle its destruction.
 */
class java_handle {
public:
    //Implicit constructor from void* returned by graalvm
    java_handle(void* handle);
    ~java_handle() {}

    //Implicit conversion to void* for use as input to graalvm
    operator void*() {
        return handle_.get();
    }

private:
    //Object handle destruction will be called when no more reference
    std::shared_ptr<void> handle_;
};


template<typename T>
class Array {
public:
    explicit Array(array* delegate)
        : delegate_(delegate) {
    }

    int length() const { return delegate_->length; }

    T* begin() const { return (T*) delegate_->ptr; }

    T* end() const { return (T*) delegate_->ptr + delegate_->length; }

    ~Array();

private:
    array* delegate_;
};

typedef Array<load_flow_component_result> LoadFlowComponentResultArray;
typedef Array<contingency_result> ContingencyResultArray;
typedef Array<limit_violation> LimitViolationArray;
typedef Array<series> SeriesArray;

template<typename T>
std::vector<T> toVector(array* arrayPtr) {
    std::vector<T> values;
    values.reserve(arrayPtr->length);
    for (int i = 0; i < arrayPtr->length; i++) {
        T value = *((T*) arrayPtr->ptr + i);
        values.push_back(value);
    }
    return values;
}

template<>
std::vector<std::string> toVector(array* arrayPtr);

enum LoadFlowComponentStatus {
    CONVERGED = 0,
    MAX_ITERATION_REACHED,
    SOLVER_FAILED,
    FAILED,
};

enum LimitType {
    CURRENT = 0,
    LOW_VOLTAGE,
    HIGH_VOLTAGE,
};

enum Side {
    NONE = -1,
    ONE,
    TWO,
};

enum VoltageInitMode {
    UNIFORM_VALUES = 0,
    PREVIOUS_VALUES,
    DC_VALUES,
};

enum BalanceType {
    PROPORTIONAL_TO_GENERATION_P = 0,
    PROPORTIONAL_TO_GENERATION_P_MAX,
    PROPORTIONAL_TO_LOAD,
    PROPORTIONAL_TO_CONFORM_LOAD,
};

enum ConnectedComponentMode {
    MAIN = 0,
    ALL,
};

char* copyStringToCharPtr(const std::string& str);
char** copyVectorStringToCharPtrPtr(const std::vector<std::string>& strings);
void deleteCharPtrPtr(char** charPtrPtr, int length);

::zone* createZone(const std::string& id, const std::vector<std::string>& injectionsIds, const std::vector<double>& injectionsShiftKeys);

void init();

void setDebugMode(bool debug);

std::string getVersionTable();

java_handle createEmptyNetwork(const std::string& id);

java_handle createIeeeNetwork(int busCount);

java_handle createEurostagTutorialExample1Network();

java_handle createFourSubstationsNodeBreakerNetwork();

bool updateSwitchPosition(java_handle network, const std::string& id, bool open);

bool updateConnectableStatus(java_handle network, const std::string& id, bool connected);

std::vector<std::string> getNetworkElementsIds(java_handle network, element_type elementType, const std::vector<double>& nominalVoltages,
                                               const std::vector<std::string>& countries, bool mainCc, bool mainSc,
                                               bool notConnectedToSameBusAtBothSides);

std::vector<std::string> getNetworkImportFormats();

std::vector<std::string> getNetworkExportFormats();

SeriesArray* createImporterParametersSeriesArray(const std::string& format);

java_handle loadNetwork(const std::string& file, const std::map<std::string, std::string>& parameters);

java_handle loadNetworkFromString(const std::string& fileName, const std::string& fileContent, const std::map<std::string, std::string>& parameters);

void dumpNetwork(java_handle network, const std::string& file, const std::string& format, const std::map<std::string, std::string>& parameters);

std::string dumpNetworkToString(java_handle network, const std::string& format, const std::map<std::string, std::string>& parameters);

void reduceNetwork(java_handle network, const double v_min, const double v_max, const std::vector<std::string>& ids, const std::vector<std::string>& vls, const std::vector<int>& depths, bool withDangLingLines);

LoadFlowComponentResultArray* runLoadFlow(java_handle network, bool dc, const std::shared_ptr<load_flow_parameters>& parameters, const std::string& provider);

void writeSingleLineDiagramSvg(java_handle network, const std::string& containerId, const std::string& svgFile);

std::string getSingleLineDiagramSvg(java_handle network, const std::string& containerId);

java_handle createSecurityAnalysis();

void addContingency(java_handle analysisContext, const std::string& contingencyId, const std::vector<std::string>& elementsIds);

java_handle runSecurityAnalysis(java_handle securityAnalysisContext, java_handle network, load_flow_parameters& parameters, const std::string& provider);

java_handle createSensitivityAnalysis();

void setZones(java_handle sensitivityAnalysisContext, const std::vector<::zone*>& zones);

void setBranchFlowFactorMatrix(java_handle sensitivityAnalysisContext, const std::vector<std::string>& branchesIds, const std::vector<std::string>& variablesIds);

void setBusVoltageFactorMatrix(java_handle sensitivityAnalysisContext, const std::vector<std::string>& busIds, const std::vector<std::string>& targetVoltageIds);

java_handle runSensitivityAnalysis(java_handle sensitivityAnalysisContext, java_handle network, bool dc, load_flow_parameters& parameters, const std::string& provider);

matrix* getBranchFlowsSensitivityMatrix(java_handle sensitivityAnalysisResultContext, const std::string &contingencyId);

matrix* getBusVoltagesSensitivityMatrix(java_handle sensitivityAnalysisResultContext, const std::string &contingencyId);

matrix* getReferenceFlows(java_handle sensitivityAnalysisResultContext, const std::string& contingencyId);

matrix* getReferenceVoltages(java_handle sensitivityAnalysisResultContext, const std::string& contingencyId);

SeriesArray* createNetworkElementsSeriesArray(java_handle network, element_type elementType);

int getSeriesType(element_type elementType, const std::string& seriesName);

void updateNetworkElementsWithIntSeries(java_handle network, element_type elementType, const std::string& seriesName, const std::vector<std::string>& ids,
                                        const std::vector<int>& values, int elementCount);

void updateNetworkElementsWithDoubleSeries(java_handle network, element_type elementType, const std::string& seriesName, const std::vector<std::string>& ids,
                                           const std::vector<double>& values, int elementCount);

void updateNetworkElementsWithStringSeries(java_handle network, element_type elementType, const std::string& seriesName, const std::vector<std::string>& ids,
                                           const std::vector<std::string>& values, int elementCount);

std::string getWorkingVariantId(java_handle network);

void setWorkingVariant(java_handle network, std::string& variant);

void removeVariant(java_handle network, std::string& variant);

void cloneVariant(java_handle network, std::string& src, std::string& variant, bool mayOverwrite);

std::vector<std::string> getVariantsIds(java_handle network);

void addMonitoredElements(java_handle securityAnalysisContext, contingency_context_type contingencyContextType, const std::vector<std::string>& branchIds,
                      const std::vector<std::string>& voltageLevelIds, const std::vector<std::string>& threeWindingsTransformerIds,
                      const std::vector<std::string>& contingencyIds);

SeriesArray* getLimitViolations(java_handle securityAnalysisResult);

ContingencyResultArray* getSecurityAnalysisResult(java_handle securityAnalysisResult);

SeriesArray* getBranchResults(java_handle securityAnalysisResult);

SeriesArray* getBusResults(java_handle securityAnalysisResult);

SeriesArray* getThreeWindingsTransformerResults(java_handle securityAnalysisResult);

}

#endif //PYPOWSYBL_H
