package com.powsybl.dataframe.update;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public class TestDoubleSeries implements DoubleSeries {

    private final List<Double> values;

    public TestDoubleSeries(double... values) {
        this.values = Arrays.stream(values).boxed().collect(Collectors.toList());
    }

    @Override
    public double get(int index) {
        return values.get(index);
    }
}
