#
# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from math import hypot, atan2
from typing import cast

from pypowsybl.opf.impl.model.model import Model
from pypowsybl.opf.impl.constraints.branch_flow_constraints import BranchFlowConstraints
from pypowsybl.opf.impl.model.constraints import Constraints
from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.model.variable_context import VariableContext
from pypowsybl.opf.impl.util import BoundaryLineRow


class BoundaryLineFlowConstraints(Constraints):
    def add(self, parameters: ModelParameters, network_cache: NetworkCache,
            variable_context: VariableContext, model: Model) -> None:
        for bl_num, row in enumerate(cast(list[BoundaryLineRow], network_cache.boundary_lines.itertuples(index=False))):
            r, x, g, b, bus_id = row.r, row.x, row.g, row.b, row.bus_id
            if bus_id:
                g1 = g
                b1 = b
                g2 = 0
                b2 = 0
                r1 = 1.0
                a1 = 0.0
                bl_index = variable_context.bl_num_2_index[bl_num]
                z = hypot(r, x)
                y = 1.0 / z
                ksi = atan2(r, x)
                bus_num = network_cache.buses.index.get_loc(bus_id)
                v1_var = variable_context.v_vars[bus_num]
                ph1_var = variable_context.ph_vars[bus_num]
                v2_var = variable_context.bl_v_vars[bl_index]
                ph2_var = variable_context.bl_ph_vars[bl_index]
                p1_var = variable_context.bl_branch_p1_vars[bl_index]
                q1_var = variable_context.bl_branch_q1_vars[bl_index]
                p2_var = variable_context.bl_branch_p2_vars[bl_index]
                q2_var = variable_context.bl_branch_q2_vars[bl_index]

                BranchFlowConstraints.add_closed_branch_constraint(a1, b1, b2, g1, g2, ksi, model, p1_var, p2_var, ph1_var, ph2_var, q1_var, q2_var, r1, v1_var, v2_var, y)
