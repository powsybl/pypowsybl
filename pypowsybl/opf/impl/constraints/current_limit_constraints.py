import logging
from typing import Any

import pyoptinterface as poi

from pypowsybl.opf.impl.model.model import Model
from pypowsybl.opf.impl.model.constraints import Constraints
from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.model.variable_context import VariableContext
from pypowsybl.opf.impl.util import TRACE_LEVEL

logger = logging.getLogger(__name__)

class CurrentLimitConstraints(Constraints):

    @staticmethod
    def _create_limit_constraints_for_side(side: str, branch_row, current_limits, model, p: Any, q: Any):
        if branch_row.Index in current_limits.index:
            limit_row = current_limits.loc[branch_row.Index]
            if branch_row.bus1_id and branch_row.bus2_id:
                i = poi.ExprBuilder()
                i += p * p + q * q
                model.add_quadratic_constraint(i, poi.Leq, limit_row.value * limit_row.value)
                logger.log(TRACE_LEVEL,
                           f"Add side {side} current limit constraint (value={limit_row.value}) to branch '{branch_row.Index}'")

    def add(self, parameters: ModelParameters, network_cache: NetworkCache,
            variable_context: VariableContext, model: Model) -> None:
        current_limits1 = network_cache.current_limits1
        current_limits2 = network_cache.current_limits2
        for branch_num, branch_row in enumerate(network_cache.lines.itertuples()):
            branch_index = variable_context.branch_num_2_index[branch_num]

            self._create_limit_constraints_for_side('1', branch_row, current_limits1, model,
                                                    variable_context.closed_branch_p1_vars[branch_index],
                                                    variable_context.closed_branch_q1_vars[branch_index])
            self._create_limit_constraints_for_side('2', branch_row, current_limits2, model,
                                                    variable_context.closed_branch_p2_vars[branch_index],
                                                    variable_context.closed_branch_q2_vars[branch_index])
