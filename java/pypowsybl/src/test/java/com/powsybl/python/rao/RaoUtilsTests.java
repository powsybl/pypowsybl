package com.powsybl.python.rao;

import com.powsybl.iidm.network.Country;
import com.powsybl.openrao.raoapi.parameters.LoopFlowParameters;
import com.powsybl.openrao.raoapi.parameters.MnecParameters;
import com.powsybl.openrao.raoapi.parameters.RelativeMarginsParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.PtdfApproximation;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoLoopFlowParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoMnecParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRelativeMarginsParameters;
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
        MnecParameters mnecExt = new MnecParameters();
        mnecExt.setAcceptableMarginDecrease(100.0);
        Map<String, String> mapRep = RaoUtils.mnecParametersExtensionToMap(mnecExt);
        MnecParameters mnecExt2 = RaoUtils.buildMnecParametersExtension(mapRep);
        compareMnecExtensions(mnecExt, mnecExt2);

        SearchTreeRaoMnecParameters mnecStExt = new SearchTreeRaoMnecParameters();
        mnecStExt.setViolationCost(99.0);
        mnecStExt.setConstraintAdjustmentCoefficient(50.0);
        Map<String, String> mapRep2 = RaoUtils.mnecParametersExtensionToMap(mnecStExt);
        SearchTreeRaoMnecParameters mnecStExt2 = RaoUtils.buildMnecSearchTreeParametersExtension(mapRep2);
        compareMnecExtensions(mnecStExt, mnecStExt2);

        RelativeMarginsParameters relativeMarginsExt = new RelativeMarginsParameters();
        relativeMarginsExt.setPtdfBoundariesFromString(List.of("{FR}-{BE}", "{FR}-{DE}", "{BE}-{NL}", "{NL}-{DE}]"));
        Map<String, String> mapRep3 = RaoUtils.relativeMarginsParametersExtensionToMap(relativeMarginsExt);
        RelativeMarginsParameters relativeMarginsExt2 = RaoUtils.buildRelativeMarginsParametersExtension(mapRep3);
        compareRelativeMargingExtensions(relativeMarginsExt, relativeMarginsExt2);

        SearchTreeRaoRelativeMarginsParameters relativeMarginsStExt = new SearchTreeRaoRelativeMarginsParameters();
        relativeMarginsStExt.setPtdfSumLowerBound(49.0);
        relativeMarginsStExt.setPtdfApproximation(PtdfApproximation.UPDATE_PTDF_WITH_TOPO);
        Map<String, String> mapRep4 = RaoUtils.relativeMarginsParametersExtensionToMap(relativeMarginsStExt);
        SearchTreeRaoRelativeMarginsParameters relativeMarginsStExt2 = RaoUtils.buildRelativeMarginsSearchTreeParametersExtension(mapRep4);
        compareRelativeMargingExtensions(relativeMarginsStExt, relativeMarginsStExt2);

        LoopFlowParameters loopFlowExt = new LoopFlowParameters();
        loopFlowExt.setAcceptableIncrease(38.0);
        loopFlowExt.setCountries(Set.of(Country.AD, Country.AE));
        Map<String, String> mapRep5 = RaoUtils.loopFlowParametersExtensionToMap(loopFlowExt);
        LoopFlowParameters loopFlowExt2 = RaoUtils.buildLoopFlowParametersExtension(mapRep5);
        compareLoopFlowExtension(loopFlowExt, loopFlowExt2);

        SearchTreeRaoLoopFlowParameters loopFlowStExt = new SearchTreeRaoLoopFlowParameters();
        loopFlowStExt.setPtdfApproximation(PtdfApproximation.UPDATE_PTDF_WITH_TOPO_AND_PST);
        loopFlowStExt.setConstraintAdjustmentCoefficient(62.0);
        Map<String, String> mapRep6 = RaoUtils.loopFlowParametersExtensionToMap(loopFlowStExt);
        SearchTreeRaoLoopFlowParameters loopFlowStExt2 = RaoUtils.buildLoopFlowSearchTreeParametersExtension(mapRep6);
        compareLoopFlowExtension(loopFlowStExt, loopFlowStExt2);

        // Single map
        Map<String, String> mergedMap = new HashMap<>();
        mergedMap.putAll(mapRep);
        mergedMap.putAll(mapRep2);
        mergedMap.putAll(mapRep3);
        mergedMap.putAll(mapRep4);
        mergedMap.putAll(mapRep5);
        mergedMap.putAll(mapRep6);

        MnecParameters mnecExt3 = RaoUtils.buildMnecParametersExtension(mergedMap);
        RelativeMarginsParameters relativeMarginsExt3 = RaoUtils.buildRelativeMarginsParametersExtension(mergedMap);
        LoopFlowParameters loopFlowExt3 = RaoUtils.buildLoopFlowParametersExtension(mergedMap);
        compareMnecExtensions(mnecExt3, mnecExt);
        compareRelativeMargingExtensions(relativeMarginsExt3, relativeMarginsExt);
        compareLoopFlowExtension(loopFlowExt3, loopFlowExt);

        SearchTreeRaoMnecParameters mnecExt4 = RaoUtils.buildMnecSearchTreeParametersExtension(mergedMap);
        SearchTreeRaoRelativeMarginsParameters relativeMarginsExt4 = RaoUtils.buildRelativeMarginsSearchTreeParametersExtension(mergedMap);
        SearchTreeRaoLoopFlowParameters loopFlowExt4 = RaoUtils.buildLoopFlowSearchTreeParametersExtension(mergedMap);
        compareMnecExtensions(mnecExt4, mnecStExt);
        compareRelativeMargingExtensions(relativeMarginsExt4, relativeMarginsStExt);
        compareLoopFlowExtension(loopFlowExt4, loopFlowStExt);
    }

    void compareMnecExtensions(MnecParameters mnecExt, MnecParameters mnecExt2) {
        assertEquals(mnecExt.getAcceptableMarginDecrease(), mnecExt2.getAcceptableMarginDecrease());
    }

    void compareMnecExtensions(SearchTreeRaoMnecParameters mnecExt, SearchTreeRaoMnecParameters mnecExt2) {
        assertEquals(mnecExt.getViolationCost(), mnecExt2.getViolationCost());
        assertEquals(mnecExt.getConstraintAdjustmentCoefficient(), mnecExt2.getConstraintAdjustmentCoefficient());
    }

    void compareRelativeMargingExtensions(RelativeMarginsParameters relativeMarginsExt, RelativeMarginsParameters relativeMarginsExt2) {
        assertEquals(relativeMarginsExt.getPtdfBoundariesAsString(), relativeMarginsExt2.getPtdfBoundariesAsString());
    }

    void compareRelativeMargingExtensions(SearchTreeRaoRelativeMarginsParameters relativeMarginsExt, SearchTreeRaoRelativeMarginsParameters relativeMarginsExt2) {
        assertEquals(relativeMarginsExt.getPtdfSumLowerBound(), relativeMarginsExt2.getPtdfSumLowerBound());
        assertEquals(relativeMarginsExt.getPtdfApproximation(), relativeMarginsExt2.getPtdfApproximation());
    }

    void compareLoopFlowExtension(LoopFlowParameters loopFlowExt, LoopFlowParameters loopFlowExt2) {
        assertEquals(loopFlowExt.getAcceptableIncrease(), loopFlowExt2.getAcceptableIncrease());
        assertEquals(loopFlowExt.getCountries(), loopFlowExt2.getCountries());
    }

    void compareLoopFlowExtension(SearchTreeRaoLoopFlowParameters loopFlowExt, SearchTreeRaoLoopFlowParameters loopFlowExt2) {
        assertEquals(loopFlowExt.getPtdfApproximation(), loopFlowExt2.getPtdfApproximation());
        assertEquals(loopFlowExt.getConstraintAdjustmentCoefficient(), loopFlowExt2.getConstraintAdjustmentCoefficient());
    }
}
