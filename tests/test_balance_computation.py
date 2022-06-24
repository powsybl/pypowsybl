import pypowsybl as pp
import pypowsybl.loadflow as lf
import pypowsybl.balancecomputation as bc

lfparameters = lf.Parameters(distributed_slack=False, provider_parameters={'maxIteration': '5'})
bcParameters = bc.BalanceComputationParameters(lfparameters, 20.0, 5)

n1 = pp.network.create_ieee14()
n2 = pp.network.create_ieee14() 

listNetworks = [n1, n2]
balanceComputation = bc.BalanceComputation()
result = balanceComputation.run(listNetworks, bcParameters)
print(result.status)
print(result.iteration_count)