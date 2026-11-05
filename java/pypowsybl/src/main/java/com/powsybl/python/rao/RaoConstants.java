/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.rao;

/**
 * Column names and values of the RAO / CRAC dataframes exchanged with Python.
 *
 * @author Bertrand Rix {@literal <bertrand.rix at artelys.com>}
 */
public final class RaoConstants {

    // identifiers
    public static final String INDEX = "index";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String OPERATOR = "operator";
    public static final String BORDER = "border";
    public static final String TSO = "tso";
    public static final String COUNTRY = "country";
    public static final String GROUP_ID = "group_id";
    public static final String NETWORK_ELEMENT_ID = "network_element_id";
    public static final String EXPORTING_NETWORK_ELEMENT_ID = "exporting_network_element_id";
    public static final String IMPORTING_NETWORK_ELEMENT_ID = "importing_network_element_id";
    public static final String REMEDIAL_ACTION_ID = "remedial_action_id";
    public static final String CNEC_ID = "cnec_id";
    public static final String CONTINGENCY = "contingency";
    public static final String CONTINGENCY_ID = "contingency_id";
    public static final String KEY = "key";
    public static final String DISTRIBUTION_KEY = "distribution_key";
    public static final String EXPORTING_COUNTRY = "exporting_country";
    public static final String IMPORTING_COUNTRY = "importing_country";
    public static final String EXPORTING_AREA = "exporting_area";
    public static final String IMPORTING_AREA = "importing_area";

    // instants
    public static final String INSTANT = "instant";
    public static final String OPTIMIZED_INSTANT = "optimized_instant";
    public static final String INITIAL_INSTANT = "initial";
    public static final String KIND = "kind";
    public static final String ORDER = "order";

    // CNECs and thresholds
    public static final String OPTIMIZED = "optimized";
    public static final String MONITORED = "monitored";
    public static final String RELIABILITY_MARGIN = "reliability_margin";
    public static final String MARGIN = "margin";
    public static final String UNIT = "unit";
    public static final String SIDE = "side";
    public static final String FLOW = "flow";
    public static final String RELATIVE_MARGIN = "relative_margin";
    public static final String COMMERCIAL_FLOW = "commercial_flow";
    public static final String LOOP_FLOW = "loop_flow";
    public static final String PTDF_ZONAL_SUM = "ptdf_zonal_sum";
    public static final String ANGLE = "angle";
    public static final String MIN_VOLTAGE = "min_voltage";
    public static final String MAX_VOLTAGE = "max_voltage";

    // ranges
    public static final String MIN = "min";
    public static final String MAX = "max";
    public static final String LOWER_BOUND = "lower_bound";
    public static final String UPPER_BOUND = "upper_bound";
    public static final String RANGE_TYPE = "range_type";

    // remedial actions
    public static final String SPEED = "speed";
    public static final String ACTIVATION_COST = "activation_cost";
    public static final String VARIATION_COST_UP = "variation_cost_up";
    public static final String VARIATION_COST_DOWN = "variation_cost_down";
    public static final String UPWARD_VARIATION_COST = "upward_variation_cost";
    public static final String DOWNWARD_VARIATION_COST = "downward_variation_cost";
    public static final String INITIAL_SET_POINT = "initial_set_point";
    public static final String INITIAL_TAP = "initial_tap";
    public static final String OPTIMIZED_TAP = "optimized_tap";
    public static final String OPTIMIZED_SET_POINT = "optimized_set_point";

    // usage rules
    public static final String USAGE_RULE_TYPE = "usage_rule_type";
    public static final String ON_INSTANT = "on_instant";
    public static final String ON_CONTINGENCY_STATE = "on_contingency_state";
    public static final String ON_CONSTRAINT = "on_constraint";
    public static final String ON_FLOW_CONSTRAINT_IN_COUNTRY = "on_flow_constraint_in_country";

    // elementary actions
    public static final String ACTION_TYPE = "action_type";
    public static final String OPEN = "open";
    public static final String CLOSE = "close";
    public static final String ACTIVE_POWER_VALUE = "active_power_value";
    public static final String TAP_POSITION = "tap_position";
    public static final String SECTION_COUNT = "section_count";
    public static final String TERMINAL_ID = "terminal_id";
    public static final String PHASE_TAP_CHANGER_ID = "phase_tap_changer_id";
    public static final String GENERATOR_ID = "generator_id";
    public static final String LOAD_ID = "load_id";
    public static final String DANGLING_LINE_ID = "dangling_line_id";
    public static final String SHUNT_COMPENSATOR_ID = "shunt_compensator_id";
    public static final String SWITCH_ID = "switch_id";
    public static final String SWITCH_TO_OPEN_ID = "switch_to_open_id";
    public static final String SWITCH_TO_CLOSE_ID = "switch_to_close_id";

    // usage limits
    public static final String VALUE = "value";
    public static final String MAX_REMEDIAL_ACTIONS = "max_remedial_actions";
    public static final String MAX_TSOS = "max_tsos";
    public static final String LIMIT_TYPE = "limit_type";
    public static final String MAX_TOPOLOGICAL_ACTIONS_PER_TSO = "max_topological_actions_per_tso";
    public static final String MAX_PSTS_PER_TSO = "max_psts_per_tso";
    public static final String MAX_REMEDIAL_ACTIONS_PER_TSO = "max_remedial_actions_per_tso";
    public static final String MAX_ELEMENTARY_ACTIONS_PER_TSO = "max_elementary_actions_per_tso";

    // costs
    public static final String FUNCTIONAL_COST = "functional_cost";
    public static final String VIRTUAL_COST = "virtual_cost";
    public static final String COST = "cost";
    public static final String GLOBAL_FUNCTIONAL_COST = "global_functional_cost";
    public static final String GLOBAL_VIRTUAL_COST = "global_virtual_cost";
    public static final String GLOBAL_COST = "global_cost";

    private RaoConstants() {
    }
}
