import logging

from pypowsybl.opf.impl.model.network_cache import NetworkCache

logger = logging.getLogger(__name__)


class DetailedDcNetworkValidator:
    """ 
    Pre-solver verification for current ACDC OPF implementation convention

    Fatal checks:
    
    - each VSC terminal voltage zone must use one nominal voltage base;
    """

    def __init__(self, network_cache: NetworkCache):
        self._network_cache = network_cache
    
    def validate(self) -> None:

        if self._network_cache.dc_nodes.empty:
            logger.info("No DC node, skipping detailed DC network validation")
            return
        self._check_vsc_terminal_nominal_voltage()



    def _check_vsc_terminal_nominal_voltage(self):
        dc_nodes = self._network_cache.dc_nodes
        vscs = self._network_cache.voltage_source_converters

        if vscs.empty:
            logger.info("No VSC, skipping VSC terminal voltage zone nominal voltage consistency check")
            return
        
        for vsc_id, row in vscs.iterrows():
            dc_node1_id = row.dc_node1_id
            dc_node2_id = row.dc_node2_id

            nominal_v1 = dc_nodes.loc[dc_node1_id, "nominal_v"]
            nominal_v2 = dc_nodes.loc[dc_node2_id, "nominal_v"]

            if nominal_v1 != nominal_v2:

                raise ValueError(
                    "Invalid detailed-DC VSC terminal voltage zone for ACDC OPF.\n\n"
                    f"VSC '{vsc_id}' connects DC nodes with different nominal voltages:\n"
                    f"- dc_node1 = {dc_node1_id}, nominal_v = {nominal_v1} kV\n"
                    f"- dc_node2 = {dc_node2_id}, nominal_v = {nominal_v2} kV\n\n"
                    "ACDC OPF currently assumes one nominal voltage base inside a VSC terminal voltage zone.\n"
                    )
