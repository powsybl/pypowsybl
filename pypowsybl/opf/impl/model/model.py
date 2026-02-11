from __future__ import annotations

from typing import Protocol, Any

import pyoptinterface as poi

from pypowsybl.opf.impl.model.model_parameters import ModelParameters, SolverType


class Model(Protocol):
    def set_raw_parameter(self, name: str, value: Any) -> None: ...

    def set_model_attribute(self, attr: poi.ModelAttribute, value: Any) -> None: ...

    def get_model_attribute(self, attr: poi.ModelAttribute) -> Any: ...

    def add_m_variables(self, n: int, name: str | None = None): ...  # returns a list/array of poi.Variable

    def add_linear_constraint(self, expr: poi.ExprBuilder, sense: Any, rhs: float) -> None: ...

    def set_objective(self, expr: poi.ExprBuilder) -> None: ...

    def get_value(self, var: Any) -> float: ...

    def set_variable_bounds(self, var: Any, lb: float, ub: float) -> None: ...

    def set_variable_start(self, var: Any, value: float) -> None: ...

    def add_nl_constraint(self, constraint: Any) -> None: ...

    def add_quadratic_constraint(self, *args: Any, **kwargs: Any) -> None: ...

    def optimize(self) -> None: ...


class IpoptModel(Model):
    def __init__(self):
        from pyoptinterface import ipopt
        self._model = ipopt.Model()
        self._model.set_raw_parameter("print_user_options", "yes")
        self._model.set_raw_parameter("print_advanced_options", "yes")
        self._model.set_model_attribute(poi.ModelAttribute.Silent, False)

    def set_raw_parameter(self, name: str, value: Any) -> None:
        self._model.set_raw_parameter(name, value)

    def set_model_attribute(self, attr: poi.ModelAttribute, value: Any) -> None:
        self._model.set_model_attribute(attr, value)

    def get_model_attribute(self, attr: poi.ModelAttribute) -> Any:
        return self._model.get_model_attribute(attr)

    def add_m_variables(self, n: int, name: str | None = None):
        return self._model.add_m_variables(n, name=name)

    def add_linear_constraint(self, expr: poi.ExprBuilder, sense: Any, rhs: float) -> None:
        self._model.add_linear_constraint(expr, sense, rhs)

    def set_objective(self, expr: poi.ExprBuilder) -> None:
        self._model.set_objective(expr)

    def get_value(self, var: Any) -> float:
        return self._model.get_value(var)

    def set_variable_bounds(self, var: Any, lb: float, ub: float) -> None:
        self._model.set_variable_bounds(var, lb, ub)

    def set_variable_start(self, var: Any, value: float) -> None:
        self._model.set_variable_start(var, value)

    def add_nl_constraint(self, constraint: Any) -> None:
        self._model.add_nl_constraint(constraint)

    def add_quadratic_constraint(self, *args: Any, **kwargs: Any) -> None:
        self._model.add_quadratic_constraint(*args, **kwargs)

    def optimize(self) -> None:
        self._model.optimize()


class KnitroModel(Model):
    def __init__(self):
        from pyoptinterface import knitro
        self._model = knitro.Model()

    def set_raw_parameter(self, name: str, value: Any) -> None:
        self._model.set_raw_parameter(name, value)

    def set_model_attribute(self, attr: poi.ModelAttribute, value: Any) -> None:
        self._model.set_model_attribute(attr, value)

    def get_model_attribute(self, attr: poi.ModelAttribute) -> Any:
        return self._model.get_model_attribute(attr)

    def add_m_variables(self, n: int, name: str | None = None):
        return self._model.add_m_variables(n, name=name)

    def add_linear_constraint(self, expr: poi.ExprBuilder, sense: Any, rhs: float) -> None:
        self._model.add_linear_constraint(expr, sense, rhs)

    def set_objective(self, expr: poi.ExprBuilder) -> None:
        self._model.set_objective(expr)

    def get_value(self, var: Any) -> float:
        return self._model.get_value(var)

    def set_variable_bounds(self, var: Any, lb: float, ub: float) -> None:
        self._model.set_variable_bounds(var, lb, ub)

    def set_variable_start(self, var: Any, value: float) -> None:
        self._model.set_variable_start(var, value)

    def add_nl_constraint(self, constraint: Any) -> None:
        self._model.add_nl_constraint(constraint)

    def add_quadratic_constraint(self, *args: Any, **kwargs: Any) -> None:
        self._model.add_quadratic_constraint(*args, **kwargs)

    def optimize(self) -> None:
        self._model.optimize()


def create_model(solver_type: SolverType) -> Model:
    if solver_type == SolverType.IPOPT:
        return IpoptModel()
    elif solver_type == SolverType.KNITRO:
        return KnitroModel()
    else:
        raise ValueError(f"Unsupported solver type: {solver_type}")
