from dataclasses import dataclass
from typing import Any


@dataclass
class VariableContext:
    ph_vars: Any
    v_vars: Any
    gen_p_vars: Any
    gen_q_vars: Any
    shunt_p_vars: Any
    shunt_q_vars: Any
    closed_branch_p1_vars: Any
    closed_branch_q1_vars: Any
    closed_branch_p2_vars: Any
    closed_branch_q2_vars: Any
    open_side1_branch_p2_vars: Any
    open_side1_branch_q2_vars: Any
    open_side2_branch_p1_vars: Any
    open_side2_branch_q1_vars: Any
    branch_num_2_index: list[int]
