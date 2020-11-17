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

