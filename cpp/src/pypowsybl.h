/**
 * Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
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


class SeriesMetadata {
public:
    SeriesMetadata(const std::string& name, int type, bool isIndex, bool isModifiable, bool isDefault):
        name_(name),
        type_(type),
        isIndex_(isIndex),
        isModifiable_(isModifiable),
        isDefault_(isDefault) {
    }

    const std::string& name() const { return name_; }
    int type() const { return type_; }
    bool isIndex() const { return isIndex_; }
    bool isModifiable() const { return isModifiable_; }
    bool isDefault() const { return isDefault_; }

private:
    std::string name_;
    int type_;
    bool isIndex_;
    bool isModifiable_;
    bool isDefault_;
};


class LoadFlowParameters {
public:
    LoadFlowParameters(load_flow_parameters* src);
    std::shared_ptr<load_flow_parameters> to_c_struct() const;
    void load_to_c_struct(load_flow_parameters& params) const;

    VoltageInitMode voltage_init_mode;
    bool transformer_voltage_control_on;
    bool no_generator_reactive_limits;
    bool phase_shifter_regulation_on;
    bool twt_split_shunt_admittance;
    bool simul_shunt;
    bool read_slack_bus;
    bool write_slack_bus;
    bool distributed_slack;
    BalanceType balance_type;
    bool dc_use_transformer_ratio;
    std::vector<std::string> countries_to_balance;
    ConnectedComponentMode connected_component_mode;
    std::vector<std::string> provider_parameters_keys;
    std::vector<std::string> provider_parameters_values;
};

class SecurityAnalysisParameters {
public:
    SecurityAnalysisParameters(security_analysis_parameters* src);
    std::shared_ptr<security_analysis_parameters> to_c_struct() const;
    
    LoadFlowParameters sa_load_flow_parameters;
    double flow_proportional_threshold;
    double low_voltage_proportional_threshold;
    double low_voltage_absolute_threshold;
    double high_voltage_proportional_threshold;
    double high_voltage_absolute_threshold;
    std::vector<std::string> provider_parameters_keys;
    std::vector<std::string> provider_parameters_values;
};


char* copyStringToCharPtr(const std::string& str);
char** copyVectorStringToCharPtrPtr(const std::vector<std::string>& strings);
int* copyVectorInt(const std::vector<int>& ints);
double* copyVectorDouble(const std::vector<double>& doubles);

void deleteCharPtrPtr(char** charPtrPtr, int length);

::zone* createZone(const std::string& id, const std::vector<std::string>& injectionsIds, const std::vector<double>& injectionsShiftKeys);

void init();

void setJavaLibraryPath(const std::string& javaLibraryPath);

void setConfigRead(bool configRead);

void setDefaultLoadFlowProvider(const std::string& loadFlowProvider);

void setDefaultSecurityAnalysisProvider(const std::string& securityAnalysisProvider);

void setDefaultSensitivityAnalysisProvider(const std::string& sensitivityAnalysisProvider);

bool isConfigRead();

std::string getDefaultLoadFlowProvider();

std::string getDefaultSecurityAnalysisProvider();

std::string getDefaultSensitivityAnalysisProvider();

std::string getVersionTable();

JavaHandle createNetwork(const std::string& name, const std::string& id);

void merge(JavaHandle network, std::vector<JavaHandle>& others);

bool updateSwitchPosition(const JavaHandle& network, const std::string& id, bool open);

bool updateConnectableStatus(const JavaHandle& network, const std::string& id, bool connected);

std::vector<std::string> getNetworkElementsIds(const JavaHandle& network, element_type elementType, const std::vector<double>& nominalVoltages,
                                               const std::vector<std::string>& countries, bool mainCc, bool mainSc,
                                               bool notConnectedToSameBusAtBothSides);

std::vector<std::string> getNetworkImportFormats();

std::vector<std::string> getNetworkExportFormats();

std::vector<std::string> getLoadFlowProviderNames();

std::vector<std::string> getSecurityAnalysisProviderNames();

std::vector<std::string> getSensitivityAnalysisProviderNames();

SeriesArray* createImporterParametersSeriesArray(const std::string& format);

SeriesArray* createExporterParametersSeriesArray(const std::string& format);

std::shared_ptr<network_metadata> getNetworkMetadata(const JavaHandle& network);

JavaHandle loadNetwork(const std::string& file, const std::map<std::string, std::string>& parameters, JavaHandle* reporter);

JavaHandle loadNetworkFromString(const std::string& fileName, const std::string& fileContent, const std::map<std::string, std::string>& parameters, JavaHandle* reporter);

void dumpNetwork(const JavaHandle& network, const std::string& file, const std::string& format, const std::map<std::string, std::string>& parameters, JavaHandle* reporter);

LoadFlowParameters* createLoadFlowParameters();

std::vector<std::string> getLoadFlowProviderParametersNames(const std::string& loadFlowProvider);

SecurityAnalysisParameters* createSecurityAnalysisParameters();

std::vector<std::string> getSecurityAnalysisProviderParametersNames(const std::string& securityAnalysisProvider);

std::string dumpNetworkToString(const JavaHandle& network, const std::string& format, const std::map<std::string, std::string>& parameters, JavaHandle* reporter);

void reduceNetwork(const JavaHandle& network, const double v_min, const double v_max, const std::vector<std::string>& ids, const std::vector<std::string>& vls, const std::vector<int>& depths, bool withDangLingLines);

LoadFlowComponentResultArray* runLoadFlow(const JavaHandle& network, bool dc, const LoadFlowParameters& parameters, const std::string& provider, JavaHandle* reporter);

SeriesArray* runLoadFlowValidation(const JavaHandle& network, validation_type validationType);

void writeSingleLineDiagramSvg(const JavaHandle& network, const std::string& containerId, const std::string& svgFile);

std::string getSingleLineDiagramSvg(const JavaHandle& network, const std::string& containerId);

void writeNetworkAreaDiagramSvg(const JavaHandle& network, const std::string& svgFile, const std::vector<std::string>& voltageLevelIds, int depth);

std::string getNetworkAreaDiagramSvg(const JavaHandle& network, const std::vector<std::string>& voltageLevelIds, int depth);

JavaHandle createSecurityAnalysis();

void addContingency(const JavaHandle& analysisContext, const std::string& contingencyId, const std::vector<std::string>& elementsIds);

JavaHandle runSecurityAnalysis(const JavaHandle& securityAnalysisContext, const JavaHandle& network, const SecurityAnalysisParameters& parameters, const std::string& provider, bool dc, JavaHandle* reporter);

JavaHandle createSensitivityAnalysis();

void setZones(const JavaHandle& sensitivityAnalysisContext, const std::vector<::zone*>& zones);

void addBranchFlowFactorMatrix(const JavaHandle& sensitivityAnalysisContext, std::string matrixId, const std::vector<std::string>& branchesIds,
                               const std::vector<std::string>& variablesIds);

void addPreContingencyBranchFlowFactorMatrix(const JavaHandle& sensitivityAnalysisContext, std::string matrixId, const std::vector<std::string>& branchesIds,
                                             const std::vector<std::string>& variablesIds);

void addPostContingencyBranchFlowFactorMatrix(const JavaHandle& sensitivityAnalysisContext, std::string matrixId, const std::vector<std::string>& branchesIds,
                                              const std::vector<std::string>& variablesIds, const std::vector<std::string>& contingenciesIds);

void setBusVoltageFactorMatrix(const JavaHandle& sensitivityAnalysisContext, const std::vector<std::string>& busIds, const std::vector<std::string>& targetVoltageIds);

JavaHandle runSensitivityAnalysis(const JavaHandle& sensitivityAnalysisContext, const JavaHandle& network, bool dc, const LoadFlowParameters& parameters, const std::string& provider, JavaHandle* reporter);

matrix* getBranchFlowsSensitivityMatrix(const JavaHandle& sensitivityAnalysisResultContext, const std::string& matrixId, const std::string &contingencyId);

matrix* getBusVoltagesSensitivityMatrix(const JavaHandle& sensitivityAnalysisResultContext, const std::string &contingencyId);

matrix* getReferenceFlows(const JavaHandle& sensitivityAnalysisResultContext, const std::string& matrixId, const std::string& contingencyId);

matrix* getReferenceVoltages(const JavaHandle& sensitivityAnalysisResultContext, const std::string& contingencyId);

SeriesArray* createNetworkElementsSeriesArray(const JavaHandle& network, element_type elementType, filter_attributes_type filterAttributesType, const std::vector<std::string>& attributes, dataframe* dataframe);

void removeNetworkElements(const JavaHandle& network, const std::vector<std::string>& elementIds);

SeriesArray* createNetworkElementsExtensionSeriesArray(const JavaHandle& network, const std::string& extensionName);

std::vector<std::string> getExtensionsNames();

void updateNetworkElementsWithSeries(pypowsybl::JavaHandle network, dataframe* dataframe, element_type elementType);

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

SeriesArray* getBusBreakerViewSwitches(const JavaHandle& network,std::string& voltageLevel);

SeriesArray* getBusBreakerViewBuses(const JavaHandle& network,std::string& voltageLevel);

SeriesArray* getBusBreakerViewElements(const JavaHandle& network,std::string& voltageLevel);

/**
 * Metadata of the dataframe of network elements data for a given element type.
 */
