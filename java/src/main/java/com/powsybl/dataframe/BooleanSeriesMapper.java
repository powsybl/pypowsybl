package com.powsybl.dataframe;

import java.util.List;
import java.util.function.Predicate;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public class BooleanSeriesMapper<T> extends SeriesMapper<T> {

    private final BooleanUpdater<T> updater;
    private final Predicate<T> value;

    @FunctionalInterface
    public interface BooleanUpdater<U> {
        void update(U object, boolean value);
    }

    public BooleanSeriesMapper(String name, Predicate<T> value) {
        this(name, value, null);
    }

    public BooleanSeriesMapper(String name, Predicate<T> value, BooleanUpdater<T> updater) {
        super(name, false);
        this.updater = updater;
        this.value = value;
    }

    @Override
    public void createSeries(List<T> items, DataframeHandler handler) {
        DataframeHandler.BooleanSeriesWriter writer = handler.newBooleanSeries(name, items.size());
        for (int i = 0; i < items.size(); i++) {
            writer.set(i, value.test(items.get(i)));
        }
    }

    @Override
    public void updateInt(T object, int value) {
        updater.update(object, value == 1);
    }
}
