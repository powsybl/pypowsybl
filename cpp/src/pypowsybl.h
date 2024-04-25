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
#include "pypowsybl-java.h"

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

class GraalVmGuard {
public:
    GraalVmGuard();

    ~GraalVmGuard() noexcept(false) {
        if (shouldDetach) {
            int c = graal_detach_thread(thread_);
            if (c != 0) {
                throw std::runtime_error("graal_detach_thread error: " + std::to_string(c));
            }
        }
    }

    graal_isolatethread_t * thread() const {
        return thread_;
    }

private:
    bool shouldDetach = false;
    graal_isolatethread_t* thread_ = nullptr;
};

/**
 * Wraps a void* corresponding to a graalvm ObjectHandle
 * in order to handle its destruction.
 */
template <typename JavaCaller>
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

template<typename F, typename... ARGS>
void directCallJava(F f, ARGS... args) {
    pypowsybl::GraalVmGuard guard;
    exception_handler exc;

    f(guard.thread(), args..., &exc);
    if (exc.message) {
        throw pypowsybl::PyPowsyblError(pypowsybl::toString(exc.message));
    }
}

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

template <typename Caller>
class ToStringVector {
public:
    ToStringVector(array* arrayPtr)
        : arrayPtr_(arrayPtr) {
    }

    ~ToStringVector() {
        Caller::callJava(::freeStringArray, arrayPtr_);
    }

    std::vector<std::string> get() {
        return toVector<std::string>(arrayPtr_);
    }

private:
    array* arrayPtr_;
};

template<typename T, typename Caller>
class ToPrimitiveVector {
public:
    ToPrimitiveVector(array* arrayPtr)
        : arrayPtr_(arrayPtr) {
    }

    ~ToPrimitiveVector() {
        Caller::callJava(::freeArray, arrayPtr_);
    }

    std::vector<T> get() {
        return toVector<T>(arrayPtr_);
    }

private:
    array* arrayPtr_;
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

protected:
    array* delegate_;
};

template <typename Caller>
class SlackBusResultArray : public Array<slack_bus_result> {
    public:
    SlackBusResultArray(array* delegate) : Array<slack_bus_result>(delegate){}
    ~SlackBusResultArray();
};

template <typename Caller>
class LoadFlowComponentResultArray : public Array<loadflow_component_result> {
    public:
    LoadFlowComponentResultArray(array* delegate) : Array<loadflow_component_result>(delegate){}
    ~LoadFlowComponentResultArray();
};

template <typename Caller>
class PostContingencyResultArray : public Array<post_contingency_result> {
    public:
    PostContingencyResultArray(array* delegate) : Array<post_contingency_result>(delegate){}
    ~PostContingencyResultArray();
};

template <typename Caller>
class OperatorStrategyResultArray : public Array<operator_strategy_result> {
    public:
    OperatorStrategyResultArray(array* delegate) : Array<operator_strategy_result>(delegate){}
    ~OperatorStrategyResultArray();
};

template <typename Caller>
class LimitViolationArray : public Array<limit_violation> {
    public:
    LimitViolationArray(array* delegate) : Array<limit_violation>(delegate){}
    ~LimitViolationArray();
};

template <typename Caller>
class SeriesArray : public Array<series> {
    public:
    SeriesArray(array* delegate) : Array<series>(delegate){}
    ~SeriesArray();
};

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
};

::zone* createZone(const std::string& id, const std::vector<std::string>& injectionsIds, const std::vector<double>& injectionsShiftKeys);

void deleteZone(::zone* z);

std::vector<SeriesMetadata> convertDataframeMetadata(dataframe_metadata* dataframeMetadata);

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

//Explicitly update log level on java side
void setLogLevelFromPythonLogger(graal_isolatethread_t* thread, exception_handler* exc);


char* copyStringToCharPtr(const std::string& str);
char** copyVectorStringToCharPtrPtr(const std::vector<std::string>& strings);
int* copyVectorInt(const std::vector<int>& ints);
double* copyVectorDouble(const std::vector<double>& doubles);
void deleteCharPtrPtr(char** charPtrPtr, int length);

void freeCString(char* str);

//copies to string and frees memory allocated by java
std::string toString(char* cstring);

std::shared_ptr<dataframe_array> createDataframeArray(const std::vector<dataframe*>& dataframes);

void init();

template<typename LanguageSpecificCaller>
class PowsyblInterface {
public:
  template<typename F, typename... ARGS>
  static void internalCallJava(F f, ARGS... args) {
      LanguageSpecificCaller::callJava(f, args...);
  }

