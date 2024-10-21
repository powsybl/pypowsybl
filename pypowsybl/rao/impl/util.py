from pypowsybl import _pypowsybl
from .rao import Rao

def create_rao() -> Rao:
    """ Creates a rao objet, which can be used to run a remedial action optimisation on a network
    Returns:
        A rao object
    """
    return Rao(_pypowsybl.create_rao())