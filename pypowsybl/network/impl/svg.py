# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import Optional


class Svg:
    """
    This class represents a single line diagram."""

    def __init__(self, content: str, metadata: str = None):
        self._content = content
        self._metadata = metadata

    @property
    def svg(self) -> str:
        return self._content

    @property
    def metadata(self) -> Optional[str]:
        return self._metadata

    def __str__(self) -> str:
        return self._content

    def _repr_svg_(self) -> str:
        return self._content
