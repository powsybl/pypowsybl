package com.powsybl.python.rao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.PowsyblException;
import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.MnecParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.PtdfApproximation;
import com.powsybl.openrao.raoapi.parameters.extensions.RelativeMarginsParametersExtension;

import java.util.*;
import java.util.function.Function;

public final class RaoUtils {

    private static final String MNEC_EXT_PREFIX = "MNEC_EXT_";
    private static final String RELATIVE_MARGIN_EXT_PREFIX = "RELATIVE_MARGIN_EXT_";
    private static final String LOOP_FLOW_EXT_PREFIX = "LOOP_FLOW_EXT_";

    private RaoUtils() {
    }

    static MnecParametersExtension buildMnecParametersExtension(Map<String, String> extensionDict) {
        MnecParametersExtension extension = new MnecParametersExtension();
        extractDouble(extensionDict, MNEC_EXT_PREFIX, "acceptable_margin_decrease")
            .ifPresent(extension::setAcceptableMarginDecrease);
        extractDouble(extensionDict, MNEC_EXT_PREFIX, "violation_cost")
            .ifPresent(extension::setViolationCost);
        extractDouble(extensionDict, MNEC_EXT_PREFIX, "constraint_adjustment_coefficient")
            .ifPresent(extension::setConstraintAdjustmentCoefficient);
        return extension;
    }

    static RelativeMarginsParametersExtension buildRelativeMarginsParametersExtension(Map<String, String> extensionDict) {
        RelativeMarginsParametersExtension extension = new RelativeMarginsParametersExtension();
        extractDouble(extensionDict, RELATIVE_MARGIN_EXT_PREFIX, "ptdf_sum_lower_bounds")
            .ifPresent(extension::setPtdfSumLowerBound);
        extractAttribute(extensionDict, RELATIVE_MARGIN_EXT_PREFIX, "ptdf_approximation", PtdfApproximation::valueOf)
            .ifPresent(extension::setPtdfApproximation);
        extractAttribute(extensionDict, RELATIVE_MARGIN_EXT_PREFIX, "ptdf_boundaries", RaoUtils::ptdfBoundariesFromString)
            .ifPresent(extension::setPtdfBoundariesFromString);
        return extension;
    }

    static LoopFlowParametersExtension buildLoopFlowParametersExtension(Map<String, String> extensionDict) {
        LoopFlowParametersExtension extension = new LoopFlowParametersExtension();
        extractDouble(extensionDict, LOOP_FLOW_EXT_PREFIX, "acceptable_increase")
            .ifPresent(extension::setAcceptableIncrease);
        extractAttribute(extensionDict, LOOP_FLOW_EXT_PREFIX, "ptdf_approximation", PtdfApproximation::valueOf)
            .ifPresent(extension::setPtdfApproximation);
        extractDouble(extensionDict, LOOP_FLOW_EXT_PREFIX, "constraint_adjustment_coefficient")
            .ifPresent(extension::setConstraintAdjustmentCoefficient);
        extractAttribute(extensionDict, LOOP_FLOW_EXT_PREFIX, "countries", RaoUtils::countryListFromString)
            .ifPresent(extension::setCountries);
        return extension;
    }

    static Map<String, String> mnecParametersExtensionToMap(MnecParametersExtension extension) {
        Map<String, String> map = new HashMap<>();
        map.put(MNEC_EXT_PREFIX + "acceptable_margin_decrease", String.valueOf(extension.getAcceptableMarginDecrease()));
        map.put(MNEC_EXT_PREFIX + "violation_cost", String.valueOf(extension.getViolationCost()));
        map.put(MNEC_EXT_PREFIX + "constraint_adjustment_coefficient", String.valueOf(extension.getConstraintAdjustmentCoefficient()));
        return map;
    }

    static Map<String, String> relativeMarginsParametersExtensionToMap(RelativeMarginsParametersExtension extension) {
        Map<String, String> map = new HashMap<>();
        map.put(RELATIVE_MARGIN_EXT_PREFIX + "ptdf_sum_lower_bounds", String.valueOf(extension.getPtdfSumLowerBound()));
        map.put(RELATIVE_MARGIN_EXT_PREFIX + "ptdf_approximation", String.valueOf(extension.getPtdfApproximation()));
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            map.put(RELATIVE_MARGIN_EXT_PREFIX + "ptdf_boundaries", objectMapper.writeValueAsString(extension.getPtdfBoundariesAsString()));
        } catch (JsonProcessingException e) {
            throw new PowsyblException("Cannot write ptdf boundaries to string.");
        }
        return map;
    }

    static Map<String, String> loopFlowParametersExtensionToMap(LoopFlowParametersExtension extension) {
        Map<String, String> map = new HashMap<>();
        map.put(LOOP_FLOW_EXT_PREFIX + "acceptable_increase", String.valueOf(extension.getAcceptableIncrease()));
        map.put(LOOP_FLOW_EXT_PREFIX + "ptdf_approximation", String.valueOf(extension.getPtdfApproximation()));
        map.put(LOOP_FLOW_EXT_PREFIX + "constraint_adjustment_coefficient", String.valueOf(extension.getConstraintAdjustmentCoefficient()));
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            map.put(LOOP_FLOW_EXT_PREFIX + "countries", objectMapper.writeValueAsString(extension.getCountries()));
        } catch (JsonProcessingException e) {
            throw new PowsyblException("Cannot write country set to string.");
        }
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
