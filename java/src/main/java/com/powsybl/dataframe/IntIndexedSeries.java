package com.powsybl.dataframe;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public interface IntIndexedSeries {

    int getSize();

    String getId(int index);

    int getValue(int index);
}
