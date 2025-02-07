package com.powsybl.python.rao;

import com.powsybl.iidm.network.Country;
import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.MnecParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.PtdfApproximation;
import com.powsybl.openrao.raoapi.parameters.extensions.RelativeMarginsParametersExtension;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RaoUtilsTests {

    @Test
    void testExtensionsConversions() {

        // Separated map
        MnecParametersExtension mnecExt = new MnecParametersExtension();
        mnecExt.setAcceptableMarginDecrease(100.0);
        mnecExt.setViolationCost(99.0);
        mnecExt.setConstraintAdjustmentCoefficient(50.0);
        Map<String, String> mapRep = RaoUtils.mnecParametersExtensionToMap(mnecExt);
        MnecParametersExtension mnecExt2 = RaoUtils.buildMnecParametersExtension(mapRep);
        compareMnecExtensions(mnecExt, mnecExt2);

        RelativeMarginsParametersExtension relativeMarginsExt = new RelativeMarginsParametersExtension();
        relativeMarginsExt.setPtdfSumLowerBound(49.0);
        relativeMarginsExt.setPtdfApproximation(PtdfApproximation.UPDATE_PTDF_WITH_TOPO);
        relativeMarginsExt.setPtdfBoundariesFromString(List.of("{FR}-{BE}", "{FR}-{DE}", "{BE}-{NL}", "{NL}-{DE}]"));
        Map<String, String> mapRep2 = RaoUtils.relativeMarginsParametersExtensionToMap(relativeMarginsExt);
        RelativeMarginsParametersExtension relativeMarginsExt2 = RaoUtils.buildRelativeMarginsParametersExtension(mapRep2);
        compareRelativeMargingExtensions(relativeMarginsExt, relativeMarginsExt2);

        LoopFlowParametersExtension loopFlowExt = new LoopFlowParametersExtension();
        loopFlowExt.setAcceptableIncrease(38.0);
        loopFlowExt.setPtdfApproximation(PtdfApproximation.UPDATE_PTDF_WITH_TOPO_AND_PST);
        loopFlowExt.setConstraintAdjustmentCoefficient(62.0);
        loopFlowExt.setCountries(Set.of(Country.AD, Country.AE));
        Map<String, String> mapRep3 = RaoUtils.loopFlowParametersExtensionToMap(loopFlowExt);
        LoopFlowParametersExtension loopFlowExt2 = RaoUtils.buildLoopFlowParametersExtension(mapRep3);
        compareLoopFlowExtension(loopFlowExt, loopFlowExt2);

        // Single map
        Map<String, String> mergedMap = new HashMap<>();
        mergedMap.putAll(mapRep);
        mergedMap.putAll(mapRep2);
        mergedMap.putAll(mapRep3);

        MnecParametersExtension mnecExt3 = RaoUtils.buildMnecParametersExtension(mergedMap);
        RelativeMarginsParametersExtension relativeMarginsExt3 = RaoUtils.buildRelativeMarginsParametersExtension(mergedMap);
        LoopFlowParametersExtension loopFlowExt3 = RaoUtils.buildLoopFlowParametersExtension(mergedMap);
        compareMnecExtensions(mnecExt3, mnecExt);
        compareRelativeMargingExtensions(relativeMarginsExt3, relativeMarginsExt);
        compareLoopFlowExtension(loopFlowExt3, loopFlowExt);
    }

    void compareMnecExtensions(MnecParametersExtension mnecExt, MnecParametersExtension mnecExt2) {
        assertEquals(mnecExt.getAcceptableMarginDecrease(), mnecExt2.getAcceptableMarginDecrease());
        assertEquals(mnecExt.getViolationCost(), mnecExt2.getViolationCost());
        assertEquals(mnecExt.getConstraintAdjustmentCoefficient(), mnecExt2.getConstraintAdjustmentCoefficient());
    }

    void compareRelativeMargingExtensions(RelativeMarginsParametersExtension relativeMarginsExt, RelativeMarginsParametersExtension relativeMarginsExt2) {
        assertEquals(relativeMarginsExt.getPtdfSumLowerBound(), relativeMarginsExt2.getPtdfSumLowerBound());
        assertEquals(relativeMarginsExt.getPtdfApproximation(), relativeMarginsExt2.getPtdfApproximation());
        assertEquals(relativeMarginsExt.getPtdfBoundariesAsString(), relativeMarginsExt2.getPtdfBoundariesAsString());
    }

    void compareLoopFlowExtension(LoopFlowParametersExtension loopFlowExt, LoopFlowParametersExtension loopFlowExt2) {
        assertEquals(loopFlowExt.getAcceptableIncrease(), loopFlowExt2.getAcceptableIncrease());
        assertEquals(loopFlowExt.getPtdfApproximation(), loopFlowExt2.getPtdfApproximation());
        assertEquals(loopFlowExt.getConstraintAdjustmentCoefficient(), loopFlowExt2.getConstraintAdjustmentCoefficient());
        assertEquals(loopFlowExt.getCountries(), loopFlowExt2.getCountries());
    }
}
