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

#include <pybind11/pybind11.h>
#include <pybind11/buffer_info.h>
namespace py = pybind11;

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

typedef Array<slack_bus_result> SlackBusResultArray;
typedef Array<loadflow_component_result> LoadFlowComponentResultArray;
typedef Array<post_contingency_result> PostContingencyResultArray;
typedef Array<operator_strategy_result> OperatorStrategyResultArray;
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
/**
 * Creates a map from the structure and ***FREE*** the memory of the pointer !
*/
std::map<std::string, std::string> convertMapStructToStdMap(string_map* map);

enum class LoadFlowComponentStatus {
    CONVERGED = 0,
    MAX_ITERATION_REACHED,
    FAILED,
    NO_CALCULATION,
};

enum class PostContingencyComputationStatus {
    CONVERGED = 0,
    MAX_ITERATION_REACHED,
    SOLVER_FAILED,
    FAILED,
    NO_IMPACT
};

enum LimitType {
    ACTIVE_POWER = 0,
    APPARENT_POWER,
    CURRENT,
    LOW_VOLTAGE,
    HIGH_VOLTAGE,
    LOW_VOLTAGE_ANGLE,
    HIGH_VOLTAGE_ANGLE,
    LOW_SHORT_CIRCUIT_CURRENT,
    HIGH_SHORT_CIRCUIT_CURRENT,
    OTHER
};

enum VoltageInitMode {
    UNIFORM_VALUES = 0,
    PREVIOUS_VALUES,
    DC_VALUES,
};

enum BalanceType {
    PROPORTIONAL_TO_GENERATION_P = 0,
    PROPORTIONAL_TO_GENERATION_P_MAX,
    PROPORTIONAL_TO_GENERATION_REMAINING_MARGIN,
    PROPORTIONAL_TO_GENERATION_PARTICIPATION_FACTOR,
    PROPORTIONAL_TO_LOAD,
    PROPORTIONAL_TO_CONFORM_LOAD,
};

enum ConnectedComponentMode {
    MAIN = 0,
    ALL,
};

enum DefaultXnecProvider {
    GT_5_PERC_ZONE_TO_ZONE_PTDF = 0,
    ALL_BRANCHES,
    INTERCONNECTIONS,
};

enum OutputWriter {
    CSV = 0,
    CSV_MULTILINE,
};

enum XnecSelectionStrategy {
    ONLY_INTERCONNECTIONS = 0,
    INTERCONNECTION_OR_ZONE_TO_ZONE_PTDF_GT_5PC,
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
    LoadFlowParameters(loadflow_parameters* src);
    std::shared_ptr<loadflow_parameters> to_c_struct() const;
    void load_to_c_struct(loadflow_parameters& params) const;

    VoltageInitMode voltage_init_mode;
    bool transformer_voltage_control_on;
    bool use_reactive_limits;
    bool phase_shifter_regulation_on;
    bool twt_split_shunt_admittance;
    bool shunt_compensator_voltage_control_on;
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

class LoadFlowValidationParameters {
public:
    LoadFlowValidationParameters(loadflow_validation_parameters* src);
    std::shared_ptr<loadflow_validation_parameters> to_c_struct() const;
    void load_to_c_struct(loadflow_validation_parameters& params) const;

    LoadFlowParameters loadflow_parameters;
    double threshold;
    bool verbose;
    std::string loadflow_name;
    double epsilon_x;
    bool apply_reactance_correction;
    bool ok_missing_values;
    bool no_requirement_if_reactive_bound_inversion;
    bool compare_results;
    bool check_main_component_only;
    bool no_requirement_if_setpoint_outside_power_bounds;
};

class SecurityAnalysisParameters {
public:
    SecurityAnalysisParameters(security_analysis_parameters* src);
    std::shared_ptr<security_analysis_parameters> to_c_struct() const;

    LoadFlowParameters loadflow_parameters;
    double flow_proportional_threshold;
    double low_voltage_proportional_threshold;
    double low_voltage_absolute_threshold;
    double high_voltage_proportional_threshold;
    double high_voltage_absolute_threshold;
    std::vector<std::string> provider_parameters_keys;
    std::vector<std::string> provider_parameters_values;
};

class SensitivityAnalysisParameters {
public:
    SensitivityAnalysisParameters(sensitivity_analysis_parameters* src);
    std::shared_ptr<sensitivity_analysis_parameters> to_c_struct() const;

    LoadFlowParameters loadflow_parameters;
    std::vector<std::string> provider_parameters_keys;
    std::vector<std::string> provider_parameters_values;
};

class FlowDecompositionParameters {
public:
    FlowDecompositionParameters(flow_decomposition_parameters* src);
    std::shared_ptr<flow_decomposition_parameters> to_c_struct() const;

