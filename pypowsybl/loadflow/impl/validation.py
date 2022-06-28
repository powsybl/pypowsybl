from typing import (
    List,
    Optional,
)
import pandas as pd
import pypowsybl._pypowsybl as _pp
from pypowsybl._pypowsybl import (
    LoadFlowComponentStatus as ComponentStatus,
    ConnectedComponentMode,
    BalanceType,
    VoltageInitMode,
    ValidationType
)

from pypowsybl.network import Network
from pypowsybl.report import Reporter
from pypowsybl.util import create_data_frame_from_series_array


ValidationType.ALL = [ValidationType.BUSES, ValidationType.FLOWS, ValidationType.GENERATORS, ValidationType.SHUNTS,
                      ValidationType.SVCS, ValidationType.TWTS, ValidationType.TWTS3W]

OptionalDf = Optional[pd.DataFrame]


class ValidationResult:
    """
    The result of a loadflow validation.
    """

    def __init__(self, branch_flows: OptionalDf, buses: OptionalDf, generators: OptionalDf, svcs: OptionalDf,
                 shunts: OptionalDf, twts: OptionalDf, t3wts: OptionalDf):
        self._branch_flows = branch_flows
        self._buses = buses
        self._generators = generators
        self._svcs = svcs
        self._shunts = shunts
        self._twts = twts
        self._t3wts = t3wts
        self._valid = self._is_valid_or_unchecked(self.branch_flows) and self._is_valid_or_unchecked(self.buses) \
                      and self._is_valid_or_unchecked(self.generators) and self._is_valid_or_unchecked(self.svcs) \
                      and self._is_valid_or_unchecked(self.shunts) and self._is_valid_or_unchecked(self.twts) \
                      and self._is_valid_or_unchecked(self.t3wts)

    @staticmethod
    def _is_valid_or_unchecked(df: OptionalDf) -> bool:
        return df is None or df['validated'].all()

    @property
    def branch_flows(self) -> OptionalDf:
        """
        Validation results for branch flows.
        """
        return self._branch_flows

    @property
    def buses(self) -> OptionalDf:
        """
        Validation results for buses.
        """
        return self._buses

    @property
    def generators(self) -> OptionalDf:
        """
        Validation results for generators.
        """
        return self._generators

    @property
    def svcs(self) -> OptionalDf:
        """
        Validation results for SVCs.
        """
        return self._svcs

    @property
    def shunts(self) -> OptionalDf:
        """
        Validation results for shunts.
        """
        return self._shunts

    @property
    def twts(self) -> OptionalDf:
        """
        Validation results for two winding transformers.
        """
        return self._twts

    @property
    def t3wts(self) -> OptionalDf:
        """
        Validation results for three winding transformers.
        """
        return self._t3wts

    @property
    def valid(self) -> bool:
        """
        True if all checked data is valid.
        """
        return self._valid


def run_validation(network: Network, validation_types: List[ValidationType] = None) -> ValidationResult:
    """
    Checks that the network data are consistent with AC loadflow equations.

    Args:
        network: The network to be checked.
        validation_types: The types of data to be checked. If None, all types will be checked.

    Returns:
        The validation result.
    """
    if validation_types is None:
        validation_types = ValidationType.ALL
    res_by_type = {}
    for validation_type in validation_types:
        series_array = _pp.run_load_flow_validation(network._handle, validation_type)
        res_by_type[validation_type] = create_data_frame_from_series_array(series_array)

    return ValidationResult(buses=res_by_type.get(ValidationType.BUSES, None),
                            branch_flows=res_by_type.get(ValidationType.FLOWS, None),
                            generators=res_by_type.get(ValidationType.GENERATORS, None),
                            svcs=res_by_type.get(ValidationType.SVCS, None),
                            shunts=res_by_type.get(ValidationType.SHUNTS, None),
                            twts=res_by_type.get(ValidationType.TWTS, None),
                            t3wts=res_by_type.get(ValidationType.TWTS3W, None))
