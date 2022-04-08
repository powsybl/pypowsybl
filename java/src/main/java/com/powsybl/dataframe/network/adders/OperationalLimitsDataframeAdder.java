package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.*;
import com.powsybl.python.LimitsDataframeAdderKey;
import com.powsybl.python.TemporaryLimitData;

import java.util.*;

public class OperationalLimitsDataframeAdder implements NetworkElementAdder {

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
    public void addElements(Network network, List<UpdatingDataframe> dataframes) {
        UpdatingDataframe primaryTable = dataframes.get(0);
        Map<LimitsDataframeAdderKey, List<Integer>> indexMap = new HashMap<>();
        for (int i = 0; i < primaryTable.getLineCount(); i++) {
            String elementId = primaryTable.getStringValue("element_id", i)
                    .orElseThrow(() -> new PowsyblException("element_id is missing"));
            String side = primaryTable.getStringValue("side", i)
                    .orElseThrow(() -> new PowsyblException("side is missing"));
            String limitType = primaryTable.getStringValue("type", i)
                    .orElseThrow(() -> new PowsyblException("type is missing"));
            LimitsDataframeAdderKey key = new LimitsDataframeAdderKey(elementId, side, limitType);
            if (!indexMap.containsKey(key)) {
                indexMap.put(key, new ArrayList<>());
            }
            indexMap.get(key).add(i);
        }

        addElements(network, primaryTable, indexMap);
    }

    public void addElements(Network network, UpdatingDataframe dataframe, Map<LimitsDataframeAdderKey, List<Integer>> indexMap) {
        indexMap.forEach((key, indexList) -> createLimits(network, dataframe, key.getElementId(),
                key.getSide(), key.getLimitType(), indexList));
    }

    public void createLimits(Network network, UpdatingDataframe dataframe, String elementId, String side, String type,
                             List<Integer> indexList) {
        IdentifiableType elementType = dataframe.getStringValue("element_type", indexList.get(0))
                .map(IdentifiableType::valueOf)
                .orElseThrow(() -> new PowsyblException("element_type is not set"));
        LimitType limitType = LimitType.valueOf(type);
        TemporaryLimitData.Side limitSide = TemporaryLimitData.Side.valueOf(side);

        LoadingLimitsAdder adder = getAdder(network, elementType, elementId, limitType, limitSide);
        for (Integer index : indexList) {
            int acceptableDuration = dataframe.getIntValue("acceptable_duration", index).orElseThrow(() -> new PowsyblException("acceptable duration is missing"));
            double value = dataframe.getDoubleValue("value", index).orElseThrow(() -> new PowsyblException("value is missing"));
            OptionalInt isFictitious = dataframe.getIntValue("is_fictitious", index);
            adder = createLimits(adder, acceptableDuration, value, isFictitious,
                    dataframe.getStringValue("name", index).orElseThrow(() -> new PowsyblException("name is missing")));
        }
        adder.add();
    }

    private LoadingLimitsAdder createLimits(LoadingLimitsAdder adder, int acceptableDuration, double value, OptionalInt isFictitious, String name) {
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

    /**
     * Wraps a branch in a flows limits holder view
     */
    private FlowsLimitsHolder getBranchAsFlowsLimitsHolder(Branch<?> branch, Branch.Side side) {
        return new FlowsLimitsHolder() {

            @Override
            public Collection<OperationalLimits> getOperationalLimits() {
                return side == Branch.Side.ONE ? branch.getOperationalLimits1() : branch.getOperationalLimits2();
            }

            @Override
            public CurrentLimits getCurrentLimits() {
                return branch.getCurrentLimits(side);
            }

            @Override
            public ActivePowerLimits getActivePowerLimits() {
                return branch.getActivePowerLimits(side);
            }

            @Override
            public ApparentPowerLimits getApparentPowerLimits() {
                return branch.getApparentPowerLimits(side);
            }

            @Override
            public CurrentLimitsAdder newCurrentLimits() {
                return side == Branch.Side.ONE ? branch.newCurrentLimits1() : branch.newCurrentLimits2();
            }

            @Override
            public ApparentPowerLimitsAdder newApparentPowerLimits() {
                return side == Branch.Side.ONE ? branch.newApparentPowerLimits1() : branch.newApparentPowerLimits2();
            }

            @Override
            public ActivePowerLimitsAdder newActivePowerLimits() {
                return side == Branch.Side.ONE ? branch.newActivePowerLimits1() : branch.newActivePowerLimits2();
            }
        };
    }

    private Branch.Side toBranchSide(TemporaryLimitData.Side side) {
        switch (side) {
            case ONE:
                return Branch.Side.ONE;
            case TWO:
                return Branch.Side.TWO;
            default:
                throw new PowsyblException("Invalid value for branch side: " + side);
        }
    }

    private FlowsLimitsHolder getLimitsHolder(Network network, IdentifiableType identifiableType, String elementId, TemporaryLimitData.Side side) {
        switch (identifiableType) {
            case LINE:
            case TWO_WINDINGS_TRANSFORMER:
                Branch<?> branch = network.getBranch(elementId);
                if (branch == null) {
                    throw new PowsyblException("Branch " + elementId + " does not exist.");
                }
                return getBranchAsFlowsLimitsHolder(branch, toBranchSide(side));
            case DANGLING_LINE:
                DanglingLine dl = network.getDanglingLine(elementId);
                if (dl == null) {
                    throw new PowsyblException("Dangling line " + elementId + " does not exist.");
                }
                if (side != TemporaryLimitData.Side.NONE) {
                    throw new PowsyblException("Invalid value for dangling line side: " + side + ", must be NONE");
                }
                return dl;
            case THREE_WINDINGS_TRANSFORMER:
                ThreeWindingsTransformer transformer = network.getThreeWindingsTransformer(elementId);
                if (transformer == null) {
                    throw new PowsyblException("Three windings transformer " + elementId + " does not exist.");
                }
                switch (side) {
                    case ONE:
                        return transformer.getLeg1();
                    case TWO:
                        return transformer.getLeg2();
                    case THREE:
                        return transformer.getLeg3();
                    default:
                        throw new PowsyblException("Invalid value for three windings transformer side: " + side);
                }
            default:
                throw new PowsyblException("Cannot create operational limits for element of type " + identifiableType);
        }
    }

    private LoadingLimitsAdder getAdder(Network network, IdentifiableType identifiableType, String elementId, LimitType type, TemporaryLimitData.Side side) {
        FlowsLimitsHolder limitsHolder = getLimitsHolder(network, identifiableType, elementId, side);
        switch (type) {
            case CURRENT:
                return limitsHolder.newCurrentLimits();
            case ACTIVE_POWER:
                return limitsHolder.newActivePowerLimits();
            case APPARENT_POWER:
                return limitsHolder.newApparentPowerLimits();
            default:
                throw new PowsyblException(String.format("Limit type %s does not exist.", type));
        }
    }
}
