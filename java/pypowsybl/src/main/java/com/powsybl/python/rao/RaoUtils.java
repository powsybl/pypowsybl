package com.powsybl.python.rao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.PowsyblException;
import com.powsybl.openrao.raoapi.parameters.LoopFlowParameters;
import com.powsybl.openrao.raoapi.parameters.MnecParameters;
import com.powsybl.openrao.raoapi.parameters.RelativeMarginsParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.PtdfApproximation;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoLoopFlowParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoMnecParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRelativeMarginsParameters;

import java.util.*;
import java.util.function.Function;

public final class RaoUtils {

    public static final String MNEC_EXT_PREFIX = "MNEC_EXT_";
    public static final String RELATIVE_MARGIN_EXT_PREFIX = "RELATIVE_MARGIN_EXT_";
    public static final String LOOP_FLOW_EXT_PREFIX = "LOOP_FLOW_EXT_";

    public static final String MNEC_ST_EXT_PREFIX = "MNEC_ST_EXT_";
    public static final String RELATIVE_MARGIN_ST_EXT_PREFIX = "RELATIVE_MARGIN_ST_EXT_";
    public static final String LOOP_FLOW_ST_EXT_PREFIX = "LOOP_FLOW_ST_EXT_";

    private RaoUtils() {
    }

    static MnecParameters buildMnecParametersExtension(Map<String, String> extensionDict) {
        MnecParameters extension = new MnecParameters();
        extractDouble(extensionDict, MNEC_EXT_PREFIX, "acceptable_margin_decrease")
            .ifPresent(extension::setAcceptableMarginDecrease);
        return extension;
    }

    static SearchTreeRaoMnecParameters buildMnecSearchTreeParametersExtension(Map<String, String> extensionDict) {
        SearchTreeRaoMnecParameters extension = new SearchTreeRaoMnecParameters();
        extractDouble(extensionDict, MNEC_ST_EXT_PREFIX, "violation_cost")
            .ifPresent(extension::setViolationCost);
        extractDouble(extensionDict, MNEC_ST_EXT_PREFIX, "constraint_adjustment_coefficient")
            .ifPresent(extension::setConstraintAdjustmentCoefficient);
        return extension;
    }

    static RelativeMarginsParameters buildRelativeMarginsParametersExtension(Map<String, String> extensionDict) {
        RelativeMarginsParameters extension = new RelativeMarginsParameters();
        extractAttribute(extensionDict, RELATIVE_MARGIN_EXT_PREFIX, "ptdf_boundaries", RaoUtils::ptdfBoundariesFromString)
            .ifPresent(extension::setPtdfBoundariesFromString);
        return extension;
    }

    static SearchTreeRaoRelativeMarginsParameters buildRelativeMarginsSearchTreeParametersExtension(Map<String, String> extensionDict) {
        SearchTreeRaoRelativeMarginsParameters extension = new SearchTreeRaoRelativeMarginsParameters();
        extractDouble(extensionDict, RELATIVE_MARGIN_ST_EXT_PREFIX, "ptdf_sum_lower_bounds")
            .ifPresent(extension::setPtdfSumLowerBound);
        extractAttribute(extensionDict, RELATIVE_MARGIN_ST_EXT_PREFIX, "ptdf_approximation", PtdfApproximation::valueOf)
            .ifPresent(extension::setPtdfApproximation);
        return extension;
    }

    static LoopFlowParameters buildLoopFlowParametersExtension(Map<String, String> extensionDict) {
        LoopFlowParameters extension = new LoopFlowParameters();
        extractDouble(extensionDict, LOOP_FLOW_EXT_PREFIX, "acceptable_increase")
            .ifPresent(extension::setAcceptableIncrease);
        extractAttribute(extensionDict, LOOP_FLOW_EXT_PREFIX, "countries", RaoUtils::countryListFromString)
            .ifPresent(extension::setCountries);
        return extension;
    }

    static SearchTreeRaoLoopFlowParameters buildLoopFlowSearchTreeParametersExtension(Map<String, String> extensionDict) {
        SearchTreeRaoLoopFlowParameters extension = new SearchTreeRaoLoopFlowParameters();
        extractAttribute(extensionDict, LOOP_FLOW_ST_EXT_PREFIX, "ptdf_approximation", PtdfApproximation::valueOf)
            .ifPresent(extension::setPtdfApproximation);
        extractDouble(extensionDict, LOOP_FLOW_ST_EXT_PREFIX, "constraint_adjustment_coefficient")
            .ifPresent(extension::setConstraintAdjustmentCoefficient);
        return extension;
    }

