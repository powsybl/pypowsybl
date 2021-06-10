package com.powsybl.dataframe;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public interface DoubleIndexedSeries {

    int getSize();

    String getId(int index);

    double getValue(int index);
}
