import gridpy.network
import gridpy.loadflow
import gridpy as gp
from _gridpy import LoadFlowParameters as Parameters

# Print the version of PowSyBl modules
gp.print_version()

# Create the IEEE 14 buses network
n = gp.network.create_ieee14()

# Run a load flow computation and print the result
results = gp.loadflow.run_ac(n)
for result in results:
    print(result)

# Print calculated voltages
for bus in n.buses:
    print("Bus '{id}': v_mag={v_mag}, v_ang={v_ang}".format(id=bus.id, v_mag=bus.v_magnitude, v_ang=bus.v_angle))
