package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.*;
import com.powsybl.python.network.TemporaryLimitData;
import gnu.trove.list.array.TIntArrayList;

import java.util.*;

import static com.powsybl.dataframe.network.adders.SeriesUtils.*;

public class OperationalLimitsDataframeAdder implements NetworkElementAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
        SeriesMetadata.stringIndex("element_id"),
        SeriesMetadata.strings("name"),
        SeriesMetadata.strings("side"),
        SeriesMetadata.strings("type"),
        SeriesMetadata.doubles("value"),
        SeriesMetadata.ints("acceptable_duration"),
        SeriesMetadata.booleans("fictitious"),
        SeriesMetadata.strings("group_name")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    private static final class OperationalLimitsSeries {

        private final StringSeries elementIds;
        private final StringSeries names;
        private final StringSeries sides;
        private final StringSeries types;
        private final DoubleSeries values;
        private final IntSeries acceptableDurations;
        private final IntSeries fictitious;
        private final StringSeries groupNames;

        OperationalLimitsSeries(UpdatingDataframe dataframe) {
            this.elementIds = getRequiredStrings(dataframe, "element_id");
            this.names = dataframe.getStrings("name");
            this.sides = getRequiredStrings(dataframe, "side");
            this.types = getRequiredStrings(dataframe, "type");
            this.values = getRequiredDoubles(dataframe, "value");
            this.acceptableDurations = getRequiredInts(dataframe, "acceptable_duration");
            this.fictitious = dataframe.getInts("fictitious");
            this.groupNames = dataframe.getStrings("group_name");
        }

        public StringSeries getElementIds() {
            return elementIds;
        }

        public StringSeries getNames() {
            return names;
        }

        public StringSeries getSides() {
            return sides;
        }

        public StringSeries getTypes() {
            return types;
        }

        public DoubleSeries getValues() {
            return values;
        }

        public IntSeries getAcceptableDurations() {
            return acceptableDurations;
        }

        public IntSeries getFictitious() {
            return fictitious;
        }

        public StringSeries getGroupNames() {
            return groupNames;
        }
    }

    @Override
    public void addElements(Network network, List<UpdatingDataframe> dataframes) {
        UpdatingDataframe primaryTable = dataframes.get(0);
        OperationalLimitsSeries series = new OperationalLimitsSeries(primaryTable);

        Map<LimitsDataframeAdderKey, TIntArrayList> indexMap = new HashMap<>();
        for (int i = 0; i < primaryTable.getRowCount(); i++) {
            String elementId = series.getElementIds().get(i);
            String side = series.getSides().get(i);
            String limitType = series.getTypes().get(i);
            StringSeries groupNames = series.getGroupNames();
            String groupName = null;
            if (groupNames != null) {
                groupName = series.getGroupNames().get(i);
            }
            LimitsDataframeAdderKey key = new LimitsDataframeAdderKey(elementId, side, limitType, groupName);
            indexMap.computeIfAbsent(key, k -> new TIntArrayList()).add(i);
        }

        addElements(network, series, indexMap);
    }

    private static void addElements(Network network, OperationalLimitsSeries series, Map<LimitsDataframeAdderKey, TIntArrayList> indexMap) {
        indexMap.forEach((key, indexList) -> createLimits(network, series, key.getElementId(),
            key.getSide(), key.getLimitType(), key.getGroupId(), indexList));
    }

    private static void createLimits(Network network, OperationalLimitsSeries series, String elementId, String side, String type,
                                     String groupId, TIntArrayList indexList) {
        LimitType limitType = LimitType.valueOf(type);
        TemporaryLimitData.Side limitSide = TemporaryLimitData.Side.valueOf(side);

        LoadingLimitsAdder<?, ?> adder = getAdder(network, elementId, limitType, limitSide, groupId);
        for (int index : indexList.toArray()) {
            createLimits(adder, index, series);
        }
        adder.add();
    }

    private static void createLimits(LoadingLimitsAdder<?, ?> adder, int row, OperationalLimitsSeries series) {
        int acceptableDuration = series.getAcceptableDurations().get(row);
        if (acceptableDuration == -1) {
            applyIfPresent(series.getValues(), row, adder::setPermanentLimit);
        } else {
            LoadingLimitsAdder.TemporaryLimitAdder<?> temporaryLimitAdder = adder.beginTemporaryLimit()
                .setAcceptableDuration(acceptableDuration);
            applyIfPresent(series.getNames(), row, temporaryLimitAdder::setName);
            applyIfPresent(series.getValues(), row, temporaryLimitAdder::setValue);
            applyBooleanIfPresent(series.getFictitious(), row, temporaryLimitAdder::setFictitious);
            temporaryLimitAdder.endTemporaryLimit();
        }
    }

    /**
     * Wraps a branch in a flows limits holder view
     */
    private static FlowsLimitsHolder getBranchAsFlowsLimitsHolder(Branch<?> branch, TwoSides side) {
        return new FlowsLimitsHolder() {

            @Override
            public Collection<OperationalLimitsGroup> getOperationalLimitsGroups() {
                return side == TwoSides.ONE ? branch.getOperationalLimitsGroups1() : branch.getOperationalLimitsGroups2();
            }

            @Override
            public Optional<String> getSelectedOperationalLimitsGroupId() {
                return side == TwoSides.ONE ? branch.getSelectedOperationalLimitsGroupId1() : branch.getSelectedOperationalLimitsGroupId2();
            }

            @Override
            public Optional<OperationalLimitsGroup> getOperationalLimitsGroup(String s) {
                return side == TwoSides.ONE ? branch.getOperationalLimitsGroup1(s) : branch.getOperationalLimitsGroup2(s);
            }

            @Override
            public Optional<OperationalLimitsGroup> getSelectedOperationalLimitsGroup() {
                return side == TwoSides.ONE ? branch.getSelectedOperationalLimitsGroup1() : branch.getSelectedOperationalLimitsGroup2();
            }

            @Override
            public OperationalLimitsGroup newOperationalLimitsGroup(String s) {
                return side == TwoSides.ONE ? branch.newOperationalLimitsGroup1(s) : branch.newOperationalLimitsGroup2(s);
            }

            @Override
            public void setSelectedOperationalLimitsGroup(String s) {
                if (side == TwoSides.ONE) {
                    branch.setSelectedOperationalLimitsGroup1(s);
                } else {
                    branch.setSelectedOperationalLimitsGroup2(s);
                }
            }

            @Override
            public void removeOperationalLimitsGroup(String s) {
                if (side == TwoSides.ONE) {
                    branch.removeOperationalLimitsGroup1(s);
                } else {
                    branch.removeOperationalLimitsGroup2(s);
                }
            }

            @Override
            public void cancelSelectedOperationalLimitsGroup() {
                if (side == TwoSides.ONE) {
                    branch.cancelSelectedOperationalLimitsGroup1();
                } else {
                    branch.cancelSelectedOperationalLimitsGroup2();
                }
            }

            @Override
            public Optional<CurrentLimits> getCurrentLimits() {
                return branch.getCurrentLimits(side);
            }

            @Override
            public CurrentLimits getNullableCurrentLimits() {
                return side == TwoSides.ONE ? branch.getNullableCurrentLimits1() : branch.getNullableCurrentLimits2();
            }

            @Override
            public Optional<ActivePowerLimits> getActivePowerLimits() {
                return branch.getActivePowerLimits(side);
            }

            @Override
            public ActivePowerLimits getNullableActivePowerLimits() {
                return side == TwoSides.ONE ? branch.getNullableActivePowerLimits1() : branch.getNullableActivePowerLimits2();
            }

            @Override
            public Optional<ApparentPowerLimits> getApparentPowerLimits() {
                return branch.getApparentPowerLimits(side);
            }

            @Override
            public ApparentPowerLimits getNullableApparentPowerLimits() {
                return getApparentPowerLimits().orElse(null);
            }

            @Override
            public CurrentLimitsAdder newCurrentLimits() {
                return side == TwoSides.ONE ? branch.newCurrentLimits1() : branch.newCurrentLimits2();
            }

            @Override
            public ApparentPowerLimitsAdder newApparentPowerLimits() {
                return side == TwoSides.ONE ? branch.newApparentPowerLimits1() : branch.newApparentPowerLimits2();
            }

            @Override
            public ActivePowerLimitsAdder newActivePowerLimits() {
                return side == TwoSides.ONE ? branch.newActivePowerLimits1() : branch.newActivePowerLimits2();
            }
        };
    }

    private static TwoSides toBranchSide(TemporaryLimitData.Side side) {
        return switch (side) {
            case ONE -> TwoSides.ONE;
            case TWO -> TwoSides.TWO;
            default -> throw new PowsyblException(String.format("Invalid value for branch side: %s", side));
        };
    }

    private static FlowsLimitsHolder getLimitsHolder(Network network, String elementId, TemporaryLimitData.Side side) {
        Identifiable<?> identifiable = NetworkUtils.getIdentifiableOrThrow(network, elementId);
        switch (identifiable.getType()) {
            case LINE, TWO_WINDINGS_TRANSFORMER -> {
                return getBranchAsFlowsLimitsHolder((Branch<?>) identifiable, toBranchSide(side));
            }
            case DANGLING_LINE -> {
                if (side != TemporaryLimitData.Side.NONE) {
                    throw new PowsyblException(String.format("Invalid value for dangling line side: %s, must be NONE", side));
                }
                return (DanglingLine) identifiable;
            }
            case THREE_WINDINGS_TRANSFORMER -> {
                ThreeWindingsTransformer transformer = (ThreeWindingsTransformer) identifiable;
                return switch (side) {
                    case ONE -> transformer.getLeg1();
                    case TWO -> transformer.getLeg2();
                    case THREE -> transformer.getLeg3();
                    default -> throw new PowsyblException(String.format("Invalid value for three windings transformer side: %s", side));
                };
            }
            default ->
                throw new PowsyblException("Cannot create operational limits for element of type " + identifiable.getType());
        }
    }

    private static LoadingLimitsAdder<?, ?> getAdder(Network network, String elementId, LimitType type,
                                                     TemporaryLimitData.Side side, String groupId) {
        FlowsLimitsHolder limitsHolder = getLimitsHolder(network, elementId, side);
        OperationalLimitsGroup group;
        if (groupId == null) {
            String selectedGroupId = limitsHolder.getSelectedOperationalLimitsGroupId().orElse("DEFAULT");
            group = limitsHolder.getSelectedOperationalLimitsGroup()
                    .orElseGet(() -> limitsHolder.newOperationalLimitsGroup("DEFAULT"));
            limitsHolder.setSelectedOperationalLimitsGroup(selectedGroupId);
        } else {
            group = limitsHolder.getOperationalLimitsGroup(groupId)
                    .orElseGet(() -> limitsHolder.newOperationalLimitsGroup(groupId));
        }

        return switch (type) {
            case CURRENT -> group.newCurrentLimits();
            case ACTIVE_POWER -> group.newActivePowerLimits();
            case APPARENT_POWER -> group.newApparentPowerLimits();
            default -> throw new PowsyblException(String.format("Limit type %s does not exist.", type));
        };
    }
}
