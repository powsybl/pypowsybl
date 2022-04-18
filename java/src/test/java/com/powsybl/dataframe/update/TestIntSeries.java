package com.powsybl.dataframe.update;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public class TestIntSeries implements IntSeries {

    private final List<Integer> values;

    public TestIntSeries(int... values) {
        this.values = Arrays.stream(values).boxed().collect(Collectors.toList());
    }

    public int get(int index) {
        return values.get(index);
    }
}