    bool enable_losses_compensation;
    float losses_compensation_epsilon;
    float sensitivity_epsilon;
    bool rescale_enabled;
    bool dc_fallback_enabled_after_ac_divergence;
    int sensitivity_variable_batch_size;
};

class SldParameters {
public:
    SldParameters(sld_parameters* src);
    std::shared_ptr<sld_parameters> to_c_struct() const;
    void sld_to_c_struct(sld_parameters& params) const;

    bool use_name;
    bool center_name;
    bool diagonal_label;
    bool nodes_infos;
    bool tooltip_enabled;
    bool topological_coloring;
    std::string component_library;
};

enum class NadLayoutType {
    FORCE_LAYOUT = 0,
    GEOGRAPHICAL
};

class NadParameters {
public:
    NadParameters(nad_parameters* src);
    std::shared_ptr<nad_parameters> to_c_struct() const;
    void nad_to_c_struct(nad_parameters& params) const;

    bool edge_name_displayed;
    bool edge_info_along_edge;
    bool id_displayed;
    int power_value_precision;
    int current_value_precision;
    int angle_value_precision;
    int voltage_value_precision;
    bool bus_legend;
    bool substation_description_displayed;
    NadLayoutType layout_type;
    int scaling_factor;
    double radius_factor;
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

JavaHandle merge(std::vector<JavaHandle>& others);

JavaHandle getSubNetwork(const JavaHandle& network, const std::string& subNetworkId);

JavaHandle detachSubNetwork(const JavaHandle& subNetwork);

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

JavaHandle loadNetwork(const std::string& file, const std::map<std::string, std::string>& parameters, JavaHandle* reportNode);

JavaHandle loadNetworkFromString(const std::string& fileName, const std::string& fileContent, const std::map<std::string, std::string>& parameters, JavaHandle* reportNode);

JavaHandle loadNetworkFromBinaryBuffers(std::vector<py::buffer> byteBuffer, const std::map<std::string, std::string>& parameters, JavaHandle* reportNode);

void saveNetwork(const JavaHandle& network, const std::string& file, const std::string& format, const std::map<std::string, std::string>& parameters, JavaHandle* reportNode);

LoadFlowParameters* createLoadFlowParameters();

std::vector<std::string> getLoadFlowProviderParametersNames(const std::string& loadFlowProvider);

SeriesArray* createLoadFlowProviderParametersSeriesArray(const std::string& provider);

LoadFlowValidationParameters* createValidationConfig();

SecurityAnalysisParameters* createSecurityAnalysisParameters();

std::vector<std::string> getSecurityAnalysisProviderParametersNames(const std::string& securityAnalysisProvider);

SensitivityAnalysisParameters* createSensitivityAnalysisParameters();

std::vector<std::string> getSensitivityAnalysisProviderParametersNames(const std::string& sensitivityAnalysisProvider);

std::string saveNetworkToString(const JavaHandle& network, const std::string& format, const std::map<std::string, std::string>& parameters, JavaHandle* reportNode);

py::bytes saveNetworkToBinaryBuffer(const JavaHandle& network, const std::string& format, const std::map<std::string, std::string>& parameters, JavaHandle* reportNode);

void reduceNetwork(const JavaHandle& network, const double v_min, const double v_max, const std::vector<std::string>& ids, const std::vector<std::string>& vls, const std::vector<int>& depths, bool withDangLingLines);

LoadFlowComponentResultArray* runLoadFlow(const JavaHandle& network, bool dc, const LoadFlowParameters& parameters, const std::string& provider, JavaHandle* reportNode);

SeriesArray* runLoadFlowValidation(const JavaHandle& network, validation_type validationType, const LoadFlowValidationParameters& validationParameters);

void writeSingleLineDiagramSvg(const JavaHandle& network, const std::string& containerId, const std::string& svgFile, const std::string& metadataFile, const SldParameters& parameters);

void writeMatrixMultiSubstationSingleLineDiagramSvg(const JavaHandle& network, const std::vector<std::vector<std::string>>& matrixIds, const std::string& svgFile, const std::string& metadataFile, const SldParameters& parameters);

std::string getSingleLineDiagramSvg(const JavaHandle& network, const std::string& containerId);

std::vector<std::string> getSingleLineDiagramSvgAndMetadata(const JavaHandle& network, const std::string& containerId, const SldParameters& parameters);

std::vector<std::string> getSingleLineDiagramComponentLibraryNames();

void writeNetworkAreaDiagramSvg(const JavaHandle& network, const std::string& svgFile, const std::vector<std::string>& voltageLevelIds, int depth, double highNominalVoltageBound, double lowNominalVoltageBound, const NadParameters& parameters);

std::string getNetworkAreaDiagramSvg(const JavaHandle& network, const std::vector<std::string>& voltageLevelIds, int depth, double highNominalVoltageBound, double lowNominalVoltageBound, const NadParameters& parameters);

std::vector<std::string> getNetworkAreaDiagramDisplayedVoltageLevels(const JavaHandle& network, const std::vector<std::string>& voltageLevelIds, int depth);

JavaHandle createSecurityAnalysis();

void addContingency(const JavaHandle& analysisContext, const std::string& contingencyId, const std::vector<std::string>& elementsIds);

JavaHandle runSecurityAnalysis(const JavaHandle& securityAnalysisContext, const JavaHandle& network, const SecurityAnalysisParameters& parameters, const std::string& provider, bool dc, JavaHandle* reportNode);

JavaHandle createSensitivityAnalysis();

void addLoadActivePowerAction(const JavaHandle& analysisContext, const std::string& actionId, const std::string& loadId, bool relativeValue, double activePower);

void addLoadReactivePowerAction(const JavaHandle& analysisContext, const std::string& actionId, const std::string& loadId, bool relativeValue, double reactivePower);

void addGeneratorActivePowerAction(const JavaHandle& analysisContext, const std::string& actionId, const std::string& generatorId, bool relativeValue, double activePower);

void addSwitchAction(const JavaHandle& analysisContext, const std::string& actionId, const std::string& switchId, bool open);

void addPhaseTapChangerPositionAction(const JavaHandle& analysisContext, const std::string& actionId, const std::string& transformerId, bool isRelative, int tapPosition, ThreeSide side);

void addRatioTapChangerPositionAction(const JavaHandle& analysisContext, const std::string& actionId, const std::string& transformerId, bool isRelative, int tapPosition, ThreeSide side);

void addShuntCompensatorPositionAction(const JavaHandle& analysisContext, const std::string& actionId, const std::string& shuntId, int sectionCount);

void addOperatorStrategy(const JavaHandle& analysisContext, std::string operatorStrategyId, std::string contingencyId, const std::vector<std::string>& actionsIds,
                         condition_type conditionType, const std::vector<std::string>& subjectIds, const std::vector<violation_type>& violationTypesFilters);

void setZones(const JavaHandle& sensitivityAnalysisContext, const std::vector<::zone*>& zones);

void addFactorMatrix(const JavaHandle& sensitivityAnalysisContext, std::string matrixId, const std::vector<std::string>& branchesIds,
                     const std::vector<std::string>& variablesIds, const std::vector<std::string>& contingenciesIds, contingency_context_type ContingencyContextType,
                     sensitivity_function_type sensitivityFunctionType, sensitivity_variable_type sensitivityVariableType);

JavaHandle runSensitivityAnalysis(const JavaHandle& sensitivityAnalysisContext, const JavaHandle& network, bool dc, SensitivityAnalysisParameters& parameters, const std::string& provider, JavaHandle* reportNode);

matrix* getSensitivityMatrix(const JavaHandle& sensitivityAnalysisResultContext, const std::string& matrixId, const std::string &contingencyId);

matrix* getReferenceMatrix(const JavaHandle& sensitivityAnalysisResultContext, const std::string& matrixId, const std::string& contingencyId);

SeriesArray* createNetworkElementsSeriesArray(const JavaHandle& network, element_type elementType, filter_attributes_type filterAttributesType, const std::vector<std::string>& attributes, dataframe* dataframe, bool perUnit, double nominalApparentPower);

void removeNetworkElements(const JavaHandle& network, const std::vector<std::string>& elementIds);

SeriesArray* createNetworkElementsExtensionSeriesArray(const JavaHandle& network, const std::string& extensionName, const std::string& tableName);

std::vector<std::string> getExtensionsNames();

SeriesArray* getExtensionsInformation();

void updateNetworkElementsWithSeries(pypowsybl::JavaHandle network, dataframe* dataframe, element_type elementType, bool perUnit, double nominalApparentPower);

std::string getWorkingVariantId(const JavaHandle& network);

void setWorkingVariant(const JavaHandle& network, std::string& variant);

void removeVariant(const JavaHandle& network, std::string& variant);

void cloneVariant(const JavaHandle& network, std::string& src, std::string& variant, bool mayOverwrite);

std::vector<std::string> getVariantsIds(const JavaHandle& network);

void addMonitoredElements(const JavaHandle& securityAnalysisContext, contingency_context_type contingencyContextType, const std::vector<std::string>& branchIds,
                      const std::vector<std::string>& voltageLevelIds, const std::vector<std::string>& threeWindingsTransformerIds,
                      const std::vector<std::string>& contingencyIds);

SeriesArray* getLimitViolations(const JavaHandle& securityAnalysisResult);

PostContingencyResultArray* getPostContingencyResults(const JavaHandle& securityAnalysisResult);

OperatorStrategyResultArray* getOperatorStrategyResults(const JavaHandle& securityAnalysisResult);

pre_contingency_result* getPreContingencyResult(const JavaHandle& securityAnalysisResult);

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

void updateNetworkElementsExtensionsWithSeries(pypowsybl::JavaHandle network, std::string& name, std::string& tableName, dataframe* dataframe);

void removeExtensions(const JavaHandle& network, std::string& name, const std::vector<std::string>& ids);

std::vector<SeriesMetadata> getNetworkExtensionsDataframeMetadata(std::string& name, std::string& tableName);

std::vector<std::vector<SeriesMetadata>> getNetworkExtensionsCreationDataframesMetadata(std::string& name);

void createExtensions(pypowsybl::JavaHandle network, dataframe_array* dataframes, std::string& name);

JavaHandle createReportNode(const std::string& taskKey, const std::string& defaultName);

std::string printReport(const JavaHandle& reportNode);

std::string jsonReport(const JavaHandle& reportNode);

JavaHandle createGLSKdocument(std::string& filename);

std::vector<std::string> getGLSKinjectionkeys(pypowsybl::JavaHandle network, const JavaHandle& importer, std::string& country, long instant);

std::vector<std::string> getGLSKcountries(const JavaHandle& importer);

std::vector<double> getGLSKInjectionFactors(pypowsybl::JavaHandle network, const JavaHandle& importer, std::string& country, long instant);

long getInjectionFactorStartTimestamp(const JavaHandle& importer);

long getInjectionFactorEndTimestamp(const JavaHandle& importer);

JavaHandle createFlowDecomposition();

void addContingencyForFlowDecomposition(const JavaHandle& flowDecompositionContext, const std::string& contingencyId, const std::vector<std::string>& elementsIds);

void addPrecontingencyMonitoredElementsForFlowDecomposition(const JavaHandle& flowDecompositionContext, const std::vector<std::string>& branchIds);

void addPostcontingencyMonitoredElementsForFlowDecomposition(const JavaHandle& flowDecompositionContext, const std::vector<std::string>& branchIds, const std::vector<std::string>& contingencyIds);

void addAdditionalXnecProviderForFlowDecomposition(const JavaHandle& flowDecompositionContext, DefaultXnecProvider defaultXnecProvider);

SeriesArray* runFlowDecomposition(const JavaHandle& flowDecompositionContext, const JavaHandle& network, const FlowDecompositionParameters& flow_decomposition_parameters, const LoadFlowParameters& loadflow_parameters);

FlowDecompositionParameters* createFlowDecompositionParameters();

SeriesArray* getConnectablesOrderPositions(const JavaHandle& network, const std::string voltage_level_id);

std::vector<int> getUnusedConnectableOrderPositions(pypowsybl::JavaHandle network, std::string busbarSectionId, std::string beforeOrAfter);

void removeAliases(pypowsybl::JavaHandle network, dataframe* dataframe);

void closePypowsybl();

void removeElementsModification(pypowsybl::JavaHandle network, const std::vector<std::string>& connectableIds, dataframe* dataframe, remove_modification_type removeModificationType, bool throwException, JavaHandle* reportNode);

SldParameters* createSldParameters();

NadParameters* createNadParameters();

//=======dynamic modeling for dynawaltz package==========

//handle creation
JavaHandle createDynamicSimulationContext();
JavaHandle createDynamicModelMapping();
JavaHandle createTimeseriesMapping();
JavaHandle createEventMapping();

JavaHandle runDynamicModel(JavaHandle dynamicModelContext, JavaHandle network, JavaHandle dynamicMapping, JavaHandle eventMapping, JavaHandle timeSeriesMapping, int start, int stop);

// timeseries/curves mapping
void addCurve(JavaHandle curveMappingHandle, std::string dynamicId, std::string variable);

// events mapping
void addEventDisconnection(const JavaHandle& eventMappingHandle, const std::string& staticId, double eventTime, int disconnectOnly);

// dynamic model mapping
void addDynamicMappings(JavaHandle dynamicMappingHandle, DynamicMappingType mappingType, dataframe* mappingDf);
std::vector<SeriesMetadata> getDynamicMappingsMetaData(DynamicMappingType mappingType);

// results
std::string getDynamicSimulationResultsStatus(JavaHandle dynamicSimulationResultsHandle);
SeriesArray* getDynamicCurve(JavaHandle resultHandle, std::string curveName);
std::vector<std::string> getAllDynamicCurvesIds(JavaHandle resultHandle);

//=======END OF dynamic modeling for dynawaltz package==========

//=======Voltage initializer mapping========

JavaHandle createVoltageInitializerParams();
void voltageInitializerAddSpecificLowVoltageLimits(const JavaHandle& paramsHandle, const std::string& voltageLevelId, bool isRelative, double limit);
void voltageInitializerAddSpecificHighVoltageLimits(const JavaHandle& paramsHandle, const std::string& voltageLevelId, bool isRelative, double limit);
void voltageInitializerAddVariableShuntCompensators(const JavaHandle& paramsHandle, const std::string& idPtr);
void voltageInitializerAddConstantQGenerators(const JavaHandle& paramsHandle, const std::string& idPtr);
void voltageInitializerAddVariableTwoWindingsTransformers(const JavaHandle& paramsHandle, const std::string& idPtr);
void voltageInitializerSetObjective(const JavaHandle& paramsHandle, VoltageInitializerObjective cObjective);
void voltageInitializerSetObjectiveDistance(const JavaHandle& paramsHandle, double dist);
void voltageInitializerApplyAllModifications(const JavaHandle& resultHandle, const JavaHandle& networkHandle);
VoltageInitializerStatus voltageInitializerGetStatus(const JavaHandle& resultHandle);
std::map<std::string, std::string> voltageInitializerGetIndicators(const JavaHandle& resultHandle);
JavaHandle runVoltageInitializer(bool debug, const JavaHandle& networkHandle, const JavaHandle& paramsHandle);

//=======End of voltage initializer mapping========

std::vector<SeriesMetadata> getModificationMetadata(network_modification_type networkModificationType);

std::vector<std::vector<SeriesMetadata>> getModificationMetadataWithElementType(network_modification_type networkModificationType, element_type elementType);

void createNetworkModification(pypowsybl::JavaHandle network, dataframe_array* dataframe, network_modification_type networkModificationType, bool throwException, JavaHandle* reportNode);

//=======short-circuit analysis==========
enum ShortCircuitStudyType {
    SUB_TRANSIENT = 0,
    TRANSIENT,
    STEADY_STATE
};

class ShortCircuitAnalysisParameters {
public:
    ShortCircuitAnalysisParameters(shortcircuit_analysis_parameters* src);
    std::shared_ptr<shortcircuit_analysis_parameters> to_c_struct() const;

