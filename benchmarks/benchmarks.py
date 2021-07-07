# Write the benchmarking functions here.
# See "Writing benchmarks" in the asv docs for more information.
import pypowsybl.network
import pypowsybl.loadflow
import pypowsybl as pp

class TimeSuite:
    """
    An example benchmark that times the performance of various kinds
    of iterating over dictionaries in Python.
    """
    def setup(self):
        self.n = pp.network.create_eurostag_tutorial_example1_network()
        for x in range(500):
            self.d[x] = None

    def time_loadflow(self):
        pp.loadflow.run_ac(self.n)

    # def time_iterkeys(self):
    #     for key in self.d.iterkeys():
    #         pass
    #
    # def time_range(self):
    #     d = self.d
    #     for key in range(500):
    #         x = d[key]
    #
    # def time_xrange(self):
    #     d = self.d
    #     for key in xrange(500):
    #         x = d[key]


class MemSuite:
    def mem_list(self):
        return [0] * 256
