# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import Union

from pypowsybl import _pypowsybl as _pp
from pypowsybl._pypowsybl import OutputVariableType # pylint: disable=protected-access
import warnings


class OutputVariableMapping:
    """
    Class to map Curves and Final State Values
    """

    def __init__(self) -> None:
        self._handle = _pp.create_timeseries_mapping()

    def add_curves(self, model_id: str, variables: Union[list[str], str]) -> None:
        """
        Adds curves on a single dynamic model or network equipment without dynamic model

        :param model_id: dynamic model or equipment id
        :param variables: single element or list of variables names to record
        """
        if isinstance(variables, str):
            variables = [variables]
        self._add_output_variables(model_id, variables, OutputVariableType.CURVE)


    def add_dynamic_model_curves(self, dynamic_model_id: str, variables: Union[list[str], str]) -> None:
        """
        .. deprecated:: 1.15.0
            Use :func:`add_curves` instead.
        """
        warnings.warn("add_dynamic_model_curves is deprecated, use add_curves instead", DeprecationWarning)
        self.add_curves(dynamic_model_id, variables)

    def add_standard_model_curves(self, static_id: str, variables: Union[list[str], str]) -> None:
        """
        .. deprecated:: 1.15.0
            Use :func:`add_curves` instead.
        """
        warnings.warn("add_standard_model_curves is deprecated, use add_curves instead", DeprecationWarning)
        self.add_curves(static_id, variables)

    def add_final_state_values(self, model_id: str, variables: Union[list[str], str]) -> None:
        """
        Adds final state values on a single dynamic model or network equipment without dynamic model

        :param model_id: dynamic model or equipment id
        :param variables: single element or list of variables names to record
        """
        if isinstance(variables, str):
            variables = [variables]
        self._add_output_variables(model_id, variables, OutputVariableType.FINAL_STATE)

    def add_dynamic_model_final_state_values(self, dynamic_model_id: str, variables: Union[list[str], str]) -> None:
        """
        .. deprecated:: 1.15.0
            Use :func:`add_final_state_values` instead.
        """
        warnings.warn("add_dynamic_model_final_state_values is deprecated, use add_final_state_values instead", DeprecationWarning)
        self.add_final_state_values(dynamic_model_id, variables)

    def add_standard_model_final_state_values(self, static_id: str, variables: Union[list[str], str]) -> None:
        """
        .. deprecated:: 1.15.0
            Use :func:`add_final_state_values` instead.
        """
        warnings.warn("add_standard_model_final_state_values is deprecated, use add_final_state_values instead", DeprecationWarning)
        self.add_final_state_values(static_id, variables)

    def _add_output_variables(self, element_id: str, variables: list[str], output_variable_type: OutputVariableType) -> None:
        _pp.add_output_variables(self._handle, element_id, variables, output_variable_type)
