package com.powsybl.dataframe.serie;

import java.util.HashMap;
import java.util.Map;

public class Dataframe {
    private final Map<String, IntSerie> intSeries = new HashMap<>();
    private final Map<String, StringSerie> stringSeries = new HashMap<>();
    private final Map<String, DoubleSerie> doubleSeries = new HashMap<>();

    public Dataframe() {

    }

    public void addIntSerie(IntSerie intSerie) {
        this.intSeries.put(intSerie.getName(), intSerie);
    }

    public void addStringSerie(StringSerie stringSerie) {
        this.stringSeries.put(stringSerie.getName(), stringSerie);
    }

    public void addDoubleSerie(DoubleSerie doubleSerie) {
        this.doubleSeries.put(doubleSerie.getName(), doubleSerie);
    }
}
