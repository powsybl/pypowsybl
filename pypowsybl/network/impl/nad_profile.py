# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from pandas import DataFrame

import pypowsybl._pypowsybl as _pp
from pypowsybl.utils import _create_c_dataframe

class NadProfile:
    """
    This class represents parameters to customize a network area diagram (e.g., labels on branches)."""

    _nad_branch_labels_metadata=[_pp.SeriesMetadata('id',0,True,False,False),
                  _pp.SeriesMetadata('side1Internal',0,False,False,False),
                  _pp.SeriesMetadata('side1External',0,False,False,False),
                  _pp.SeriesMetadata('middle1',0,False,False,False),
                  _pp.SeriesMetadata('middle2',0,False,False,False),
                  _pp.SeriesMetadata('side2Internal',0,False,False,False),
                  _pp.SeriesMetadata('side2External',0,False,False,False),
                  _pp.SeriesMetadata('arrow1',0,False,False,False),
                  _pp.SeriesMetadata('arrowMiddle',0,False,False,False),
                  _pp.SeriesMetadata('arrow2',0,False,False,False)]

    _nad_three_wt_metadata=[_pp.SeriesMetadata('id',0,True,False,False),
                  _pp.SeriesMetadata('side1Internal',0,False,False,False),
                  _pp.SeriesMetadata('side1External',0,False,False,False),
                  _pp.SeriesMetadata('side2Internal',0,False,False,False),
                  _pp.SeriesMetadata('side2External',0,False,False,False),
                  _pp.SeriesMetadata('side3Internal',0,False,False,False),
                  _pp.SeriesMetadata('side3External',0,False,False,False),
                  _pp.SeriesMetadata('arrow1',0,False,False,False),
                  _pp.SeriesMetadata('arrow2',0,False,False,False),
                  _pp.SeriesMetadata('arrow3',0,False,False,False)]

    _nad_injections_metadata=[_pp.SeriesMetadata('id',0,True,False,False),
                  _pp.SeriesMetadata('labelInternal',0,False,False,False),
                  _pp.SeriesMetadata('labelExternal',0,False,False,False),
                  _pp.SeriesMetadata('arrow',0,False,False,False)]

    _nad_descriptions_metadata=[_pp.SeriesMetadata('id',0,True,False,False),
                  _pp.SeriesMetadata('type',0,False,False,False),
                  _pp.SeriesMetadata('description',0,False,False,False)]

    _nad_bus_descriptions_metadata=[_pp.SeriesMetadata('id',0,True,False,False),
                  _pp.SeriesMetadata('description',0,False,False,False)]

    _nad_bus_node_styles_metadata=[_pp.SeriesMetadata('id',0,True,False,False),
                  _pp.SeriesMetadata('fill',0,False,False,False),
                  _pp.SeriesMetadata('edge',0,False,False,False),
                  _pp.SeriesMetadata('edge-width',0,False,False,False)]

    _nad_edge_styles_metadata=[_pp.SeriesMetadata('id',0,True,False,False),
                  _pp.SeriesMetadata('edge1',0,False,False,False),
                  _pp.SeriesMetadata('width1',0,False,False,False),
                  _pp.SeriesMetadata('dash1',0,False,False,False),
                  _pp.SeriesMetadata('edge2',0,False,False,False),
                  _pp.SeriesMetadata('width2',0,False,False,False),
                  _pp.SeriesMetadata('dash2',0,False,False,False)]

    _nad_three_wt_styles_metadata=[_pp.SeriesMetadata('id',0,True,False,False),
                  _pp.SeriesMetadata('edge1',0,False,False,False),
                  _pp.SeriesMetadata('width1',0,False,False,False),
                  _pp.SeriesMetadata('dash1',0,False,False,False),
                  _pp.SeriesMetadata('edge2',0,False,False,False),
                  _pp.SeriesMetadata('width2',0,False,False,False),
                  _pp.SeriesMetadata('dash2',0,False,False,False),
                  _pp.SeriesMetadata('edge3',0,False,False,False),
                  _pp.SeriesMetadata('width3',0,False,False,False),
                  _pp.SeriesMetadata('dash3',0,False,False,False)]


    def __init__(self, branch_labels: DataFrame | None = None, three_wt_labels: DataFrame | None = None,
                 injections_labels: DataFrame | None = None, bus_descriptions: DataFrame | None = None,
                 vl_descriptions: DataFrame | None = None, bus_node_styles: DataFrame | None = None,
                 edge_styles: DataFrame | None = None, three_wt_styles: DataFrame | None = None):
        self._branch_labels = branch_labels
        self._three_wt_labels = three_wt_labels
        self._injections_labels = injections_labels
        self._bus_descriptions = bus_descriptions
        self._vl_descriptions = vl_descriptions
        self._bus_node_styles = bus_node_styles
        self._edge_styles = edge_styles
        self._three_wt_styles = three_wt_styles

    @property
    def branch_labels(self) -> DataFrame | None:
        """branch_labels"""
        return self._branch_labels

    @property
    def three_wt_labels(self) -> DataFrame | None:
        """three_wt_labels"""
        return self._three_wt_labels

    @property
    def injections_labels(self) -> DataFrame | None:
        """injections_labels"""
        return self._injections_labels

    @property
    def bus_descriptions(self) -> DataFrame | None:
        """bus_description"""
        return self._bus_descriptions

    @property
    def vl_descriptions(self) -> DataFrame | None:
        """vl_descriptions"""
        return self._vl_descriptions

    @property
    def bus_node_styles(self) -> DataFrame | None:
        """bus_node_styles"""
        return self._bus_node_styles

    @property
    def edge_styles(self) -> DataFrame | None:
        """edge_styles"""
        return self._edge_styles

    @property
    def three_wt_styles(self) -> DataFrame | None:
        """three_wt_styles"""
        return self._three_wt_styles

    def _create_nad_branch_labels_c_dataframe(self) -> _pp.Dataframe | None:
        return None if self._branch_labels is None else _create_c_dataframe(self._branch_labels.fillna(''),
                                                                           NadProfile._nad_branch_labels_metadata)

    def _create_nad_three_wt_labels_c_dataframe(self) -> _pp.Dataframe | None:
        return None if self._three_wt_labels is None else _create_c_dataframe(self._three_wt_labels.fillna(''),
                                                                           NadProfile._nad_three_wt_metadata)

    def _create_nad_injections_labels_c_dataframe(self) -> _pp.Dataframe | None:
        return None if self._injections_labels is None else _create_c_dataframe(self._injections_labels.fillna(''),
                                                                           NadProfile._nad_injections_metadata)

    def _create_nad_bus_descriptions_c_dataframe(self) -> _pp.Dataframe | None:
        return None if self._bus_descriptions is None else _create_c_dataframe(self._bus_descriptions.fillna(''),
                                                                           NadProfile._nad_bus_descriptions_metadata)

    def _create_nad_vl_descriptions_c_dataframe(self) -> _pp.Dataframe | None:
        return None if self._vl_descriptions is None else _create_c_dataframe(self._vl_descriptions.fillna(''),
                                                                           NadProfile._nad_descriptions_metadata)

    def _create_nad_bus_node_styles_c_dataframe(self) -> _pp.Dataframe | None:
        return None if self._bus_node_styles is None else _create_c_dataframe(self._bus_node_styles.fillna(''),
                                                                           NadProfile._nad_bus_node_styles_metadata)

    def _create_nad_edge_styles_c_dataframe(self) -> _pp.Dataframe | None:
        return None if self._edge_styles is None else _create_c_dataframe(self._edge_styles.fillna(''),
                                                                           NadProfile._nad_edge_styles_metadata)
    def _create_nad_three_wt_styles_c_dataframe(self) -> _pp.Dataframe | None:
        return None if self._three_wt_styles is None else _create_c_dataframe(self._three_wt_styles.fillna(''),
                                                                           NadProfile._nad_three_wt_styles_metadata)
