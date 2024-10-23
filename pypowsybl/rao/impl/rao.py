import io

from pypowsybl import _pypowsybl
from pypowsybl.network import Network

class Rao:
    """
    Allows to run a remedial action optimisation on a network
    """

    def __init__(self, handle: _pypowsybl.JavaHandle):
        self._handle = handle

    def run(self, network: Network, crac_file: str, parameter_file: str, glsk_file: str) -> None:
        _pypowsybl.run_rao(network._handle, self._handle, crac_file, parameter_file, glsk_file)

    def run_with_buffers(self, network: Network, crac_buffer: io.BytesIO, parameter_buffer: io.BytesIO, glsk_buffer: io.BytesIO) -> None:
        _pypowsybl.run_rao_from_buffers(network._handle, self._handle, crac_buffer.getbuffer(), parameter_buffer.getbuffer(), glsk_buffer.getbuffer())

    def serialize_rao_results(self, output_file: str) -> None:
        _pypowsybl.serialize_rao_results_to_file(self._handle, output_file)

    def serialize_rao_results_to_buffer(self) -> io.BytesIO:
        return io.BytesIO(_pypowsybl.serialize_rao_results_to_buffer(self._handle))