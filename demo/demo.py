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
for bus in n.buses:
    print("Bus '{id}': v_mag={v_mag}, v_ang={v_ang}".format(id=bus.id, v_mag=bus.v_magnitude, v_ang=bus.v_angle))
