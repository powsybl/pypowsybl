package com.powsybl.dataframe.serie;

public class Serie {

    private final String name;
    private final int size;

    public Serie(String name, int size) {
        this.name = name;
        this.size = size;
    }

    public String getName() {
        return name;
    }
}
