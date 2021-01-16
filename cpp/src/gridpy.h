/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
#ifndef GRIDPY_H
#define GRIDPY_H

#include <string>
#include <vector>
#include "gridpy-api.h"

namespace gridpy {

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
typedef Array<bus> BusArray;
typedef Array<contingency_result> ContingencyResultArray;
typedef Array<limit_violation> LimitViolationArray;

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

void init();

void setDebugMode(bool debug);

std::string getVersionTable();

void* createEmptyNetwork(const std::string& id);

void* createIeee14Network();

void* createEurostagTutorialExample1Network();

bool updateSwitchPosition(void* network, const std::string& id, bool open);

bool updateConnectableStatus(void* network, const std::string& id, bool connected);

std::vector<std::string> getNetworkElementsIds(void* network, element_type elementType, double nominalVoltage, bool mainCc);

void* loadNetwork(const std::string& file);

void dumpNetwork(void* network, const std::string& file, const std::string& format);

LoadFlowComponentResultArray* runLoadFlow(void* network, bool dc, load_flow_parameters& parameters);

BusArray* getBusArray(void* network, bool busBreakerView);

void writeSingleLineDiagramSvg(void* network, const std::string& containerId, const std::string& svgFile);

void* createSecurityAnalysis();

void addContingency(void* analysisContext, const std::string& contingencyId, const std::vector<std::string>& elementsIds);

ContingencyResultArray* runSecurityAnalysis(void* securityAnalysisContext, void* network, load_flow_parameters& parameters);

void* createSensitivityAnalysis();

void setFactorMatrix(void* sensitivityAnalysisContext, const std::vector<std::string>& branchesIds, const std::vector<std::string>& injectionsOrTransfosIds);

void* runSensitivityAnalysis(void* sensitivityAnalysisContext, void* network, load_flow_parameters& parameters);

matrix* getSensitivityMatrix(void* sensitivityAnalysisResultContext, const std::string& contingencyId);

void destroyObjectHandle(void* objectHandle);

}

#endif //GRIDPY_H
