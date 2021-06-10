package com.powsybl.dataframe;

import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public abstract class AbstractDataframeMapper<T extends Identifiable<T>> implements DataframeMapper {

    protected final Map<String, SeriesMapper<T>> seriesMappers;

    public AbstractDataframeMapper(List<SeriesMapper<T>> seriesMappers) {
        this.seriesMappers = seriesMappers.stream().collect(toImmutableMap(SeriesMapper::getName, Function.identity()));
    }

    public AbstractDataframeMapper(SeriesMapper<T>... seriesMappers) {
        this(Arrays.asList(seriesMappers));
    }

    public void createDataframe(Network network, DataframeHandler dataframeHandler) {
        dataframeHandler.allocate(seriesMappers.size());
        List<T> items = getObjects(network);
        seriesMappers.values().stream().forEach(mapper -> mapper.createSeries(items, dataframeHandler));
        addPropertiesSeries(items);
    }

    private void addPropertiesSeries(List<T> items) {
        Set<String> propertyNames = items.stream()
            .filter(Identifiable::hasProperty)
            .flatMap(e -> e.getPropertyNames().stream())
            .collect(Collectors.toSet());
        for (String propertyName : propertyNames) {
            new StringSeriesMapper<T>(propertyName, t -> t.getProperty(propertyName));
        }
    }

    @Override
    public void updateDoubleSeries(Network network, String seriesName, DoubleIndexedSeries values) {
        SeriesMapper<T> series = seriesMappers.get(seriesName);
        for (int i = 0; i < values.getSize(); i++) {
            series.updateDouble(getObject(network, values.getId(i)), values.getValue(i));
        }
    }

    @Override
    public void updateIntSeries(Network network, String seriesName, IntIndexedSeries values) {
        SeriesMapper<T> series = seriesMappers.get(seriesName);
        for (int i = 0; i < values.getSize(); i++) {
            series.updateInt(getObject(network, values.getId(i)), values.getValue(i));
        }
    }

    @Override
    public void updateStringSeries(Network network, String seriesName, IndexedSeries<String> values) {
        SeriesMapper<T> series = seriesMappers.get(seriesName);
        for (int i = 0; i < values.getSize(); i++) {
            series.updateString(getObject(network, values.getId(i)), values.getValue(i));
        }
    }

    protected abstract List<T> getObjects(Network network);

    protected abstract T getObject(Network network, String id);

}