  template<typename T, typename F, typename... ARGS>
  static T internalCallJava(F f, ARGS... args) {
      return LanguageSpecificCaller::callJava<T>(f, args...);
  }

static void setJavaLibraryPath(const std::string& javaLibraryPath);

static void setConfigRead(bool configRead);

static void setDefaultLoadFlowProvider(const std::string& loadFlowProvider);

static void setDefaultSecurityAnalysisProvider(const std::string& securityAnalysisProvider);

static void setDefaultSensitivityAnalysisProvider(const std::string& sensitivityAnalysisProvider);

static bool isConfigRead();

static std::string getDefaultLoadFlowProvider();

static std::string getDefaultSecurityAnalysisProvider();

static std::string getDefaultSensitivityAnalysisProvider();

static std::string getVersionTable();

static JavaHandle<LanguageSpecificCaller> createNetwork(const std::string& name, const std::string& id);

static JavaHandle<LanguageSpecificCaller> merge(std::vector<JavaHandle<LanguageSpecificCaller>>& others);

static JavaHandle<LanguageSpecificCaller> getSubNetwork(const JavaHandle<LanguageSpecificCaller>& network, const std::string& subNetworkId);

static JavaHandle<LanguageSpecificCaller> detachSubNetwork(const JavaHandle<LanguageSpecificCaller>& subNetwork);

static bool updateSwitchPosition(const JavaHandle<LanguageSpecificCaller>& network, const std::string& id, bool open);

static bool updateConnectableStatus(const JavaHandle<LanguageSpecificCaller>& network, const std::string& id, bool connected);

static std::vector<std::string> getNetworkElementsIds(const JavaHandle<LanguageSpecificCaller>& network, element_type elementType, const std::vector<double>& nominalVoltages,
                                               const std::vector<std::string>& countries, bool mainCc, bool mainSc,
                                               bool notConnectedToSameBusAtBothSides);

static std::vector<std::string> getNetworkImportFormats();

static std::vector<std::string> getNetworkExportFormats();

static std::vector<std::string> getLoadFlowProviderNames();

static std::vector<std::string> getSecurityAnalysisProviderNames();

static std::vector<std::string> getSensitivityAnalysisProviderNames();

static SeriesArray<LanguageSpecificCaller>* createImporterParametersSeriesArray(const std::string& format);

static SeriesArray<LanguageSpecificCaller>* createExporterParametersSeriesArray(const std::string& format);

static std::shared_ptr<network_metadata> getNetworkMetadata(const JavaHandle<LanguageSpecificCaller>& network);

static JavaHandle<LanguageSpecificCaller> loadNetwork(const std::string& file, const std::map<std::string, std::string>& parameters, JavaHandle<LanguageSpecificCaller>* reportNode);

static JavaHandle<LanguageSpecificCaller> loadNetworkFromString(const std::string& fileName, const std::string& fileContent, const std::map<std::string, std::string>& parameters, JavaHandle<LanguageSpecificCaller>* reportNode);

static JavaHandle<LanguageSpecificCaller> loadNetworkFromBinaryBuffers(std::vector<py::buffer> byteBuffer, const std::map<std::string, std::string>& parameters, JavaHandle<LanguageSpecificCaller>* reportNode);

static void saveNetwork(const JavaHandle<LanguageSpecificCaller>& network, const std::string& file, const std::string& format, const std::map<std::string, std::string>& parameters, JavaHandle<LanguageSpecificCaller>* reportNode);

static LoadFlowParameters* createLoadFlowParameters();

static std::vector<std::string> getLoadFlowProviderParametersNames(const std::string& loadFlowProvider);

static SeriesArray<LanguageSpecificCaller>* createLoadFlowProviderParametersSeriesArray(const std::string& provider);

static LoadFlowValidationParameters* createValidationConfig();

static SecurityAnalysisParameters* createSecurityAnalysisParameters();

static std::vector<std::string> getSecurityAnalysisProviderParametersNames(const std::string& securityAnalysisProvider);

static SensitivityAnalysisParameters* createSensitivityAnalysisParameters();

static std::vector<std::string> getSensitivityAnalysisProviderParametersNames(const std::string& sensitivityAnalysisProvider);

static std::string saveNetworkToString(const JavaHandle<LanguageSpecificCaller>& network, const std::string& format, const std::map<std::string, std::string>& parameters, JavaHandle<LanguageSpecificCaller>* reportNode);

static py::bytes saveNetworkToBinaryBuffer(const JavaHandle<LanguageSpecificCaller>& network, const std::string& format, const std::map<std::string, std::string>& parameters, JavaHandle<LanguageSpecificCaller>* reportNode);

static void reduceNetwork(const JavaHandle<LanguageSpecificCaller>& network, const double v_min, const double v_max, const std::vector<std::string>& ids, const std::vector<std::string>& vls, const std::vector<int>& depths, bool withDangLingLines);

static LoadFlowComponentResultArray<LanguageSpecificCaller>* runLoadFlow(const JavaHandle<LanguageSpecificCaller>& network, bool dc, const LoadFlowParameters& parameters, const std::string& provider, JavaHandle<LanguageSpecificCaller>* reportNode);

static SeriesArray<LanguageSpecificCaller>* runLoadFlowValidation(const JavaHandle<LanguageSpecificCaller>& network, validation_type validationType, const LoadFlowValidationParameters& validationParameters);

static void writeSingleLineDiagramSvg(const JavaHandle<LanguageSpecificCaller>& network, const std::string& containerId, const std::string& svgFile, const std::string& metadataFile, const SldParameters& parameters);

static void writeMatrixMultiSubstationSingleLineDiagramSvg(const JavaHandle<LanguageSpecificCaller>& network, const std::vector<std::vector<std::string>>& matrixIds, const std::string& svgFile, const std::string& metadataFile, const SldParameters& parameters);

static std::string getSingleLineDiagramSvg(const JavaHandle<LanguageSpecificCaller>& network, const std::string& containerId);

static std::vector<std::string> getSingleLineDiagramSvgAndMetadata(const JavaHandle<LanguageSpecificCaller>& network, const std::string& containerId, const SldParameters& parameters);

static std::vector<std::string> getSingleLineDiagramComponentLibraryNames();

static void writeNetworkAreaDiagramSvg(const JavaHandle<LanguageSpecificCaller>& network, const std::string& svgFile, const std::vector<std::string>& voltageLevelIds, int depth, double highNominalVoltageBound, double lowNominalVoltageBound, const NadParameters& parameters);

static std::string getNetworkAreaDiagramSvg(const JavaHandle<LanguageSpecificCaller>& network, const std::vector<std::string>& voltageLevelIds, int depth, double highNominalVoltageBound, double lowNominalVoltageBound, const NadParameters& parameters);

static std::vector<std::string> getNetworkAreaDiagramDisplayedVoltageLevels(const JavaHandle<LanguageSpecificCaller>& network, const std::vector<std::string>& voltageLevelIds, int depth);

static JavaHandle<LanguageSpecificCaller> createSecurityAnalysis();

static void addContingency(const JavaHandle<LanguageSpecificCaller>& analysisContext, const std::string& contingencyId, const std::vector<std::string>& elementsIds);

static JavaHandle<LanguageSpecificCaller> runSecurityAnalysis(const JavaHandle<LanguageSpecificCaller>& securityAnalysisContext, const JavaHandle<LanguageSpecificCaller>& network, const SecurityAnalysisParameters& parameters, const std::string& provider, bool dc, JavaHandle<LanguageSpecificCaller>* reportNode);

static JavaHandle<LanguageSpecificCaller> createSensitivityAnalysis();

static void addLoadActivePowerAction(const JavaHandle<LanguageSpecificCaller>& analysisContext, const std::string& actionId, const std::string& loadId, bool relativeValue, double activePower);

static void addLoadReactivePowerAction(const JavaHandle<LanguageSpecificCaller>& analysisContext, const std::string& actionId, const std::string& loadId, bool relativeValue, double reactivePower);

static void addGeneratorActivePowerAction(const JavaHandle<LanguageSpecificCaller>& analysisContext, const std::string& actionId, const std::string& generatorId, bool relativeValue, double activePower);

static void addSwitchAction(const JavaHandle<LanguageSpecificCaller>& analysisContext, const std::string& actionId, const std::string& switchId, bool open);

static void addPhaseTapChangerPositionAction(const JavaHandle<LanguageSpecificCaller>& analysisContext, const std::string& actionId, const std::string& transformerId, bool isRelative, int tapPosition, ThreeSide side);

static void addRatioTapChangerPositionAction(const JavaHandle<LanguageSpecificCaller>& analysisContext, const std::string& actionId, const std::string& transformerId, bool isRelative, int tapPosition, ThreeSide side);

static void addShuntCompensatorPositionAction(const JavaHandle<LanguageSpecificCaller>& analysisContext, const std::string& actionId, const std::string& shuntId, int sectionCount);

static void addOperatorStrategy(const JavaHandle<LanguageSpecificCaller>& analysisContext, std::string operatorStrategyId, std::string contingencyId, const std::vector<std::string>& actionsIds,
                         condition_type conditionType, const std::vector<std::string>& subjectIds, const std::vector<violation_type>& violationTypesFilters);

static void setZones(const JavaHandle<LanguageSpecificCaller>& sensitivityAnalysisContext, const std::vector<::zone*>& zones);

static void addFactorMatrix(const JavaHandle<LanguageSpecificCaller>& sensitivityAnalysisContext, std::string matrixId, const std::vector<std::string>& branchesIds,
                     const std::vector<std::string>& variablesIds, const std::vector<std::string>& contingenciesIds, contingency_context_type ContingencyContextType,
                     sensitivity_function_type sensitivityFunctionType, sensitivity_variable_type sensitivityVariableType);

static JavaHandle<LanguageSpecificCaller> runSensitivityAnalysis(const JavaHandle<LanguageSpecificCaller>& sensitivityAnalysisContext, const JavaHandle<LanguageSpecificCaller>& network, bool dc, SensitivityAnalysisParameters& parameters, const std::string& provider, JavaHandle<LanguageSpecificCaller>* reportNode);

static matrix* getSensitivityMatrix(const JavaHandle<LanguageSpecificCaller>& sensitivityAnalysisResultContext, const std::string& matrixId, const std::string &contingencyId);

static matrix* getReferenceMatrix(const JavaHandle<LanguageSpecificCaller>& sensitivityAnalysisResultContext, const std::string& matrixId, const std::string& contingencyId);

static SeriesArray<LanguageSpecificCaller>* createNetworkElementsSeriesArray(const JavaHandle<LanguageSpecificCaller>& network, element_type elementType, filter_attributes_type filterAttributesType, const std::vector<std::string>& attributes, dataframe* dataframe);

static void removeNetworkElements(const JavaHandle<LanguageSpecificCaller>& network, const std::vector<std::string>& elementIds);

static SeriesArray<LanguageSpecificCaller>* createNetworkElementsExtensionSeriesArray(const JavaHandle<LanguageSpecificCaller>& network, const std::string& extensionName, const std::string& tableName);

static std::vector<std::string> getExtensionsNames();

static SeriesArray<LanguageSpecificCaller>* getExtensionsInformation();

static void updateNetworkElementsWithSeries(pypowsybl::JavaHandle<LanguageSpecificCaller> network, dataframe* dataframe, element_type elementType);

static std::string getWorkingVariantId(const JavaHandle<LanguageSpecificCaller>& network);

static void setWorkingVariant(const JavaHandle<LanguageSpecificCaller>& network, std::string& variant);

static void removeVariant(const JavaHandle<LanguageSpecificCaller>& network, std::string& variant);

static void cloneVariant(const JavaHandle<LanguageSpecificCaller>& network, std::string& src, std::string& variant, bool mayOverwrite);

static std::vector<std::string> getVariantsIds(const JavaHandle<LanguageSpecificCaller>& network);

static void addMonitoredElements(const JavaHandle<LanguageSpecificCaller>& securityAnalysisContext, contingency_context_type contingencyContextType, const std::vector<std::string>& branchIds,
                      const std::vector<std::string>& voltageLevelIds, const std::vector<std::string>& threeWindingsTransformerIds,
                      const std::vector<std::string>& contingencyIds);

static SeriesArray<LanguageSpecificCaller>* getLimitViolations(const JavaHandle<LanguageSpecificCaller>& securityAnalysisResult);

static PostContingencyResultArray<LanguageSpecificCaller>* getPostContingencyResults(const JavaHandle<LanguageSpecificCaller>& securityAnalysisResult);

static OperatorStrategyResultArray<LanguageSpecificCaller>* getOperatorStrategyResults(const JavaHandle<LanguageSpecificCaller>& securityAnalysisResult);

static pre_contingency_result* getPreContingencyResult(const JavaHandle<LanguageSpecificCaller>& securityAnalysisResult);

static SeriesArray<LanguageSpecificCaller>* getBranchResults(const JavaHandle<LanguageSpecificCaller>& securityAnalysisResult);

static SeriesArray<LanguageSpecificCaller>* getBusResults(const JavaHandle<LanguageSpecificCaller>& securityAnalysisResult);

static SeriesArray<LanguageSpecificCaller>* getThreeWindingsTransformerResults(const JavaHandle<LanguageSpecificCaller>& securityAnalysisResult);

static SeriesArray<LanguageSpecificCaller>* getNodeBreakerViewSwitches(const JavaHandle<LanguageSpecificCaller>& network,std::string& voltageLevel);

static SeriesArray<LanguageSpecificCaller>* getNodeBreakerViewNodes(const JavaHandle<LanguageSpecificCaller>& network,std::string& voltageLevel);

static SeriesArray<LanguageSpecificCaller>* getNodeBreakerViewInternalConnections(const JavaHandle<LanguageSpecificCaller>& network,std::string& voltageLevel);

static SeriesArray<LanguageSpecificCaller>* getBusBreakerViewSwitches(const JavaHandle<LanguageSpecificCaller>& network,std::string& voltageLevel);

static SeriesArray<LanguageSpecificCaller>* getBusBreakerViewBuses(const JavaHandle<LanguageSpecificCaller>& network,std::string& voltageLevel);

static SeriesArray<LanguageSpecificCaller>* getBusBreakerViewElements(const JavaHandle<LanguageSpecificCaller>& network,std::string& voltageLevel);

/**
 * Metadata of the dataframe of network elements data for a given element type.
 */
static std::vector<SeriesMetadata> getNetworkDataframeMetadata(element_type elementType);

/**
 * Metadata of the list of dataframes to create network elements of the given type.
 */
static std::vector<std::vector<SeriesMetadata>> getNetworkElementCreationDataframesMetadata(element_type elementType);

static void createElement(JavaHandle<LanguageSpecificCaller> network, const std::vector<dataframe*>& dataframes, element_type elementType);

static ::validation_level_type getValidationLevel(const JavaHandle<LanguageSpecificCaller>& network);

static ::validation_level_type validate(const JavaHandle<LanguageSpecificCaller>& network);

static void setMinValidationLevel(pypowsybl::JavaHandle<LanguageSpecificCaller> network, validation_level_type validationLevel);

static void setupLoggerCallback(void *& callback);

static void addNetworkElementProperties(pypowsybl::JavaHandle<LanguageSpecificCaller> network, dataframe* dataframe);

static void removeNetworkElementProperties(pypowsybl::JavaHandle<LanguageSpecificCaller> network, const std::vector<std::string>& ids, const std::vector<std::string>& properties);

static void updateNetworkElementsExtensionsWithSeries(pypowsybl::JavaHandle<LanguageSpecificCaller> network, std::string& name, std::string& tableName, dataframe* dataframe);

static void removeExtensions(const JavaHandle<LanguageSpecificCaller>& network, std::string& name, const std::vector<std::string>& ids);

static std::vector<SeriesMetadata> getNetworkExtensionsDataframeMetadata(std::string& name, std::string& tableName);

static std::vector<std::vector<SeriesMetadata>> getNetworkExtensionsCreationDataframesMetadata(std::string& name);

static void createExtensions(JavaHandle<LanguageSpecificCaller> network, const std::vector<dataframe*>& dataframes, std::string& name);

static JavaHandle<LanguageSpecificCaller> createReportNode(const std::string& taskKey, const std::string& defaultName);

static std::string printReport(const JavaHandle<LanguageSpecificCaller>& reportNode);

static std::string jsonReport(const JavaHandle<LanguageSpecificCaller>& reportNode);

static JavaHandle<LanguageSpecificCaller> createGLSKdocument(std::string& filename);

static std::vector<std::string> getGLSKinjectionkeys(pypowsybl::JavaHandle<LanguageSpecificCaller> network, const JavaHandle<LanguageSpecificCaller>& importer, std::string& country, long instant);

static std::vector<std::string> getGLSKcountries(const JavaHandle<LanguageSpecificCaller>& importer);

static std::vector<double> getGLSKInjectionFactors(pypowsybl::JavaHandle<LanguageSpecificCaller> network, const JavaHandle<LanguageSpecificCaller>& importer, std::string& country, long instant);

static long getInjectionFactorStartTimestamp(const JavaHandle<LanguageSpecificCaller>& importer);

static long getInjectionFactorEndTimestamp(const JavaHandle<LanguageSpecificCaller>& importer);

static JavaHandle<LanguageSpecificCaller> createFlowDecomposition();

static void addContingencyForFlowDecomposition(const JavaHandle<LanguageSpecificCaller>& flowDecompositionContext, const std::string& contingencyId, const std::vector<std::string>& elementsIds);

static void addPrecontingencyMonitoredElementsForFlowDecomposition(const JavaHandle<LanguageSpecificCaller>& flowDecompositionContext, const std::vector<std::string>& branchIds);

static void addPostcontingencyMonitoredElementsForFlowDecomposition(const JavaHandle<LanguageSpecificCaller>& flowDecompositionContext, const std::vector<std::string>& branchIds, const std::vector<std::string>& contingencyIds);

static void addAdditionalXnecProviderForFlowDecomposition(const JavaHandle<LanguageSpecificCaller>& flowDecompositionContext, DefaultXnecProvider defaultXnecProvider);

static SeriesArray<LanguageSpecificCaller>* runFlowDecomposition(const JavaHandle<LanguageSpecificCaller>& flowDecompositionContext, const JavaHandle<LanguageSpecificCaller>& network, const FlowDecompositionParameters& flow_decomposition_parameters, const LoadFlowParameters& loadflow_parameters);

static FlowDecompositionParameters* createFlowDecompositionParameters();

static SeriesArray<LanguageSpecificCaller>* getConnectablesOrderPositions(const JavaHandle<LanguageSpecificCaller>& network, const std::string voltage_level_id);

static std::vector<int> getUnusedConnectableOrderPositions(pypowsybl::JavaHandle<LanguageSpecificCaller> network, std::string busbarSectionId, std::string beforeOrAfter);

static void removeAliases(pypowsybl::JavaHandle<LanguageSpecificCaller> network, dataframe* dataframe);

static void closePypowsybl();

static void removeElementsModification(pypowsybl::JavaHandle<LanguageSpecificCaller> network, const std::vector<std::string>& connectableIds, dataframe* dataframe, remove_modification_type removeModificationType, bool throwException, JavaHandle<LanguageSpecificCaller>* reportNode);

static SldParameters* createSldParameters();

static NadParameters* createNadParameters();

//=======dynamic modeling for dynawaltz package==========

//handle creation
static JavaHandle<LanguageSpecificCaller> createDynamicSimulationContext();
static JavaHandle<LanguageSpecificCaller> createDynamicModelMapping();
static JavaHandle<LanguageSpecificCaller> createTimeseriesMapping();
static JavaHandle<LanguageSpecificCaller> createEventMapping();

static JavaHandle<LanguageSpecificCaller> runDynamicModel(JavaHandle<LanguageSpecificCaller> dynamicModelContext, JavaHandle<LanguageSpecificCaller> network, JavaHandle<LanguageSpecificCaller> dynamicMapping, JavaHandle<LanguageSpecificCaller> eventMapping, JavaHandle<LanguageSpecificCaller> timeSeriesMapping, int start, int stop);

// timeseries/curves mapping
static void addCurve(JavaHandle<LanguageSpecificCaller> curveMappingHandle, std::string dynamicId, std::string variable);

// events mapping
static void addEventDisconnection(const JavaHandle<LanguageSpecificCaller>& eventMappingHandle, const std::string& staticId, double eventTime, int disconnectOnly);

// dynamic model mapping
static void addDynamicMappings(JavaHandle<LanguageSpecificCaller> dynamicMappingHandle, DynamicMappingType mappingType, dataframe* mappingDf);
static std::vector<SeriesMetadata> getDynamicMappingsMetaData(DynamicMappingType mappingType);

// results
static std::string getDynamicSimulationResultsStatus(JavaHandle<LanguageSpecificCaller> dynamicSimulationResultsHandle);
static SeriesArray<LanguageSpecificCaller>* getDynamicCurve(JavaHandle<LanguageSpecificCaller> resultHandle, std::string curveName);
static std::vector<std::string> getAllDynamicCurvesIds(JavaHandle<LanguageSpecificCaller> resultHandle);

//=======END OF dynamic modeling for dynawaltz package==========

//=======Voltage initializer mapping========

static JavaHandle<LanguageSpecificCaller> createVoltageInitializerParams();
static void voltageInitializerAddSpecificLowVoltageLimits(const JavaHandle<LanguageSpecificCaller>& paramsHandle, const std::string& voltageLevelId, bool isRelative, double limit);
static void voltageInitializerAddSpecificHighVoltageLimits(const JavaHandle<LanguageSpecificCaller>& paramsHandle, const std::string& voltageLevelId, bool isRelative, double limit);
static void voltageInitializerAddVariableShuntCompensators(const JavaHandle<LanguageSpecificCaller>& paramsHandle, const std::string& idPtr);
static void voltageInitializerAddConstantQGenerators(const JavaHandle<LanguageSpecificCaller>& paramsHandle, const std::string& idPtr);
static void voltageInitializerAddVariableTwoWindingsTransformers(const JavaHandle<LanguageSpecificCaller>& paramsHandle, const std::string& idPtr);
static void voltageInitializerSetObjective(const JavaHandle<LanguageSpecificCaller>& paramsHandle, VoltageInitializerObjective cObjective);
static void voltageInitializerSetObjectiveDistance(const JavaHandle<LanguageSpecificCaller>& paramsHandle, double dist);
static void voltageInitializerApplyAllModifications(const JavaHandle<LanguageSpecificCaller>& resultHandle, const JavaHandle<LanguageSpecificCaller>& networkHandle);
static VoltageInitializerStatus voltageInitializerGetStatus(const JavaHandle<LanguageSpecificCaller>& resultHandle);
static std::map<std::string, std::string> voltageInitializerGetIndicators(const JavaHandle<LanguageSpecificCaller>& resultHandle);
static JavaHandle<LanguageSpecificCaller> runVoltageInitializer(bool debug, const JavaHandle<LanguageSpecificCaller>& networkHandle, const JavaHandle<LanguageSpecificCaller>& paramsHandle);

//=======End of voltage initializer mapping========

static std::vector<SeriesMetadata> getModificationMetadata(network_modification_type networkModificationType);

static std::vector<std::vector<SeriesMetadata>> getModificationMetadataWithElementType(network_modification_type networkModificationType, element_type elementType);

static void createNetworkModification(JavaHandle<LanguageSpecificCaller> network, const std::vector<dataframe*>& dataframes, network_modification_type networkModificationType, bool throwException, JavaHandle<LanguageSpecificCaller>* reportNode);
static void setDefaultShortCircuitAnalysisProvider(const std::string& shortCircuitAnalysisProvider);
static std::string getDefaultShortCircuitAnalysisProvider();
static std::vector<std::string> getShortCircuitAnalysisProviderNames();
static ShortCircuitAnalysisParameters* createShortCircuitAnalysisParameters();
static std::vector<std::string> getShortCircuitAnalysisProviderParametersNames(const std::string& shortCircuitAnalysisProvider);
static JavaHandle<LanguageSpecificCaller> createShortCircuitAnalysis();
static JavaHandle<LanguageSpecificCaller> runShortCircuitAnalysis(const JavaHandle<LanguageSpecificCaller>& shortCircuitAnalysisContext, const JavaHandle<LanguageSpecificCaller>& network, const ShortCircuitAnalysisParameters& parameters, const std::string& provider, JavaHandle<LanguageSpecificCaller>* reportNode);
static std::vector<SeriesMetadata> getFaultsMetaData();
static void setFaults(pypowsybl::JavaHandle<LanguageSpecificCaller> analysisContext, dataframe* dataframe);
static SeriesArray<LanguageSpecificCaller>* getFaultResults(const JavaHandle<LanguageSpecificCaller>& shortCircuitAnalysisResult, bool withFortescueResult);
static SeriesArray<LanguageSpecificCaller>* getFeederResults(const JavaHandle<LanguageSpecificCaller>& shortCircuitAnalysisResult, bool withFortescueResult);
static SeriesArray<LanguageSpecificCaller>* getShortCircuitLimitViolations(const JavaHandle<LanguageSpecificCaller>& shortCircuitAnalysisResult);
static SeriesArray<LanguageSpecificCaller>* getShortCircuitBusResults(const JavaHandle<LanguageSpecificCaller>& shortCircuitAnalysisResult, bool withFortescueResult);

};

