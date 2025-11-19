package com.powsybl.dataframe.security;

import com.powsybl.commons.PowsyblException;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.contingency.ContingencyContextType;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.adders.SeriesUtils;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.criteria.AtLeastOneCountryCriterion;
import com.powsybl.iidm.criteria.AtLeastOneNominalVoltageCriterion;
import com.powsybl.iidm.criteria.IdentifiableCriterion;
import com.powsybl.iidm.criteria.VoltageInterval;
import com.powsybl.iidm.criteria.duration.AllTemporaryDurationCriterion;
import com.powsybl.iidm.criteria.duration.IntervalTemporaryDurationCriterion;
import com.powsybl.iidm.criteria.duration.LimitDurationCriterion;
import com.powsybl.iidm.criteria.duration.PermanentDurationCriterion;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.LimitType;
import com.powsybl.python.security.SecurityAnalysisContext;
import com.powsybl.security.limitreduction.LimitReduction;

import java.util.ArrayList;
import java.util.List;

public class LimitReductionDataframeAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("limit_type"),
            SeriesMetadata.booleans("monitoring"),
            SeriesMetadata.strings("contingency_context"),
            SeriesMetadata.doubles("min_voltage"),
            SeriesMetadata.doubles("max_voltage"),
            SeriesMetadata.strings("country"),
            SeriesMetadata.booleans("permanent"),
            SeriesMetadata.booleans("temporary"),
            SeriesMetadata.ints("min_temporary_duration"),
            SeriesMetadata.ints("max_temporary_duration"),
            SeriesMetadata.doubles("value")
    );

    public List<SeriesMetadata> getMetadata() {
        return METADATA;
    }

    private static final class LimitReductionSeries {
        private final StringSeries limitType;
        private final IntSeries monitoring;
        private final StringSeries contingencyContext;
        private final DoubleSeries minVoltage;
        private final DoubleSeries maxVoltage;
        private final StringSeries country;
        private final IntSeries permanent;
        private final IntSeries temporary;
        private final IntSeries minTemporaryDuration;
        private final IntSeries maxTemporaryDuration;
        private final DoubleSeries value;

        LimitReductionSeries(UpdatingDataframe dataframe) {
            this.limitType = SeriesUtils.getRequiredStrings(dataframe, "limit_type");
            this.monitoring = dataframe.getInts("monitoring");
            this.contingencyContext = SeriesUtils.getRequiredStrings(dataframe, "contingency_context");
            this.minVoltage = dataframe.getDoubles("min_voltage");
            this.maxVoltage = dataframe.getDoubles("max_voltage");
            this.country = dataframe.getStrings("country");
            this.permanent = SeriesUtils.getRequiredInts(dataframe, "permanent");
            this.temporary = SeriesUtils.getRequiredInts(dataframe, "temporary");
            this.minTemporaryDuration = dataframe.getInts("min_temporary_duration");
            this.maxTemporaryDuration = dataframe.getInts("max_temporary_duration");
            this.value = SeriesUtils.getRequiredDoubles(dataframe, "value");
        }

        public StringSeries getLimitType() {
            return limitType;
        }

        public StringSeries getContingencyContext() {
            return contingencyContext;
        }

        public DoubleSeries getValue() {
            return value;
        }

        public IntSeries getMonitoring() {
            return monitoring;
        }

        public DoubleSeries getMinVoltage() {
            return minVoltage;
        }

        public DoubleSeries getMaxVoltage() {
            return maxVoltage;
        }

        public StringSeries getCountry() {
            return country;
        }

        public IntSeries getPermanent() {
            return permanent;
        }

        public IntSeries getTemporary() {
            return temporary;
        }

        public IntSeries getMinTemporaryDuration() {
            return minTemporaryDuration;
        }

        public IntSeries getMaxTemporaryDuration() {
            return maxTemporaryDuration;
        }
    }

    public void addElements(SecurityAnalysisContext context, UpdatingDataframe dataframe) {
        if (dataframe.getRowCount() > 0) {
            LimitReductionSeries series = new LimitReductionSeries(dataframe);
            for (int row = 0; row < dataframe.getRowCount(); row++) {
                LimitType type = LimitType.valueOf(series.getLimitType().get(row));
                ContingencyContextType contingencyContextType = ContingencyContextType.valueOf(series.getContingencyContext().get(row));
                double value = series.getValue().get(row);
                LimitReduction.Builder reductionBuilder = LimitReduction.builder(type, value)
                        .withContingencyContext(ContingencyContext.create(null, contingencyContextType));
                SeriesUtils.applyBooleanIfPresent(series.getMonitoring(), row, reductionBuilder::withMonitoringOnly);

                addLimitDurationCriteria(series, row, reductionBuilder);
                addNetworkElementCriterion(series, row, reductionBuilder);

                context.addLimitReduction(reductionBuilder.build());
            }
        }
    }

    private void addLimitDurationCriteria(LimitReductionSeries series, int row, LimitReduction.Builder reductionBuilder) {
        List<LimitDurationCriterion> durationCriteria = new ArrayList<>();
        if (series.getPermanent().get(row) == 1) {
            durationCriteria.add(new PermanentDurationCriterion());
        }
        if (series.getTemporary().get(row) == 1) {
            boolean hasMinTemporaryDuration = series.getMinTemporaryDuration() != null;
            boolean hasMaxTemporaryDuration = series.getMaxTemporaryDuration() != null;
            if (hasMaxTemporaryDuration && hasMinTemporaryDuration) {
                durationCriteria.add(IntervalTemporaryDurationCriterion.between(
                        series.getMinTemporaryDuration().get(row), series.getMaxTemporaryDuration().get(row), true, true
                ));
            } else if (hasMaxTemporaryDuration) {
                durationCriteria.add(IntervalTemporaryDurationCriterion.lowerThan(
                        series.getMaxTemporaryDuration().get(row), true
                ));
            } else if (hasMinTemporaryDuration) {
                durationCriteria.add(IntervalTemporaryDurationCriterion.greaterThan(
                        series.getMinTemporaryDuration().get(row), true
                ));
            } else {
                durationCriteria.add(new AllTemporaryDurationCriterion());
            }
        }
        reductionBuilder.withLimitDurationCriteria(durationCriteria);
    }

    private void addNetworkElementCriterion(LimitReductionSeries series, int row, LimitReduction.Builder reductionBuilder) {
        AtLeastOneCountryCriterion countryCriterion = series.getCountry() != null ?
                new AtLeastOneCountryCriterion(List.of(Country.valueOf(series.getCountry().get(row)))) : null;
        AtLeastOneNominalVoltageCriterion voltageCriterion = null;
        Double minVoltage = series.getMinVoltage() != null ? series.getMinVoltage().get(row) : null;
        Double maxVoltage = series.getMaxVoltage() != null ? series.getMaxVoltage().get(row) : null;
        if (minVoltage != null && maxVoltage != null) {
            voltageCriterion = new AtLeastOneNominalVoltageCriterion(VoltageInterval.between(
                    minVoltage, maxVoltage, true, true
            ));
        } else if (minVoltage != null) {
            voltageCriterion = new AtLeastOneNominalVoltageCriterion(VoltageInterval.greaterThan(
                    minVoltage, true
            ));
        } else if (maxVoltage != null) {
            voltageCriterion = new AtLeastOneNominalVoltageCriterion(VoltageInterval.lowerThan(
                    maxVoltage, true
            ));
        }
        if (countryCriterion != null || voltageCriterion != null) {
            reductionBuilder.withNetworkElementCriteria(List.of(new IdentifiableCriterion(countryCriterion, voltageCriterion)));
        }
    }
}
