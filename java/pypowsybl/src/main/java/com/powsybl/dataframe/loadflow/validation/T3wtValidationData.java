package com.powsybl.dataframe.loadflow.validation;

import com.powsybl.iidm.network.util.TwtData;

/**
 * @author Yichen TANG {@literal <yichen.tang at rte-france.com>}
 */
class T3wtValidationData {
    String id;
    TwtData twtData;
    boolean validated;

    T3wtValidationData(String id, TwtData twtData, boolean validated) {
        this.id = id;
        this.twtData = twtData;
        this.validated = validated;
    }

    String getId() {
        return id;
    }

    TwtData getTwtData() {
        return twtData;
    }

    boolean isValidated() {
        return validated;
    }
}