//Destruction of java object when the shared_ptr has no more references
template <typename JavaCaller>
JavaHandle<JavaCaller>::JavaHandle(void* handle):
    handle_(handle, [](void* to_be_deleted) {
        if (to_be_deleted) {
            JavaCaller::callJava(::destroyObjectHandle, to_be_deleted);
        }
    })
{
}

template <typename T>
void PowsyblInterface<T>::setJavaLibraryPath(const std::string& javaLibraryPath) {
    internalCallJava(::setJavaLibraryPath, (char*) javaLibraryPath.data());
}

template <typename T>
void PowsyblInterface<T>::setConfigRead(bool configRead) {
    internalCallJava(::setConfigRead, configRead);
}

template <typename T>
void PowsyblInterface<T>::setDefaultLoadFlowProvider(const std::string& loadFlowProvider) {
    internalCallJava(::setDefaultLoadFlowProvider, (char*) loadFlowProvider.data());
}

template <typename T>
void PowsyblInterface<T>::setDefaultSecurityAnalysisProvider(const std::string& securityAnalysisProvider) {
    internalCallJava(::setDefaultSecurityAnalysisProvider, (char*) securityAnalysisProvider.data());
}

template <typename T>
void PowsyblInterface<T>::setDefaultSensitivityAnalysisProvider(const std::string& sensitivityAnalysisProvider) {
    internalCallJava(::setDefaultSensitivityAnalysisProvider, (char*) sensitivityAnalysisProvider.data());
}

template <typename T>
std::string PowsyblInterface<T>::getDefaultLoadFlowProvider() {
    return toString(internalCallJava<char*>(::getDefaultLoadFlowProvider));
}

template <typename T>
std::string PowsyblInterface<T>::getDefaultSecurityAnalysisProvider() {
    return toString(internalCallJava<char*>(::getDefaultSecurityAnalysisProvider));
}

template <typename T>
std::string PowsyblInterface<T>::getDefaultSensitivityAnalysisProvider() {
    return toString(internalCallJava<char*>(::getDefaultSensitivityAnalysisProvider));
}

template <typename T>
bool PowsyblInterface<T>::isConfigRead() {
    return internalCallJava<bool>(::isConfigRead);
}

template <typename T>
std::string PowsyblInterface<T>::getVersionTable() {
    return toString(internalCallJava<char*>(::getVersionTable));
}

template <typename T>
JavaHandle<T> PowsyblInterface<T>::createNetwork(const std::string& name, const std::string& id) {
    return internalCallJava<JavaHandle<T>>(::createNetwork, (char*) name.data(), (char*) id.data());
}

template <typename T>
JavaHandle<T> PowsyblInterface<T>::merge(std::vector<JavaHandle<T>>& networks) {
    std::vector<void*> networksPtrs;
    networksPtrs.reserve(networks.size());
    for (int i = 0; i < networks.size(); ++i) {
        void* ptr = networks[i];
        networksPtrs.push_back(ptr);
    }
    int networkCount = networksPtrs.size();
    void** networksData = (void**) networksPtrs.data();

    return internalCallJava<JavaHandle<T>>(::merge, networksData, networkCount);
}

template <typename T>
JavaHandle<T> PowsyblInterface<T>::getSubNetwork(const JavaHandle<T>& network, const std::string& subNetworkId) {
    return internalCallJava<JavaHandle<T>>(::getSubNetwork, network, (char*) subNetworkId.data());
}

template <typename T>
JavaHandle<T> PowsyblInterface<T>::detachSubNetwork(const JavaHandle<T>& subNetwork) {
    return internalCallJava<JavaHandle<T>>(::detachSubNetwork, subNetwork);
}

template <typename T>
std::vector<std::string> PowsyblInterface<T>::getNetworkImportFormats() {
    auto formatsArrayPtr = internalCallJava<array*>(::getNetworkImportFormats);
    ToStringVector<T> formats(formatsArrayPtr);
    return formats.get();
}

template <typename T>
std::vector<std::string> PowsyblInterface<T>::getNetworkExportFormats() {
    auto formatsArrayPtr = internalCallJava<array*>(::getNetworkExportFormats);
    ToStringVector<T> formats(formatsArrayPtr);
    return formats.get();
}

template <typename T>
std::vector<std::string> PowsyblInterface<T>::getLoadFlowProviderNames() {
    auto formatsArrayPtr = internalCallJava<array*>(::getLoadFlowProviderNames);
    ToStringVector<T> formats(formatsArrayPtr);
    return formats.get();
}

template <typename T>
std::vector<std::string> PowsyblInterface<T>::getSingleLineDiagramComponentLibraryNames() {
    auto formatsArrayPtr = internalCallJava<array*>(::getSingleLineDiagramComponentLibraryNames);
    ToStringVector<T> formats(formatsArrayPtr);
    return formats.get();
}

template <typename T>
std::vector<std::string> PowsyblInterface<T>::getSecurityAnalysisProviderNames() {
    auto formatsArrayPtr = internalCallJava<array*>(::getSecurityAnalysisProviderNames);
    ToStringVector<T> formats(formatsArrayPtr);
    return formats.get();
}

template <typename T>
std::vector<std::string> PowsyblInterface<T>::getSensitivityAnalysisProviderNames() {
    auto formatsArrayPtr = internalCallJava<array*>(::getSensitivityAnalysisProviderNames);
    ToStringVector<T> formats(formatsArrayPtr);
    return formats.get();
}

template <typename T>
SeriesArray<T>* PowsyblInterface<T>::createImporterParametersSeriesArray(const std::string& format) {
    return new SeriesArray<T>(internalCallJava<array*>(::createImporterParametersSeriesArray, (char*) format.data()));
}

