package com.powsybl.dataframe.update;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public class TestIntSeries implements IntSeries {

    private final List<Integer> values = new ArrayList<>();

    public TestIntSeries(List<Integer> values) {
        this.values.addAll(values);
    }

    public int get(int index) {
        return values.get(index);
    }
}
