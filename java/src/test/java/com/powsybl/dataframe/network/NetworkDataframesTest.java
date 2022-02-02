/**
 * Copyright (c) 2021-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network;

import com.powsybl.dataframe.DataframeElementType;
import com.powsybl.dataframe.DataframeFilter;
import com.powsybl.dataframe.DoubleIndexedSeries;
import com.powsybl.dataframe.impl.DefaultDataframeHandler;
import com.powsybl.dataframe.impl.Series;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.powsybl.dataframe.DataframeElementType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
class NetworkDataframesTest {

    private static List<Series> createDataFrame(DataframeElementType type, Network network) {
        return createDataFrame(type, network, new DataframeFilter());
    }

    private static List<Series> createDataFrame(DataframeElementType type, Network network, DataframeFilter dataframeFilter) {
        List<Series> series = new ArrayList<>();
        NetworkDataframeMapper mapper = NetworkDataframes.getDataframeMapper(type);
        assertNotNull(mapper);
        mapper.createDataframe(network, new DefaultDataframeHandler(series::add), dataframeFilter);
        return series;
    }

    private DoubleIndexedSeries createInput(List<String> names, double... values) {
        return new DoubleIndexedSeries() {
            @Override
            public int getSize() {
                return names.size();
            }

            @Override
            public String getId(int index) {
                return names.get(index);
            }

            @Override
            public double getValue(int index) {
                return values[index];
            }
        };
    }

    @Test
    void buses() {
        Network network = EurostagTutorialExample1Factory.create();
        List<Series> series = createDataFrame(BUS, network);

        assertThat(series)
            .extracting(Series::getName)
            .containsExactly("id", "name", "v_mag", "v_angle", "connected_component", "synchronous_component", "voltage_level_id");
        assertThat(series.get(2).getDoubles())
            .containsExactly(Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        assertThat(series.get(4).getInts())
            .containsExactly(0, 0, 0, 0);
        assertThat(series.get(4).getInts())
            .containsExactly(0, 0, 0, 0);
        assertThat(series.get(6).getStrings())
            .containsExactly("VLGEN", "VLHV1", "VLHV2", "VLLOAD");
    }

    @Test
    void generators() {
        Network network = EurostagTutorialExample1Factory.create();
        List<Series> series = createDataFrame(GENERATOR, network);

        assertThat(series)
            .extracting(Series::getName)
            .containsExactly("id", "name", "energy_source", "target_p", "min_p", "max_p", "min_q", "max_q", "target_v",
                "target_q", "voltage_regulator_on", "regulated_element_id",  "p", "q", "i", "voltage_level_id", "bus_id", "connected");

        assertThat(series.get(3).getDoubles())
            .containsExactly(607);
    }

    @Test
    void batteries() {
        Network network = EurostagTutorialExample1Factory.create();
        List<Series> series = createDataFrame(BATTERY, network);
        assertThat(series)
            .extracting(Series::getName)
            .containsExactly("id", "name", "max_p", "min_p", "p0", "q0", "p", "q", "i", "voltage_level_id", "bus_id", "connected");
    }

    @Test
    void loads() {
        Network network = EurostagTutorialExample1Factory.create();
        List<Series> series = createDataFrame(LOAD, network);

        assertThat(series)
            .extracting(Series::getName)
            .containsExactly("id", "name", "type", "p0", "q0", "p", "q", "i", "voltage_level_id", "bus_id", "connected");
    }

    @Test
    void danglingLines() {
        Network network = EurostagTutorialExample1Factory.create();
        List<Series> series = createDataFrame(DANGLING_LINE, network);

        assertThat(series)
            .extracting(Series::getName)
            .containsExactly("id", "name", "r", "x", "g", "b", "p0", "q0", "p", "q", "i", "voltage_level_id", "bus_id", "connected", "ucte-x-node-code");
    }

    @Test
    void lines() {
        Network network = EurostagTutorialExample1Factory.create();
        List<Series> series = createDataFrame(LINE, network);

        assertThat(series)
            .extracting(Series::getName)
            .containsExactly("id", "name", "r", "x", "g1", "b1", "g2", "b2", "p1", "q1", "i1", "p2", "q2", "i2",
                "voltage_level1_id", "voltage_level2_id", "bus1_id", "bus2_id", "connected1", "connected2");
    }

    @Test
    void shunts() {
        Network network = EurostagTutorialExample1Factory.create();
        List<Series> series = createDataFrame(SHUNT_COMPENSATOR, network);

        assertThat(series)
            .extracting(Series::getName)
            .containsExactly("id", "name", "g", "b", "model_type", "max_section_count", "section_count", "voltage_regulation_on", "" +
                "target_v", "target_deadband", "regulating_bus_id", "p", "q", "i", "voltage_level_id", "bus_id", "connected");
    }

    @Test
    void lccs() {
        Network network = EurostagTutorialExample1Factory.create();
        List<Series> series = createDataFrame(LCC_CONVERTER_STATION, network);

        assertThat(series)
            .extracting(Series::getName)
            .containsExactly("id", "name", "power_factor", "loss_factor", "p", "q", "i", "voltage_level_id", "bus_id", "connected");
    }

    @Test
    void vscs() {
        Network network = EurostagTutorialExample1Factory.create();
        List<Series> series = createDataFrame(VSC_CONVERTER_STATION, network);

        assertThat(series)
            .extracting(Series::getName)
            .containsExactly("id", "name", "loss_factor", "target_v", "target_q", "voltage_regulator_on",
                "p", "q", "i", "voltage_level_id", "bus_id", "connected");
    }

    @Test
    void twoWindingTransformers() {
        Network network = EurostagTutorialExample1Factory.create();
        List<Series> series = createDataFrame(TWO_WINDINGS_TRANSFORMER, network);

        assertThat(series)
            .extracting(Series::getName)
            .containsExactly("id", "name", "r", "x", "g", "b", "rated_u1", "rated_u2", "rated_s", "p1", "q1", "i1", "p2", "q2", "i2",
                "voltage_level1_id", "voltage_level2_id", "bus1_id", "bus2_id", "connected1", "connected2");
    }

    @Test
    void threeWindingTransformers() {
        Network network = EurostagTutorialExample1Factory.create();
        List<Series> series = createDataFrame(THREE_WINDINGS_TRANSFORMER, network);

        assertThat(series)
            .extracting(Series::getName)
            .containsExactly("id", "name", "rated_u0",
                "r1", "x1", "g1", "b1", "rated_u1", "rated_s1", "ratio_tap_position1", "phase_tap_position1", "p1", "q1", "i1", "voltage_level1_id", "bus1_id", "connected1",
                "r2", "x2", "g2", "b2", "rated_u2", "rated_s2", "ratio_tap_position2", "phase_tap_position2", "p2", "q2", "i2", "voltage_level2_id", "bus2_id", "connected2",
                "r3", "x3", "g3", "b3", "rated_u3", "rated_s3", "ratio_tap_position3", "phase_tap_position3", "p3", "q3", "i3", "voltage_level3_id", "bus3_id", "connected3");
    }

    @Test
    void hvdcs() {
        Network network = EurostagTutorialExample1Factory.create();
        List<Series> series = createDataFrame(HVDC_LINE, network);

        assertThat(series)
            .extracting(Series::getName)
            .containsExactly("id", "name", "converters_mode", "target_p", "max_p", "nominal_v", "r",
                "converter_station1_id", "converter_station2_id", "connected1", "connected2");
    }

    @Test
    void svcs() {
        Network network = EurostagTutorialExample1Factory.create();
        List<Series> series = createDataFrame(STATIC_VAR_COMPENSATOR, network);

        assertThat(series)
            .extracting(Series::getName)
            .containsExactly("id", "name", "b_min", "b_max", "target_v", "target_q", "regulation_mode", "p", "q", "i", "voltage_level_id", "bus_id", "connected");
    }

    @Test
    void substations() {
        Network network = EurostagTutorialExample1Factory.create();
        List<Series> series = createDataFrame(SUBSTATION, network);

        assertThat(series)
            .extracting(Series::getName)
            .containsExactly("id", "name", "TSO", "geo_tags", "country");
    }

    @Test
    void properties() {
        Network network = EurostagTutorialExample1Factory.create();
        network.getSubstation("P1").setProperty("prop1", "val1");
        network.getSubstation("P2").setProperty("prop2", "val2");
        List<Series> series = createDataFrame(SUBSTATION, network,
                new DataframeFilter(DataframeFilter.AttributeFilterType.ALL_ATTRIBUTES, Collections.emptyList()));

        assertThat(series)
            .extracting(Series::getName)
            .containsExactly("id", "name", "TSO", "geo_tags", "country", "prop1", "prop2");
    }

    @Test
    void voltageLevels() {
        Network network = EurostagTutorialExample1Factory.create();
        List<Series> series = createDataFrame(VOLTAGE_LEVEL, network);

        assertThat(series)
            .extracting(Series::getName)
            .containsExactly("id", "name", "substation_id", "nominal_v", "high_voltage_limit", "low_voltage_limit");
    }

    @Test
    void ratioTapChangerSteps() {
        Network network = EurostagTutorialExample1Factory.create();
        List<Series> series = createDataFrame(RATIO_TAP_CHANGER_STEP, network);

        assertThat(series)
            .extracting(Series::getName)
            .containsExactly("id", "position", "rho", "r", "x", "g", "b");
    }

    @Test
    void phaseTapChangerSteps() {
        Network network = EurostagTutorialExample1Factory.create();
        List<Series> series = createDataFrame(PHASE_TAP_CHANGER_STEP, network);

        assertThat(series)
            .extracting(Series::getName)
            .containsExactly("id", "position", "rho", "alpha", "r", "x", "g", "b");
    }

    @Test
    void ratioTapChangers() {
        Network network = EurostagTutorialExample1Factory.create();
        List<Series> series = createDataFrame(RATIO_TAP_CHANGER, network);

        assertThat(series)
            .extracting(Series::getName)
            .containsExactly("id", "tap", "low_tap", "high_tap", "step_count", "on_load", "regulating", "target_v",
                    "target_deadband", "regulating_bus_id", "rho", "alpha");
    }

    @Test
    void phaseTapChangers() {
        Network network = EurostagTutorialExample1Factory.create();
        List<Series> series = createDataFrame(PHASE_TAP_CHANGER, network);

        assertThat(series)
            .extracting(Series::getName)
            .containsExactly("id", "tap", "low_tap", "high_tap", "step_count", "regulating", "regulation_mode",
                    "regulation_value", "target_deadband", "regulating_bus_id");
    }

    @Test
    void reactiveLimits() {
        Network network = EurostagTutorialExample1Factory.create();
        List<Series> series = createDataFrame(REACTIVE_CAPABILITY_CURVE_POINT, network);

        assertThat(series)
            .extracting(Series::getName)
            .containsExactly("id", "num", "p", "min_q", "max_q");
    }

    @Test
    void attributesFiltering() {
        Network network = EurostagTutorialExample1Factory.create();
        network.getSubstation("P1").setProperty("prop1", "val1");
        network.getSubstation("P2").setProperty("prop2", "val2");
        List<Series> seriesDefaults = createDataFrame(SUBSTATION, network,
                new DataframeFilter());
        assertThat(seriesDefaults)
                .extracting(Series::getName)
                .containsExactly("id", "name", "TSO", "geo_tags", "country");

        List<Series> seriesAll = createDataFrame(SUBSTATION, network,
                new DataframeFilter(DataframeFilter.AttributeFilterType.ALL_ATTRIBUTES, Collections.emptyList()));
        assertThat(seriesAll)
                .extracting(Series::getName)
                .containsExactly("id", "name", "TSO", "geo_tags", "country", "prop1", "prop2");

        List<Series> seriesAttributesSubset = createDataFrame(SUBSTATION, network,
                new DataframeFilter(DataframeFilter.AttributeFilterType.INPUT_ATTRIBUTES,
                        List.of("name",  "name", "geo_tags", "prop1")));
        assertThat(seriesAttributesSubset)
                .extracting(Series::getName)
                .containsExactly("id", "name", "geo_tags", "prop1");
    }

    @Test
    void twoWindingsTransformersFilteredRows() {
        Network network = FourSubstationsNodeBreakerFactory.create();
        List<Series> series = createDataFrame(TWO_WINDINGS_TRANSFORMER, network,
                new DataframeFilter(List.of("TWT"), Collections.emptyList()));
        assertThat(series.get(0).getStrings()).containsExactly("TWT");
    }

    @Test
    void threeWindingsTransformersFilteredRows() {
        Network network = ThreeWindingsTransformerNetworkFactory.create();
        List<Series> series = createDataFrame(THREE_WINDINGS_TRANSFORMER, network,
                new DataframeFilter(List.of("3WT"), Collections.emptyList()));
        assertThat(series.get(0).getStrings()).containsExactly("3WT");
    }

    @Test
    void shuntsCompensatorFilteredRows() {
        Network network = ShuntTestCaseFactory.create();
        List<Series> series = createDataFrame(SHUNT_COMPENSATOR, network,
                new DataframeFilter(List.of("SHUNT"), Collections.emptyList()));
        assertThat(series.get(0).getStrings()).containsExactly("SHUNT");
    }

    @Test
    void batteriesFilteredRows() {
        Network network = BatteryNetworkFactory.create();
        List<Series> series = createDataFrame(BATTERY, network,
                new DataframeFilter(List.of("BAT2"), Collections.emptyList()));
        assertThat(series.get(0).getStrings()).containsExactly("BAT2");
    }

    @Test
    void busbarSectionsFilteredRows() {
        Network network = FourSubstationsNodeBreakerFactory.create();
        List<Series> series = createDataFrame(BUSBAR_SECTION, network,
                new DataframeFilter(List.of("S3VL1_BBS"), Collections.emptyList()));
        assertThat(series.get(0).getStrings()).containsExactly("S3VL1_BBS");
    }

    @Test
    void busesFilteredRows() {
        Network network = FourSubstationsNodeBreakerFactory.create();
        List<Series> series = createDataFrame(BUS, network,
                new DataframeFilter(List.of("S3VL1_0"), Collections.emptyList()));
        assertThat(series.get(0).getStrings()).containsExactly("S3VL1_0");
    }

    @Test
    void danglingLinesFilteredRows() {
        Network network = DanglingLineNetworkFactory.create();
        List<Series> series = createDataFrame(DANGLING_LINE, network,
                new DataframeFilter(List.of("DL"), Collections.emptyList()));
        assertThat(series.get(0).getStrings()).containsExactly("DL");
    }

    @Test
    void generatorsFilteredRows() {
        Network network = EurostagTutorialExample1Factory.create();
        List<Series> series = createDataFrame(GENERATOR, network,
                new DataframeFilter(List.of("GEN"), Collections.emptyList()));
        assertThat(series.get(0).getStrings()).containsExactly("GEN");
    }

    @Test
    void hvdcLinesFilteredRows() {
        Network network = FourSubstationsNodeBreakerFactory.create();
        List<Series> series = createDataFrame(HVDC_LINE, network,
                new DataframeFilter(List.of("HVDC1"), Collections.emptyList()));
        assertThat(series.get(0).getStrings()).containsExactly("HVDC1");
    }

    @Test
    void lccConverterStationsFilteredRows() {
        Network network = FourSubstationsNodeBreakerFactory.create();
        List<Series> series = createDataFrame(LCC_CONVERTER_STATION, network,
                new DataframeFilter(List.of("LCC2"), Collections.emptyList()));
        assertThat(series.get(0).getStrings()).containsExactly("LCC2");
    }

    @Test
    void linearShuntCompensatorSectionsFilteredRows() {
        Network network = FourSubstationsNodeBreakerFactory.create();
        List<Series> series = createDataFrame(LINEAR_SHUNT_COMPENSATOR_SECTION, network,
                new DataframeFilter(List.of("SHUNT"), Collections.emptyList()));
        assertThat(series.get(0).getStrings()).containsExactly("SHUNT");
    }

    @Test
    void linesFilteredRows() {
        Network network = FourSubstationsNodeBreakerFactory.create();
        List<Series> series = createDataFrame(LINE, network,
                new DataframeFilter(List.of("LINE_S3S4"), Collections.emptyList()));
        assertThat(series.get(0).getStrings()).containsExactly("LINE_S3S4");
    }

    @Test
    void loadsFilteredRows() {
        Network network = FourSubstationsNodeBreakerFactory.create();
        List<Series> series = createDataFrame(LOAD, network,
                new DataframeFilter(List.of("LD4"), Collections.emptyList()));
        assertThat(series.get(0).getStrings()).containsExactly("LD4");
    }

    @Test
    void nonLinearShuntCompensatorSectionFilteredRows() {
        Network network = ShuntTestCaseFactory.createNonLinear();
        List<Series> series = createDataFrame(NON_LINEAR_SHUNT_COMPENSATOR_SECTION, network,
                new DataframeFilter(List.of("SHUNT"), List.of(1)));
        assertThat(series.get(0).getStrings()).containsExactly("SHUNT");
        assertThat(series.get(1).getInts()).containsExactly(1);
    }

    @Test
    void phaseTapChangerStepsFilteredRows() {
        Network network = FourSubstationsNodeBreakerFactory.create();
        List<Series> series = createDataFrame(PHASE_TAP_CHANGER_STEP, network,
                new DataframeFilter(List.of("TWT"), List.of(6)));
        assertThat(series.get(0).getStrings()).containsExactly("TWT");
        assertThat(series.get(1).getInts()).containsExactly(6);
    }

    @Test
    void phaseTapChangersFilteredRows() {
        Network network = FourSubstationsNodeBreakerFactory.create();
        List<Series> series = createDataFrame(PHASE_TAP_CHANGER, network,
                new DataframeFilter(List.of("TWT"), Collections.emptyList()));
        assertThat(series.get(0).getStrings()).containsExactly("TWT");
    }

    @Test
    void ratioTapChangerStepsFilteredRows() {
        Network network = EurostagTutorialExample1Factory.create();
        List<Series> series = createDataFrame(RATIO_TAP_CHANGER_STEP, network,
                new DataframeFilter(List.of("NHV2_NLOAD", "NHV2_NLOAD"), List.of(0, 2)));
        assertThat(series.get(0).getStrings()).containsExactly("NHV2_NLOAD", "NHV2_NLOAD");
        assertThat(series.get(1).getInts()).containsExactly(0, 2);
    }

    @Test
    void ratioTapChangersFilteredRows() {
        Network network = EurostagTutorialExample1Factory.create();
        List<Series> series = createDataFrame(RATIO_TAP_CHANGER, network,
                new DataframeFilter(List.of("NHV2_NLOAD"), Collections.emptyList()));
        assertThat(series.get(0).getStrings()).containsExactly("NHV2_NLOAD");
    }

    @Test
    void reactiveCapabilityCurvePointsFilteredRows() {
        Network network = FourSubstationsNodeBreakerFactory.create();
        List<Series> series = createDataFrame(REACTIVE_CAPABILITY_CURVE_POINT, network,
                new DataframeFilter(List.of("GH3"), List.of(1)));
        assertThat(series.get(0).getStrings()).containsExactly("GH3");
        assertThat(series.get(1).getInts()).containsExactly(1);
    }

    @Test
    void staticVarCompensatorsFilteredRows() {
        Network network = FourSubstationsNodeBreakerFactory.create();
        List<Series> series = createDataFrame(STATIC_VAR_COMPENSATOR, network,
                new DataframeFilter(List.of("SVC"), Collections.emptyList()));
        assertThat(series.get(0).getStrings()).containsExactly("SVC");
    }

    @Test
    void substationsFilteredRows() {
        Network network = EurostagTutorialExample1Factory.create();
        List<Series> series = createDataFrame(SUBSTATION, network,
                new DataframeFilter(List.of("P2"), Collections.emptyList()));
        assertThat(series.get(0).getStrings()).containsExactly("P2");
    }

    @Test
    void switchesFilteredRows() {
        Network network = FourSubstationsNodeBreakerFactory.create();
        List<Series> series = createDataFrame(SWITCH, network,
                new DataframeFilter(List.of("S1VL2_GH1_BREAKER"), Collections.emptyList()));
        assertThat(series.get(0).getStrings()).containsExactly("S1VL2_GH1_BREAKER");
    }

    @Test
    void voltageLevelsFilteredRows() {
        Network network = FourSubstationsNodeBreakerFactory.create();
        List<Series> series = createDataFrame(VOLTAGE_LEVEL, network,
                new DataframeFilter(List.of("S2VL1"), Collections.emptyList()));
        assertThat(series.get(0).getStrings()).containsExactly("S2VL1");
    }

    @Test
    void vscConverterStationsFilteredRows() {
        Network network = FourSubstationsNodeBreakerFactory.create();
        List<Series> series = createDataFrame(VSC_CONVERTER_STATION, network,
                new DataframeFilter(List.of("VSC2"), Collections.emptyList()));
        assertThat(series.get(0).getStrings()).containsExactly("VSC2");
    }

}
