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
class JavaHandle {
public:
    //Implicit constructor from void* returned by graalvm
    JavaHandle(void* handle);
    ~JavaHandle() {}

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

void setConfigRead(bool configRead);

bool isConfigRead();

std::string getVersionTable();

JavaHandle createNetwork(const std::string& name, const std::string& id);

bool updateSwitchPosition(const JavaHandle& network, const std::string& id, bool open);

bool updateConnectableStatus(const JavaHandle& network, const std::string& id, bool connected);

std::vector<std::string> getNetworkElementsIds(const JavaHandle& network, element_type elementType, const std::vector<double>& nominalVoltages,
                                               const std::vector<std::string>& countries, bool mainCc, bool mainSc,
                                               bool notConnectedToSameBusAtBothSides);

std::vector<std::string> getNetworkImportFormats();

std::vector<std::string> getNetworkExportFormats();

SeriesArray* createImporterParametersSeriesArray(const std::string& format);

std::shared_ptr<network_metadata> getNetworkMetadata(const JavaHandle& network);

JavaHandle loadNetwork(const std::string& file, const std::map<std::string, std::string>& parameters);

JavaHandle loadNetworkFromString(const std::string& fileName, const std::string& fileContent, const std::map<std::string, std::string>& parameters);

void dumpNetwork(const JavaHandle& network, const std::string& file, const std::string& format, const std::map<std::string, std::string>& parameters);

std::shared_ptr<load_flow_parameters> createLoadFlowParameters();

std::string dumpNetworkToString(const JavaHandle& network, const std::string& format, const std::map<std::string, std::string>& parameters);

void reduceNetwork(const JavaHandle& network, const double v_min, const double v_max, const std::vector<std::string>& ids, const std::vector<std::string>& vls, const std::vector<int>& depths, bool withDangLingLines);

LoadFlowComponentResultArray* runLoadFlow(const JavaHandle& network, bool dc, const std::shared_ptr<load_flow_parameters>& parameters, const std::string& provider);

void writeSingleLineDiagramSvg(const JavaHandle& network, const std::string& containerId, const std::string& svgFile);

std::string getSingleLineDiagramSvg(const JavaHandle& network, const std::string& containerId);

JavaHandle createSecurityAnalysis();

void addContingency(const JavaHandle& analysisContext, const std::string& contingencyId, const std::vector<std::string>& elementsIds);

JavaHandle runSecurityAnalysis(const JavaHandle& securityAnalysisContext, const JavaHandle& network, load_flow_parameters& parameters, const std::string& provider);

JavaHandle createSensitivityAnalysis();

void setZones(const JavaHandle& sensitivityAnalysisContext, const std::vector<::zone*>& zones);

void setBranchFlowFactorMatrix(const JavaHandle& sensitivityAnalysisContext, const std::vector<std::string>& branchesIds, const std::vector<std::string>& variablesIds);

void setBusVoltageFactorMatrix(const JavaHandle& sensitivityAnalysisContext, const std::vector<std::string>& busIds, const std::vector<std::string>& targetVoltageIds);

JavaHandle runSensitivityAnalysis(const JavaHandle& sensitivityAnalysisContext, const JavaHandle& network, bool dc, load_flow_parameters& parameters, const std::string& provider);

matrix* getBranchFlowsSensitivityMatrix(const JavaHandle& sensitivityAnalysisResultContext, const std::string &contingencyId);

matrix* getBusVoltagesSensitivityMatrix(const JavaHandle& sensitivityAnalysisResultContext, const std::string &contingencyId);

matrix* getReferenceFlows(const JavaHandle& sensitivityAnalysisResultContext, const std::string& contingencyId);

matrix* getReferenceVoltages(const JavaHandle& sensitivityAnalysisResultContext, const std::string& contingencyId);

SeriesArray* createNetworkElementsSeriesArray(const JavaHandle& network, element_type elementType);

int getSeriesType(element_type elementType, const std::string& seriesName);

void updateNetworkElementsWithIntSeries(const JavaHandle& network, element_type elementType, const std::string& seriesName, const std::vector<std::string>& ids,
                                        const std::vector<int>& values, int elementCount);

void updateNetworkElementsWithDoubleSeries(const JavaHandle& network, element_type elementType, const std::string& seriesName, const std::vector<std::string>& ids,
                                           const std::vector<double>& values, int elementCount);

void updateNetworkElementsWithStringSeries(const JavaHandle& network, element_type elementType, const std::string& seriesName, const std::vector<std::string>& ids,
                                           const std::vector<std::string>& values, int elementCount);

std::string getWorkingVariantId(const JavaHandle& network);

void setWorkingVariant(const JavaHandle& network, std::string& variant);

void removeVariant(const JavaHandle& network, std::string& variant);

void cloneVariant(const JavaHandle& network, std::string& src, std::string& variant, bool mayOverwrite);

std::vector<std::string> getVariantsIds(const JavaHandle& network);

void addMonitoredElements(const JavaHandle& securityAnalysisContext, contingency_context_type contingencyContextType, const std::vector<std::string>& branchIds,
                      const std::vector<std::string>& voltageLevelIds, const std::vector<std::string>& threeWindingsTransformerIds,
                      const std::vector<std::string>& contingencyIds);

SeriesArray* getLimitViolations(const JavaHandle& securityAnalysisResult);

ContingencyResultArray* getSecurityAnalysisResult(const JavaHandle& securityAnalysisResult);

SeriesArray* getBranchResults(const JavaHandle& securityAnalysisResult);

SeriesArray* getBusResults(const JavaHandle& securityAnalysisResult);

SeriesArray* getThreeWindingsTransformerResults(const JavaHandle& securityAnalysisResult);

SeriesArray* getNodeBreakerViewSwitches(const JavaHandle& network,std::string& voltageLevel);

SeriesArray* getNodeBreakerViewNodes(const JavaHandle& network,std::string& voltageLevel);

SeriesArray* getNodeBreakerViewInternalConnections(const JavaHandle& network,std::string& voltageLevel);

}

#endif //PYPOWSYBL_H
