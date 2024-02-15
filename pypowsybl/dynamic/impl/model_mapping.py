# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import Union
import pandas as pd
from pypowsybl import _pypowsybl as _pp
from pypowsybl._pypowsybl import DynamicMappingType, BranchSide # pylint: disable=protected-access
from pypowsybl.utils import \
    _adapt_df_or_kwargs, _add_index_to_kwargs, _create_c_dataframe # pylint: disable=protected-access


class ModelMapping:
    """
        class to map elements of a network to their respective dynamic behavior
    """

    def __init__(self) -> None:
        self._handle = _pp.create_dynamic_model_mapping()

    def add_alpha_beta_load(self, static_id: str, parameter_set_id: str) -> None:
        """
        Add a alpha beta load mapping

        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      mapping_type=DynamicMappingType.ALPHA_BETA_LOAD)

    def add_one_transformer_load(self, static_id: str, parameter_set_id: str) -> None:
        """
        Add a one transformer load mapping

        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      mapping_type=DynamicMappingType.ONE_TRANSFORMER_LOAD)

    def add_generator_synchronous_three_windings(self, static_id: str, parameter_set_id: str) -> None:
        """
        Add a generator synchronous three windings mapping

        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      mapping_type=DynamicMappingType.GENERATOR_SYNCHRONOUS_THREE_WINDINGS)

    def add_generator_synchronous_three_windings_proportional_regulations(self, static_id: str,
                                                                          parameter_set_id: str) -> None:
        """
        Add a generator synchronous three windings proportional regulations mapping

        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      mapping_type=DynamicMappingType.GENERATOR_SYNCHRONOUS_THREE_WINDINGS_PROPORTIONAL_REGULATIONS)

    def add_generator_synchronous_four_windings(self, static_id: str, parameter_set_id: str) -> None:
        """
        Add a generator synchronous four windings mapping

        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      mapping_type=DynamicMappingType.GENERATOR_SYNCHRONOUS_FOUR_WINDINGS)

    def add_generator_synchronous_four_windings_proportional_regulations(self, static_id: str,
                                                                         parameter_set_id: str) -> None:
        """
        Add a generator synchronous four windings proportional regulations mapping

        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      mapping_type=DynamicMappingType.GENERATOR_SYNCHRONOUS_FOUR_WINDINGS_PROPORTIONAL_REGULATIONS)

    def add_current_limit_automaton(self, static_id: str, parameter_set_id: str, branch_side: BranchSide) -> None:
        """
        Add a current limit automaton mapping

        :param branch_side:
        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      branch_side=branch_side,
                                      mapping_type=DynamicMappingType.CURRENT_LIMIT_AUTOMATON)

    def add_all_dynamic_mappings(self, mapping_type: DynamicMappingType, mapping_df: pd.DataFrame = None,
                                 **kwargs: Union[str, BranchSide, DynamicMappingType]) -> None:
        """
        Update the dynamic mapping of a simulation, must provide a :class:`~pandas.DataFrame` or as named arguments.

        | The dataframe must contains these three columns:
        |     - static_id: id of the network element to map
        |     - parameter_set_id: set id in the parameter file
        |     - mapping_type: value of enum DynamicMappingType

        """
        metadata = _pp.get_dynamic_mappings_meta_data(mapping_type)
        if kwargs:
            kwargs = _add_index_to_kwargs(metadata, **kwargs)
        mapping_df = _adapt_df_or_kwargs(metadata, mapping_df, **kwargs)
        c_mapping_df = _create_c_dataframe(mapping_df, metadata)
        _pp.add_all_dynamic_mappings(self._handle, mapping_type, c_mapping_df)