    static Map<String, String> mnecParametersExtensionToMap(MnecParameters mnecParameters) {
        Map<String, String> map = new HashMap<>();
        map.put(MNEC_EXT_PREFIX + "acceptable_margin_decrease", String.valueOf(mnecParameters.getAcceptableMarginDecrease()));
        return map;
    }

    static Map<String, String> mnecParametersExtensionToMap(SearchTreeRaoMnecParameters searchTreeExtension) {
        Map<String, String> map = new HashMap<>();
        map.put(MNEC_ST_EXT_PREFIX + "violation_cost", String.valueOf(searchTreeExtension.getViolationCost()));
        map.put(MNEC_ST_EXT_PREFIX + "constraint_adjustment_coefficient", String.valueOf(searchTreeExtension.getConstraintAdjustmentCoefficient()));
        return map;
    }

    static Map<String, String> relativeMarginsParametersExtensionToMap(RelativeMarginsParameters extension) {
        Map<String, String> map = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            map.put(RELATIVE_MARGIN_EXT_PREFIX + "ptdf_boundaries", objectMapper.writeValueAsString(extension.getPtdfBoundariesAsString()));
        } catch (JsonProcessingException e) {
            throw new PowsyblException("Cannot write ptdf boundaries to string.");
        }
        return map;
    }

    static Map<String, String> relativeMarginsParametersExtensionToMap(SearchTreeRaoRelativeMarginsParameters searchTreeExtension) {
        Map<String, String> map = new HashMap<>();
        map.put(RELATIVE_MARGIN_ST_EXT_PREFIX + "ptdf_sum_lower_bounds", String.valueOf(searchTreeExtension.getPtdfSumLowerBound()));
        map.put(RELATIVE_MARGIN_ST_EXT_PREFIX + "ptdf_approximation", String.valueOf(searchTreeExtension.getPtdfApproximation()));
        return map;
    }

    static Map<String, String> loopFlowParametersExtensionToMap(LoopFlowParameters extension) {
        Map<String, String> map = new HashMap<>();
        map.put(LOOP_FLOW_EXT_PREFIX + "acceptable_increase", String.valueOf(extension.getAcceptableIncrease()));
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            map.put(LOOP_FLOW_EXT_PREFIX + "countries", objectMapper.writeValueAsString(extension.getCountries()));
        } catch (JsonProcessingException e) {
            throw new PowsyblException("Cannot write country set to string.");
        }
        return map;
    }

    static Map<String, String> loopFlowParametersExtensionToMap(SearchTreeRaoLoopFlowParameters searchTreeExtension) {
        Map<String, String> map = new HashMap<>();
        map.put(LOOP_FLOW_ST_EXT_PREFIX + "ptdf_approximation", String.valueOf(searchTreeExtension.getPtdfApproximation()));
        map.put(LOOP_FLOW_ST_EXT_PREFIX + "constraint_adjustment_coefficient", String.valueOf(searchTreeExtension.getConstraintAdjustmentCoefficient()));
        return map;
    }

    static List<String> ptdfBoundariesFromString(String jsonRepresentation) {
        return parseStringList(jsonRepresentation, "Cannot read ptdf boundaries from string.");
    }

    static List<String> countryListFromString(String jsonRepresentation) {
        return parseStringList(jsonRepresentation, "Cannot read country list from string.");
    }

    static List<String> parseStringList(String jsonRepresentation, String exceptionMessage) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<String> stringList = null;
        try {
            stringList = objectMapper.readValue(jsonRepresentation, new TypeReference<List<String>>() { });
        } catch (JsonProcessingException e) {
            throw new PowsyblException(exceptionMessage);
        }
        return stringList;
    }

    static Optional<Double> extractDouble(Map<String, String> extensionDict, String prefix, String attribute) {
        return extractAttribute(extensionDict, prefix, attribute, Double::parseDouble);
    }

    static <T> Optional<T> extractAttribute(Map<String, String> extensionDict, String prefix, String attribute, Function<String, T> converter) {
        String valueStr = extensionDict.get(prefix + attribute);
        if (valueStr != null) {
            return Optional.of(converter.apply(valueStr));
        }
        return Optional.empty();
    }
}
