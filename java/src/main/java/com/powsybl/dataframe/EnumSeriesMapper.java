/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe;

import com.powsybl.iidm.network.*;
import com.powsybl.iidm.parameters.ParameterType;
import com.powsybl.security.LimitViolationType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public class EnumSeriesMapper<T, E extends Enum<E>> implements SeriesMapper<T> {

    private final SeriesMetadata metadata;
    private final IntSeriesMapper.IntUpdater<T> updater;
    private final ToIntFunction<T> value;
    private static final Map<Class, String> ENUM_CLASS_MAP = new HashMap<>();

    static {
        ENUM_CLASS_MAP.put(Country.class, "country");
        ENUM_CLASS_MAP.put(Branch.Side.class, "branch_side");
        ENUM_CLASS_MAP.put(ThreeWindingsTransformer.Side.class, "three_windings_transformer_side");
        ENUM_CLASS_MAP.put(EnergySource.class, "energy_source");
        ENUM_CLASS_MAP.put(StaticVarCompensator.RegulationMode.class, "static_var_compensator_regulation_mode");
        ENUM_CLASS_MAP.put(PhaseTapChanger.RegulationMode.class, "phase_tap_changer_regulation_mode");
        ENUM_CLASS_MAP.put(ShuntCompensatorModelType.class, "shunt_compensator_model_type");
        ENUM_CLASS_MAP.put(LoadType.class, "load_type");
        ENUM_CLASS_MAP.put(SwitchKind.class, "switch_kind");
        ENUM_CLASS_MAP.put(HvdcLine.ConvertersMode.class, "converters_mode");
        ENUM_CLASS_MAP.put(LimitType.class, "limit_type");
        ENUM_CLASS_MAP.put(ParameterType.class, "parameter_type");
        ENUM_CLASS_MAP.put(LimitViolationType.class, "limit_violation_type");
    }

    public EnumSeriesMapper(String name, Class<E> enumClass, ToIntFunction<T> value) {
        this(name, enumClass, value, null, true);
    }

    public EnumSeriesMapper(String name, Class<E> enumClass, ToIntFunction<T> value, IntSeriesMapper.IntUpdater<T> updater, boolean defaultAttribute) {
        this.metadata = new SeriesMetadata(false, name, updater != null, SeriesDataType.ENUM, defaultAttribute, ENUM_CLASS_MAP.get(enumClass));
        this.updater = updater;
        this.value = value;
    }

    @Override
    public SeriesMetadata getMetadata() {
        return metadata;
    }

    @Override
    public void createSeries(List<T> items, DataframeHandler factory) {
        DataframeHandler.EnumSeriesWriter writer = factory.newEnumIndex(metadata.getName(), items.size());
        for (int i = 0; i < items.size(); i++) {
            writer.set(i, value.applyAsInt(items.get(i)));
        }
    }

    @Override
    public void updateInt(T object, int value) {
        if (updater == null) {
            throw new UnsupportedOperationException("Series '" + getMetadata().getName() + "' is not modifiable.");
        }
        updater.update(object, value);
    }
}
