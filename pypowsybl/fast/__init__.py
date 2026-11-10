#
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
"""Positional fast path next to the dataframe API, for code that makes many small reads and writes on a network.

Elements are referred to by their position in the id list of their type (the row order of the corresponding
dataframe), numeric columns are read into / written from numpy arrays, and nothing goes through pandas or strings
after the id lists have been fetched once::

    fast = pypowsybl.fast.FastNetwork(network)
    switch_ids = fast.ids('switch')                       # once
    p0 = fast.get('load', 'p0')                           # float64 array, in id list order
    fast.get('load', 'p0', out=p0)                        # refresh the same buffer, no allocation
    fast.set('switch', 'open', [1, 0], positions=[12, 40])
    topo = fast.apply_switches_and_get_topology([12], [0])  # switch states in, bus of every terminal out

All values are snapshots at call time, in SI units. Integer columns are int32; booleans are 0 / 1; ``NONE`` (-1)
means "no bus / no node / no component".

Bus numbers are positions in ``fast.ids('bus')`` *at the time of the call*: powsybl recomputes bus view buses after
a topology change, so they must not be kept across switch changes (use :meth:`apply_switches_and_get_topology`, or
read the bus columns again).
"""
from collections.abc import Iterable, Sequence
from os import PathLike
from typing import Any, NamedTuple

import numpy as np

import pypowsybl.network
from pypowsybl import _pypowsybl as _pp
from pypowsybl.network import Network

__all__ = ['FastNetwork', 'Topology', 'NONE', 'SWITCH_KINDS', 'SKIP_PARAMETER', 'SKIPPABLE', 'load']

#: value of an integer column when there is nothing to refer to
NONE = -1

#: meaning of the ``kind`` column of switches
SWITCH_KINDS = ('BREAKER', 'DISCONNECTOR', 'LOAD_BREAK_SWITCH')

_BUS = 'bus'

#: import parameter of ``pypowsybl.network.load`` listing (comma separated) what not to read from a .biidm file
SKIP_PARAMETER = 'pypowsybl.import.skip'

#: what can be skipped when loading a .biidm file:
#:
#: - ``temporary-limits``: the temporary limits; permanent limits are kept
#: - ``limits``: all current / active power / apparent power limits (includes ``temporary-limits``)
#: - ``bus-state``: voltage and angle stored for the buses of node/breaker voltage levels
#: - ``properties``: the properties of all elements
#: - ``extensions``: all extensions
SKIPPABLE = ('temporary-limits', 'limits', 'bus-state', 'properties', 'extensions')


def load(file: str | PathLike, skip: Iterable[str] = (), parameters: dict[str, str] | None = None,
         **kwargs: Any) -> Network:
    """Loads a binary IIDM (.biidm) file, leaving out the parts listed in ``skip`` (see :data:`SKIPPABLE`).

    Shorthand for ``pypowsybl.network.load(file, parameters={SKIP_PARAMETER: 'a,b'})``: the parameter can as well be
    given to ``pypowsybl.network.load`` directly. Other arguments are those of ``pypowsybl.network.load``.
    """
    skip = list(skip)
    unknown = [s for s in skip if s not in SKIPPABLE]
    if unknown:
        raise ValueError(f'Cannot skip {unknown}, expected values among {list(SKIPPABLE)}')
    parameters = dict(parameters or {})
    parameters[SKIP_PARAMETER] = ','.join(skip)
    return pypowsybl.network.load(file, parameters=parameters, **kwargs)


class Topology(NamedTuple):
    """Result of :meth:`FastNetwork.apply_switches_and_get_topology`.

    The ``*_bus`` arrays are views on one buffer; ``NONE`` marks a terminal which is not in a bus.
    """
    load_bus: np.ndarray
    generator_bus: np.ndarray
    line_bus1: np.ndarray
    line_bus2: np.ndarray
    two_windings_transformer_bus1: np.ndarray
    two_windings_transformer_bus2: np.ndarray
    #: connected component number of every bus; its length is the bus count
    bus_connected_component: np.ndarray


