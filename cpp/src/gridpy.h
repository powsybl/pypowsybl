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

void init();

void printVersion();

void* createEmptyNetwork(const std::string& id);

void* createIeee14Network();

bool updateSwitchPosition(void* network, const std::string& id, bool open);

bool updateConnectableStatus(void* network, const std::string& id, bool connected);

void* loadNetwork(const std::string& file);

void dumpNetwork(void* network, const std::string& file, const std::string& format);

LoadFlowComponentResultArray* runLoadFlow(void* network, bool distributedSlack, bool dc);

BusArray* getBusArray(void* network, bool busBreakerView);

void writeSingleLineDiagramSvg(void* network, const std::string& containerId, const std::string& svgFile);

void* createSecurityAnalysis();

void addContingencyToSecurityAnalysis(void* securityAnalysisContext, const std::string& contingencyId, const std::vector<std::string>& elementsIds);

ContingencyResultArray* runSecurityAnalysis(void* securityAnalysisContext, void* network);

void destroyObjectHandle(void* objectHandle);

}

#endif //GRIDPY_H
