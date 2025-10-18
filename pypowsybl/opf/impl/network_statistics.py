import logging
from typing import Optional

import numpy as np
from pandas import Series
from tabulate import tabulate

from pypowsybl._pypowsybl import ElementType
from pypowsybl.opf.impl.model.network_cache import NetworkCache

logger = logging.getLogger(__name__)


class NetworkStatistics:
    def __init__(self, network_cache: NetworkCache) -> None:
        self._network_cache = network_cache
        self._initial_values = {}

    def _get_column(self, element_type: ElementType, attribute_id: str) -> Optional[Series]:
        if element_type == ElementType.GENERATOR:
            return self._network_cache.generators[attribute_id] if len(self._network_cache.generators) > 0 else None
        elif element_type == ElementType.BATTERY:
            return self._network_cache.batteries[attribute_id] if len(self._network_cache.batteries) > 0 else None
        elif element_type == ElementType.VSC_CONVERTER_STATION:
            return self._network_cache.vsc_converter_stations[attribute_id] if len(self._network_cache.vsc_converter_stations) > 0 else None
        else:
            raise ValueError(f"Unknown element type: {element_type}")

    def add(self, element_type: ElementType, attribute_id: str):
        column = self._get_column(element_type, attribute_id)
        if column is not None:
            self._initial_values[(element_type, attribute_id)] = column.copy()

    def print(self):
        for (element_type, attribute_id), initial_values in self._initial_values.items():
            final_values = self._get_column(element_type, attribute_id)

            table_data = [
                ['Initial', initial_values.min(), initial_values.max(), initial_values.mean(),
                 initial_values.std(), np.median(initial_values)],
                ['Final', final_values.min(), final_values.max(), final_values.mean(),
                 final_values.std(), np.median(final_values)]
            ]

            headers = ['', 'Min', 'Max', 'Mean', 'Std', 'Median']
            table = tabulate(table_data, headers=headers, floatfmt='.3f', tablefmt='simple')

            logger.info(f"\nStatistics: {element_type} - {attribute_id}\n{table}")