class FastNetwork:
    """Positional access to the loads, generators, lines, two windings transformers, switches, buses, voltage levels
    and busbar sections of a network. See the module documentation.

    The element lists (not the buses) are captured at construction: call :meth:`refresh` after creating or removing
    elements. A call made on a stale view raises instead of returning misaligned data.
    """

    def __init__(self, network: Network):
        self._network = network
        self._handle = _pp.fast_create(network._handle)
        self._types: dict[str, int] = {name: code for code, name in enumerate(_pp.fast_type_names())}
        # (type name, column name) -> (type code, column code, is double, writable)
        self._columns: dict[tuple[str, str], tuple[int, int, bool, bool]] = {}
        self._column_names: dict[str, list[str]] = {}
        for type_name, type_code in self._types.items():
            names = []
            for column_code, description in enumerate(_pp.fast_column_descriptions(self._handle, type_code)):
                name, kind, access = description.split(':')
                self._columns[(type_name, name)] = (type_code, column_code, kind == 'd', access == 'rw')
                names.append(name)
            self._column_names[type_name] = names
        self._ids: dict[str, np.ndarray] = {}
        self._positions: dict[str, dict[str, int]] = {}
        self._counts: dict[str, int] = {}
        self._bus_capacity: int | None = None

    @property
    def network(self) -> Network:
        return self._network

    @property
    def element_types(self) -> list[str]:
        return list(self._types)

    def columns(self, element_type: str) -> dict[str, tuple[np.dtype, bool]]:
        """Columns of an element type: name -> (dtype, writable)."""
        self._type(element_type)
        return {name: (np.dtype(np.float64 if self._columns[(element_type, name)][2] else np.int32),
                       self._columns[(element_type, name)][3])
                for name in self._column_names[element_type]}

    def refresh(self) -> None:
        """Captures the element lists again. Needed after elements have been created or removed."""
        _pp.fast_refresh(self._handle)
        self._ids.clear()
        self._positions.clear()
        self._counts.clear()
        self._bus_capacity = None

    def _type(self, element_type: str) -> int:
        try:
            return self._types[element_type]
        except KeyError:
            raise ValueError(f"Unknown element type '{element_type}', expected one of {list(self._types)}") from None

    def _column(self, element_type: str, column: str) -> tuple[int, int, bool, bool]:
        try:
            return self._columns[(element_type, column)]
        except KeyError:
            self._type(element_type)
            raise ValueError(f"Unknown column '{column}' for '{element_type}', "
                             f"expected one of {self._column_names[element_type]}") from None

    def count(self, element_type: str) -> int:
        """Number of elements of a type (for buses: at the time of the call)."""
        if element_type == _BUS:
            return _pp.fast_count(self._handle, self._type(element_type))
        n = self._counts.get(element_type)
        if n is None:
            n = _pp.fast_count(self._handle, self._type(element_type))
            self._counts[element_type] = n
        return n

    def ids(self, element_type: str) -> np.ndarray:
        """Ids of the elements of a type, in the row order of the corresponding dataframe.

        The array is fetched once and cached (do not modify it), except for buses, fetched at each call.
        """
        if element_type == _BUS:
            return np.array(_pp.fast_ids(self._handle, self._type(element_type)), dtype=object)
        ids = self._ids.get(element_type)
        if ids is None:
            ids = np.array(_pp.fast_ids(self._handle, self._type(element_type)), dtype=object)
            self._ids[element_type] = ids
        return ids

    def positions(self, element_type: str, ids: Sequence[str]) -> np.ndarray:
        """Positions of the given ids, as an int32 array (to be computed once and reused)."""
        index = None if element_type == _BUS else self._positions.get(element_type)
        if index is None:
            index = {id_: i for i, id_ in enumerate(self.ids(element_type))}
            if element_type != _BUS:  # bus ids change with the topology: never cached
                self._positions[element_type] = index
        try:
            return np.fromiter((index[id_] for id_ in ids), dtype=np.int32, count=len(ids))
        except KeyError as e:
            raise ValueError(f"No {element_type} with id {e.args[0]!r}") from None

    def get(self, element_type: str, column: str, out: np.ndarray | None = None) -> np.ndarray:
        """Reads a column for all the elements of a type, in id list order.

        :param out: optional C contiguous destination of the dtype of the column (float64 or int32) and of the
                    length of the element list; Java writes straight into it. Allocated when not given.
        :return: ``out``
        """
        type_code, column_code, is_double, _ = self._column(element_type, column)
        if out is None:
            out = np.empty(self.count(element_type), dtype=np.float64 if is_double else np.int32)
        if is_double:
            _pp.fast_get_doubles(self._handle, type_code, column_code, out)
        else:
            _pp.fast_get_ints(self._handle, type_code, column_code, out)
        return out

    def set(self, element_type: str, column: str, values: Sequence, positions: Sequence[int] | None = None) -> None:
        """Writes a column, for the elements at ``positions`` or for all the elements in id list order.

        float64 / int32 C contiguous arrays are passed to Java without a copy; anything else is converted first.
        Positions are validated before anything is modified.
        """
        type_code, column_code, is_double, writable = self._column(element_type, column)
        if not writable:
            raise ValueError(f"Column '{column}' of '{element_type}' is read only")
        where = None if positions is None else np.ascontiguousarray(positions, dtype=np.int32)
        if is_double:
            _pp.fast_set_doubles(self._handle, type_code, column_code, where,
                                 np.ascontiguousarray(values, dtype=np.float64))
        else:
            _pp.fast_set_ints(self._handle, type_code, column_code, where,
                              np.ascontiguousarray(values, dtype=np.int32))

    def set_at(self, element_type: str, column: str, position: int, value: float) -> None:
        """Writes one value of a column: the cheapest way to change a single element (e.g. open one switch)."""
        type_code, column_code, is_double, writable = self._column(element_type, column)
        if not writable:
            raise ValueError(f"Column '{column}' of '{element_type}' is read only")
        if is_double:
            _pp.fast_set_double_at(self._handle, type_code, column_code, position, value)
        else:
            _pp.fast_set_int_at(self._handle, type_code, column_code, position, int(value))

    def apply_switches_and_get_topology(self, switch_positions: Sequence[int], switch_open: Sequence[int]) -> Topology:
        """Sets the open / closed state of the given switches, then returns the bus number of every load, generator,
        line and two windings transformer terminal and the connected component number of every bus, in one call.

        The returned arrays are freshly allocated at each call (a snapshot, safe to keep).
        """
        n_load, n_gen = self.count('load'), self.count('generator')
        n_line, n_twt = self.count('line'), self.count('two_windings_transformer')
        terminal_bus = np.empty(n_load + n_gen + 2 * n_line + 2 * n_twt, dtype=np.int32)
        # the bus count is only known after the switches are applied: the previous count plus a margin is nearly
        # always enough, and a too small buffer costs one more (cheap) column read
        capacity = self._bus_capacity if self._bus_capacity is not None else self.count(_BUS) + 256
        bus_component = np.empty(capacity, dtype=np.int32)
        n_bus = _pp.fast_apply_switches_and_get_topology(
            self._handle, np.ascontiguousarray(switch_positions, dtype=np.int32),
            np.ascontiguousarray(switch_open, dtype=np.int32), terminal_bus, bus_component)
        if n_bus > capacity:
            bus_component = self.get(_BUS, 'connected_component')
        self._bus_capacity = n_bus + 256
        o1 = n_load
        o2 = o1 + n_gen
        o3 = o2 + n_line
        o4 = o3 + n_line
        o5 = o4 + n_twt
        return Topology(terminal_bus[:o1], terminal_bus[o1:o2], terminal_bus[o2:o3], terminal_bus[o3:o4],
                        terminal_bus[o4:o5], terminal_bus[o5:], bus_component[:n_bus])

    def internal_connections(self) -> np.ndarray:
        """Internal connections of all the node/breaker voltage levels: an (n, 3) int32 array of
        (voltage level position, node1, node2).

        With the ``node*`` and ``voltage_level*`` columns of switches, busbar sections and terminals, this gives the
        node/breaker graph of the whole network without one call per voltage level.
        """
        n = _pp.fast_internal_connections(self._handle, np.empty(0, dtype=np.int32))
        out = np.empty((n, 3), dtype=np.int32)
        if n > 0 and _pp.fast_internal_connections(self._handle, out.reshape(-1)) != n:
            raise RuntimeError('internal connections changed during the call')
        return out
