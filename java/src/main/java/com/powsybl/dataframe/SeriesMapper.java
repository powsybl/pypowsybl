package com.powsybl.dataframe;

import java.util.List;

/**
 * Base class to define a mapping between objects and a series view.
 * It defines a name for the series, a way to retrieve data from the underlying objects
 * as a series, and a way to write data to the objects from a series input.
 *
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public class SeriesMapper<T> {

    protected final String name;
    protected final boolean index;

    public SeriesMapper(String name) {
        this(name, false);
    }

    public SeriesMapper(String name, boolean index) {
        this.name = name;
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void createSeries(List<T> items, DataframeHandler factory) {
        throw new UnsupportedOperationException("Cannot update series with int: " + name);
    }

    public void updateInt(T object, int value) {
        throw new UnsupportedOperationException("Cannot update series with int: " + name);
    }

    public void updateDouble(T object, double value) {
        throw new UnsupportedOperationException("Cannot update series with double: " + name);
    }

    public void updateString(T object, String value) {
        throw new UnsupportedOperationException("Cannot update series with string: " + name);
    }
}