template <typename T>
SeriesArray<T>* PowsyblInterface<T>::createExporterParametersSeriesArray(const std::string& format) {
    return new SeriesArray<T>(internalCallJava<array*>(::createExporterParametersSeriesArray, (char*) format.data()));
}

template <typename T>
std::shared_ptr<network_metadata> PowsyblInterface<T>::getNetworkMetadata(const JavaHandle<T>& network) {
    network_metadata* attributes = internalCallJava<network_metadata*>(::getNetworkMetadata, network);
    return std::shared_ptr<network_metadata>(attributes, [](network_metadata* ptr){
        internalCallJava(::freeNetworkMetadata, ptr);
    });
}

template <typename T>
JavaHandle<T> PowsyblInterface<T>::loadNetwork(const std::string& file, const std::map<std::string, std::string>& parameters, JavaHandle<T>* reportNode) {
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
    return internalCallJava<JavaHandle<T>>(::loadNetwork, (char*) file.data(), parameterNamesPtr.get(), parameterNames.size(),
                              parameterValuesPtr.get(), parameterValues.size(), (reportNode == nullptr) ? nullptr : *reportNode);
}

template <typename T>
JavaHandle<T> PowsyblInterface<T>::loadNetworkFromString(const std::string& fileName, const std::string& fileContent, const std::map<std::string, std::string>& parameters, JavaHandle<T>* reportNode) {
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
    return internalCallJava<JavaHandle<T>>(::loadNetworkFromString, (char*) fileName.data(), (char*) fileContent.data(),
                           parameterNamesPtr.get(), parameterNames.size(),
                           parameterValuesPtr.get(), parameterValues.size(), (reportNode == nullptr) ? nullptr : *reportNode);
}

template <typename T>
JavaHandle<T> PowsyblInterface<T>::loadNetworkFromBinaryBuffers(std::vector<py::buffer> byteBuffers, const std::map<std::string, std::string>& parameters, JavaHandle<T>* reportNode) {
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

    char** dataPtrs = new char*[byteBuffers.size()];
    int* dataSizes = new int[byteBuffers.size()];
    for(int i=0; i < byteBuffers.size(); ++i) {
        py::buffer_info info = byteBuffers[i].request();
        dataPtrs[i] = static_cast<char*>(info.ptr);
        dataSizes[i] = info.size;
    }

    JavaHandle<T> networkHandle = internalCallJava<JavaHandle<T>>(::loadNetworkFromBinaryBuffers, dataPtrs, dataSizes, byteBuffers.size(),
                           parameterNamesPtr.get(), parameterNames.size(),
                           parameterValuesPtr.get(), parameterValues.size(), (reportNode == nullptr) ? nullptr : *reportNode);
    delete[] dataPtrs;
    delete[] dataSizes;
    return networkHandle;
}

template <typename T>
void PowsyblInterface<T>::saveNetwork(const JavaHandle<T>& network, const std::string& file, const std::string& format, const std::map<std::string, std::string>& parameters, JavaHandle<T>* reportNode) {
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
    internalCallJava(::saveNetwork, network, (char*) file.data(), (char*) format.data(), parameterNamesPtr.get(), parameterNames.size(),
                parameterValuesPtr.get(), parameterValues.size(), (reportNode == nullptr) ? nullptr : *reportNode);
}

template <typename T>
std::string PowsyblInterface<T>::saveNetworkToString(const JavaHandle<T>& network, const std::string& format, const std::map<std::string, std::string>& parameters, JavaHandle<T>* reportNode) {
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
    return toString(internalCallJava<char*>(::saveNetworkToString, network, (char*) format.data(), parameterNamesPtr.get(), parameterNames.size(),
             parameterValuesPtr.get(), parameterValues.size(), (reportNode == nullptr) ? nullptr : *reportNode));
}

template <typename T>
py::bytes PowsyblInterface<T>::saveNetworkToBinaryBuffer(const JavaHandle<T>& network, const std::string& format, const std::map<std::string, std::string>& parameters, JavaHandle<T>* reportNode) {
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
    array* byteArray = internalCallJava<array*>(::saveNetworkToBinaryBuffer, network, (char*) format.data(), parameterNamesPtr.get(), parameterNames.size(),
                     parameterValuesPtr.get(), parameterValues.size(), reportNode == nullptr ? nullptr : *reportNode);
    py::gil_scoped_acquire acquire;
    py::bytes bytes((char*) byteArray->ptr, byteArray->length);
    internalCallJava(::freeNetworkBinaryBuffer, byteArray);
    return bytes;
}

template <typename T>
void PowsyblInterface<T>::reduceNetwork(const JavaHandle<T>& network, double v_min, double v_max, const std::vector<std::string>& ids,
                   const std::vector<std::string>& vls, const std::vector<int>& depths, bool withDangLingLines) {
    ToCharPtrPtr elementIdPtr(ids);
    ToCharPtrPtr vlsPtr(vls);
    ToIntPtr depthsPtr(depths);
    internalCallJava(::reduceNetwork, network, v_min, v_max, elementIdPtr.get(), ids.size(), vlsPtr.get(), vls.size(), depthsPtr.get(), depths.size(), withDangLingLines);
}

template <typename T>
bool PowsyblInterface<T>::updateSwitchPosition(const JavaHandle<T>& network, const std::string& id, bool open) {
    return internalCallJava<bool>(::updateSwitchPosition, network, (char*) id.data(), open);
}

template <typename T>
bool PowsyblInterface<T>::updateConnectableStatus(const JavaHandle<T>& network, const std::string& id, bool connected) {
    return internalCallJava<bool>(::updateConnectableStatus, network, (char*) id.data(), connected);
}

template <typename T>
std::vector<std::string> PowsyblInterface<T>::getNetworkElementsIds(const JavaHandle<T>& network, element_type elementType, const std::vector<double>& nominalVoltages,
                                               const std::vector<std::string>& countries, bool mainCc, bool mainSc,
                                               bool notConnectedToSameBusAtBothSides) {
    ToDoublePtr nominalVoltagePtr(nominalVoltages);
    ToCharPtrPtr countryPtr(countries);
    auto elementsIdsArrayPtr = internalCallJava<array*>(::getNetworkElementsIds, network, elementType,
                                                       nominalVoltagePtr.get(), nominalVoltages.size(),
                                                       countryPtr.get(), countries.size(), mainCc, mainSc,
                                                       notConnectedToSameBusAtBothSides);
    ToStringVector<T> elementsIds(elementsIdsArrayPtr);
    return elementsIds.get();
}

template <typename T>
LoadFlowParameters* PowsyblInterface<T>::createLoadFlowParameters() {
    loadflow_parameters* parameters_ptr = internalCallJava<loadflow_parameters*>(::createLoadFlowParameters);
    auto parameters = std::shared_ptr<loadflow_parameters>(parameters_ptr, [](loadflow_parameters* ptr){
       //Memory has been allocated on java side, we need to clean it up on java side
       internalCallJava(::freeLoadFlowParameters, ptr);
    });
    return new LoadFlowParameters(parameters.get());
}

template <typename T>
LoadFlowValidationParameters* PowsyblInterface<T>::createValidationConfig() {
    loadflow_validation_parameters* parameters_ptr = internalCallJava<loadflow_validation_parameters*>(::createValidationConfig);
    auto parameters = std::shared_ptr<loadflow_validation_parameters>(parameters_ptr, [](loadflow_validation_parameters* ptr){
       //Memory has been allocated on java side, we need to clean it up on java side
       internalCallJava(::freeValidationConfig, ptr);
    });
    return new LoadFlowValidationParameters(parameters.get());
}


template <typename T>
SecurityAnalysisParameters* PowsyblInterface<T>::createSecurityAnalysisParameters() {
    security_analysis_parameters* parameters_ptr = internalCallJava<security_analysis_parameters*>(::createSecurityAnalysisParameters);
    auto parameters = std::shared_ptr<security_analysis_parameters>(parameters_ptr, [](security_analysis_parameters* ptr){
        internalCallJava(::freeSecurityAnalysisParameters, ptr);
    });
    return new SecurityAnalysisParameters(parameters.get());
}

template <typename T>
SensitivityAnalysisParameters* PowsyblInterface<T>::createSensitivityAnalysisParameters() {
    sensitivity_analysis_parameters* parameters_ptr = internalCallJava<sensitivity_analysis_parameters*>(::createSensitivityAnalysisParameters);
     auto parameters = std::shared_ptr<sensitivity_analysis_parameters>(parameters_ptr, [](sensitivity_analysis_parameters* ptr){
        internalCallJava(::freeSensitivityAnalysisParameters, ptr);
    });
    return new SensitivityAnalysisParameters(parameters.get());
}

template <typename T>
LoadFlowComponentResultArray<T>* PowsyblInterface<T>::runLoadFlow(const JavaHandle<T>& network, bool dc, const LoadFlowParameters& parameters,
                                          const std::string& provider, JavaHandle<T>* reportNode) {
    auto c_parameters = parameters.to_c_struct();
    return new LoadFlowComponentResultArray<T>(
            internalCallJava<array*>(::runLoadFlow, network, dc, c_parameters.get(), (char *) provider.data(), (reportNode == nullptr) ? nullptr : *reportNode));
}

template <typename T>
SeriesArray<T>* PowsyblInterface<T>::runLoadFlowValidation(const JavaHandle<T>& network, validation_type validationType, const LoadFlowValidationParameters& loadflow_validation_parameters) {
    auto c_validation_parameters = loadflow_validation_parameters.to_c_struct();
    return new SeriesArray<T>(internalCallJava<array*>(::runLoadFlowValidation, network, validationType, c_validation_parameters.get()));
}

template <typename T>
void PowsyblInterface<T>::writeSingleLineDiagramSvg(const JavaHandle<T>& network, const std::string& containerId, const std::string& svgFile, const std::string& metadataFile, const SldParameters& parameters) {
    auto c_parameters = parameters.to_c_struct();
    internalCallJava(::writeSingleLineDiagramSvg, network, (char*) containerId.data(), (char*) svgFile.data(), (char*) metadataFile.data(), c_parameters.get());
}

template <typename T>
void PowsyblInterface<T>::writeMatrixMultiSubstationSingleLineDiagramSvg(const JavaHandle<T>& network, const std::vector<std::vector<std::string>>& matrixIds, const std::string& svgFile, const std::string& metadataFile, const SldParameters& parameters) {
    auto c_parameters = parameters.to_c_struct();
    int nbRows = matrixIds.size();
    std::vector<std::string> substationIds;
    for (int row = 0; row < nbRows; ++row) {
        const std::vector<std::string>& colIds = matrixIds[row];
        for (int col = 0; col < matrixIds[row].size(); ++col) {
            substationIds.push_back(colIds[col]);
        }
    }
    ToCharPtrPtr substationIdPtr(substationIds);
    internalCallJava(::writeMatrixMultiSubstationSingleLineDiagramSvg, network, substationIdPtr.get(), substationIds.size(), nbRows, (char*) svgFile.data(), (char*) metadataFile.data(), c_parameters.get());
}

template <typename T>
std::string PowsyblInterface<T>::getSingleLineDiagramSvg(const JavaHandle<T>& network, const std::string& containerId) {
    return toString(internalCallJava<char*>(::getSingleLineDiagramSvg, network, (char*) containerId.data()));
}

template <typename T>
std::vector<std::string> PowsyblInterface<T>::getSingleLineDiagramSvgAndMetadata(const JavaHandle<T>& network, const std::string& containerId, const SldParameters& parameters) {
    auto c_parameters = parameters.to_c_struct();
    auto svgAndMetadataArrayPtr = internalCallJava<array*>(::getSingleLineDiagramSvgAndMetadata, network, (char*) containerId.data(), c_parameters.get());
    ToStringVector<T> svgAndMetadata(svgAndMetadataArrayPtr);
    return svgAndMetadata.get();
}

