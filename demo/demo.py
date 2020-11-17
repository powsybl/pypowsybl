import gridpy.network
import gridpy.loadflow
import gridpy as gp

# Print the version of PowSyBl modules
gp.print_version()

# Create the IEEE 14 buses network
n = gp.network.create_ieee14()

# Run a load flow computation and print the result
r = gp.loadflow.run(n)
print(r.is_ok())

# Print calculated voltages
for bus in n.get_buses():
    print("Bus '{id}': v_mag={v_mag}, v_ang={v_ang}".format(id=bus.get_id(), v_mag=bus.get_v_magnitude(), v_ang=bus.get_v_angle()))
