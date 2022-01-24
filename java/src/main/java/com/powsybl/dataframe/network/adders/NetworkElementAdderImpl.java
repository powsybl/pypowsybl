package com.powsybl.dataframe.network.adders;

import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;

import java.util.List;

/**
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public class NetworkElementAdderImpl<A, T> {

    private final AdderFactory<A> adderFactory;
    private final ElementAdder<T, A> elementAdder;
    private final List<Setter> setters;

    public class Builder {

    }

    @FunctionalInterface
    interface AdderFactory<A> {
        A createAdder(UpdatingDataframe elements, int idx);
    }

    @FunctionalInterface
    interface ElementAdder<T, A> {
        T addElement(A adder);
    }

    @FunctionalInterface
    interface Setter {
        void set(UpdatingDataframe elements, int idx);
    }

    void addElements(Network network, UpdatingDataframe elements) {
        for (int idx = 0; idx < elements.getLineCount(); idx++) {
            int i = idx;
            A adder = adderFactory.createAdder(elements, idx);
            setters.forEach(s -> s.set(elements, i));
            elementAdder.addElement(adder);
        }
    }

}
