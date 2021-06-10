package com.powsybl.dataframe;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public interface IndexedSeries<T> {

    int getSize();

    String getId(int index);

    T getValue(int index);
}
