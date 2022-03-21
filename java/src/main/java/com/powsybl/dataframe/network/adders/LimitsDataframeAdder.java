package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.*;
import com.powsybl.python.LimitsDataframeAdderKey;

import java.util.*;

public class LimitsDataframeAdder implements NetworkElementAdder {
    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("element_id"),
            SeriesMetadata.strings("name"),
            SeriesMetadata.strings("element_type"),
            SeriesMetadata.strings("side"),
            SeriesMetadata.strings("type"),
            SeriesMetadata.doubles("value"),
            SeriesMetadata.ints("acceptable_duration"),
            SeriesMetadata.booleans("is_fictitious")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    @Override
    public void addElement(Network network, List<UpdatingDataframe> dataframes, int index) {
    }

    public void addElements(Network network, UpdatingDataframe dataframe, Map<LimitsDataframeAdderKey, List<Integer>> indexMap) {
        indexMap.forEach((key, indexList) -> createLimits(network, dataframe, key.getElementId(),
                key.getSide(), key.getLimitType(), indexList));

    }

    public void createLimits(Network network, UpdatingDataframe dataframe, String elementId, String side, String type,
                             List<Integer> indexList) {
        Identifiable identifiable = network.getIdentifiable(elementId);
        Optional<String> elementType = dataframe.getStringValue("element_type", indexList.get(0));
        IdentifiableType identifiableType = identifiable.getType();
        if (elementType.isPresent()) {
            if (!elementType.get().equals(identifiableType.name())) {
                throw new PowsyblException("type set does not match type of the element id");
            }
        }
        LoadingLimitsAdder adder = getAdder(network, elementId, type, side, identifiableType);
        for (Integer index : indexList) {
            int acceptableDuration = dataframe.getIntValue("acceptable_duration", index).orElseThrow(() -> new PowsyblException("acceptable duration is missing"));
            double value = dataframe.getDoubleValue("value", index).orElseThrow(() -> new PowsyblException("value is missing"));
            OptionalInt isFictitious = dataframe.getIntValue("is_fictitious", index);
            adder = createLimit(adder, acceptableDuration, value, isFictitious,
                    dataframe.getStringValue("name", index).orElseThrow(() -> new PowsyblException("name is missing")));
        }
        adder.add();
    }

    private LoadingLimitsAdder createLimit(LoadingLimitsAdder adder, int acceptableDuration, double value, OptionalInt isFictitious, String name) {
        if (acceptableDuration == -1) {
            adder.setPermanentLimit(value);
            return adder;
        } else {

            LoadingLimitsAdder.TemporaryLimitAdder temporaryLimitAdder = adder.beginTemporaryLimit().setName(name)
                    .setAcceptableDuration(acceptableDuration)
                    .setValue(value);
            if (isFictitious.isPresent()) {
                temporaryLimitAdder.setFictitious(isFictitious.getAsInt() == 1);
            }
            return (LoadingLimitsAdder) temporaryLimitAdder.endTemporaryLimit();
        }
    }

    private LoadingLimitsAdder getAdder(Network network, String elementId, String type, String side, IdentifiableType identifiableType) {
        LoadingLimitsAdder adder;
        switch (identifiableType) {
            case LINE:
            case TWO_WINDINGS_TRANSFORMER:
                switch (type) {
                    case "CURRENT":
                        if (side.equals("ONE")) {
                            adder = network.getBranch(elementId).newCurrentLimits1();
                        } else if (side.equals("TWO")) {
                            adder = network.getBranch(elementId).newCurrentLimits2();
                        } else {
                            throw new PowsyblException(String.format("side %s does not exist", side));
                        }
                        break;
                    case "ACTIVE_POWER":
                        if (side.equals("ONE")) {
                            adder = network.getBranch(elementId).newActivePowerLimits1();
                        } else if (side.equals("TWO")) {
                            adder = network.getBranch(elementId).newActivePowerLimits2();
                        } else {
                            throw new PowsyblException(String.format("side %s does not exist", side));
                        }
                        break;
                    case "APPARENT_POWER":
                        if (side.equals("ONE")) {
                            adder = network.getBranch(elementId).newApparentPowerLimits1();
                        } else if (side.equals("TWO")) {
                            adder = network.getBranch(elementId).newApparentPowerLimits2();
                        } else {
                            throw new PowsyblException(String.format("side %s does not exist", side));
                        }
                        break;
                    default:
                        throw new PowsyblException(String.format("type %s does not exist", type));
                }
                break;
            case DANGLING_LINE:
                switch (type) {
                    case "CURRENT":
                        if (side.equals("NONE")) {
                            adder = network.getDanglingLine(elementId).newCurrentLimits();
                        } else {
                            throw new PowsyblException(String.format("side %s does not exist must be NONE for dangling lines", side));
                        }
                        break;
                    case "ACTIVE_POWER":
                        if (side.equals("NONE")) {
                            adder = network.getDanglingLine(elementId).newActivePowerLimits();
                        } else {
                            throw new PowsyblException(String.format("side %s does not exist must be NONE for dangling lines", side));
                        }
                        break;
                    case "APPARENT_POWER":
                        if (side.equals("NONE")) {
                            adder = network.getDanglingLine(elementId).newApparentPowerLimits();
                        } else {
                            throw new PowsyblException(String.format("side %s does not exist must be NONE for dangling lines", side));
                        }
                        break;
                    default:
                        throw new PowsyblException(String.format("type %s does not exist", type));
                }
                break;
            case THREE_WINDINGS_TRANSFORMER:
                switch (type) {
                    case "CURRENT":
                        switch (side) {
                            case "ONE":
                                adder = network.getThreeWindingsTransformer(elementId).getLeg1().newCurrentLimits();
                                break;
                            case "TWO":
                                adder = network.getThreeWindingsTransformer(elementId).getLeg2().newCurrentLimits();
                                break;
                            case "THREE":
                                adder = network.getThreeWindingsTransformer(elementId).getLeg3().newCurrentLimits();
                                break;
                            default:
                                throw new PowsyblException(String.format("side %s does not exist", side));
                        }
                        break;
                    case "ACTIVE_POWER":
                        switch (side) {
                            case "ONE":
                                adder = network.getThreeWindingsTransformer(elementId).getLeg1().newActivePowerLimits();
                                break;
                            case "TWO":
                                adder = network.getThreeWindingsTransformer(elementId).getLeg2().newActivePowerLimits();
                                break;
                            case "THREE":
                                adder = network.getThreeWindingsTransformer(elementId).getLeg3().newActivePowerLimits();
                                break;
                            default:
                                throw new PowsyblException(String.format("side %s does not exist", side));
                        }
                        break;
                    case "APPARENT_POWER":
                        switch (side) {
                            case "ONE":
                                adder = network.getThreeWindingsTransformer(elementId).getLeg1().newApparentPowerLimits();
                                break;
                            case "TWO":
                                adder = network.getThreeWindingsTransformer(elementId).getLeg2().newApparentPowerLimits();
                                break;
                            case "THREE":
                                adder = network.getThreeWindingsTransformer(elementId).getLeg3().newApparentPowerLimits();
                                break;
                            default:
                                throw new PowsyblException(String.format("side %s does not exist", side));
                        }
                        break;
                    default:
                        throw new PowsyblException(String.format("type %s does not exist", type));
                }
                break;
            default:
                throw new PowsyblException(String.format("element type %s is missing or does not have limits", identifiableType));
        }
        return adder;
    }
}
