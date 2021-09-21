package com.powsybl.dataframe.serie;

import org.graalvm.nativeimage.c.type.CDoublePointer;

public class DoubleSerie extends Serie {

    CDoublePointer values;

    public DoubleSerie(String name, int size, CDoublePointer values) {
        super(name, size);
        this.values = values;
    }

    public CDoublePointer getValues() {
        return values;
    }
}
