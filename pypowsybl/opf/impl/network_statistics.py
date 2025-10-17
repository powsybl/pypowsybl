import json
import logging

import numpy as np
from pypowsybl._pypowsybl import ElementType

from pypowsybl.opf.impl.model.network_cache import NetworkCache

logger = logging.getLogger(__name__)


class NetworkStatistics:
    def __init__(self, network_cache: NetworkCache) -> None:
        self._network_cache = network_cache
        self._initial_values = {}

    def _get_column(self, element_type: ElementType, attribute_id: str):
        if element_type == ElementType.GENERATOR:
            return self._network_cache.generators[attribute_id]
        elif element_type == ElementType.BATTERY:
            return self._network_cache.batteries[attribute_id]
        elif element_type == ElementType.VSC_CONVERTER_STATION:
            return self._network_cache.vsc_converter_stations[attribute_id]
        else:
            raise ValueError(f"Unknown element type: {element_type}")

    def add(self, element_type: ElementType, attribute_id: str):
        self._initial_values[(element_type, attribute_id)] = self._get_column(element_type, attribute_id).copy()

    def print(self):
        for (element_type, attribute_id), initial_values in self._initial_values.items():
            final_values = self._get_column(element_type, attribute_id)
            changes = final_values - initial_values
            statistics = {
                'initial': {
                    'min': initial_values.min(),
                    'max': initial_values.max(),
                    'mean': initial_values.mean(),
                    'std': initial_values.std(),
                    'median': initial_values.median()
                },
                'final': {
                    'min': final_values.min(),
                    'max': final_values.max(),
                    'mean': final_values.mean(),
                    'std': final_values.std(),
                    'median': final_values.median()
                },
                'changes': {
                    'min': changes.min(),
                    'max': changes.max(),
                    'mean': changes.mean(),
                    'std': changes.std(),
                    'median': changes.median(),
                    'abs_mean': np.abs(changes).mean(),
                    'abs_max': np.abs(changes).max()
                }
            }
            logger.info(f"Statistics of {element_type} {attribute_id}: {json.dumps(statistics, indent=4)}")
