import logging

from pypowsybl.opf.impl.model.network_cache import NetworkCache

logger = logging.getLogger(__name__)


class AcdcNetworkValidator:
    """ 
    Pre-solver verification for current ACDC OPF implementation 

    Fatal checks:
    
    - each VSC terminal voltage zone must use one nominal voltage base;
    """

    def __init__(self, network_cache: NetworkCache):
        self._network_cache = network_cache
        
    def validate(self) -> None:
        if self._network_cache.dc_nodes.empty:
            return

        self._check_all_dc_nodes_have_same_nominal_voltage()
        self._check_dc_line_components_have_vdc_converter()

   
    def _check_all_dc_nodes_have_same_nominal_voltage(self) -> None:
        dc_nodes = self._network_cache.dc_nodes

        if dc_nodes.empty:
            return

        if dc_nodes["nominal_v"].isna().any():
            nodes_without_nominal_v = sorted(dc_nodes[dc_nodes["nominal_v"].isna()].index)

            raise ValueError(
                "\nInvalid detailed-DC network for ACDC OPF.\n\n"
                "Some DC nodes have no nominal voltage:\n"
                f"{nodes_without_nominal_v}"
            )

        nominal_voltages = dc_nodes["nominal_v"].unique()

        if len(nominal_voltages) != 1:
            raise ValueError(
                "\nInvalid detailed-DC network for ACDC OPF.\n\n"
                "All DC nodes must have the same nominal voltage.\n\n"
                f"Found nominal voltages: {sorted(nominal_voltages)}\n"
                "ACDC OPF currently requires one nominal voltage base for the whole DC network."
            )



    def _check_dc_line_components_have_vdc_converter(self) -> None:
        """
        Warn when a DC-line connected component has no VSC in V_DC mode.

        Graph definition:
        - nodes = detailed DC nodes
        - edges = detailed DC lines only

        This is a warning, not a fatal validation error.
        """
        dc_nodes = self._network_cache.dc_nodes
        dc_lines = self._network_cache.dc_lines
        vscs = self._network_cache.voltage_source_converters

        if dc_lines.empty:
            return

        adjacency = {dc_node_id: set() for dc_node_id in dc_nodes.index}

        for _, row in dc_lines.iterrows():
            node1 = row.dc_node1_id
            node2 = row.dc_node2_id

            adjacency[node1].add(node2)
            adjacency[node2].add(node1)

        vdc_controlled_nodes = set()

        if not vscs.empty:
            for _, row in vscs.iterrows():
                if row.control_mode == "V_DC":
                    vdc_controlled_nodes.add(row.dc_node1_id)
                    vdc_controlled_nodes.add(row.dc_node2_id)

        visited = set()

        for start_node in dc_nodes.index:
            if start_node in visited:
                continue

            if not adjacency[start_node]:
                visited.add(start_node)
                continue

            component_nodes = set()
            stack = [start_node]
            visited.add(start_node)

            while stack:
                node = stack.pop()
                component_nodes.add(node)

                for neighbor in adjacency[node]:
                    if neighbor not in visited:
                        visited.add(neighbor)
                        stack.append(neighbor)

            if component_nodes.isdisjoint(vdc_controlled_nodes):
                component_lines = []

                for dc_line_id, row in dc_lines.iterrows():
                    if row.dc_node1_id in component_nodes and row.dc_node2_id in component_nodes:
                        component_lines.append(dc_line_id)

                logger.warning(
                    "\nDetailed-DC topology warning for ACDC OPF.\n\n"
                    "A DC-line connected component has no VSC in V_DC mode attached.\n\n"
                    f"DC component nodes:\n{sorted(component_nodes)}\n\n"
                    f"DC component lines:\n{sorted(component_lines)}\n\n"
                    "This may be valid for future or advanced DC control formulations, but the current "
                    "ACDC OPF formulation may need at least one DC-voltage controller per modeled "
                    "DC-line component for convergence."
                )
