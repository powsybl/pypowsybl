import pypowsybl as pp
import pypowsybl.network as pn
import pypowsybl.opf as opf


def test_public_api_exposure():
    assert hasattr(opf, "OptimalPowerFlow")
    assert hasattr(opf, "run_ac")
    assert hasattr(opf, "OptimalPowerFlowParameters")
    assert hasattr(opf, "OptimalPowerFlowMode")
    assert hasattr(opf, "SolverType")


def test_basic_opf_runs():
    n = pn.create_ieee14()

    params = opf.OptimalPowerFlowParameters(
        mode=opf.OptimalPowerFlowMode.LOADFLOW,
        solver_type=opf.SolverType.IPOPT,
    )

    success = opf.run_ac(n, params)

    assert isinstance(success, bool)


def test_fluent_api():
    n = pn.create_ieee14()

    params = (
        opf.OptimalPowerFlowParameters()
        .with_mode(opf.OptimalPowerFlowMode.REDISPATCHING)
        .with_solver_type(opf.SolverType.IPOPT)
        .with_default_voltage_bounds((0.95, 1.05))
        .with_solver_option("max_iter", 50)
    )

    success = opf.run_ac(n, params)

    assert isinstance(success, bool)


def test_enum_values():
    assert opf.OptimalPowerFlowMode.LOADFLOW.name == "LOADFLOW"
    assert opf.SolverType.IPOPT.name == "IPOPT"


def test_acdc_mode_smoke():
    n = pn.create_ieee14()

    params = opf.OptimalPowerFlowParameters(
        mode=opf.OptimalPowerFlowMode.ACDC,
        solver_type=opf.SolverType.IPOPT,
    )

    success = opf.run_ac(n, params)

    assert isinstance(success, bool)