import pypowsybl.network
import pypowsybl.loadflow
import pypowsybl as pp

# Print the version of PowSyBl modules
pp.print_version()

# Create the IEEE 14 buses network
n = pp.network.create_ieee14()

# Run a load flow computation and print the result
results = pp.loadflow.run_ac(n)
for result in results:
    print(result)

# Print calculated voltages
buses = n.get_buses()
print(buses)