template <typename T>
void PowsyblInterface<T>::writeNetworkAreaDiagramSvg(const JavaHandle<T>& network, const std::string& svgFile, const std::vector<std::string>& voltageLevelIds, int depth, double highNominalVoltageBound, double lowNominalVoltageBound, const NadParameters& parameters) {
    auto c_parameters = parameters.to_c_struct();
    ToCharPtrPtr voltageLevelIdPtr(voltageLevelIds);
    internalCallJava(::writeNetworkAreaDiagramSvg, network, (char*) svgFile.data(), voltageLevelIdPtr.get(), voltageLevelIds.size(), depth, highNominalVoltageBound, lowNominalVoltageBound, c_parameters.get());
}

template <typename T>
std::string PowsyblInterface<T>::getNetworkAreaDiagramSvg(const JavaHandle<T>& network, const std::vector<std::string>&  voltageLevelIds, int depth, double highNominalVoltageBound, double lowNominalVoltageBound, const NadParameters& parameters) {
    auto c_parameters = parameters.to_c_struct();
    ToCharPtrPtr voltageLevelIdPtr(voltageLevelIds);
    return toString(internalCallJava<char*>(::getNetworkAreaDiagramSvg, network, voltageLevelIdPtr.get(), voltageLevelIds.size(), depth, highNominalVoltageBound, lowNominalVoltageBound, c_parameters.get()));
}

template <typename T>
std::vector<std::string> PowsyblInterface<T>::getNetworkAreaDiagramDisplayedVoltageLevels(const JavaHandle<T>& network, const std::vector<std::string>& voltageLevelIds, int depth) {
    ToCharPtrPtr voltageLevelIdPtr(voltageLevelIds);
    auto displayedVoltageLevelIdsArrayPtr = internalCallJava<array*>(::getNetworkAreaDiagramDisplayedVoltageLevels, network, voltageLevelIdPtr.get(), voltageLevelIds.size(), depth);
    ToStringVector<T> displayedVoltageLevelIds(displayedVoltageLevelIdsArrayPtr);
    return displayedVoltageLevelIds.get();
}

template <typename T>
JavaHandle<T> PowsyblInterface<T>::createSecurityAnalysis() {
    return internalCallJava<JavaHandle<T>>(::createSecurityAnalysis);
}

template <typename T>
void PowsyblInterface<T>::addContingency(const JavaHandle<T>& analysisContext, const std::string& contingencyId, const std::vector<std::string>& elementsIds) {
    ToCharPtrPtr elementIdPtr(elementsIds);
    internalCallJava(::addContingency, analysisContext, (char*) contingencyId.data(), elementIdPtr.get(), elementsIds.size());
}

template <typename T>
JavaHandle<T> PowsyblInterface<T>::runSecurityAnalysis(const JavaHandle<T>& securityAnalysisContext, const JavaHandle<T>& network, const SecurityAnalysisParameters& parameters,
                               const std::string& provider, bool dc, JavaHandle<T>* reportNode) {
    auto c_parameters = parameters.to_c_struct();
    return internalCallJava<JavaHandle<T>>(::runSecurityAnalysis, securityAnalysisContext, network, c_parameters.get(), (char *) provider.data(), dc, (reportNode == nullptr) ? nullptr : *reportNode);
}

template <typename T>
JavaHandle<T> PowsyblInterface<T>::createSensitivityAnalysis() {
    return internalCallJava<JavaHandle<T>>(::createSensitivityAnalysis);
}

template <typename T>
void PowsyblInterface<T>::addLoadActivePowerAction(const JavaHandle<T>& analysisContext, const std::string& actionId, const std::string& loadId, bool relativeValue, double activePower) {
    internalCallJava(::addLoadActivePowerAction, analysisContext, (char*) actionId.data(), (char*) loadId.data(), relativeValue, activePower);
}

template <typename T>
void PowsyblInterface<T>::addLoadReactivePowerAction(const JavaHandle<T>& analysisContext, const std::string& actionId, const std::string& loadId, bool relativeValue, double reactivePower) {
    internalCallJava(::addLoadReactivePowerAction, analysisContext, (char*) actionId.data(), (char*) loadId.data(), relativeValue, reactivePower);
}

template <typename T>
void PowsyblInterface<T>::addGeneratorActivePowerAction(const JavaHandle<T>& analysisContext, const std::string& actionId, const std::string& generatorId, bool relativeValue, double activePower) {
    internalCallJava(::addGeneratorActivePowerAction, analysisContext, (char*) actionId.data(), (char*) generatorId.data(), relativeValue, activePower);
}

template <typename T>
void PowsyblInterface<T>::addSwitchAction(const JavaHandle<T>& analysisContext, const std::string& actionId, const std::string& switchId, bool open) {
    internalCallJava(::addSwitchAction, analysisContext, (char*) actionId.data(), (char*) switchId.data(), open);
}

template <typename T>
void PowsyblInterface<T>::addPhaseTapChangerPositionAction(const JavaHandle<T>& analysisContext, const std::string& actionId, const std::string& transformerId,
                                      bool isRelative, int tapPosition, ThreeSide side) {
    internalCallJava(::addPhaseTapChangerPositionAction, analysisContext, (char*) actionId.data(), (char*) transformerId.data(), isRelative, tapPosition, side);
}
template <typename T>

void PowsyblInterface<T>::addRatioTapChangerPositionAction(const JavaHandle<T>& analysisContext, const std::string& actionId, const std::string& transformerId,
                                      bool isRelative, int tapPosition, ThreeSide side) {
    internalCallJava(::addRatioTapChangerPositionAction, analysisContext, (char*) actionId.data(), (char*) transformerId.data(), isRelative, tapPosition, side);
}

template <typename T>
void PowsyblInterface<T>::addShuntCompensatorPositionAction(const JavaHandle<T>& analysisContext, const std::string& actionId, const std::string& shuntId,
                                       int sectionCount) {
    internalCallJava(::addShuntCompensatorPositionAction, analysisContext, (char*) actionId.data(), (char*) shuntId.data(), sectionCount);
}

