package com.powsybl.dataframe.update;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public class TestDoubleSeries implements DoubleSeries {

    private final List<Double> values = new ArrayList<>();

    public TestDoubleSeries(List<Double> values) {
        this.values.addAll(values);
    }

    @Override
    public double get(int index) {
        return values.get(index);
    }
}
