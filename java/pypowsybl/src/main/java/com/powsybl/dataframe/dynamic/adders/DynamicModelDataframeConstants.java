/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public final class DynamicModelDataframeConstants {

    public static final String DYNAMIC_MODEL_ID = "dynamic_model_id";
    public static final String STATIC_ID = "static_id";
    public static final String PARAMETER_SET_ID = "parameter_set_id";
    public static final String MODEL_NAME = "model_name";
    public static final String CONTROLLED_BRANCH = "controlled_branch";
    public static final String I_MEASUREMENT = "i_measurement";
    public static final String I_MEASUREMENT_SIDE = "i_measurement_side";
    public static final String I_MEASUREMENT_1 = "i_measurement_1";
    public static final String I_MEASUREMENT_1_SIDE = "i_measurement_1_side";
    public static final String I_MEASUREMENT_2 = "i_measurement_2";
    public static final String I_MEASUREMENT_2_SIDE = "i_measurement_2_side";
    public static final String GENERATOR = "generator";
    public static final String TRANSFORMER = "transformer";
    public static final String SIDE = "side";
    public static final String MEASUREMENT_POINT_ID = "measurement_point_id";
    public static final String TRANSFORMER_ID = "transformer_id";
    public static final String START_TIME = "start_time";
    public static final String DISCONNECT_ONLY = "disconnect_only";
    public static final String DELTA_P = "delta_p";
    public static final String FAULT_TIME = "fault_time";
    public static final String X_PU = "x_pu";
    public static final String R_PU = "r_pu";
    public static final String PHASE_SHIFTER_ID = "phase_shifter_id";

    private DynamicModelDataframeConstants() {
    }
}