template <typename T>
void PowsyblInterface<T>::addOperatorStrategy(const JavaHandle<T>& analysisContext, std::string operatorStrategyId, std::string contingencyId, const std::vector<std::string>& actionsIds,
                         condition_type conditionType, const std::vector<std::string>& subjectIds, const std::vector<violation_type>& violationTypesFilters) {
    ToCharPtrPtr actionsPtr(actionsIds);
    ToCharPtrPtr subjectIdsPtr(subjectIds);
    std::vector<int> violationTypes;
    for(int i = 0; i < violationTypesFilters.size(); ++i) {
        violationTypes.push_back(violationTypesFilters[i]);
    }
    ToIntPtr violationTypesPtr(violationTypes);
    internalCallJava(::addOperatorStrategy, analysisContext, (char*) operatorStrategyId.data(), (char*) contingencyId.data(), actionsPtr.get(), actionsIds.size(),
        conditionType, subjectIdsPtr.get(), subjectIds.size(), violationTypesPtr.get(), violationTypesFilters.size());
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

template <typename T>
void PowsyblInterface<T>::setZones(const JavaHandle<T>& sensitivityAnalysisContext, const std::vector<::zone*>& zones) {
    ZonesPtr zonesPtr(zones);
    internalCallJava(::setZones, sensitivityAnalysisContext, zonesPtr.get(), zones.size());
}

template <typename T>
void PowsyblInterface<T>::addFactorMatrix(const JavaHandle<T>& sensitivityAnalysisContext, std::string matrixId, const std::vector<std::string>& branchesIds,
                     const std::vector<std::string>& variablesIds, const std::vector<std::string>& contingenciesIds, contingency_context_type ContingencyContextType,
                     sensitivity_function_type sensitivityFunctionType, sensitivity_variable_type sensitivityVariableType) {
       ToCharPtrPtr branchIdPtr(branchesIds);
       ToCharPtrPtr variableIdPtr(variablesIds);
       ToCharPtrPtr contingenciesIdPtr(contingenciesIds);
       internalCallJava(::addFactorMatrix, sensitivityAnalysisContext, branchIdPtr.get(), branchesIds.size(),
                  variableIdPtr.get(), variablesIds.size(), contingenciesIdPtr.get(), contingenciesIds.size(),
                  (char*) matrixId.c_str(), ContingencyContextType, sensitivityFunctionType, sensitivityVariableType);
}

template <typename T>
JavaHandle<T> PowsyblInterface<T>::runSensitivityAnalysis(const JavaHandle<T>& sensitivityAnalysisContext, const JavaHandle<T>& network, bool dc, SensitivityAnalysisParameters& parameters, const std::string& provider, JavaHandle<T>* reportNode) {
    auto c_parameters = parameters.to_c_struct();
    return internalCallJava<JavaHandle<T>>(::runSensitivityAnalysis, sensitivityAnalysisContext, network, dc, c_parameters.get(), (char *) provider.data(), (reportNode == nullptr) ? nullptr : *reportNode);
}

template <typename T>
matrix* PowsyblInterface<T>::getSensitivityMatrix(const JavaHandle<T>& sensitivityAnalysisResultContext, const std::string& matrixId, const std::string& contingencyId) {
    return internalCallJava<matrix*>(::getSensitivityMatrix, sensitivityAnalysisResultContext,
                                (char*) matrixId.c_str(), (char*) contingencyId.c_str());
}

template <typename T>
matrix* PowsyblInterface<T>::getReferenceMatrix(const JavaHandle<T>& sensitivityAnalysisResultContext, const std::string& matrixId, const std::string& contingencyId) {
    return internalCallJava<matrix*>(::getReferenceMatrix, sensitivityAnalysisResultContext,
                                (char*) matrixId.c_str(), (char*) contingencyId.c_str());
}

template <typename T>
SeriesArray<T>* PowsyblInterface<T>::createNetworkElementsSeriesArray(const JavaHandle<T>& network, element_type elementType, filter_attributes_type filterAttributesType, const std::vector<std::string>& attributes, dataframe* dataframe) {
	ToCharPtrPtr attributesPtr(attributes);
    return new SeriesArray<T>(internalCallJava<array*>(::createNetworkElementsSeriesArray, network, elementType, filterAttributesType, attributesPtr.get(), attributes.size(), dataframe));
}

template <typename T>
SeriesArray<T>* PowsyblInterface<T>::createNetworkElementsExtensionSeriesArray(const JavaHandle<T>& network, const std::string& extensionName, const std::string& tableName) {
    return new SeriesArray<T>(internalCallJava<array*>(::createNetworkElementsExtensionSeriesArray, network, (char*) extensionName.c_str(), (char*) tableName.c_str()));
}

template <typename T>
std::vector<std::string> PowsyblInterface<T>::getExtensionsNames() {
    auto formatsArrayPtr = internalCallJava<array*>(::getExtensionsNames);
    ToStringVector<T> formats(formatsArrayPtr);
    return formats.get();
}

template <typename T>
SeriesArray<T>* PowsyblInterface<T>::getExtensionsInformation() {
    return new SeriesArray<T>(internalCallJava<array*>(::getExtensionsInformation));
}

template <typename T>
std::string PowsyblInterface<T>::getWorkingVariantId(const JavaHandle<T>& network) {
    return toString(internalCallJava<char*>(::getWorkingVariantId, network));
}

template <typename T>
void PowsyblInterface<T>::setWorkingVariant(const JavaHandle<T>& network, std::string& variant) {
    internalCallJava(::setWorkingVariant, network, (char*) variant.c_str());
}

template <typename T>
void PowsyblInterface<T>::removeVariant(const JavaHandle<T>& network, std::string& variant) {
    internalCallJava(::removeVariant, network, (char*) variant.c_str());
}

template <typename T>
void PowsyblInterface<T>::cloneVariant(const JavaHandle<T>& network, std::string& src, std::string& variant, bool mayOverwrite) {
    internalCallJava(::cloneVariant, network, (char*) src.c_str(), (char*) variant.c_str(), mayOverwrite);
}

template <typename T>
std::vector<std::string> PowsyblInterface<T>::getVariantsIds(const JavaHandle<T>& network) {
    auto formatsArrayPtr = internalCallJava<array*>(::getVariantsIds, network);
    ToStringVector<T> formats(formatsArrayPtr);
    return formats.get();
}

template <typename T>
void PowsyblInterface<T>::addMonitoredElements(const JavaHandle<T>& securityAnalysisContext, contingency_context_type contingencyContextType, const std::vector<std::string>& branchIds,
                      const std::vector<std::string>& voltageLevelIds, const std::vector<std::string>& threeWindingsTransformerIds,
                      const std::vector<std::string>& contingencyIds) {
    ToCharPtrPtr branchIdsPtr(branchIds);
    ToCharPtrPtr voltageLevelIdsPtr(voltageLevelIds);
    ToCharPtrPtr threeWindingsTransformerIdsPtr(threeWindingsTransformerIds);
    ToCharPtrPtr contingencyIdsPtr(contingencyIds);
    internalCallJava(::addMonitoredElements, securityAnalysisContext, contingencyContextType, branchIdsPtr.get(), branchIds.size(),
    voltageLevelIdsPtr.get(), voltageLevelIds.size(), threeWindingsTransformerIdsPtr.get(),
    threeWindingsTransformerIds.size(), contingencyIdsPtr.get(), contingencyIds.size());
}

template <typename T>
PostContingencyResultArray<T>* PowsyblInterface<T>::getPostContingencyResults(const JavaHandle<T>& securityAnalysisResult) {
    return new PostContingencyResultArray<T>(internalCallJava<array*>(::getPostContingencyResults, securityAnalysisResult));
}

template <typename T>
OperatorStrategyResultArray<T>* PowsyblInterface<T>::getOperatorStrategyResults(const JavaHandle<T>& securityAnalysisResult) {
    return new OperatorStrategyResultArray<T>(internalCallJava<array*>(::getOperatorStrategyResults, securityAnalysisResult));
}

template <typename T>
pre_contingency_result* PowsyblInterface<T>::getPreContingencyResult(const JavaHandle<T>& securityAnalysisResult) {
    return internalCallJava<pre_contingency_result*>(::getPreContingencyResult, securityAnalysisResult);
}

template <typename T>
SeriesArray<T>* PowsyblInterface<T>::getLimitViolations(const JavaHandle<T>& securityAnalysisResult) {
    return new SeriesArray<T>(internalCallJava<array*>(::getLimitViolations, securityAnalysisResult));
}

template <typename T>
SeriesArray<T>* PowsyblInterface<T>::getBranchResults(const JavaHandle<T>& securityAnalysisResult) {
    return new SeriesArray<T>(internalCallJava<array*>(::getBranchResults, securityAnalysisResult));
}

template <typename T>
SeriesArray<T>* PowsyblInterface<T>::getBusResults(const JavaHandle<T>& securityAnalysisResult) {
    return new SeriesArray<T>(internalCallJava<array*>(::getBusResults, securityAnalysisResult));
}

template <typename T>
SeriesArray<T>* PowsyblInterface<T>::getThreeWindingsTransformerResults(const JavaHandle<T>& securityAnalysisResult) {
    return new SeriesArray<T>(internalCallJava<array*>(::getThreeWindingsTransformerResults, securityAnalysisResult));
}

template <typename T>
SeriesArray<T>* PowsyblInterface<T>::getNodeBreakerViewSwitches(const JavaHandle<T>& network, std::string& voltageLevel) {
    return new SeriesArray<T>(internalCallJava<array*>(::getNodeBreakerViewSwitches, network, (char*) voltageLevel.c_str()));
}

template <typename T>
SeriesArray<T>* PowsyblInterface<T>::getNodeBreakerViewNodes(const JavaHandle<T>& network, std::string& voltageLevel) {
    return new SeriesArray<T>(internalCallJava<array*>(::getNodeBreakerViewNodes, network, (char*) voltageLevel.c_str()));
}

template <typename T>
SeriesArray<T>* PowsyblInterface<T>::getNodeBreakerViewInternalConnections(const JavaHandle<T>& network, std::string& voltageLevel) {
    return new SeriesArray<T>(internalCallJava<array*>(::getNodeBreakerViewInternalConnections, network, (char*) voltageLevel.c_str()));
}

template <typename T>
SeriesArray<T>* PowsyblInterface<T>::getBusBreakerViewSwitches(const JavaHandle<T>& network, std::string& voltageLevel) {
    return new SeriesArray<T>(internalCallJava<array*>(::getBusBreakerViewSwitches, network, (char*) voltageLevel.c_str()));
}

template <typename T>
SeriesArray<T>* PowsyblInterface<T>::getBusBreakerViewBuses(const JavaHandle<T>& network, std::string& voltageLevel) {
    return new SeriesArray<T>(internalCallJava<array*>(::getBusBreakerViewBuses, network, (char*) voltageLevel.c_str()));
}

template <typename T>
SeriesArray<T>* PowsyblInterface<T>::getBusBreakerViewElements(const JavaHandle<T>& network, std::string& voltageLevel) {
    return new SeriesArray<T>(internalCallJava<array*>(::getBusBreakerViewElements, network, (char*) voltageLevel.c_str()));
}

template <typename T>
void PowsyblInterface<T>::updateNetworkElementsWithSeries(pypowsybl::JavaHandle<T> network, dataframe* dataframe, element_type elementType) {
    internalCallJava(::updateNetworkElementsWithSeries, network, elementType, dataframe);
}

template <typename T>
std::vector<SeriesMetadata> PowsyblInterface<T>::getNetworkDataframeMetadata(element_type elementType) {
    dataframe_metadata* metadata = internalCallJava<dataframe_metadata*>(::getSeriesMetadata, elementType);
    std::vector<SeriesMetadata> res = convertDataframeMetadata(metadata);
    internalCallJava(::freeDataframeMetadata, metadata);
    return res;
}

template <typename T>
std::vector<std::vector<SeriesMetadata>> PowsyblInterface<T>::getNetworkElementCreationDataframesMetadata(element_type elementType) {

    dataframes_metadata* allDataframesMetadata = internalCallJava<dataframes_metadata*>(::getCreationMetadata, elementType);
    std::vector<std::vector<SeriesMetadata>> res;
    for (int i =0; i < allDataframesMetadata->dataframes_count; i++) {
        res.push_back(convertDataframeMetadata(allDataframesMetadata->dataframes_metadata + i));
    }
    internalCallJava(::freeDataframesMetadata, allDataframesMetadata);
    return res;
}

template <typename T>
::validation_level_type PowsyblInterface<T>::getValidationLevel(const JavaHandle<T>& network) {
    // TBD
    //return validation_level_type::EQUIPMENT;
    return internalCallJava<validation_level_type>(::getValidationLevel, network);
}

template <typename T>
::validation_level_type PowsyblInterface<T>::validate(const JavaHandle<T>& network) {
    // TBD
    //return validation_level_type::STEADY_STATE_HYPOTHESIS;
    return internalCallJava<validation_level_type>(::validate, network);
}

template <typename T>
void PowsyblInterface<T>::setMinValidationLevel(pypowsybl::JavaHandle<T> network, validation_level_type validationLevel) {
    internalCallJava(::setMinValidationLevel, network, validationLevel);
}

template <typename T>
void PowsyblInterface<T>::setupLoggerCallback(void *& callback) {
    internalCallJava(::setupLoggerCallback, callback);
}

template <typename T>
void PowsyblInterface<T>::removeNetworkElements(const JavaHandle<T>& network, const std::vector<std::string>& elementIds) {
    ToCharPtrPtr elementIdsPtr(elementIds);
    internalCallJava(::removeNetworkElements, network, elementIdsPtr.get(), elementIds.size());
}

template <typename T>
void PowsyblInterface<T>::addNetworkElementProperties(pypowsybl::JavaHandle<T> network, dataframe* dataframe) {
    internalCallJava(::addNetworkElementProperties, network, dataframe);
}

template <typename T>
void PowsyblInterface<T>::removeNetworkElementProperties(pypowsybl::JavaHandle<T> network, const std::vector<std::string>& ids, const std::vector<std::string>& properties) {
    ToCharPtrPtr idsPtr(ids);
    ToCharPtrPtr propertiesPtr(properties);
    internalCallJava(::removeNetworkElementProperties, network, idsPtr.get(), ids.size(), propertiesPtr.get(), properties.size());
}

template <typename T>
std::vector<std::string> PowsyblInterface<T>::getLoadFlowProviderParametersNames(const std::string& loadFlowProvider) {
    auto providerParametersArrayPtr = internalCallJava<array*>(::getLoadFlowProviderParametersNames, (char*) loadFlowProvider.c_str());
    ToStringVector<T> providerParameters(providerParametersArrayPtr);
    return providerParameters.get();
}

template <typename T>
SeriesArray<T>* PowsyblInterface<T>::createLoadFlowProviderParametersSeriesArray(const std::string& provider) {
    return new SeriesArray<T>(internalCallJava<array*>(::createLoadFlowProviderParametersSeriesArray, (char*) provider.data()));
}

template <typename T>
std::vector<std::string> PowsyblInterface<T>::getSecurityAnalysisProviderParametersNames(const std::string& securityAnalysisProvider) {
    auto providerParametersArrayPtr = internalCallJava<array*>(::getSecurityAnalysisProviderParametersNames, (char*) securityAnalysisProvider.c_str());
    ToStringVector<T> providerParameters(providerParametersArrayPtr);
    return providerParameters.get();
}

template <typename T>
std::vector<std::string> PowsyblInterface<T>::getSensitivityAnalysisProviderParametersNames(const std::string& sensitivityAnalysisProvider) {
    auto providerParametersArrayPtr = internalCallJava<array*>(::getSensitivityAnalysisProviderParametersNames, (char*) sensitivityAnalysisProvider.c_str());
    ToStringVector<T> providerParameters(providerParametersArrayPtr);
    return providerParameters.get();
}

template <typename T>
void PowsyblInterface<T>::updateNetworkElementsExtensionsWithSeries(pypowsybl::JavaHandle<T> network, std::string& name, std::string& tableName, dataframe* dataframe) {
    internalCallJava(::updateNetworkElementsExtensionsWithSeries, network, (char*) name.data(), (char*) tableName.data(), dataframe);
}

template <typename T>
void PowsyblInterface<T>::removeExtensions(const JavaHandle<T>& network, std::string& name, const std::vector<std::string>& ids) {
    ToCharPtrPtr idsPtr(ids);
    internalCallJava(::removeExtensions, network, (char*) name.data(), idsPtr.get(), ids.size());
}

template <typename T>
std::vector<SeriesMetadata> PowsyblInterface<T>::getNetworkExtensionsDataframeMetadata(std::string& name, std::string& tableName) {
    dataframe_metadata* metadata = internalCallJava<dataframe_metadata*>(::getExtensionSeriesMetadata, (char*) name.data(), (char*) tableName.data());
    std::vector<SeriesMetadata> res = convertDataframeMetadata(metadata);
    internalCallJava(::freeDataframeMetadata, metadata);
    return res;
}

template <typename T>
std::vector<std::vector<SeriesMetadata>> PowsyblInterface<T>::getNetworkExtensionsCreationDataframesMetadata(std::string& name) {
    dataframes_metadata* allDataframesMetadata = internalCallJava<dataframes_metadata*>(::getExtensionsCreationMetadata, (char*) name.data());
    std::vector<std::vector<SeriesMetadata>> res;
    for (int i =0; i < allDataframesMetadata->dataframes_count; i++) {
        res.push_back(convertDataframeMetadata(allDataframesMetadata->dataframes_metadata + i));
    }
    internalCallJava(::freeDataframesMetadata, allDataframesMetadata);
    return res;
}

template <typename T>
JavaHandle<T> PowsyblInterface<T>::createGLSKdocument(std::string& filename) {
    return internalCallJava<JavaHandle<T>>(::createGLSKdocument, (char*) filename.c_str());
}

template <typename T>
std::vector<std::string> PowsyblInterface<T>::getGLSKinjectionkeys(pypowsybl::JavaHandle<T> network, const JavaHandle<T>& importer, std::string& country, long instant) {
    auto keysArrayPtr = internalCallJava<array*>(::getGLSKinjectionkeys, network, importer, (char*) country.c_str(), instant);
    ToStringVector<T> keys(keysArrayPtr);
    return keys.get();
}

template <typename T>
std::vector<std::string> PowsyblInterface<T>::getGLSKcountries(const JavaHandle<T>& importer) {
    auto countriesArrayPtr = internalCallJava<array*>(::getGLSKcountries, importer);
    ToStringVector<T> countries(countriesArrayPtr);
    return countries.get();
}

template <typename T>
std::vector<double> PowsyblInterface<T>::getGLSKInjectionFactors(pypowsybl::JavaHandle<T> network, const JavaHandle<T>& importer, std::string& country, long instant) {
    auto countriesArrayPtr = internalCallJava<array*>(::getInjectionFactor, network, importer, (char*) country.c_str(), instant);
    ToPrimitiveVector<double, T> values(countriesArrayPtr);
    return values.get();
}

template <typename T>
long PowsyblInterface<T>::getInjectionFactorStartTimestamp(const JavaHandle<T>& importer) {
    return internalCallJava<long>(::getInjectionFactorStartTimestamp, importer);
}

template <typename T>
long PowsyblInterface<T>::getInjectionFactorEndTimestamp(const JavaHandle<T>& importer) {
    return internalCallJava<long>(::getInjectionFactorEndTimestamp, importer);
}

template <typename T>
JavaHandle<T> PowsyblInterface<T>::createReportNode(const std::string& taskKey, const std::string& defaultName) {
    return internalCallJava<JavaHandle<T>>(::createReportNode, (char*) taskKey.data(), (char*) defaultName.data());
}

template <typename T>
std::string PowsyblInterface<T>::printReport(const JavaHandle<T>& reportNodeModel) {
    return toString(internalCallJava<char*>(::printReport, reportNodeModel));
}

template <typename T>
std::string PowsyblInterface<T>::jsonReport(const JavaHandle<T>& reportNodeModel) {
    return toString(internalCallJava<char*>(::jsonReport, reportNodeModel));
}

template <typename T>
JavaHandle<T> PowsyblInterface<T>::createFlowDecomposition() {
    return internalCallJava<JavaHandle<T>>(::createFlowDecomposition);
}

template <typename T>
void PowsyblInterface<T>::addContingencyForFlowDecomposition(const JavaHandle<T>& flowDecompositionContext, const std::string& contingencyId, const std::vector<std::string>& elementsIds) {
    ToCharPtrPtr elementIdPtr(elementsIds);
    internalCallJava(::addContingencyForFlowDecomposition, flowDecompositionContext, (char*) contingencyId.data(), elementIdPtr.get(), elementsIds.size());
}

template <typename T>
void PowsyblInterface<T>::addPrecontingencyMonitoredElementsForFlowDecomposition(const JavaHandle<T>& flowDecompositionContext, const std::vector<std::string>& branchIds) {
    ToCharPtrPtr branchIdPtr(branchIds);
    internalCallJava(::addPrecontingencyMonitoredElementsForFlowDecomposition, flowDecompositionContext, branchIdPtr.get(), branchIds.size());
}

template <typename T>
void PowsyblInterface<T>::addPostcontingencyMonitoredElementsForFlowDecomposition(const JavaHandle<T>& flowDecompositionContext, const std::vector<std::string>& branchIds, const std::vector<std::string>& contingencyIds) {
    ToCharPtrPtr branchIdPtr(branchIds);
    ToCharPtrPtr contingencyIdPtr(contingencyIds);
    internalCallJava(::addPostcontingencyMonitoredElementsForFlowDecomposition, flowDecompositionContext, branchIdPtr.get(), branchIds.size(), contingencyIdPtr.get(), contingencyIds.size());
}

template <typename T>
void PowsyblInterface<T>::addAdditionalXnecProviderForFlowDecomposition(const JavaHandle<T>& flowDecompositionContext, DefaultXnecProvider defaultXnecProvider) {
    internalCallJava(::addAdditionalXnecProviderForFlowDecomposition, flowDecompositionContext, defaultXnecProvider);
}

template <typename T>
SeriesArray<T>* PowsyblInterface<T>::runFlowDecomposition(const JavaHandle<T>& flowDecompositionContext, const JavaHandle<T>& network, const FlowDecompositionParameters& flow_decomposition_parameters, const LoadFlowParameters& loadflow_parameters) {
    auto c_flow_decomposition_parameters = flow_decomposition_parameters.to_c_struct();
    auto c_loadflow_parameters  = loadflow_parameters.to_c_struct();
    return new SeriesArray<T>(internalCallJava<array*>(::runFlowDecomposition, flowDecompositionContext, network, c_flow_decomposition_parameters.get(), c_loadflow_parameters.get()));
}

template <typename T>
FlowDecompositionParameters* PowsyblInterface<T>::createFlowDecompositionParameters() {
    flow_decomposition_parameters* parameters_ptr = internalCallJava<flow_decomposition_parameters*>(::createFlowDecompositionParameters);
    auto parameters = std::shared_ptr<flow_decomposition_parameters>(parameters_ptr, [](flow_decomposition_parameters* ptr){
       //Memory has been allocated on java side, we need to clean it up on java side
       internalCallJava(::freeFlowDecompositionParameters, ptr);
    });
    return new FlowDecompositionParameters(parameters.get());
}

template <typename T>
SeriesArray<T>* PowsyblInterface<T>::getConnectablesOrderPositions(const JavaHandle<T>& network, const std::string voltage_level_id) {
    return new SeriesArray<T>(internalCallJava<array*>(::getConnectablesOrderPositions, network, (char*) voltage_level_id.c_str()));
}

template <typename T>
std::vector<int> PowsyblInterface<T>::getUnusedConnectableOrderPositions(const pypowsybl::JavaHandle<T> network, const std::string busbarSectionId, const std::string beforeOrAfter) {
    auto positionsArrayPtr = internalCallJava<array*>(::getUnusedConnectableOrderPositions, network, (char*) busbarSectionId.c_str(), (char*) beforeOrAfter.c_str());
    ToPrimitiveVector<int, T> res(positionsArrayPtr);
    return res.get();
}

template <typename T>
void PowsyblInterface<T>::removeAliases(pypowsybl::JavaHandle<T> network, dataframe* dataframe) {
    internalCallJava(::removeAliases, network, dataframe);
}

template <typename T>
void PowsyblInterface<T>::closePypowsybl() {
    internalCallJava(::closePypowsybl);
}

template <typename T>
SldParameters* PowsyblInterface<T>::createSldParameters() {
    sld_parameters* parameters_ptr = internalCallJava<sld_parameters*>(::createSldParameters);
    auto parameters = std::shared_ptr<sld_parameters>(parameters_ptr, [](sld_parameters* ptr){
       //Memory has been allocated on java side, we need to clean it up on java side
       internalCallJava(::freeSldParameters, ptr);
    });
    return new SldParameters(parameters.get());
}

template <typename T>
NadParameters* PowsyblInterface<T>::createNadParameters() {
    nad_parameters* parameters_ptr = internalCallJava<nad_parameters*>(::createNadParameters);
    auto parameters = std::shared_ptr<nad_parameters>(parameters_ptr, [](nad_parameters* ptr){
       //Memory has been allocated on java side, we need to clean it up on java side
       internalCallJava(::freeNadParameters, ptr);
    });
    return new NadParameters(parameters.get());
}

template <typename T>
void PowsyblInterface<T>::removeElementsModification(pypowsybl::JavaHandle<T> network, const std::vector<std::string>& connectableIds, dataframe* dataframe, remove_modification_type removeModificationType, bool throwException, JavaHandle<T>* reportNode) {
    ToCharPtrPtr connectableIdsPtr(connectableIds);
    internalCallJava(::removeElementsModification, network, connectableIdsPtr.get(), connectableIds.size(), dataframe, removeModificationType, throwException, (reportNode == nullptr) ? nullptr : *reportNode);
}

/*---------------------------------DYNAMIC MODELLING WITH DYNAWALTZ---------------------------*/

template <typename T>
JavaHandle<T> PowsyblInterface<T>::createDynamicSimulationContext() {
    return internalCallJava<JavaHandle<T>>(::createDynamicSimulationContext);
}


template <typename T>
JavaHandle<T> PowsyblInterface<T>::createDynamicModelMapping() {
    return internalCallJava<JavaHandle<T>>(::createDynamicModelMapping);
}

template <typename T>
JavaHandle<T> PowsyblInterface<T>::createTimeseriesMapping() {
    return internalCallJava<JavaHandle<T>>(::createTimeseriesMapping);
}

template <typename T>
JavaHandle<T> PowsyblInterface<T>::createEventMapping() {
    return internalCallJava<JavaHandle<T>>(::createEventMapping);
}

template <typename T>
JavaHandle<T> PowsyblInterface<T>::runDynamicModel(JavaHandle<T> dynamicModelContext, JavaHandle<T> network, JavaHandle<T> dynamicMapping, JavaHandle<T> eventMapping, JavaHandle<T> timeSeriesMapping, int start, int stop) {
    return internalCallJava<JavaHandle<T>>(::runDynamicModel, dynamicModelContext, network, dynamicMapping, eventMapping, timeSeriesMapping, start, stop);
}

template <typename T>
void PowsyblInterface<T>::addDynamicMappings(JavaHandle<T> dynamicMappingHandle, DynamicMappingType mappingType, dataframe* mappingDf) {
    internalCallJava(::addDynamicMappings, dynamicMappingHandle, mappingType, mappingDf);
}

template <typename T>
void PowsyblInterface<T>::addCurve(JavaHandle<T> curveMappingHandle, std::string dynamicId, std::string variable) {
    internalCallJava(::addCurve, curveMappingHandle, (char*) dynamicId.c_str(), (char*) variable.c_str());
}

template <typename T>
void PowsyblInterface<T>::addEventDisconnection(const JavaHandle<T>& eventMappingHandle, const std::string& staticId, double eventTime, int disconnectOnly) {
    internalCallJava(::addEventDisconnection, eventMappingHandle, (char*) staticId.c_str(), eventTime, disconnectOnly);
}

template <typename T>
std::string PowsyblInterface<T>::getDynamicSimulationResultsStatus(JavaHandle<T> dynamicSimulationResultsHandle) {
    return internalCallJava<std::string>(::getDynamicSimulationResultsStatus, dynamicSimulationResultsHandle);
}

template <typename T>
SeriesArray<T>* PowsyblInterface<T>::getDynamicCurve(JavaHandle<T> resultHandle, std::string curveName) {
    return new SeriesArray<T>(internalCallJava<array*>(::getDynamicCurve, resultHandle, (char*) curveName.c_str()));
}

template <typename T>
std::vector<std::string> PowsyblInterface<T>::getAllDynamicCurvesIds(JavaHandle<T> resultHandle) {
    ToStringVector<T> vector(internalCallJava<array*>(::getAllDynamicCurvesIds, resultHandle));
    return vector.get();
}

template <typename T>
std::vector<SeriesMetadata> PowsyblInterface<T>::getDynamicMappingsMetaData(DynamicMappingType mappingType) {
    dataframe_metadata* metadata = internalCallJava<dataframe_metadata*>(::getDynamicMappingsMetaData, mappingType);
    std::vector<SeriesMetadata> res = convertDataframeMetadata(metadata);
    internalCallJava(::freeDataframeMetadata, metadata);
    return res;
    }

template <typename T>
std::vector<SeriesMetadata> PowsyblInterface<T>::getModificationMetadata(network_modification_type networkModificationType) {
    dataframe_metadata* metadata = internalCallJava<dataframe_metadata*>(::getModificationMetadata, networkModificationType);
    std::vector<SeriesMetadata> res = convertDataframeMetadata(metadata);
    internalCallJava(::freeDataframeMetadata, metadata);
    return res;
}

template <typename T>
std::vector<std::vector<SeriesMetadata>> PowsyblInterface<T>::getModificationMetadataWithElementType(network_modification_type networkModificationType, element_type elementType) {
    dataframes_metadata* metadata = internalCallJava<dataframes_metadata*>(::getModificationMetadataWithElementType, networkModificationType, elementType);
    std::vector<std::vector<SeriesMetadata>> res;
    for (int i =0; i < metadata->dataframes_count; i++) {
        res.push_back(convertDataframeMetadata(metadata->dataframes_metadata + i));
    }
    internalCallJava(::freeDataframesMetadata, metadata);
    return res;
}


/*---------------------------------SHORT-CIRCUIT ANALYSIS---------------------------*/

template <typename T>
void PowsyblInterface<T>::setDefaultShortCircuitAnalysisProvider(const std::string& shortCircuitAnalysisProvider) {
    internalCallJava(::setDefaultShortCircuitAnalysisProvider, (char*) shortCircuitAnalysisProvider.data());
}

template <typename T>
std::string PowsyblInterface<T>::getDefaultShortCircuitAnalysisProvider() {
    return toString(internalCallJava<char*>(::getDefaultShortCircuitAnalysisProvider));
}

template <typename T>
std::vector<std::string> PowsyblInterface<T>::getShortCircuitAnalysisProviderNames() {
    auto formatsArrayPtr = internalCallJava<array*>(::getShortCircuitAnalysisProviderNames);
    ToStringVector<T> formats(formatsArrayPtr);
    return formats.get();
}

template <typename T>
std::vector<std::string> PowsyblInterface<T>::getShortCircuitAnalysisProviderParametersNames(const std::string& shortCircuitAnalysisProvider) {
    auto providerParametersArrayPtr = internalCallJava<array*>(::getShortCircuitAnalysisProviderParametersNames, (char*) shortCircuitAnalysisProvider.c_str());
    ToStringVector<T> providerParameters(providerParametersArrayPtr);
    return providerParameters.get();
}

template <typename T>
JavaHandle<T> PowsyblInterface<T>::createShortCircuitAnalysis() {
    return internalCallJava<JavaHandle<T>>(::createShortCircuitAnalysis);
}

template <typename T>
JavaHandle<T> PowsyblInterface<T>::runShortCircuitAnalysis(const JavaHandle<T>& shortCircuitAnalysisContext, const JavaHandle<T>& network, const ShortCircuitAnalysisParameters& parameters,
    const std::string& provider, JavaHandle<T>* reportNode) {
    auto c_parameters = parameters.to_c_struct();
    return internalCallJava<JavaHandle<T>>(::runShortCircuitAnalysis, shortCircuitAnalysisContext, network, c_parameters.get(), (char *) provider.data(), (reportNode == nullptr) ? nullptr : *reportNode);
}

template <typename T>
ShortCircuitAnalysisParameters* PowsyblInterface<T>::createShortCircuitAnalysisParameters() {
    shortcircuit_analysis_parameters* parameters_ptr = internalCallJava<shortcircuit_analysis_parameters*>(::createShortCircuitAnalysisParameters);
    auto parameters = std::shared_ptr<shortcircuit_analysis_parameters>(parameters_ptr, [](shortcircuit_analysis_parameters* ptr){
        internalCallJava(::freeShortCircuitAnalysisParameters, ptr);
    });
    return new ShortCircuitAnalysisParameters(parameters.get());
}

template <typename T>
std::vector<SeriesMetadata> PowsyblInterface<T>::getFaultsMetaData() {
    dataframe_metadata* metadata = internalCallJava<dataframe_metadata*>(::getFaultsDataframeMetaData);
    std::vector<SeriesMetadata> res = convertDataframeMetadata(metadata);
    internalCallJava(::freeDataframeMetadata, metadata);
    return res;
}

template <typename T>
void PowsyblInterface<T>::setFaults(pypowsybl::JavaHandle<T> analysisContext, dataframe* dataframe) {
    internalCallJava(::setFaults, analysisContext, dataframe);
}

template <typename T>
SeriesArray<T>* PowsyblInterface<T>::getFaultResults(const JavaHandle<T>& shortCircuitAnalysisResult, bool withFortescueResult) {
    return new SeriesArray<T>(internalCallJava<array*>(::getShortCircuitAnalysisFaultResults, shortCircuitAnalysisResult, withFortescueResult));
}

template <typename T>
SeriesArray<T>* PowsyblInterface<T>::getFeederResults(const JavaHandle<T>& shortCircuitAnalysisResult, bool withFortescueResult) {
    return new SeriesArray<T>(internalCallJava<array*>(::getShortCircuitAnalysisFeederResults, shortCircuitAnalysisResult, withFortescueResult));
}

template <typename T>
SeriesArray<T>*PowsyblInterface<T>:: getShortCircuitLimitViolations(const JavaHandle<T>& shortCircuitAnalysisResult) {
    return new SeriesArray<T>(internalCallJava<array*>(::getShortCircuitAnalysisLimitViolationsResults, shortCircuitAnalysisResult));
}

template <typename T>
SeriesArray<T>* PowsyblInterface<T>::getShortCircuitBusResults(const JavaHandle<T>& shortCircuitAnalysisResult, bool withFortescueResult) {
    return new SeriesArray<T>(internalCallJava<array*>(::getShortCircuitAnalysisBusResults, shortCircuitAnalysisResult, withFortescueResult));
}

template <typename T>
JavaHandle<T> PowsyblInterface<T>::createVoltageInitializerParams() {
    return internalCallJava<JavaHandle<T>>(::createVoltageInitializerParams);
}

template <typename T>
void PowsyblInterface<T>::voltageInitializerAddSpecificLowVoltageLimits(const JavaHandle<T>& paramsHandle, const std::string& voltageLevelId, bool isRelative, double limit) {
    internalCallJava(::voltageInitializerAddSpecificLowVoltageLimits, paramsHandle, (char*) voltageLevelId.c_str(), isRelative, limit);
}

template <typename T>
void PowsyblInterface<T>::voltageInitializerAddSpecificHighVoltageLimits(const JavaHandle<T>& paramsHandle, const std::string& voltageLevelId, bool isRelative, double limit) {
    internalCallJava(::voltageInitializerAddSpecificHighVoltageLimits, paramsHandle, (char*) voltageLevelId.c_str(), isRelative, limit);
}

template <typename T>
void PowsyblInterface<T>::voltageInitializerAddVariableShuntCompensators(const JavaHandle<T>& paramsHandle, const std::string& idPtr) {
    internalCallJava(::voltageInitializerAddVariableShuntCompensators, paramsHandle, (char*) idPtr.c_str());
}

template <typename T>
void PowsyblInterface<T>::voltageInitializerAddConstantQGenerators(const JavaHandle<T>& paramsHandle, const std::string& idPtr) {
    internalCallJava(::voltageInitializerAddConstantQGenerators, paramsHandle, (char*) idPtr.c_str());
}

template <typename T>
void PowsyblInterface<T>::voltageInitializerAddVariableTwoWindingsTransformers(const JavaHandle<T>& paramsHandle, const std::string& idPtr) {
    internalCallJava(::voltageInitializerAddVariableTwoWindingsTransformers, paramsHandle, (char*) idPtr.c_str());
}

template <typename T>
void PowsyblInterface<T>::voltageInitializerSetObjective(const JavaHandle<T>& paramsHandle, VoltageInitializerObjective cObjective) {
    internalCallJava(::voltageInitializerSetObjective, paramsHandle, cObjective);
}

template <typename T>
void PowsyblInterface<T>::voltageInitializerSetObjectiveDistance(const JavaHandle<T>& paramsHandle, double dist) {
    internalCallJava(::voltageInitializerSetObjectiveDistance, paramsHandle, dist);
}

template <typename T>
void PowsyblInterface<T>::voltageInitializerApplyAllModifications(const JavaHandle<T>& resultHandle, const JavaHandle<T>& networkHandle) {
    internalCallJava(::voltageInitializerApplyAllModifications, resultHandle, networkHandle);
}

template <typename T>
VoltageInitializerStatus PowsyblInterface<T>::voltageInitializerGetStatus(const JavaHandle<T>& resultHandle) {
    return internalCallJava<VoltageInitializerStatus>(::voltageInitializerGetStatus, resultHandle);
}

template <typename T>
std::map<std::string, std::string> PowsyblInterface<T>::voltageInitializerGetIndicators(const JavaHandle<T>& resultHandle) {
    string_map* indicators = internalCallJava<string_map*>(::voltageInitializerGetIndicators, resultHandle);
    std::map<std::string, std::string> map = convertMapStructToStdMap(indicators);
    internalCallJava(::freeStringMap, indicators);
    return map;
}

template <typename T>
JavaHandle<T> PowsyblInterface<T>::runVoltageInitializer(bool debug, const JavaHandle<T>& networkHandle, const JavaHandle<T>& paramsHandle) {
    return internalCallJava<JavaHandle<T>>(::runVoltageInitializer, debug, networkHandle, paramsHandle);
}

template <typename T>
void PowsyblInterface<T>::createElement(JavaHandle<T> network, const std::vector<dataframe*>& dataframes, element_type elementType) {
    std::shared_ptr<dataframe_array> dataframeArray = createDataframeArray(dataframes);
    internalCallJava(::createElement, network, elementType, dataframeArray.get());
}

template <typename T>
void PowsyblInterface<T>::createNetworkModification(JavaHandle<T> network, const std::vector<dataframe*>& dataframes, network_modification_type networkModificationType, bool throwException, JavaHandle<T>* reportNode) {
    std::shared_ptr<dataframe_array> dataframeArray = createDataframeArray(dataframes);
    internalCallJava(::createNetworkModification, network, dataframeArray.get(), networkModificationType, throwException, (reportNode == nullptr) ? nullptr : *reportNode);
}

template <typename T>
void PowsyblInterface<T>::createExtensions(JavaHandle<T> network, const std::vector<dataframe*>& dataframes, std::string& name) {
    std::shared_ptr<dataframe_array> dataframeArray = createDataframeArray(dataframes);
    internalCallJava(::createExtensions, network, (char*) name.data(), dataframeArray.get());
}

template<typename T>
LoadFlowComponentResultArray<T>::~LoadFlowComponentResultArray() {
    T::callJava(::freeLoadFlowComponentResultPointer, delegate_);
}

template<typename T>
SlackBusResultArray<T>::~SlackBusResultArray() {
    // already freed by loadflow_component_result
}

template<typename T>
PostContingencyResultArray<T>::~PostContingencyResultArray() {
    T::callJava(::freeContingencyResultArrayPointer, delegate_);
}

template<typename T>
OperatorStrategyResultArray<T>::~OperatorStrategyResultArray() {
    T::callJava(::freeOperatorStrategyResultArrayPointer, delegate_);
}

template<typename T>
LimitViolationArray<T>::~LimitViolationArray() {
    // already freed by contingency_result
}

template<typename T>
SeriesArray<T>::~SeriesArray() {
    T::callJava(::freeSeriesArray, delegate_);
}

}
#endif //PYPOWSYBL_H
