package com.powsybl.dataframe.serie;

import org.graalvm.nativeimage.c.type.CIntPointer;

public class IntSerie extends Serie {

    private final CIntPointer values;

    public IntSerie(String name, int size, CIntPointer values) {
        super(name, size);
        this.values = values;
    }

    public CIntPointer getValues() {
        return values;
    }
}
