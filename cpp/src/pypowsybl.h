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

void* createEmptyNetwork(const std::string& id);

void* createIeeeNetwork(int busCount);

void* createEurostagTutorialExample1Network();

void* createFourSubstationsNodeBreakerNetwork();

bool updateSwitchPosition(void* network, const std::string& id, bool open);

bool updateConnectableStatus(void* network, const std::string& id, bool connected);

std::vector<std::string> getNetworkElementsIds(void* network, element_type elementType, const std::vector<double>& nominalVoltages,
                                               const std::vector<std::string>& countries, bool mainCc, bool mainSc,
                                               bool notConnectedToSameBusAtBothSides);

std::vector<std::string> getNetworkImportFormats();

std::vector<std::string> getNetworkExportFormats();

SeriesArray* createImporterParametersSeriesArray(const std::string& format);

void* loadNetwork(const std::string& file, const std::map<std::string, std::string>& parameters);

void dumpNetwork(void* network, const std::string& file, const std::string& format, const std::map<std::string, std::string>& parameters);

void reduceNetwork(void* network, const double v_min, const double v_max, const std::vector<std::string>& ids, const std::vector<std::string>& vls, const std::vector<int>& depths, bool withDangLingLines);

LoadFlowComponentResultArray* runLoadFlow(void* network, bool dc, const std::shared_ptr<load_flow_parameters>& parameters, const std::string& provider);

void writeSingleLineDiagramSvg(void* network, const std::string& containerId, const std::string& svgFile);

void* createSecurityAnalysis();

void addContingency(void* analysisContext, const std::string& contingencyId, const std::vector<std::string>& elementsIds);

ContingencyResultArray* runSecurityAnalysis(void* securityAnalysisContext, void* network, load_flow_parameters& parameters, const std::string& provider);

void* createSensitivityAnalysis();

void setZones(void* sensitivityAnalysisContext, const std::vector<::zone*>& zones);

void setBranchFlowFactorMatrix(void* sensitivityAnalysisContext, const std::vector<std::string>& branchesIds, const std::vector<std::string>& variablesIds);

void setBusVoltageFactorMatrix(void* sensitivityAnalysisContext, const std::vector<std::string>& busIds, const std::vector<std::string>& targetVoltageIds);

void* runSensitivityAnalysis(void* sensitivityAnalysisContext, void* network, bool dc, load_flow_parameters& parameters, const std::string& provider);

matrix* getBranchFlowsSensitivityMatrix(void* sensitivityAnalysisResultContext, const std::string &contingencyId);

matrix* getBusVoltagesSensitivityMatrix(void* sensitivityAnalysisResultContext, const std::string &contingencyId);

matrix* getReferenceFlows(void* sensitivityAnalysisResultContext, const std::string& contingencyId);

matrix* getReferenceVoltages(void* sensitivityAnalysisResultContext, const std::string& contingencyId);

SeriesArray* createNetworkElementsSeriesArray(void* network, element_type elementType);

int getSeriesType(element_type elementType, const std::string& seriesName);

void updateNetworkElementsWithIntSeries(void* network, element_type elementType, const std::string& seriesName, const std::vector<std::string>& ids,
                                        const std::vector<int>& values, int elementCount);

void updateNetworkElementsWithDoubleSeries(void* network, element_type elementType, const std::string& seriesName, const std::vector<std::string>& ids,
                                           const std::vector<double>& values, int elementCount);

void updateNetworkElementsWithStringSeries(void* network, element_type elementType, const std::string& seriesName, const std::vector<std::string>& ids,
                                           const std::vector<std::string>& values, int elementCount);

void destroyObjectHandle(void* objectHandle);

}

#endif //PYPOWSYBL_H
