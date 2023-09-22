# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import List

from pypowsybl import _pypowsybl
from pypowsybl._pypowsybl import LimitViolation, PreContingencyResult, PostContingencyResult
from .security import SecurityAnalysis


def create_analysis() -> SecurityAnalysis:
    """ Creates a sensitivity analysis objet, which can be used to run a sensitivity analysis on a network

    Examples:
        .. code-block::

            >>> analysis = pypowsybl.sensitivity.create_analysis()
            >>> analysis.add_single_element_contingencies(['line 1', 'line 2'])
            >>> res = analysis.run_ac(network)

    Returns:
        A sensitivity analysis object, which allows to run a sensitivity analysis on a network.
    """
    return SecurityAnalysis(_pypowsybl.create_security_analysis())


def set_default_provider(provider: str) -> None:
    """
    Set the default sensitivity analysis provider.

    Args:
        provider: name of the default sensitivity analysis provider to set
    """
    _pypowsybl.set_default_security_analysis_provider(provider)


def get_default_provider() -> str:
    """
    Get the current default sensitivity analysis provider.

    Returns:
        the name of the current default sensitivity analysis provider
    """
    return _pypowsybl.get_default_security_analysis_provider()


def get_provider_names() -> List[str]:
    """
    Get list of supported provider names

    Returns:
        the list of supported provider names
    """
    return _pypowsybl.get_security_analysis_provider_names()


def get_provider_parameters_names(provider: str = '') -> List[str]:
    """
    Get list of parameters for the specified sensitivity analysis provider.

    If not specified the provider will be the default one.

    Returns:
        the list of provider's parameters
    """
    return _pypowsybl.get_security_analysis_provider_parameters_names(provider)


def _post_contingency_result_repr(self: PostContingencyResult) -> str:
    return f"{self.__class__.__name__}(" \
           f"contingency_id={self.contingency_id!r}" \
           f", status={self.status.name}" \
           f", limit_violations=[{len(self.limit_violations)}]" \
           f")"


PostContingencyResult.__repr__ = _post_contingency_result_repr  # type: ignore


def _pre_contingency_result_repr(self: PreContingencyResult) -> str:
    return f"{self.__class__.__name__}(" \
           f", status={self.status.name}" \
           f", limit_violations=[{len(self.limit_violations)}]" \
           f")"


PreContingencyResult.__repr__ = _pre_contingency_result_repr  # type: ignore


def _limit_violation_repr(self: LimitViolation) -> str:
    return f"{self.__class__.__name__}(" \
           f"subject_id={self.subject_id!r}" \
           f", subject_name={self.subject_name!r}" \
           f", limit_type={self.limit_type.name}" \
           f", limit={self.limit!r}" \
           f", limit_name={self.limit_name!r}" \
           f", acceptable_duration={self.acceptable_duration!r}" \
           f", limit_reduction={self.limit_reduction!r}" \
           f", value={self.value!r}" \
           f", side={self.side.name}" \
           f")"


LimitViolation.__repr__ = _limit_violation_repr  # type: ignore
