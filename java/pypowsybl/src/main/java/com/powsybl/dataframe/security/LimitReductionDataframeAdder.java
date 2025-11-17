package com.powsybl.dataframe.security;

import com.powsybl.contingency.ContingencyContext;
import com.powsybl.contingency.ContingencyContextType;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.adders.SeriesUtils;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.LimitType;
import com.powsybl.python.security.SecurityAnalysisContext;
import com.powsybl.security.limitreduction.LimitReduction;

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
            SeriesMetadata.doubles("min_temporary_duration"),
            SeriesMetadata.doubles("max_temporary_duration"),
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
        private final DoubleSeries minTemporaryDuration;
        private final DoubleSeries maxTemporaryDuration;
        private final DoubleSeries value;

        LimitReductionSeries(UpdatingDataframe dataframe) {
            this.limitType = SeriesUtils.getRequiredStrings(dataframe, "limit_type");
            this.monitoring = dataframe.getInts("monitoring");
            this.contingencyContext = SeriesUtils.getRequiredStrings(dataframe, "contingency_context");
            this.minVoltage = dataframe.getDoubles("min_voltage");
            this.maxVoltage = dataframe.getDoubles("max_voltage");
            this.country = dataframe.getStrings("country");
            this.permanent = dataframe.getInts("permanent");
            this.minTemporaryDuration = dataframe.getDoubles("min_temporary_duration");
            this.maxTemporaryDuration = dataframe.getDoubles("max_temporary_duration");
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
                
                context.addLimitReduction(reductionBuilder.build());
            }
        }
    }
}