std::vector<SeriesMetadata> getNetworkDataframeMetadata(element_type elementType);

/**
 * Metadata of the list of dataframes to create network elements of the given type.
 */
std::vector<std::vector<SeriesMetadata>> getNetworkElementCreationDataframesMetadata(element_type elementType);

void createElement(pypowsybl::JavaHandle network, dataframe_array* dataframes, element_type elementType);

::validation_level_type getValidationLevel(const JavaHandle& network);

::validation_level_type validate(const JavaHandle& network);

void setMinValidationLevel(pypowsybl::JavaHandle network, validation_level_type validationLevel);

void setupLoggerCallback(void *& callback);

void addNetworkElementProperties(pypowsybl::JavaHandle network, dataframe* dataframe);

void removeNetworkElementProperties(pypowsybl::JavaHandle network, const std::vector<std::string>& ids, const std::vector<std::string>& properties);

void updateNetworkElementsExtensionsWithSeries(pypowsybl::JavaHandle network, std::string& name, dataframe* dataframe);

void removeExtensions(const JavaHandle& network, std::string& name, const std::vector<std::string>& ids);

std::vector<SeriesMetadata> getNetworkExtensionsDataframeMetadata(std::string& name);

std::vector<std::vector<SeriesMetadata>> getNetworkExtensionsCreationDataframesMetadata(std::string& name);

void createExtensions(pypowsybl::JavaHandle network, dataframe_array* dataframes, std::string& name);

JavaHandle createReporterModel(const std::string& taskKey, const std::string& defaultName);

std::string printReport(const JavaHandle& reporterModel);

JavaHandle createGLSKdocument(std::string& filename);

std::vector<std::string> getGLSKinjectionkeys(pypowsybl::JavaHandle network, const JavaHandle& importer, std::string& country, long instant);

std::vector<std::string> getGLSKcountries(const JavaHandle& importer);

std::vector<double> getGLSKInjectionFactors(pypowsybl::JavaHandle network, const JavaHandle& importer, std::string& country, long instant);

long getInjectionFactorStartTimestamp(const JavaHandle& importer);

long getInjectionFactorEndTimestamp(const JavaHandle& importer);

}

#endif //PYPOWSYBL_H
