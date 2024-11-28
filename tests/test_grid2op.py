import numpy as np
import numpy.testing as npt
import pytest

import pypowsybl as pp
from pypowsybl import grid2op

TOLERANCE = 1e-6

@pytest.fixture(autouse=True)
def no_config():
    pp.set_config_read(False)

def test_backend():
    n = pp.network.create_eurostag_tutorial_example1_network()
    with grid2op.Backend(n) as backend:
        assert ['GEN', 'GEN2'] == backend.get_generator_name()
        npt.assert_allclose(np.array([607.0, 0.0]), backend.get_generator_p(), rtol=TOLERANCE, atol=TOLERANCE)
