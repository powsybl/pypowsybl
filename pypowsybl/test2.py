import logging
from math import hypot, atan2

import pyoptinterface as poi
from pyoptinterface import nlfunc, ipopt

import pypowsybl as pp
from pypowsybl import PyPowsyblError
from pypowsybl.opf import OptimalPowerFlow

if __name__ == "__main__":
    import pandas as pd

    pd.options.display.max_columns = None
    pd.options.display.expand_frame_repr = False

    n = pp.network.create_ieee118()
#    n = pp.network.create_eurostag_tutorial_example1_network()
    opf = OptimalPowerFlow(n)
    opf.run()

    parameters = pp.loadflow.Parameters(voltage_init_mode=pp.loadflow.VoltageInitMode.PREVIOUS_VALUES)
    logging.basicConfig(level=logging.DEBUG)
    logging.getLogger('powsybl').setLevel(logging.DEBUG)
    r = pp.loadflow.run_ac(n, parameters)
    print(r)
