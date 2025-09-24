# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from asyncio import AbstractEventLoop, Future
from typing import List

from pypowsybl import PyPowsyblError, _pypowsybl

from .component_result import ComponentResult


class LoadFlowResultsFutureWrapper:
    def __init__(self, loop: AbstractEventLoop, future: Future):
        self._loop = loop
        self._future = future

    def set_results(self, results: List[_pypowsybl.LoadFlowComponentResult]) -> None:
        self._loop.call_soon_threadsafe(
            self._future.set_result, [ComponentResult(result) for result in results]
        )

    def set_exception_message(self, message: str) -> None:
        self._loop.call_soon_threadsafe(
            self._future.set_exception, PyPowsyblError(message)
        )