    bool with_voltage_result;
    bool with_feeder_result;
    bool with_limit_violations;
    ShortCircuitStudyType study_type;
    bool with_fortescue_result;
    double min_voltage_drop_proportional_threshold;

    std::vector<std::string> provider_parameters_keys;
    std::vector<std::string> provider_parameters_values;
};

void setDefaultShortCircuitAnalysisProvider(const std::string& shortCircuitAnalysisProvider);
std::string getDefaultShortCircuitAnalysisProvider();
std::vector<std::string> getShortCircuitAnalysisProviderNames();
ShortCircuitAnalysisParameters* createShortCircuitAnalysisParameters();
std::vector<std::string> getShortCircuitAnalysisProviderParametersNames(const std::string& shortCircuitAnalysisProvider);
JavaHandle createShortCircuitAnalysis();
JavaHandle runShortCircuitAnalysis(const JavaHandle& shortCircuitAnalysisContext, const JavaHandle& network, const ShortCircuitAnalysisParameters& parameters, const std::string& provider, JavaHandle* reportNode);
std::vector<SeriesMetadata> getFaultsMetaData();
void setFaults(pypowsybl::JavaHandle analysisContext, dataframe* dataframe);
SeriesArray* getFaultResults(const JavaHandle& shortCircuitAnalysisResult, bool withFortescueResult);
SeriesArray* getFeederResults(const JavaHandle& shortCircuitAnalysisResult, bool withFortescueResult);
SeriesArray* getShortCircuitLimitViolations(const JavaHandle& shortCircuitAnalysisResult);
SeriesArray* getShortCircuitBusResults(const JavaHandle& shortCircuitAnalysisResult, bool withFortescueResult);

}
#endif //PYPOWSYBL_H
