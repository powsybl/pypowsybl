package com.powsybl.dataframe;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;
import com.powsybl.python.PyPowsyblApiHeader;

import java.util.Map;

/**
 * Main user entry point of the package :
 * defines the mappings for all elements of the network.
 *
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public final class NetworkDataframes {

    private static final Map<PyPowsyblApiHeader.ElementType, DataframeMapper> ARRAY_PROVIDERS =
        Map.of(
            PyPowsyblApiHeader.ElementType.BUS, buses(),
            PyPowsyblApiHeader.ElementType.GENERATOR, generators(),
            PyPowsyblApiHeader.ElementType.LOAD, loads()
        );

    private NetworkDataframes() {
    }

    public static DataframeMapper generators() {
        return DataframeMapperBuilder.ofStream(Network::getGeneratorStream, NetworkDataframes::getGeneratorOrThrowsException)
            .strings("id", Generator::getId)
            .enums("energy_source", EnergySource.class, Generator::getEnergySource)
            .doubles("target_p", Generator::getTargetP, Generator::setTargetP)
            .doubles("max_p", Generator::getMaxP, Generator::setMaxP)
            .doubles("min_p", Generator::getMinP, Generator::setMinP)
            .doubles("target_v", Generator::getTargetV, Generator::setTargetV)
            .doubles("target_q", Generator::getTargetQ, Generator::setTargetQ)
            .booleans("voltage_regulator_on", Generator::isVoltageRegulatorOn, Generator::setVoltageRegulatorOn)
            .doubles("p", g -> g.getTerminal().getP(), (g, p) -> g.getTerminal().setP(p))
            .doubles("q", g -> g.getTerminal().getQ(), (g, q) -> g.getTerminal().setQ(q))
            .strings("voltage_level_id",  g -> g.getTerminal().getVoltageLevel().getId())
            .strings("bus_id",  g -> getBusId(g.getTerminal()))
            .build();
    }

    public static DataframeMapper buses() {
        return DataframeMapperBuilder.ofStream(n -> n.getBusView().getBusStream())
            .strings("id", Bus::getId)
            .doubles("v_mag", Bus::getV)
            .doubles("v_angle", Bus::getAngle)
            .build();
    }

    public static DataframeMapper loads() {
        return DataframeMapperBuilder.ofStream(Network::getLoadStream, NetworkDataframes::getLoadOrThrowsException)
            .stringsIndex("id", Load::getId)
            .enums("type", LoadType.class, Load::getLoadType)
            .doubles("p0", Load::getP0, Load::setP0)
            .doubles("q0", Load::getQ0, Load::setQ0)
            .doubles("p", l -> l.getTerminal().getP(), (l, p) -> l.getTerminal().setP(p))
            .doubles("q",  l -> l.getTerminal().getQ(), (l, q) -> l.getTerminal().setQ(q))
            .strings("voltage_level_id",  g -> g.getTerminal().getVoltageLevel().getId())
            .strings("bus_id",  g -> getBusId(g.getTerminal()))
            .build();
    }

    public static DataframeMapper getDataframeMapper(PyPowsyblApiHeader.ElementType type) {
        return ARRAY_PROVIDERS.get(type);
    }

    private static String getBusId(Terminal t) {
        Bus bus = t.getBusView().getBus();
        return bus != null ? bus.getId() : "";
    }

    private static TwoWindingsTransformer getTransformerOrThrowsException(String id, Network network) {
        TwoWindingsTransformer twt = network.getTwoWindingsTransformer(id);
        if (twt == null) {
            throw new PowsyblException("Two windings transformer '" + id + "' not found");
        }
        return twt;
    }

    private static RatioTapChanger getRatioTapChanger(TwoWindingsTransformer twt) {
        RatioTapChanger rtc = twt.getRatioTapChanger();
        if (rtc == null) {
            throw new PowsyblException("Two windings transformer '" + twt.getId() + "' does not have a ratio tap changer");
        }
        return rtc;
    }

    private static PhaseTapChanger getPhaseTapChanger(TwoWindingsTransformer twt) {
        PhaseTapChanger ptc = twt.getPhaseTapChanger();
        if (ptc == null) {
            throw new PowsyblException("Two windings transformer '" + twt.getId() + "' does not have a phase tap changer");
        }
        return ptc;
    }

    private static Generator getGeneratorOrThrowsException(Network network, String id) {
        Generator g = network.getGenerator(id);
        if (g == null) {
            throw new PowsyblException("Generator '" + id + "' not found");
        }
        return g;
    }

    private static Switch getSwitchOrThrowsException(String id, Network network) {
        Switch sw = network.getSwitch(id);
        if (sw == null) {
            throw new PowsyblException("Switch '" + id + "' not found");
        }
        return sw;
    }

    private static Load getLoadOrThrowsException(Network network, String id) {
        Load load = network.getLoad(id);
        if (load == null) {
            throw new PowsyblException("Load '" + id + "' not found");
        }
        return load;
    }

    private static Battery getBatteryOrThrowsException(String id, Network network) {
        Battery b = network.getBattery(id);
        if (b == null) {
            throw new PowsyblException("Battery '" + id + "' not found");
        }
        return b;
    }

    private static DanglingLine getDanglingLineOrThrowsException(String id, Network network) {
        DanglingLine l = network.getDanglingLine(id);
        if (l == null) {
            throw new PowsyblException("DanglingLine '" + id + "' not found");
        }
        return l;
    }

    private static VscConverterStation getVscOrThrowsException(String id, Network network) {
        VscConverterStation vsc = network.getVscConverterStation(id);
        if (vsc == null) {
            throw new PowsyblException("VscConverterStation '" + id + "' not found");
        }
        return vsc;
    }

    private static StaticVarCompensator getSvcOrThrowsException(String id, Network network) {
        StaticVarCompensator svc = network.getStaticVarCompensator(id);
        if (svc == null) {
            throw new PowsyblException("StaticVarCompensator '" + id + "' not found");
        }
        return svc;
    }

    private static HvdcLine getHvdcOrThrowsException(String id, Network network) {
        HvdcLine hvdc = network.getHvdcLine(id);
        if (hvdc == null) {
            throw new PowsyblException("HvdcLine '" + id + "' not found");
        }
        return hvdc;
    }
}
