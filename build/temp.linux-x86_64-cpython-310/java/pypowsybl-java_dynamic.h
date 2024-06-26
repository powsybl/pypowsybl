#ifndef __PYPOWSYBL_JAVA_H
#define __PYPOWSYBL_JAVA_H

#include <graal_isolate_dynamic.h>


#if defined(__cplusplus)
extern "C" {
#endif

typedef void (*setJavaLibraryPath_fn_t)(graal_isolatethread_t*, char*, exception_handler*);

typedef void (*setConfigRead_fn_t)(graal_isolatethread_t*, int, exception_handler*);

typedef int (*isConfigRead_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef char* (*getVersionTable_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef void (*freeStringArray_fn_t)(graal_isolatethread_t*, array*, exception_handler*);

typedef void (*freeArray_fn_t)(graal_isolatethread_t*, array*, exception_handler*);

typedef void (*freeSeriesArray_fn_t)(graal_isolatethread_t*, array*, exception_handler*);

typedef void (*destroyObjectHandle_fn_t)(graal_isolatethread_t*, void *, exception_handler*);

typedef char* (*getWorkingVariantId_fn_t)(graal_isolatethread_t*, void *, exception_handler*);

typedef void (*freeString_fn_t)(graal_isolatethread_t*, char*, exception_handler*);

typedef void (*closePypowsybl_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef void (*freeStringMap_fn_t)(graal_isolatethread_t*, string_map*, exception_handler*);

typedef void * (*createDynamicSimulationContext_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef void * (*createDynamicModelMapping_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef void * (*createTimeseriesMapping_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef void * (*createEventMapping_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef void * (*runDynamicModel_fn_t)(graal_isolatethread_t*, void *, void *, void *, void *, void *, int, int, exception_handler*);

typedef void (*addDynamicMappings_fn_t)(graal_isolatethread_t*, void *, DynamicMappingType, dataframe*, exception_handler*);

typedef dataframe_metadata* (*getDynamicMappingsMetaData_fn_t)(graal_isolatethread_t*, DynamicMappingType, exception_handler*);

typedef void (*addCurve_fn_t)(graal_isolatethread_t*, void *, char*, char*, exception_handler*);

typedef void (*addEventDisconnection_fn_t)(graal_isolatethread_t*, void *, char*, double, int, exception_handler*);

typedef char* (*getDynamicSimulationResultsStatus_fn_t)(graal_isolatethread_t*, void *, exception_handler*);

typedef array* (*getDynamicCurve_fn_t)(graal_isolatethread_t*, void *, char*, exception_handler*);

typedef array* (*getAllDynamicCurvesIds_fn_t)(graal_isolatethread_t*, void *, exception_handler*);

typedef void * (*createFlowDecomposition_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef void (*addContingencyForFlowDecomposition_fn_t)(graal_isolatethread_t*, void *, char*, char**, int, exception_handler*);

typedef void (*addPrecontingencyMonitoredElementsForFlowDecomposition_fn_t)(graal_isolatethread_t*, void *, char**, int, exception_handler*);

typedef void (*addPostcontingencyMonitoredElementsForFlowDecomposition_fn_t)(graal_isolatethread_t*, void *, char**, int, char**, int, exception_handler*);

typedef void (*addAdditionalXnecProviderForFlowDecomposition_fn_t)(graal_isolatethread_t*, void *, int, exception_handler*);

typedef array* (*runFlowDecomposition_fn_t)(graal_isolatethread_t*, void *, void *, flow_decomposition_parameters*, loadflow_parameters*, exception_handler*);

typedef flow_decomposition_parameters* (*createFlowDecompositionParameters_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef void (*freeFlowDecompositionParameters_fn_t)(graal_isolatethread_t*, flow_decomposition_parameters*, exception_handler*);

typedef void * (*createGLSKdocument_fn_t)(graal_isolatethread_t*, char*, exception_handler*);

typedef array* (*getGLSKinjectionkeys_fn_t)(graal_isolatethread_t*, void *, void *, char*, long long int, exception_handler*);

typedef array* (*getGLSKcountries_fn_t)(graal_isolatethread_t*, void *, exception_handler*);

typedef array* (*getInjectionFactor_fn_t)(graal_isolatethread_t*, void *, void *, char*, long long int, exception_handler*);

typedef long long int (*getInjectionFactorStartTimestamp_fn_t)(graal_isolatethread_t*, void *, exception_handler*);

typedef long long int (*getInjectionFactorEndTimestamp_fn_t)(graal_isolatethread_t*, void *, exception_handler*);

typedef void (*setDefaultLoadFlowProvider_fn_t)(graal_isolatethread_t*, char*, exception_handler*);

typedef char* (*getDefaultLoadFlowProvider_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef void (*freeLoadFlowComponentResultPointer_fn_t)(graal_isolatethread_t*, array*, exception_handler*);

typedef array* (*getLoadFlowProviderNames_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef array* (*runLoadFlow_fn_t)(graal_isolatethread_t*, void *, int, loadflow_parameters*, char*, void *, exception_handler*);

typedef loadflow_parameters* (*createLoadFlowParameters_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef void (*freeLoadFlowParameters_fn_t)(graal_isolatethread_t*, loadflow_parameters*, exception_handler*);

typedef array* (*getLoadFlowProviderParametersNames_fn_t)(graal_isolatethread_t*, char*, exception_handler*);

typedef array* (*createLoadFlowProviderParametersSeriesArray_fn_t)(graal_isolatethread_t*, char*, exception_handler*);

typedef array* (*runLoadFlowValidation_fn_t)(graal_isolatethread_t*, void *, validation_type, loadflow_validation_parameters*, exception_handler*);

typedef loadflow_validation_parameters* (*createValidationConfig_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef void (*freeValidationConfig_fn_t)(graal_isolatethread_t*, loadflow_validation_parameters*, exception_handler*);

typedef void (*setupLoggerCallback_fn_t)(graal_isolatethread_t*, void *, exception_handler*);

typedef void (*setLogLevel_fn_t)(graal_isolatethread_t*, int, exception_handler*);

typedef array* (*getNetworkImportFormats_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef array* (*getNetworkExportFormats_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef void * (*createNetwork_fn_t)(graal_isolatethread_t*, char*, char*, exception_handler*);

typedef network_metadata* (*getNetworkMetadata_fn_t)(graal_isolatethread_t*, void *, exception_handler*);

typedef void (*freeNetworkMetadata_fn_t)(graal_isolatethread_t*, network_metadata*, exception_handler*);

typedef void * (*loadNetwork_fn_t)(graal_isolatethread_t*, char*, char**, int, char**, int, void *, exception_handler*);

typedef void * (*loadNetworkFromString_fn_t)(graal_isolatethread_t*, char*, char*, char**, int, char**, int, void *, exception_handler*);

typedef void * (*loadNetworkFromBinaryBuffers_fn_t)(graal_isolatethread_t*, char**, int*, int, char**, int, char**, int, void *, exception_handler*);

typedef void (*saveNetwork_fn_t)(graal_isolatethread_t*, void *, char*, char*, char**, int, char**, int, void *, exception_handler*);

typedef char* (*saveNetworkToString_fn_t)(graal_isolatethread_t*, void *, char*, char**, int, char**, int, void *, exception_handler*);

typedef array* (*saveNetworkToBinaryBuffer_fn_t)(graal_isolatethread_t*, void *, char*, char**, int, char**, int, void *, exception_handler*);

typedef void (*freeNetworkBinaryBuffer_fn_t)(graal_isolatethread_t*, array*, exception_handler*);

typedef void (*reduceNetwork_fn_t)(graal_isolatethread_t*, void *, double, double, char**, int, char**, int, int*, int, int, exception_handler*);

typedef array* (*getNetworkElementsIds_fn_t)(graal_isolatethread_t*, void *, element_type, double*, int, char**, int, int, int, int, exception_handler*);

typedef void (*cloneVariant_fn_t)(graal_isolatethread_t*, void *, char*, char*, int, exception_handler*);

typedef void (*setWorkingVariant_fn_t)(graal_isolatethread_t*, void *, char*, exception_handler*);

typedef void (*removeVariant_fn_t)(graal_isolatethread_t*, void *, char*, exception_handler*);

typedef array* (*getVariantsIds_fn_t)(graal_isolatethread_t*, void *, exception_handler*);

typedef array* (*createNetworkElementsSeriesArray_fn_t)(graal_isolatethread_t*, void *, element_type, filter_attributes_type, char**, int, dataframe*, int, double, exception_handler*);

typedef array* (*createNetworkElementsExtensionSeriesArray_fn_t)(graal_isolatethread_t*, void *, char*, char*, exception_handler*);

typedef array* (*getExtensionsNames_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef array* (*getExtensionsInformation_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef void (*createElement_fn_t)(graal_isolatethread_t*, void *, element_type, dataframe_array*, exception_handler*);

typedef void (*updateNetworkElementsWithSeries_fn_t)(graal_isolatethread_t*, void *, element_type, dataframe*, int, double, exception_handler*);

typedef void (*removeAliases_fn_t)(graal_isolatethread_t*, void *, dataframe*, exception_handler*);

typedef void (*removeNetworkElements_fn_t)(graal_isolatethread_t*, void *, char**, int, exception_handler*);

typedef array* (*getNodeBreakerViewSwitches_fn_t)(graal_isolatethread_t*, void *, char*, exception_handler*);

typedef array* (*getNodeBreakerViewNodes_fn_t)(graal_isolatethread_t*, void *, char*, exception_handler*);

typedef array* (*getNodeBreakerViewInternalConnections_fn_t)(graal_isolatethread_t*, void *, char*, exception_handler*);

typedef array* (*getBusBreakerViewSwitches_fn_t)(graal_isolatethread_t*, void *, char*, exception_handler*);

typedef array* (*getBusBreakerViewBuses_fn_t)(graal_isolatethread_t*, void *, char*, exception_handler*);

typedef array* (*getBusBreakerViewElements_fn_t)(graal_isolatethread_t*, void *, char*, exception_handler*);

typedef void * (*merge_fn_t)(graal_isolatethread_t*, void**, int, exception_handler*);

typedef dataframe_metadata* (*getSeriesMetadata_fn_t)(graal_isolatethread_t*, element_type, exception_handler*);

typedef void (*freeDataframeMetadata_fn_t)(graal_isolatethread_t*, dataframe_metadata*, exception_handler*);

typedef dataframes_metadata* (*getCreationMetadata_fn_t)(graal_isolatethread_t*, element_type, exception_handler*);

typedef void (*freeDataframesMetadata_fn_t)(graal_isolatethread_t*, dataframes_metadata*, exception_handler*);

typedef void (*addNetworkElementProperties_fn_t)(graal_isolatethread_t*, void *, dataframe*, exception_handler*);

typedef void (*removeNetworkElementProperties_fn_t)(graal_isolatethread_t*, void *, char**, int, char**, int, exception_handler*);

typedef void (*updateNetworkElementsExtensionsWithSeries_fn_t)(graal_isolatethread_t*, void *, char*, char*, dataframe*, exception_handler*);

typedef void (*removeExtensions_fn_t)(graal_isolatethread_t*, void *, char*, char**, int, exception_handler*);

typedef dataframe_metadata* (*getExtensionSeriesMetadata_fn_t)(graal_isolatethread_t*, char*, char*, exception_handler*);

typedef void (*createExtensions_fn_t)(graal_isolatethread_t*, void *, char*, dataframe_array*, exception_handler*);

typedef dataframes_metadata* (*getExtensionsCreationMetadata_fn_t)(graal_isolatethread_t*, char*, exception_handler*);

typedef array* (*createImporterParametersSeriesArray_fn_t)(graal_isolatethread_t*, char*, exception_handler*);

typedef array* (*createExporterParametersSeriesArray_fn_t)(graal_isolatethread_t*, char*, exception_handler*);

typedef int (*updateSwitchPosition_fn_t)(graal_isolatethread_t*, void *, char*, int, exception_handler*);

typedef int (*updateConnectableStatus_fn_t)(graal_isolatethread_t*, void *, char*, int, exception_handler*);

typedef sld_parameters* (*createSldParameters_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef nad_parameters* (*createNadParameters_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef void (*freeSldParameters_fn_t)(graal_isolatethread_t*, sld_parameters*, exception_handler*);

typedef void (*freeNadParameters_fn_t)(graal_isolatethread_t*, nad_parameters*, exception_handler*);

typedef void (*writeSingleLineDiagramSvg_fn_t)(graal_isolatethread_t*, void *, char*, char*, char*, sld_parameters*, exception_handler*);

typedef void (*writeMatrixMultiSubstationSingleLineDiagramSvg_fn_t)(graal_isolatethread_t*, void *, char**, int, int, char*, char*, sld_parameters*, exception_handler*);

typedef char* (*getSingleLineDiagramSvg_fn_t)(graal_isolatethread_t*, void *, char*, exception_handler*);

typedef array* (*getSingleLineDiagramSvgAndMetadata_fn_t)(graal_isolatethread_t*, void *, char*, sld_parameters*, exception_handler*);

typedef array* (*getMatrixMultiSubstationSvgAndMetadata_fn_t)(graal_isolatethread_t*, void *, char**, int, int, sld_parameters*, exception_handler*);

typedef array* (*getSingleLineDiagramComponentLibraryNames_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef void (*writeNetworkAreaDiagramSvg_fn_t)(graal_isolatethread_t*, void *, char*, char**, int, int, double, double, nad_parameters*, exception_handler*);

typedef char* (*getNetworkAreaDiagramSvg_fn_t)(graal_isolatethread_t*, void *, char**, int, int, double, double, nad_parameters*, exception_handler*);

typedef array* (*getNetworkAreaDiagramDisplayedVoltageLevels_fn_t)(graal_isolatethread_t*, void *, char**, int, int, exception_handler*);

typedef validation_level_type (*getValidationLevel_fn_t)(graal_isolatethread_t*, void *, exception_handler*);

typedef validation_level_type (*validate_fn_t)(graal_isolatethread_t*, void *, exception_handler*);

typedef void (*setMinValidationLevel_fn_t)(graal_isolatethread_t*, void *, validation_level_type, exception_handler*);

typedef dataframes_metadata* (*getModificationMetadataWithElementType_fn_t)(graal_isolatethread_t*, network_modification_type, element_type, exception_handler*);

typedef void * (*getSubNetwork_fn_t)(graal_isolatethread_t*, void *, char*, exception_handler*);

typedef void * (*detachSubNetwork_fn_t)(graal_isolatethread_t*, void *, exception_handler*);

typedef array* (*getConnectablesOrderPositions_fn_t)(graal_isolatethread_t*, void *, char*, exception_handler*);

typedef array* (*getUnusedConnectableOrderPositions_fn_t)(graal_isolatethread_t*, void *, char*, char*, exception_handler*);

typedef void (*createNetworkModification_fn_t)(graal_isolatethread_t*, void *, dataframe_array*, network_modification_type, int, void *, exception_handler*);

typedef dataframe_metadata* (*getModificationMetadata_fn_t)(graal_isolatethread_t*, network_modification_type, exception_handler*);

typedef void (*removeElementsModification_fn_t)(graal_isolatethread_t*, void *, char**, int, dataframe*, remove_modification_type, int, void *, exception_handler*);

typedef void * (*createReportNode_fn_t)(graal_isolatethread_t*, char*, char*, exception_handler*);

typedef char* (*printReport_fn_t)(graal_isolatethread_t*, void *, exception_handler*);

typedef char* (*jsonReport_fn_t)(graal_isolatethread_t*, void *, exception_handler*);

typedef array* (*getSecurityAnalysisProviderNames_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef void (*setDefaultSecurityAnalysisProvider_fn_t)(graal_isolatethread_t*, char*, exception_handler*);

typedef char* (*getDefaultSecurityAnalysisProvider_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef void (*addMonitoredElements_fn_t)(graal_isolatethread_t*, void *, contingency_context_type, char**, int, char**, int, char**, int, char**, int, exception_handler*);

typedef array* (*getBranchResults_fn_t)(graal_isolatethread_t*, void *, exception_handler*);

typedef array* (*getBusResults_fn_t)(graal_isolatethread_t*, void *, exception_handler*);

typedef array* (*getThreeWindingsTransformerResults_fn_t)(graal_isolatethread_t*, void *, exception_handler*);

typedef void * (*createSecurityAnalysis_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef void (*addContingency_fn_t)(graal_isolatethread_t*, void *, char*, char**, int, exception_handler*);

typedef void * (*runSecurityAnalysis_fn_t)(graal_isolatethread_t*, void *, void *, security_analysis_parameters*, char*, int, void *, exception_handler*);

typedef array* (*getPostContingencyResults_fn_t)(graal_isolatethread_t*, void *, exception_handler*);

typedef array* (*getOperatorStrategyResults_fn_t)(graal_isolatethread_t*, void *, exception_handler*);

typedef pre_contingency_result* (*getPreContingencyResult_fn_t)(graal_isolatethread_t*, void *, exception_handler*);

typedef array* (*getLimitViolations_fn_t)(graal_isolatethread_t*, void *, exception_handler*);

typedef void (*freeContingencyResultArrayPointer_fn_t)(graal_isolatethread_t*, array*, exception_handler*);

typedef void (*freeOperatorStrategyResultArrayPointer_fn_t)(graal_isolatethread_t*, array*, exception_handler*);

typedef void (*freeSecurityAnalysisParameters_fn_t)(graal_isolatethread_t*, security_analysis_parameters*, exception_handler*);

typedef security_analysis_parameters* (*createSecurityAnalysisParameters_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef array* (*getSecurityAnalysisProviderParametersNames_fn_t)(graal_isolatethread_t*, char*, exception_handler*);

typedef void (*addLoadActivePowerAction_fn_t)(graal_isolatethread_t*, void *, char*, char*, int, double, exception_handler*);

typedef void (*addLoadReactivePowerAction_fn_t)(graal_isolatethread_t*, void *, char*, char*, int, double, exception_handler*);

typedef void (*addGeneratorActivePowerAction_fn_t)(graal_isolatethread_t*, void *, char*, char*, int, double, exception_handler*);

typedef void (*addSwitchAction_fn_t)(graal_isolatethread_t*, void *, char*, char*, int, exception_handler*);

typedef void (*addPhaseTapChangerPositionAction_fn_t)(graal_isolatethread_t*, void *, char*, char*, int, int, ThreeSide, exception_handler*);

typedef void (*addRatioTapChangerPositionAction_fn_t)(graal_isolatethread_t*, void *, char*, char*, int, int, ThreeSide, exception_handler*);

typedef void (*addShuntCompensatorPositionAction_fn_t)(graal_isolatethread_t*, void *, char*, char*, int, exception_handler*);

typedef void (*addOperatorStrategy_fn_t)(graal_isolatethread_t*, void *, char*, char*, char**, int, condition_type, char**, int, int*, int, exception_handler*);

typedef array* (*getSensitivityAnalysisProviderNames_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef void (*setDefaultSensitivityAnalysisProvider_fn_t)(graal_isolatethread_t*, char*, exception_handler*);

typedef char* (*getDefaultSensitivityAnalysisProvider_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef void * (*createSensitivityAnalysis_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef void (*setZones_fn_t)(graal_isolatethread_t*, void *, zone**, int, exception_handler*);

typedef void (*addFactorMatrix_fn_t)(graal_isolatethread_t*, void *, char**, int, char**, int, char**, int, char*, contingency_context_type, sensitivity_function_type, sensitivity_variable_type, exception_handler*);

typedef void * (*runSensitivityAnalysis_fn_t)(graal_isolatethread_t*, void *, void *, int, sensitivity_analysis_parameters*, char*, void *, exception_handler*);

typedef matrix* (*getSensitivityMatrix_fn_t)(graal_isolatethread_t*, void *, char*, char*, exception_handler*);

typedef matrix* (*getReferenceMatrix_fn_t)(graal_isolatethread_t*, void *, char*, char*, exception_handler*);

typedef void (*freeSensitivityAnalysisParameters_fn_t)(graal_isolatethread_t*, sensitivity_analysis_parameters*, exception_handler*);

typedef sensitivity_analysis_parameters* (*createSensitivityAnalysisParameters_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef array* (*getSensitivityAnalysisProviderParametersNames_fn_t)(graal_isolatethread_t*, char*, exception_handler*);

typedef array* (*getShortCircuitAnalysisProviderNames_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef void (*setDefaultShortCircuitAnalysisProvider_fn_t)(graal_isolatethread_t*, char*, exception_handler*);

typedef char* (*getDefaultShortCircuitAnalysisProvider_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef void * (*createShortCircuitAnalysis_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef void * (*runShortCircuitAnalysis_fn_t)(graal_isolatethread_t*, void *, void *, shortcircuit_analysis_parameters*, char*, void *, exception_handler*);

typedef void (*freeShortCircuitAnalysisParameters_fn_t)(graal_isolatethread_t*, shortcircuit_analysis_parameters*, exception_handler*);

typedef shortcircuit_analysis_parameters* (*createShortCircuitAnalysisParameters_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef array* (*getShortCircuitAnalysisProviderParametersNames_fn_t)(graal_isolatethread_t*, char*, exception_handler*);

typedef dataframe_metadata* (*getFaultsDataframeMetaData_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef void (*setFaults_fn_t)(graal_isolatethread_t*, void *, dataframe*, exception_handler*);

typedef array* (*getShortCircuitAnalysisFaultResults_fn_t)(graal_isolatethread_t*, void *, int, exception_handler*);

typedef array* (*getShortCircuitAnalysisFeederResults_fn_t)(graal_isolatethread_t*, void *, int, exception_handler*);

typedef array* (*getShortCircuitAnalysisLimitViolationsResults_fn_t)(graal_isolatethread_t*, void *, exception_handler*);

typedef array* (*getShortCircuitAnalysisBusResults_fn_t)(graal_isolatethread_t*, void *, int, exception_handler*);

typedef void * (*createVoltageInitializerParams_fn_t)(graal_isolatethread_t*, exception_handler*);

typedef void (*voltageInitializerAddSpecificLowVoltageLimits_fn_t)(graal_isolatethread_t*, void *, char*, int, double, exception_handler*);

typedef void (*voltageInitializerAddSpecificHighVoltageLimits_fn_t)(graal_isolatethread_t*, void *, char*, int, double, exception_handler*);

typedef void (*voltageInitializerAddVariableShuntCompensators_fn_t)(graal_isolatethread_t*, void *, char*, exception_handler*);

typedef void (*voltageInitializerAddConstantQGenerators_fn_t)(graal_isolatethread_t*, void *, char*, exception_handler*);

typedef void (*voltageInitializerAddVariableTwoWindingsTransformers_fn_t)(graal_isolatethread_t*, void *, char*, exception_handler*);

typedef void (*voltageInitializerAddConfiguredReactiveSlackBuses_fn_t)(graal_isolatethread_t*, void *, char*, exception_handler*);

typedef void (*voltageInitializerSetObjective_fn_t)(graal_isolatethread_t*, void *, VoltageInitializerObjective, exception_handler*);

typedef void (*voltageInitializerSetObjectiveDistance_fn_t)(graal_isolatethread_t*, void *, double, exception_handler*);

typedef void (*voltageInitializerSetLogLevelAmpl_fn_t)(graal_isolatethread_t*, void *, VoltageInitializerLogLevelAmpl, exception_handler*);

typedef void (*voltageInitializerSetLogLevelSolver_fn_t)(graal_isolatethread_t*, void *, VoltageInitializerLogLevelSolver, exception_handler*);

typedef void (*voltageInitializerSetReactiveSlackBusesMode_fn_t)(graal_isolatethread_t*, void *, VoltageInitializerReactiveSlackBusesMode, exception_handler*);

typedef void (*voltageInitializerSetMinPlausibleLowVoltageLimit_fn_t)(graal_isolatethread_t*, void *, double, exception_handler*);

typedef void (*voltageInitializerSetMaxPlausibleHighVoltageLimit_fn_t)(graal_isolatethread_t*, void *, double, exception_handler*);

typedef void (*voltageInitializerSetActivePowerVariationRate_fn_t)(graal_isolatethread_t*, void *, double, exception_handler*);

typedef void (*voltageInitializerSetMinPlausibleActivePowerThreshold_fn_t)(graal_isolatethread_t*, void *, double, exception_handler*);

typedef void (*voltageInitializerSetLowImpedanceThreshold_fn_t)(graal_isolatethread_t*, void *, double, exception_handler*);

typedef void (*voltageInitializerSetMinNominalVoltageIgnoredBus_fn_t)(graal_isolatethread_t*, void *, double, exception_handler*);

typedef void (*voltageInitializerSetMinNominalVoltageIgnoredVoltageBounds_fn_t)(graal_isolatethread_t*, void *, double, exception_handler*);

typedef void (*voltageInitializerSetMaxPlausiblePowerLimit_fn_t)(graal_isolatethread_t*, void *, double, exception_handler*);

typedef void (*voltageInitializerSetDefaultMinimalQPRange_fn_t)(graal_isolatethread_t*, void *, double, exception_handler*);

typedef void (*voltageInitializerSetHighActivePowerDefaultLimit_fn_t)(graal_isolatethread_t*, void *, double, exception_handler*);

typedef void (*voltageInitializerSetLowActivePowerDefaultLimit_fn_t)(graal_isolatethread_t*, void *, double, exception_handler*);

typedef void (*voltageInitializerSetDefaultQmaxPmaxRatio_fn_t)(graal_isolatethread_t*, void *, double, exception_handler*);

typedef void (*voltageInitializerSetDefaultVariableScalingFactor_fn_t)(graal_isolatethread_t*, void *, double, exception_handler*);

typedef void (*voltageInitializerSetDefaultConstraintScalingFactor_fn_t)(graal_isolatethread_t*, void *, double, exception_handler*);

typedef void (*voltageInitializerSetReactiveSlackVariableScalingFactor_fn_t)(graal_isolatethread_t*, void *, double, exception_handler*);

typedef void (*voltageInitializerSetTwoWindingTransformerRatioVariableScalingFactor_fn_t)(graal_isolatethread_t*, void *, double, exception_handler*);

typedef void (*voltageInitializerApplyAllModifications_fn_t)(graal_isolatethread_t*, void *, void *, exception_handler*);

typedef VoltageInitializerStatus (*voltageInitializerGetStatus_fn_t)(graal_isolatethread_t*, void *, exception_handler*);

typedef string_map* (*voltageInitializerGetIndicators_fn_t)(graal_isolatethread_t*, void *, exception_handler*);

typedef void * (*runVoltageInitializer_fn_t)(graal_isolatethread_t*, int, void *, void *, exception_handler*);

#if defined(__cplusplus)
}
#endif
#endif
