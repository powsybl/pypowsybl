package com.powsybl.dataframe.update;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public class TestStringSeries implements StringSeries {

    private final List<String> values = new ArrayList<>();

    public TestStringSeries(List<String> values) {
        this.values.addAll(values);
    }

    @Override
    public String get(int index) {
        return values.get(index);
    }
}
