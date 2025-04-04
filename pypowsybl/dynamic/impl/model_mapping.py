# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import List, Optional
from numpy.typing import ArrayLike
from pandas import DataFrame
from pypowsybl import _pypowsybl as _pp
from pypowsybl._pypowsybl import DynamicMappingType # pylint: disable=protected-access
from pypowsybl.utils import _get_c_dataframes  # pylint: disable=protected-access


class ModelMapping:
    """
        class to map elements of a network to their respective dynamic behavior
    """

    def __init__(self) -> None:
        self._handle = _pp.create_dynamic_model_mapping()

    def get_supported_models(self, mapping_type: DynamicMappingType) -> List[str]:
        return _pp.get_supported_models(mapping_type)

    def add_base_load(self, df: DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Add a load mapping

        Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **static_id**: id of the network element to map
            - **parameter_set_id**: id of the parameter for this model given in the dynawo configuration
            - **model_name**: name of the model used for the mapping (if none the default model will be used)

        Examples:
            Using keyword arguments:

            .. code-block:: python

                model_mapping.add_base_load(static_id='LOAD',
                                            parameter_set_id='lab',
                                            model_name='LoadPQ')
        """
        self._add_all_dynamic_mappings(DynamicMappingType.BASE_LOAD, [df], **kwargs)

    def add_load_one_transformer(self, df: DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Add a load with one transformer mapping

        :Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **static_id**: id of the network element to map
            - **parameter_set_id**: id of the parameter for this model given in the dynawo configuration

            - **model_name**: name of the model used for the mapping (if none the default model will be used)

        Examples:
            Using keyword arguments:

            .. code-block:: python

                model_mapping.add_load_one_transformer(static_id='LOAD',
                                                       parameter_set_id='lt',
                                                       model_name='LoadOneTransformer')
        """
        self._add_all_dynamic_mappings(DynamicMappingType.LOAD_ONE_TRANSFORMER, [df], **kwargs)

    def add_load_one_transformer_tap_changer(self, df: DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Add a load with one transformer and tap changer mapping

        :Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **static_id**: id of the network element to map
            - **parameter_set_id**: id of the parameter for this model given in the dynawo configuration

            - **model_name**: name of the model used for the mapping (if none the default model will be used)

        Examples:
            Using keyword arguments:

            .. code-block:: python

                model_mapping.add_load_one_transformer_tap_changer(static_id='LOAD',
                                                                   parameter_set_id='lt_tc',
                                                                   model_name='LoadOneTransformerTapChanger')
        """
        self._add_all_dynamic_mappings(DynamicMappingType.LOAD_ONE_TRANSFORMER_TAP_CHANGER, [df], **kwargs)

    def add_load_two_transformers(self, df: DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Add a load with two transformers mapping

        :Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **static_id**: id of the network element to map
            - **parameter_set_id**: id of the parameter for this model given in the dynawo configuration

            - **model_name**: name of the model used for the mapping (if none the default model will be used)

        Examples:
            Using keyword arguments:

            .. code-block:: python

                model_mapping.add_load_two_transformers(static_id='LOAD',
                                                        parameter_set_id='ltt',
                                                        model_name='LoadTwoTransformers')
        """
        self._add_all_dynamic_mappings(DynamicMappingType.LOAD_TWO_TRANSFORMERS, [df], **kwargs)

    def add_load_two_transformers_tap_changers(self, df: DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Add a load with two transformers and tap changers mapping

        :Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **static_id**: id of the network element to map
            - **parameter_set_id**: id of the parameter for this model given in the dynawo configuration

            - **model_name**: name of the model used for the mapping (if none the default model will be used)

        Examples:
            Using keyword arguments:

            .. code-block:: python

                model_mapping.add_load_two_transformers_tap_changers(static_id='LOAD',
                                                                     parameter_set_id='ltt_tc',
                                                                     model_name='LoadTwoTransformersTapChangers')
        """
        self._add_all_dynamic_mappings(DynamicMappingType.LOAD_TWO_TRANSFORMERS_TAP_CHANGERS, [df], **kwargs)

    def add_base_generator(self, df: DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Add a base generator mapping

        :Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **static_id**: id of the network element to map
            - **parameter_set_id**: id of the parameter for this model given in the dynawo configuration

            - **model_name**: name of the model used for the mapping (if none the default model will be used)

        Examples:
            Using keyword arguments:

            .. code-block:: python

                model_mapping.add_base_generator(static_id='GEN',
                                                 parameter_set_id='gen',
                                                 model_name='GeneratorFictitious')
        """
        self._add_all_dynamic_mappings(DynamicMappingType.BASE_GENERATOR, [df], **kwargs)

    def add_synchronized_generator(self, df: DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Add a synchronized generator mapping

        :Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **static_id**: id of the network element to map
            - **parameter_set_id**: id of the parameter for this model given in the dynawo configuration

            - **model_name**: name of the model used for the mapping (if none the default model will be used)

        Examples:
            Using keyword arguments:

            .. code-block:: python

                model_mapping.add_synchronized_generator(static_id='GEN',
                                                         parameter_set_id='sgen',
                                                         model_name='GeneratorPVFixed')
        """
        self._add_all_dynamic_mappings(DynamicMappingType.SYNCHRONIZED_GENERATOR, [df], **kwargs)

    def add_synchronous_generator(self, df: DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Add a synchronous generator mapping

        :Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **static_id**: id of the network element to map
            - **parameter_set_id**: id of the parameter for this model given in the dynawo configuration

            - **model_name**: name of the model used for the mapping (if none the default model will be used)

        Examples:
            Using keyword arguments:

            .. code-block:: python

                    model_mapping.add_synchronous_generator(static_id='GEN',
                                                            parameter_set_id='ssgen',
                                                            model_name='GeneratorSynchronousThreeWindings')
        """
        self._add_all_dynamic_mappings(DynamicMappingType.SYNCHRONOUS_GENERATOR, [df], **kwargs)

    def add_wecc(self, df: DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Add a WECC mapping

        :Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **static_id**: id of the network element to map
            - **parameter_set_id**: id of the parameter for this model given in the dynawo configuration

            - **model_name**: name of the model used for the mapping (if none the default model will be used)

        Examples:
            Using keyword arguments:

            .. code-block:: python

                    model_mapping.add_wecc(static_id='GEN',
                                           parameter_set_id='wecc',
                                           model_name='WT4BWeccCurrentSource')
        """
        self._add_all_dynamic_mappings(DynamicMappingType.WECC, [df], **kwargs)

    def add_grid_forming_converter(self, df: DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Add a grid forming converter mapping

        :Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **static_id**: id of the network element to map
            - **parameter_set_id**: id of the parameter for this model given in the dynawo configuration

            - **model_name**: name of the model used for the mapping (if none the default model will be used)

        Examples:
            Using keyword arguments:

            .. code-block:: python

                model_mapping.add_grid_forming_converter(static_id='GEN',
                                                         parameter_set_id='gf',
                                                         model_name='GridFormingConverterMatchingControl')
        """
        self._add_all_dynamic_mappings(DynamicMappingType.GRID_FORMING_CONVERTER, [df], **kwargs)

    def add_signal_n_generator(self, df: DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Add a signal N generator mapping

        :Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **static_id**: id of the network element to map
            - **parameter_set_id**: id of the parameter for this model given in the dynawo configuration

            - **model_name**: name of the model used for the mapping (if none the default model will be used)

        Examples:
            Using keyword arguments:

            .. code-block:: python

                model_mapping.add_signal_n_generator(static_id='GEN',
                                                     parameter_set_id='signal_n',
                                                     model_name='GeneratorPVSignalN')
        """
        self._add_all_dynamic_mappings(DynamicMappingType.SIGNAL_N_GENERATOR, [df], **kwargs)

    def add_hvdc_p(self, df: DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Add an HVDC P mapping

        :Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **static_id**: id of the network element to map
            - **parameter_set_id**: id of the parameter for this model given in the dynawo configuration

            - **model_name**: name of the model used for the mapping (if none the default model will be used)

        Examples:
            Using keyword arguments:

            .. code-block:: python

                model_mapping.add_hvdc_p(static_id='HVDC_LINE',
                                         parameter_set_id='hvdc_p',
                                         model_name='HvdcPV')
        """
        self._add_all_dynamic_mappings(DynamicMappingType.HVDC_P, [df], **kwargs)

    def add_hvdc_vsc(self, df: DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Add an HVDC VSC mapping

        :Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **static_id**: id of the network element to map
            - **parameter_set_id**: id of the parameter for this model given in the dynawo configuration

            - **model_name**: name of the model used for the mapping (if none the default model will be used)

        Examples:
            Using keyword arguments:

            .. code-block:: python

                model_mapping.add_hvdc_vsc(static_id='HVDC_LINE',
                                           parameter_set_id='hvdc_vsc',
                                           model_name='HvdcVSCDanglingP')
        """
        self._add_all_dynamic_mappings(DynamicMappingType.HVDC_VSC, [df], **kwargs)

    def add_base_transformer(self, df: DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Add a transformer mapping

        :Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **static_id**: id of the network element to map
            - **parameter_set_id**: id of the parameter for this model given in the dynawo configuration

            - **model_name**: name of the model used for the mapping (if none the default model will be used)

        Examples:
            Using keyword arguments:

            .. code-block:: python

                model_mapping.add_base_transformer(static_id='TFO',
                                                   parameter_set_id='tfo',
                                                   model_name='TransformerFixedRatio')
        """
        self._add_all_dynamic_mappings(DynamicMappingType.BASE_TRANSFORMER, [df], **kwargs)

    def add_base_static_var_compensator(self, df: DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Add a static var compensator mapping

        :Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **static_id**: id of the network element to map
            - **parameter_set_id**: id of the parameter for this model given in the dynawo configuration

            - **model_name**: name of the model used for the mapping (if none the default model will be used)

        Examples:
            Using keyword arguments:

            .. code-block:: python

                model_mapping.add_base_static_var_compensator(static_id='SVARC',
                                                              parameter_set_id='svarc',
                                                              model_name='StaticVarCompensatorPV')
        """
        self._add_all_dynamic_mappings(DynamicMappingType.BASE_STATIC_VAR_COMPENSATOR, [df], **kwargs)

    def add_base_line(self, df: DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Add a line mapping

        :Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **static_id**: id of the network element to map
            - **parameter_set_id**: id of the parameter for this model given in the dynawo configuration

            - **model_name**: name of the model used for the mapping (if none the default model will be used)

        Examples:
            Using keyword arguments:

            .. code-block:: python

                mmodel_mapping.add_base_line(static_id='LINE',
                                             parameter_set_id='l',
                                             model_name='Line')
        """
        self._add_all_dynamic_mappings(DynamicMappingType.BASE_LINE, [df], **kwargs)

    def add_base_bus(self, df: DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Add a base bus mapping

        :Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **static_id**: id of the network element to map
            - **parameter_set_id**: id of the parameter for this model given in the dynawo configuration

            - **model_name**: name of the model used for the mapping (if none the default model will be used)

        Examples:
            Using keyword arguments:

            .. code-block:: python

                model_mapping.add_base_bus(static_id='BUS',
                                           parameter_set_id='bus',
                                           model_name='Bus')
        """
        self._add_all_dynamic_mappings(DynamicMappingType.BASE_BUS, [df], **kwargs)

    def add_infinite_bus(self, df: DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Add an infinite bus mapping

        :Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **static_id**: id of the network element to map
            - **parameter_set_id**: id of the parameter for this model given in the dynawo configuration

            - **model_name**: name of the model used for the mapping (if none the default model will be used)

        Examples:
            Using keyword arguments:

            .. code-block:: python

                model_mapping.add_infinite_bus(static_id='BUS',
                                               parameter_set_id='inf_bus',
                                               model_name='InfiniteBus')
        """
        self._add_all_dynamic_mappings(DynamicMappingType.INFINITE_BUS, [df], **kwargs)

    def add_overload_management_system(self, df: DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Add a dynamic overload management system (not link to a network element)
        
        :Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:
            
                - **dynamic_model_id**: id of the overload management system
                - **parameter_set_id**: id of the parameter for this model given in the dynawo configuration
                - **controlled_branch**: id of the branch controlled by the automation system
                - **i_measurement**: id of the branch used for the current intensity measurement
                - **i_measurement_side**: measured side of the i_measurement branch (ONE or TWO)
                - **model_name**: name of the model used for the mapping (if none the default model will be used)

        Examples:
            Using keyword arguments:

            .. code-block:: python

                model_mapping.add_overload_management_system(dynamic_model_id='DM_OV',
                                                             parameter_set_id='ov',
                                                             controlled_branch='LINE1',
                                                             i_measurement='LINE2',
                                                             i_measurement_side='TWO',
                                                             model_name='OverloadManagementSystem')
        """
        self._add_all_dynamic_mappings(DynamicMappingType.OVERLOAD_MANAGEMENT_SYSTEM, [df], **kwargs)

    def add_two_level_overload_management_system(self, df: DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Add a dynamic two level overload management system (not link to a network element)

        :Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:
            
                - **dynamic_model_id**: id of the two level overload management system
                - **parameter_set_id**: id of the parameter for this model given in the dynawo configuration
                - **controlled_branch**: id of the branch controlled by the automation system
                - **i_measurement_1**: id of the first branch used for the current intensity measurement
                - **i_measurement_1_side**: measured side of the i_measurement_1 branch (ONE or TWO)
                - **i_measurement_2**: id of the second branch used for the current intensity measurement
                - **i_measurement_2_side**: measured side of the i_measurement_2 branch (ONE or TWO)
                - **model_name**: name of the model used for the mapping (if none the default model will be used)

        Examples:
            Using keyword arguments:

            .. code-block:: python

                model_mapping.add_two_level_overload_management_system(dynamic_model_id='DM_TOV',
                                                                        parameter_set_id='tov',
                                                                        controlled_branch= 'LINE1',
                                                                        i_measurement_1='LINE1',
                                                                        i_measurement_1_side='TWO',
                                                                        i_measurement_2='LINE2',
                                                                        i_measurement_2_side='ONE',
                                                                        model_name='TwoLevelsOverloadManagementSystem')
        """
        self._add_all_dynamic_mappings(DynamicMappingType.TWO_LEVEL_OVERLOAD_MANAGEMENT_SYSTEM, [df], **kwargs)

    def add_under_voltage_automation_system(self, df: DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Add a dynamic under voltage automation system (not link to a network element)

        :Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:
            
                - **dynamic_model_id**: id of the under voltage automation system
                - **parameter_set_id**: id of the parameter for this model given in the dynawo configuration
                - **generator**: id of the generator controlled by the automation system
                - **model_name**: name of the model used for the mapping (if none the default model will be used)

        Examples:
            Using keyword arguments:

            .. code-block:: python

                model_mapping.add_under_voltage_automation_system(dynamic_model_id='DM_UV',
                                                                  parameter_set_id='psi',
                                                                  generator='GEN',
                                                                  model_name='UnderVoltage'
        """
        self._add_all_dynamic_mappings(DynamicMappingType.UNDER_VOLTAGE, [df], **kwargs)

    def add_phase_shifter_i_automation_system(self, df: DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Add a dynamic phase shifter I automation system (not link to a network element)
        
        :Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:
            
                - **dynamic_model_id**: id of the phase shifter I automation system
                - **parameter_set_id**: id of the parameter for this model given in the dynawo configuration
                - **transformer**: id of the transformer controlled by the automation system
                - **model_name**: name of the model used for the mapping (if none the default model will be used)

        Examples:
            Using keyword arguments:

            .. code-block:: python

                model_mapping.add_phase_shifter_i_automation_system(dynamic_model_id='DM_PS_I',
                                                                    parameter_set_id='psi',
                                                                    transformer='TRA',
                                                                    model_name='PhaseShifterI')
        """
        self._add_all_dynamic_mappings(DynamicMappingType.PHASE_SHIFTER_I, [df], **kwargs)

    def add_phase_shifter_p_automation_system(self, df: DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Add a dynamic phase shifter P automation system (not link to a network element)
        
        :Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:
            
                - **dynamic_model_id**: id of the phase shifter P automation system
                - **parameter_set_id**: id of the parameter for this model given in the dynawo configuration
                - **transformer**: id of the transformer controlled by the automation system
                - **model_name**: name of the model used for the mapping (if none the default model will be used)

        Examples:
            Using keyword arguments:

            .. code-block:: python

                model_mapping.add_phase_shifter_p_automation_system(dynamic_model_id='DM_PS_P',
                                                                    parameter_set_id='ov',
                                                                    transformer='TRA',
                                                                    model_name='PhaseShifterP')
        """
        self._add_all_dynamic_mappings(DynamicMappingType.PHASE_SHIFTER_P, [df], **kwargs)

    def add_phase_shifter_blocking_i_automation_system(self, df: DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Add a dynamic phase shifter blocking I automation system (not link to a network element)
        
        :Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:
            
                - **dynamic_model_id**: id of the phase shifter blocking I automation system
                - **parameter_set_id**: id of the parameter for this model given in the dynawo configuration
                - **phase_shifter_id**: id of the phase shifter I automation system controlled by the automation system
                - **model_name**: name of the model used for the mapping (if none the default model will be used)

        Examples:
            Using keyword arguments:

            .. code-block:: python

                model_mapping.add_phase_shifter_blocking_i_automation_system(dynamic_model_id='DM_PSB_I',
                                                                             parameter_set_id='psb',
                                                                             phase_shifter_id='PSI',
                                                                             model_name='PhaseShifterBlockingI')
        """
        self._add_all_dynamic_mappings(DynamicMappingType.PHASE_SHIFTER_BLOCKING_I, [df], **kwargs)

    def add_tap_changer_automation_system(self, df: DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Add a dynamic tap changer automation system (not link to a network element)
        
        :Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:
            
                - **dynamic_model_id**: id of the tap changer automation system
                - **parameter_set_id**: id of the parameter for this model given in the dynawo configuration
                - **static_id**: id of the load on which the tap changer is added
                - **side**: transformer side of the tap changer (HIGH_VOLTAGE, LOW_VOLTAGE or NONE)
                - **model_name**: name of the model used for the mapping (if none the default model will be used)

        Examples:
            Using keyword arguments:

            .. code-block:: python

                model_mapping.add_tap_changer_automation_system(dynamic_model_id='DM_TC',
                                                                parameter_set_id='tc',
                                                                static_id='LOAD',
                                                                side='HIGH_VOLTAGE',
                                                                model_name='TapChangerAutomaton')
        """
        self._add_all_dynamic_mappings(DynamicMappingType.TAP_CHANGER, [df], **kwargs)

    def add_tap_changer_blocking_automation_system(self, df: DataFrame, tfo_df: DataFrame, mp1_df: DataFrame,
                                                   mp2_df: DataFrame = None, mp3_df: DataFrame = None,
                                                   mp5_df: DataFrame = None, mp4_df: DataFrame = None) -> None:
        """
        Add a dynamic tap changer blocking automation system (not link to a network element)
        
        :Args:
            df: Primary attributes as a dataframe.
            tfo_df: Dataframe for transformer data.
            mpN_df: Dataframes for a measurement point data, the automation system can handle up to 5 measurement points,
            at least 1 measurement point is expected. For each measurement point dataframe, alternative points can be input
            (for example bus or busbar section) the first energized element found in the network will be used

        Notes:

            Valid attributes for the primary dataframes are:
            
                - **dynamic_model_id**: id of the tap changer blocking automation system
                - **parameter_set_id**: id of the parameter for this model given in the dynawo configuration
                - **model_name**: name of the model used for the mapping (if none the default model will be used)

            Valid attributes for the transformer dataframes are:
                - **dynamic_model_id**: id of the tap changer blocking automation system
                - **transformer_id**: id of a transformer controlled by the automation system

            Valid attributes for the measurement point dataframes are:
                - **dynamic_model_id**: id of the tap changer blocking automation system
                - **measurement_point_id**: id of the bus or busbar section used for the voltage measurement

        Examples:

            We need to provide 2 dataframes, 1 for tap changer blocking automation system basic data, and one for transformer data:

            .. code-block:: python

                df = pd.DataFrame.from_records(
                    index='dynamic_model_id',
                    columns=['dynamic_model_id', 'parameter_set_id', 'u_measurements', 'model_name'],
                    data=[('DM_TCB', 'tcb', 'BUS', 'TapChangerBlockingAutomaton')])
                tfo_df = pd.DataFrame.from_records(
                    index='dynamic_model_id',
                    columns=['dynamic_model_id', 'transformer_id'],
                    data=[('DM_TCB', 'TFO1'),
                          ('DM_TCB', 'TFO2'),
                          ('DM_TCB', 'TFO3')])
                measurement1_df = pd.DataFrame.from_records(
                    index='dynamic_model_id',
                    columns=['dynamic_model_id', 'measurement_point_id'],
                    data=[('DM_TCB', 'B1'),
                          ('DM_TCB', 'BS1')])
                measurement2_df = pd.DataFrame.from_records(
                    index='dynamic_model_id',
                    columns=['dynamic_model_id', 'measurement_point_id'],
                    data=[('DM_TCB', 'B4')])
                model_mapping.add_tap_changer_blocking_automation_system(df, tfo_df, measurement1_df, measurement2_df)
        """
        dfs = [df, tfo_df, mp1_df, mp2_df, mp3_df, mp4_df, mp5_df]
        self._add_all_dynamic_mappings(DynamicMappingType.TAP_CHANGER_BLOCKING, [DataFrame() if df is None else df for df in dfs])

    def _add_all_dynamic_mappings(self, mapping_type: DynamicMappingType, mapping_dfs: List[Optional[DataFrame]], **kwargs: ArrayLike) -> None:
        metadata = _pp.get_dynamic_mappings_meta_data(mapping_type)
        c_dfs = _get_c_dataframes(mapping_dfs, metadata, **kwargs)
        _pp.add_all_dynamic_mappings(self._handle, mapping_type, c_dfs)
