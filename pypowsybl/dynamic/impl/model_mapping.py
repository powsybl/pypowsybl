# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import Union
import pandas as pd
from pypowsybl import _pypowsybl as _pp
from pypowsybl._pypowsybl import DynamicMappingType, Side # pylint: disable=protected-access
from pypowsybl.utils import \
    _adapt_df_or_kwargs, _add_index_to_kwargs, _create_c_dataframe # pylint: disable=protected-access


class ModelMapping:
    """
        class to map elements of a network to their respective dynamic behavior
    """

    def __init__(self) -> None:
        self._handle = _pp.create_dynamic_model_mapping()

    def add_base_load(self, static_id: str, parameter_set_id: str, model_name: str = None) -> None:
        """
        Add a load mapping

        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        :param model_name: name of the model used for the mapping (if none the default model will be used)
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      model_name=model_name,
                                      mapping_type=DynamicMappingType.BASE_LOAD)

    def add_load_one_transformer(self, static_id: str, parameter_set_id: str, model_name: str = None) -> None:
        """
        Add a load with one transformer mapping

        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        :param model_name: name of the model used for the mapping (if none the default model will be used)
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      model_name=model_name,
                                      mapping_type=DynamicMappingType.LOAD_ONE_TRANSFORMER)

    def add_load_one_transformer_tap_changer(self, static_id: str, parameter_set_id: str,
                                             model_name: str = None) -> None:
        """
        Add a load with one transformer and tap changer mapping

        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        :param model_name: name of the model used for the mapping (if none the default model will be used)
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      model_name=model_name,
                                      mapping_type=DynamicMappingType.LOAD_ONE_TRANSFORMER_TAP_CHANGER)

    def add_load_two_transformers(self, static_id: str, parameter_set_id: str, model_name: str = None) -> None:
        """
        Add a load with two transformers mapping

        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        :param model_name: name of the model used for the mapping (if none the default model will be used)
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      model_name=model_name,
                                      mapping_type=DynamicMappingType.LOAD_TWO_TRANSFORMERS)

    def add_load_two_transformers_tap_changers(self, static_id: str, parameter_set_id: str,
                                               model_name: str = None) -> None:
        """
        Add a load with two transformers and tap changers mapping

        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        :param model_name: name of the model used for the mapping (if none the default model will be used)
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      model_name=model_name,
                                      mapping_type=DynamicMappingType.LOAD_TWO_TRANSFORMERS_TAP_CHANGERS)

    def add_base_generator(self, static_id: str, parameter_set_id: str, model_name: str = None) -> None:
        """
        Add a base generator mapping

        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        :param model_name: name of the model used for the mapping (if none the default model will be used)
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      model_name=model_name,
                                      mapping_type=DynamicMappingType.BASE_GENERATOR)

    def add_synchronized_generator(self, static_id: str, parameter_set_id: str, model_name: str = None) -> None:
        """
        Add a synchronized generator mapping

        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        :param model_name: name of the model used for the mapping (if none the default model will be used)
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      model_name=model_name,
                                      mapping_type=DynamicMappingType.SYNCHRONIZED_GENERATOR)

    def add_synchronous_generator(self, static_id: str, parameter_set_id: str, model_name: str = None) -> None:
        """
        Add a synchronous generator mapping

        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        :param model_name: name of the model used for the mapping (if none the default model will be used)
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      model_name=model_name,
                                      mapping_type=DynamicMappingType.SYNCHRONOUS_GENERATOR)

    def add_wecc(self, static_id: str, parameter_set_id: str, model_name: str = None) -> None:
        """
        Add a WECC mapping

        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        :param model_name: name of the model used for the mapping (if none the default model will be used)
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      model_name=model_name,
                                      mapping_type=DynamicMappingType.WECC)

    def add_grid_forming_converter(self, static_id: str, parameter_set_id: str, model_name: str = None) -> None:
        """
        Add a grid forming converter mapping

        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        :param model_name: name of the model used for the mapping (if none the default model will be used)
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      model_name=model_name,
                                      mapping_type=DynamicMappingType.GRID_FORMING_CONVERTER)

    def add_hvdc_p(self, static_id: str, parameter_set_id: str, dangling_side: Side = Side.NONE,
                   model_name: str = None) -> None:
        """
        Add an HVDC P mapping

        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        :param dangling_side: side of a dangling line if the model have this property (NONE by default)
        :param model_name: name of the model used for the mapping (if none the default model will be used)
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      model_name=model_name,
                                      dangling_side=dangling_side,
                                      mapping_type=DynamicMappingType.HVDC_P)

    def add_hvdc_vsc(self, static_id: str, parameter_set_id: str, dangling_side: Side = Side.NONE,
                     model_name: str = None) -> None:
        """
        Add an HVDC VSC mapping

        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        :param dangling_side: side of a dangling line if the model have this property (NONE by default)
        :param model_name: name of the model used for the mapping (if none the default model will be used)
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      model_name=model_name,
                                      dangling_side=dangling_side,
                                      mapping_type=DynamicMappingType.HVDC_VSC)

    def add_base_transformer(self, static_id: str, parameter_set_id: str, model_name: str = None) -> None:
        """
        Add a transformer mapping

        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        :param model_name: name of the model used for the mapping (if none the default model will be used)
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      model_name=model_name,
                                      mapping_type=DynamicMappingType.BASE_TRANSFORMER)

    def add_base_static_var_compensator(self, static_id: str, parameter_set_id: str, model_name: str = None) -> None:
        """
        Add a static var compensator mapping

        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        :param model_name: name of the model used for the mapping (if none the default model will be used)
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      model_name=model_name,
                                      mapping_type=DynamicMappingType.BASE_STATIC_VAR_COMPENSATOR)

    def add_base_line(self, static_id: str, parameter_set_id: str, model_name: str = None) -> None:
        """
        Add a line mapping

        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        :param model_name: name of the model used for the mapping (if none the default model will be used)
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      model_name=model_name,
                                      mapping_type=DynamicMappingType.BASE_LINE)

    def add_base_bus(self, static_id: str, parameter_set_id: str, model_name: str = None) -> None:
        """
        Add a base bus mapping

        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        :param model_name: name of the model used for the mapping (if none the default model will be used)
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      model_name=model_name,
                                      mapping_type=DynamicMappingType.BASE_BUS)

    def add_infinite_bus(self, static_id: str, parameter_set_id: str, model_name: str = None) -> None:
        """
        Add an infinite bus mapping

        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        :param model_name: name of the model used for the mapping (if none the default model will be used)
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      model_name=model_name,
                                      mapping_type=DynamicMappingType.INFINITE_BUS)

    def add_overload_management_system(self, dynamic_id: str, parameter_set_id: str, controlled_branch: str,
                                       i_measurement: str, i_measurement_side: Side, model_name: str = None) -> None:
        """
        Add a dynamic overload management system (not link to a network element)

        :param dynamic_id: id of the overload management system
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        :param controlled_branch: id of the branch controlled by the automation system
        :param i_measurement: id of the branch used for the current intensity measurement
        :param i_measurement_side: measured side of the i_measurement branch
        :param model_name: name of the model used for the mapping (if none the default model will be used)
        """
        self.add_all_dynamic_mappings(dynamic_id=dynamic_id,
                                      parameter_set_id=parameter_set_id,
                                      controlled_branch=controlled_branch,
                                      i_measurement=i_measurement,
                                      i_measurement_side=i_measurement_side,
                                      model_name=model_name,
                                      mapping_type=DynamicMappingType.OVERLOAD_MANAGEMENT_SYSTEM)

    def add_two_levels_overload_management_system(self, dynamic_id: str, parameter_set_id: str, controlled_branch: str,
                                                  i_measurement_1: str, i_measurement_1_side: Side,
                                                  i_measurement_2: str, i_measurement_2_side: Side,
                                                  model_name: str = None) -> None:
        """
        Add a dynamic two levels overload management system (not link to a network element)

        :param dynamic_id: id of the overload management system
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        :param controlled_branch: id of the branch controlled by the automation system
        :param i_measurement_1: id of the first branch used for the current intensity measurement
        :param i_measurement_1_side: measured side of the i_measurement_1 branch
        :param i_measurement_2: id of the second branch used for the current intensity measurement
        :param i_measurement_2_side: measured side of the i_measurement_2 branch
        :param model_name: name of the model used for the mapping (if none the default model will be used)
        """
        self.add_all_dynamic_mappings(dynamic_id=dynamic_id,
                                      parameter_set_id=parameter_set_id,
                                      controlled_branch=controlled_branch,
                                      i_measurement_1=i_measurement_1,
                                      i_measurement_1_side=i_measurement_1_side,
                                      i_measurement_2=i_measurement_2,
                                      i_measurement_2_side=i_measurement_2_side,
                                      model_name=model_name,
                                      mapping_type=DynamicMappingType.TWO_LEVELS_OVERLOAD_MANAGEMENT_SYSTEM)

    def add_under_voltage_automation_system(self, dynamic_id: str, parameter_set_id: str, generator: str,
                                            model_name: str = None) -> None:
        """
        Add a dynamic under voltage automation system (not link to a network element)

        :param dynamic_id: id of the overload management system
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        :param generator: id of the generator controlled by the automation system
        :param model_name: name of the model used for the mapping (if none the default model will be used)
        """
        self.add_all_dynamic_mappings(dynamic_id=dynamic_id,
                                      parameter_set_id=parameter_set_id,
                                      generator=generator,
                                      model_name=model_name,
                                      mapping_type=DynamicMappingType.UNDER_VOLTAGE)

    def add_phase_shifter_i_automation_system(self, dynamic_id: str, parameter_set_id: str, transformer: str,
                                              model_name: str = None) -> None:
        """
        Add a dynamic phase shifter I automation system (not link to a network element)

        :param dynamic_id: id of the overload management system
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        :param transformer: id of the transformer controlled by the automation system
        :param model_name: name of the model used for the mapping (if none the default model will be used)
        """
        self.add_all_dynamic_mappings(dynamic_id=dynamic_id,
                                      parameter_set_id=parameter_set_id,
                                      transformer=transformer,
                                      model_name=model_name,
                                      mapping_type=DynamicMappingType.PHASE_SHIFTER_I)

    def add_phase_shifter_p_automation_system(self, dynamic_id: str, parameter_set_id: str, transformer: str,
                                              model_name: str = None) -> None:
        """
        Add a dynamic phase shifter P automation system (not link to a network element)

        :param dynamic_id: id of the overload management system
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        :param transformer: id of the transformer controlled by the automation system
        :param model_name: name of the model used for the mapping (if none the default model will be used)
        """
        self.add_all_dynamic_mappings(dynamic_id=dynamic_id,
                                      parameter_set_id=parameter_set_id,
                                      transformer=transformer,
                                      model_name=model_name,
                                      mapping_type=DynamicMappingType.PHASE_SHIFTER_P)

    def add_tap_changer_automation_system(self, dynamic_id: str, parameter_set_id: str, static_id: str,
                                          side: Side, model_name: str = None) -> None:
        """
        Add a dynamic tap changer automation system (not link to a network element)

        :param dynamic_id: id of the overload management system
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        :param static_id: id of the load on which the tap changer is added
        :param side: side of the tap changer
        :param model_name: name of the model used for the mapping (if none the default model will be used)
        """
        self.add_all_dynamic_mappings(dynamic_id=dynamic_id,
                                      parameter_set_id=parameter_set_id,
                                      static_id=static_id,
                                      side=side,
                                      model_name=model_name,
                                      mapping_type=DynamicMappingType.TAP_CHANGER)

    def add_tap_changer_blocking_automation_system(self, dynamic_id: str, parameter_set_id: str, transformers: str,
                                                   u_measurements: str, model_name: str = None) -> None:
        """
        Add a dynamic tap changer blocking automation system (not link to a network element)

        :param dynamic_id: id of the overload management system
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        :param transformers: ids of the transformers controlled by the automation system
        :param u_measurements: id of the bus or busbar section used for the voltage measurement
        :param model_name: name of the model used for the mapping (if none the default model will be used)
        """
        self.add_all_dynamic_mappings(dynamic_id=dynamic_id,
                                      parameter_set_id=parameter_set_id,
                                      transformers=transformers,
                                      u_measurements=u_measurements,
                                      model_name=model_name,
                                      mapping_type=DynamicMappingType.TAP_CHANGER_BLOCKING)

    def add_all_dynamic_mappings(self, mapping_type: DynamicMappingType, mapping_df: pd.DataFrame = None,
                                 **kwargs: Union[str, Side, DynamicMappingType]) -> None:
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
