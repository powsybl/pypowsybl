package com.powsybl.dataframe.serie;

import org.graalvm.nativeimage.c.type.CCharPointer;

public class StringSerie extends Serie {

    private final CCharPointer values;

    public StringSerie(String name, int size, CCharPointer values) {
        super(name, size);
        this.values = values;
    }

    public CCharPointer getValues() {
        return values;
    }
}
