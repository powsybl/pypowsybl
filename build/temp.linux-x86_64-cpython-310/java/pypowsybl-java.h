#ifndef __PYPOWSYBL_JAVA_H
#define __PYPOWSYBL_JAVA_H

#include <graal_isolate.h>


#if defined(__cplusplus)
extern "C" {
#endif

void setJavaLibraryPath(graal_isolatethread_t*, char*, exception_handler*);

void setConfigRead(graal_isolatethread_t*, int, exception_handler*);

int isConfigRead(graal_isolatethread_t*, exception_handler*);

char* getVersionTable(graal_isolatethread_t*, exception_handler*);

void freeStringArray(graal_isolatethread_t*, array*, exception_handler*);

void freeArray(graal_isolatethread_t*, array*, exception_handler*);

void freeSeriesArray(graal_isolatethread_t*, array*, exception_handler*);

void destroyObjectHandle(graal_isolatethread_t*, void *, exception_handler*);

char* getWorkingVariantId(graal_isolatethread_t*, void *, exception_handler*);

void freeString(graal_isolatethread_t*, char*, exception_handler*);

void closePypowsybl(graal_isolatethread_t*, exception_handler*);

void freeStringMap(graal_isolatethread_t*, string_map*, exception_handler*);

void * createDynamicSimulationContext(graal_isolatethread_t*, exception_handler*);

void * createDynamicModelMapping(graal_isolatethread_t*, exception_handler*);

void * createTimeseriesMapping(graal_isolatethread_t*, exception_handler*);

void * createEventMapping(graal_isolatethread_t*, exception_handler*);

void * runDynamicModel(graal_isolatethread_t*, void *, void *, void *, void *, void *, int, int, exception_handler*);

void addDynamicMappings(graal_isolatethread_t*, void *, DynamicMappingType, dataframe*, exception_handler*);

dataframe_metadata* getDynamicMappingsMetaData(graal_isolatethread_t*, DynamicMappingType, exception_handler*);

void addCurve(graal_isolatethread_t*, void *, char*, char*, exception_handler*);

void addEventDisconnection(graal_isolatethread_t*, void *, char*, double, int, exception_handler*);

char* getDynamicSimulationResultsStatus(graal_isolatethread_t*, void *, exception_handler*);

array* getDynamicCurve(graal_isolatethread_t*, void *, char*, exception_handler*);

array* getAllDynamicCurvesIds(graal_isolatethread_t*, void *, exception_handler*);

void * createFlowDecomposition(graal_isolatethread_t*, exception_handler*);

void addContingencyForFlowDecomposition(graal_isolatethread_t*, void *, char*, char**, int, exception_handler*);

void addPrecontingencyMonitoredElementsForFlowDecomposition(graal_isolatethread_t*, void *, char**, int, exception_handler*);

void addPostcontingencyMonitoredElementsForFlowDecomposition(graal_isolatethread_t*, void *, char**, int, char**, int, exception_handler*);

void addAdditionalXnecProviderForFlowDecomposition(graal_isolatethread_t*, void *, int, exception_handler*);

array* runFlowDecomposition(graal_isolatethread_t*, void *, void *, flow_decomposition_parameters*, loadflow_parameters*, exception_handler*);

flow_decomposition_parameters* createFlowDecompositionParameters(graal_isolatethread_t*, exception_handler*);

void freeFlowDecompositionParameters(graal_isolatethread_t*, flow_decomposition_parameters*, exception_handler*);

void * createGLSKdocument(graal_isolatethread_t*, char*, exception_handler*);

array* getGLSKinjectionkeys(graal_isolatethread_t*, void *, void *, char*, long long int, exception_handler*);

array* getGLSKcountries(graal_isolatethread_t*, void *, exception_handler*);

array* getInjectionFactor(graal_isolatethread_t*, void *, void *, char*, long long int, exception_handler*);

long long int getInjectionFactorStartTimestamp(graal_isolatethread_t*, void *, exception_handler*);

long long int getInjectionFactorEndTimestamp(graal_isolatethread_t*, void *, exception_handler*);

void setDefaultLoadFlowProvider(graal_isolatethread_t*, char*, exception_handler*);

char* getDefaultLoadFlowProvider(graal_isolatethread_t*, exception_handler*);

void freeLoadFlowComponentResultPointer(graal_isolatethread_t*, array*, exception_handler*);

array* getLoadFlowProviderNames(graal_isolatethread_t*, exception_handler*);

array* runLoadFlow(graal_isolatethread_t*, void *, int, loadflow_parameters*, char*, void *, exception_handler*);

loadflow_parameters* createLoadFlowParameters(graal_isolatethread_t*, exception_handler*);

void freeLoadFlowParameters(graal_isolatethread_t*, loadflow_parameters*, exception_handler*);

array* getLoadFlowProviderParametersNames(graal_isolatethread_t*, char*, exception_handler*);

array* createLoadFlowProviderParametersSeriesArray(graal_isolatethread_t*, char*, exception_handler*);

array* runLoadFlowValidation(graal_isolatethread_t*, void *, validation_type, loadflow_validation_parameters*, exception_handler*);

loadflow_validation_parameters* createValidationConfig(graal_isolatethread_t*, exception_handler*);

void freeValidationConfig(graal_isolatethread_t*, loadflow_validation_parameters*, exception_handler*);

void setupLoggerCallback(graal_isolatethread_t*, void *, exception_handler*);

void setLogLevel(graal_isolatethread_t*, int, exception_handler*);

array* getNetworkImportFormats(graal_isolatethread_t*, exception_handler*);

array* getNetworkExportFormats(graal_isolatethread_t*, exception_handler*);

void * createNetwork(graal_isolatethread_t*, char*, char*, exception_handler*);

network_metadata* getNetworkMetadata(graal_isolatethread_t*, void *, exception_handler*);

void freeNetworkMetadata(graal_isolatethread_t*, network_metadata*, exception_handler*);

void * loadNetwork(graal_isolatethread_t*, char*, char**, int, char**, int, void *, exception_handler*);

void * loadNetworkFromString(graal_isolatethread_t*, char*, char*, char**, int, char**, int, void *, exception_handler*);

void * loadNetworkFromBinaryBuffers(graal_isolatethread_t*, char**, int*, int, char**, int, char**, int, void *, exception_handler*);

void saveNetwork(graal_isolatethread_t*, void *, char*, char*, char**, int, char**, int, void *, exception_handler*);

char* saveNetworkToString(graal_isolatethread_t*, void *, char*, char**, int, char**, int, void *, exception_handler*);

array* saveNetworkToBinaryBuffer(graal_isolatethread_t*, void *, char*, char**, int, char**, int, void *, exception_handler*);

void freeNetworkBinaryBuffer(graal_isolatethread_t*, array*, exception_handler*);

void reduceNetwork(graal_isolatethread_t*, void *, double, double, char**, int, char**, int, int*, int, int, exception_handler*);

array* getNetworkElementsIds(graal_isolatethread_t*, void *, element_type, double*, int, char**, int, int, int, int, exception_handler*);

void cloneVariant(graal_isolatethread_t*, void *, char*, char*, int, exception_handler*);

void setWorkingVariant(graal_isolatethread_t*, void *, char*, exception_handler*);

void removeVariant(graal_isolatethread_t*, void *, char*, exception_handler*);

array* getVariantsIds(graal_isolatethread_t*, void *, exception_handler*);

array* createNetworkElementsSeriesArray(graal_isolatethread_t*, void *, element_type, filter_attributes_type, char**, int, dataframe*, int, double, exception_handler*);

array* createNetworkElementsExtensionSeriesArray(graal_isolatethread_t*, void *, char*, char*, exception_handler*);

array* getExtensionsNames(graal_isolatethread_t*, exception_handler*);

array* getExtensionsInformation(graal_isolatethread_t*, exception_handler*);

void createElement(graal_isolatethread_t*, void *, element_type, dataframe_array*, exception_handler*);

void updateNetworkElementsWithSeries(graal_isolatethread_t*, void *, element_type, dataframe*, int, double, exception_handler*);

void removeAliases(graal_isolatethread_t*, void *, dataframe*, exception_handler*);

void removeNetworkElements(graal_isolatethread_t*, void *, char**, int, exception_handler*);

array* getNodeBreakerViewSwitches(graal_isolatethread_t*, void *, char*, exception_handler*);

array* getNodeBreakerViewNodes(graal_isolatethread_t*, void *, char*, exception_handler*);

array* getNodeBreakerViewInternalConnections(graal_isolatethread_t*, void *, char*, exception_handler*);

array* getBusBreakerViewSwitches(graal_isolatethread_t*, void *, char*, exception_handler*);

array* getBusBreakerViewBuses(graal_isolatethread_t*, void *, char*, exception_handler*);

array* getBusBreakerViewElements(graal_isolatethread_t*, void *, char*, exception_handler*);

void * merge(graal_isolatethread_t*, void**, int, exception_handler*);

dataframe_metadata* getSeriesMetadata(graal_isolatethread_t*, element_type, exception_handler*);

void freeDataframeMetadata(graal_isolatethread_t*, dataframe_metadata*, exception_handler*);

dataframes_metadata* getCreationMetadata(graal_isolatethread_t*, element_type, exception_handler*);

void freeDataframesMetadata(graal_isolatethread_t*, dataframes_metadata*, exception_handler*);

void addNetworkElementProperties(graal_isolatethread_t*, void *, dataframe*, exception_handler*);

void removeNetworkElementProperties(graal_isolatethread_t*, void *, char**, int, char**, int, exception_handler*);

void updateNetworkElementsExtensionsWithSeries(graal_isolatethread_t*, void *, char*, char*, dataframe*, exception_handler*);

void removeExtensions(graal_isolatethread_t*, void *, char*, char**, int, exception_handler*);

dataframe_metadata* getExtensionSeriesMetadata(graal_isolatethread_t*, char*, char*, exception_handler*);

void createExtensions(graal_isolatethread_t*, void *, char*, dataframe_array*, exception_handler*);

dataframes_metadata* getExtensionsCreationMetadata(graal_isolatethread_t*, char*, exception_handler*);

array* createImporterParametersSeriesArray(graal_isolatethread_t*, char*, exception_handler*);

array* createExporterParametersSeriesArray(graal_isolatethread_t*, char*, exception_handler*);

int updateSwitchPosition(graal_isolatethread_t*, void *, char*, int, exception_handler*);

int updateConnectableStatus(graal_isolatethread_t*, void *, char*, int, exception_handler*);

sld_parameters* createSldParameters(graal_isolatethread_t*, exception_handler*);

nad_parameters* createNadParameters(graal_isolatethread_t*, exception_handler*);

void freeSldParameters(graal_isolatethread_t*, sld_parameters*, exception_handler*);

void freeNadParameters(graal_isolatethread_t*, nad_parameters*, exception_handler*);

void writeSingleLineDiagramSvg(graal_isolatethread_t*, void *, char*, char*, char*, sld_parameters*, exception_handler*);

void writeMatrixMultiSubstationSingleLineDiagramSvg(graal_isolatethread_t*, void *, char**, int, int, char*, char*, sld_parameters*, exception_handler*);

char* getSingleLineDiagramSvg(graal_isolatethread_t*, void *, char*, exception_handler*);

array* getSingleLineDiagramSvgAndMetadata(graal_isolatethread_t*, void *, char*, sld_parameters*, exception_handler*);

array* getMatrixMultiSubstationSvgAndMetadata(graal_isolatethread_t*, void *, char**, int, int, sld_parameters*, exception_handler*);

array* getSingleLineDiagramComponentLibraryNames(graal_isolatethread_t*, exception_handler*);

void writeNetworkAreaDiagramSvg(graal_isolatethread_t*, void *, char*, char**, int, int, double, double, nad_parameters*, exception_handler*);

char* getNetworkAreaDiagramSvg(graal_isolatethread_t*, void *, char**, int, int, double, double, nad_parameters*, exception_handler*);

array* getNetworkAreaDiagramDisplayedVoltageLevels(graal_isolatethread_t*, void *, char**, int, int, exception_handler*);

validation_level_type getValidationLevel(graal_isolatethread_t*, void *, exception_handler*);

validation_level_type validate(graal_isolatethread_t*, void *, exception_handler*);

void setMinValidationLevel(graal_isolatethread_t*, void *, validation_level_type, exception_handler*);

dataframes_metadata* getModificationMetadataWithElementType(graal_isolatethread_t*, network_modification_type, element_type, exception_handler*);

void * getSubNetwork(graal_isolatethread_t*, void *, char*, exception_handler*);

void * detachSubNetwork(graal_isolatethread_t*, void *, exception_handler*);

array* getConnectablesOrderPositions(graal_isolatethread_t*, void *, char*, exception_handler*);

array* getUnusedConnectableOrderPositions(graal_isolatethread_t*, void *, char*, char*, exception_handler*);

void createNetworkModification(graal_isolatethread_t*, void *, dataframe_array*, network_modification_type, int, void *, exception_handler*);

dataframe_metadata* getModificationMetadata(graal_isolatethread_t*, network_modification_type, exception_handler*);

void removeElementsModification(graal_isolatethread_t*, void *, char**, int, dataframe*, remove_modification_type, int, void *, exception_handler*);

void * createReportNode(graal_isolatethread_t*, char*, char*, exception_handler*);

char* printReport(graal_isolatethread_t*, void *, exception_handler*);

char* jsonReport(graal_isolatethread_t*, void *, exception_handler*);

array* getSecurityAnalysisProviderNames(graal_isolatethread_t*, exception_handler*);

void setDefaultSecurityAnalysisProvider(graal_isolatethread_t*, char*, exception_handler*);

char* getDefaultSecurityAnalysisProvider(graal_isolatethread_t*, exception_handler*);

void addMonitoredElements(graal_isolatethread_t*, void *, contingency_context_type, char**, int, char**, int, char**, int, char**, int, exception_handler*);

array* getBranchResults(graal_isolatethread_t*, void *, exception_handler*);

array* getBusResults(graal_isolatethread_t*, void *, exception_handler*);

array* getThreeWindingsTransformerResults(graal_isolatethread_t*, void *, exception_handler*);

void * createSecurityAnalysis(graal_isolatethread_t*, exception_handler*);

void addContingency(graal_isolatethread_t*, void *, char*, char**, int, exception_handler*);

void * runSecurityAnalysis(graal_isolatethread_t*, void *, void *, security_analysis_parameters*, char*, int, void *, exception_handler*);

array* getPostContingencyResults(graal_isolatethread_t*, void *, exception_handler*);

array* getOperatorStrategyResults(graal_isolatethread_t*, void *, exception_handler*);

pre_contingency_result* getPreContingencyResult(graal_isolatethread_t*, void *, exception_handler*);

array* getLimitViolations(graal_isolatethread_t*, void *, exception_handler*);

void freeContingencyResultArrayPointer(graal_isolatethread_t*, array*, exception_handler*);

void freeOperatorStrategyResultArrayPointer(graal_isolatethread_t*, array*, exception_handler*);

void freeSecurityAnalysisParameters(graal_isolatethread_t*, security_analysis_parameters*, exception_handler*);

security_analysis_parameters* createSecurityAnalysisParameters(graal_isolatethread_t*, exception_handler*);

array* getSecurityAnalysisProviderParametersNames(graal_isolatethread_t*, char*, exception_handler*);

void addLoadActivePowerAction(graal_isolatethread_t*, void *, char*, char*, int, double, exception_handler*);

void addLoadReactivePowerAction(graal_isolatethread_t*, void *, char*, char*, int, double, exception_handler*);

void addGeneratorActivePowerAction(graal_isolatethread_t*, void *, char*, char*, int, double, exception_handler*);

void addSwitchAction(graal_isolatethread_t*, void *, char*, char*, int, exception_handler*);

void addPhaseTapChangerPositionAction(graal_isolatethread_t*, void *, char*, char*, int, int, ThreeSide, exception_handler*);

void addRatioTapChangerPositionAction(graal_isolatethread_t*, void *, char*, char*, int, int, ThreeSide, exception_handler*);

void addShuntCompensatorPositionAction(graal_isolatethread_t*, void *, char*, char*, int, exception_handler*);

void addOperatorStrategy(graal_isolatethread_t*, void *, char*, char*, char**, int, condition_type, char**, int, int*, int, exception_handler*);

array* getSensitivityAnalysisProviderNames(graal_isolatethread_t*, exception_handler*);

void setDefaultSensitivityAnalysisProvider(graal_isolatethread_t*, char*, exception_handler*);

char* getDefaultSensitivityAnalysisProvider(graal_isolatethread_t*, exception_handler*);

void * createSensitivityAnalysis(graal_isolatethread_t*, exception_handler*);

void setZones(graal_isolatethread_t*, void *, zone**, int, exception_handler*);

void addFactorMatrix(graal_isolatethread_t*, void *, char**, int, char**, int, char**, int, char*, contingency_context_type, sensitivity_function_type, sensitivity_variable_type, exception_handler*);

void * runSensitivityAnalysis(graal_isolatethread_t*, void *, void *, int, sensitivity_analysis_parameters*, char*, void *, exception_handler*);

matrix* getSensitivityMatrix(graal_isolatethread_t*, void *, char*, char*, exception_handler*);

matrix* getReferenceMatrix(graal_isolatethread_t*, void *, char*, char*, exception_handler*);

void freeSensitivityAnalysisParameters(graal_isolatethread_t*, sensitivity_analysis_parameters*, exception_handler*);

sensitivity_analysis_parameters* createSensitivityAnalysisParameters(graal_isolatethread_t*, exception_handler*);

array* getSensitivityAnalysisProviderParametersNames(graal_isolatethread_t*, char*, exception_handler*);

array* getShortCircuitAnalysisProviderNames(graal_isolatethread_t*, exception_handler*);

void setDefaultShortCircuitAnalysisProvider(graal_isolatethread_t*, char*, exception_handler*);

char* getDefaultShortCircuitAnalysisProvider(graal_isolatethread_t*, exception_handler*);

void * createShortCircuitAnalysis(graal_isolatethread_t*, exception_handler*);

void * runShortCircuitAnalysis(graal_isolatethread_t*, void *, void *, shortcircuit_analysis_parameters*, char*, void *, exception_handler*);

void freeShortCircuitAnalysisParameters(graal_isolatethread_t*, shortcircuit_analysis_parameters*, exception_handler*);

shortcircuit_analysis_parameters* createShortCircuitAnalysisParameters(graal_isolatethread_t*, exception_handler*);

array* getShortCircuitAnalysisProviderParametersNames(graal_isolatethread_t*, char*, exception_handler*);

dataframe_metadata* getFaultsDataframeMetaData(graal_isolatethread_t*, exception_handler*);

void setFaults(graal_isolatethread_t*, void *, dataframe*, exception_handler*);

array* getShortCircuitAnalysisFaultResults(graal_isolatethread_t*, void *, int, exception_handler*);

array* getShortCircuitAnalysisFeederResults(graal_isolatethread_t*, void *, int, exception_handler*);

array* getShortCircuitAnalysisLimitViolationsResults(graal_isolatethread_t*, void *, exception_handler*);

array* getShortCircuitAnalysisBusResults(graal_isolatethread_t*, void *, int, exception_handler*);

void * createVoltageInitializerParams(graal_isolatethread_t*, exception_handler*);

void voltageInitializerAddSpecificLowVoltageLimits(graal_isolatethread_t*, void *, char*, int, double, exception_handler*);

void voltageInitializerAddSpecificHighVoltageLimits(graal_isolatethread_t*, void *, char*, int, double, exception_handler*);

void voltageInitializerAddVariableShuntCompensators(graal_isolatethread_t*, void *, char*, exception_handler*);

void voltageInitializerAddConstantQGenerators(graal_isolatethread_t*, void *, char*, exception_handler*);

void voltageInitializerAddVariableTwoWindingsTransformers(graal_isolatethread_t*, void *, char*, exception_handler*);

void voltageInitializerAddConfiguredReactiveSlackBuses(graal_isolatethread_t*, void *, char*, exception_handler*);

void voltageInitializerSetObjective(graal_isolatethread_t*, void *, VoltageInitializerObjective, exception_handler*);

void voltageInitializerSetObjectiveDistance(graal_isolatethread_t*, void *, double, exception_handler*);

void voltageInitializerSetLogLevelAmpl(graal_isolatethread_t*, void *, VoltageInitializerLogLevelAmpl, exception_handler*);

void voltageInitializerSetLogLevelSolver(graal_isolatethread_t*, void *, VoltageInitializerLogLevelSolver, exception_handler*);

void voltageInitializerSetReactiveSlackBusesMode(graal_isolatethread_t*, void *, VoltageInitializerReactiveSlackBusesMode, exception_handler*);

void voltageInitializerSetMinPlausibleLowVoltageLimit(graal_isolatethread_t*, void *, double, exception_handler*);

void voltageInitializerSetMaxPlausibleHighVoltageLimit(graal_isolatethread_t*, void *, double, exception_handler*);

void voltageInitializerSetActivePowerVariationRate(graal_isolatethread_t*, void *, double, exception_handler*);

void voltageInitializerSetMinPlausibleActivePowerThreshold(graal_isolatethread_t*, void *, double, exception_handler*);

void voltageInitializerSetLowImpedanceThreshold(graal_isolatethread_t*, void *, double, exception_handler*);

void voltageInitializerSetMinNominalVoltageIgnoredBus(graal_isolatethread_t*, void *, double, exception_handler*);

void voltageInitializerSetMinNominalVoltageIgnoredVoltageBounds(graal_isolatethread_t*, void *, double, exception_handler*);

void voltageInitializerSetMaxPlausiblePowerLimit(graal_isolatethread_t*, void *, double, exception_handler*);

void voltageInitializerSetDefaultMinimalQPRange(graal_isolatethread_t*, void *, double, exception_handler*);

void voltageInitializerSetHighActivePowerDefaultLimit(graal_isolatethread_t*, void *, double, exception_handler*);

void voltageInitializerSetLowActivePowerDefaultLimit(graal_isolatethread_t*, void *, double, exception_handler*);

void voltageInitializerSetDefaultQmaxPmaxRatio(graal_isolatethread_t*, void *, double, exception_handler*);

void voltageInitializerSetDefaultVariableScalingFactor(graal_isolatethread_t*, void *, double, exception_handler*);

void voltageInitializerSetDefaultConstraintScalingFactor(graal_isolatethread_t*, void *, double, exception_handler*);

void voltageInitializerSetReactiveSlackVariableScalingFactor(graal_isolatethread_t*, void *, double, exception_handler*);

void voltageInitializerSetTwoWindingTransformerRatioVariableScalingFactor(graal_isolatethread_t*, void *, double, exception_handler*);

void voltageInitializerApplyAllModifications(graal_isolatethread_t*, void *, void *, exception_handler*);

VoltageInitializerStatus voltageInitializerGetStatus(graal_isolatethread_t*, void *, exception_handler*);

string_map* voltageInitializerGetIndicators(graal_isolatethread_t*, void *, exception_handler*);

void * runVoltageInitializer(graal_isolatethread_t*, int, void *, void *, exception_handler*);

#if defined(__cplusplus)
}
#endif
#endif
