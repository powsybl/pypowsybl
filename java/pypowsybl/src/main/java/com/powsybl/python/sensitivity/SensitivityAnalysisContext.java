/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.sensitivity;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.contingency.ContingencyContextType;
import com.powsybl.iidm.network.*;
import com.powsybl.openloadflow.sensi.AcSensitivityAnalysis;
import com.powsybl.openloadflow.sensi.OpenSensitivityAnalysisProvider;
import com.powsybl.python.commons.CommonObjects;
import com.powsybl.python.contingency.ContingencyContainerImpl;
import com.powsybl.sensitivity.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
class SensitivityAnalysisContext extends ContingencyContainerImpl {

    private List<SensitivityVariableSet> variableSets = Collections.emptyList();

    public static class MatrixInfo {
        private final ContingencyContextType contingencyContextType;

        private final SensitivityFunctionType functionType;

        private final SensitivityVariableType variableType;

        private final List<String> columnIds;

        private final List<String> rowIds;

        private final List<String> contingencyIds;

        private int offsetData;

        private int offsetColumn;

        MatrixInfo(ContingencyContextType context, SensitivityFunctionType functionType, SensitivityVariableType variableType,
                   List<String> columnIds, List<String> rowIds, List<String> contingencyIds) {
            this.contingencyContextType = context;
            this.functionType = functionType;
            this.variableType = variableType;
            this.columnIds = columnIds;
            this.rowIds = rowIds;
            this.contingencyIds = contingencyIds;
        }

        ContingencyContextType getContingencyContextType() {
            return contingencyContextType;
        }

        SensitivityFunctionType getFunctionType() {
            return functionType;
        }

        public SensitivityVariableType getVariableType() {
            return variableType;
        }

        void setOffsetData(int offset) {
            this.offsetData = offset;
        }

        void setOffsetColumn(int offset) {
            this.offsetColumn = offset;
        }

        int getOffsetData() {
            return offsetData;
        }

        int getOffsetColumn() {
            return offsetColumn;
        }

        List<String> getRowIds() {
            return rowIds;
        }

        List<String> getColumnIds() {
            return columnIds;
        }

        List<String> getContingencyIds() {
            return contingencyIds;
        }

        int getRowCount() {
            return rowIds.size();
        }

        int getColumnCount() {
            return columnIds.size();
        }
    }

    // LinkedHashMap: preserve matrix declaration order so the flat column layout (offsetColumn) — hence the
    // adjoint cotangent vector alignment — is deterministic and matches the Python-side declaration order.
    private final Map<String, MatrixInfo> factorsMatrix = new LinkedHashMap<>();

    void addFactorMatrix(String matrixId, List<String> branchesIds, List<String> variablesIds,
                         List<String> contingencies, ContingencyContextType contingencyContextType,
                         SensitivityFunctionType sensitivityFunctionType, SensitivityVariableType sensitivityVariableType) {
        if (factorsMatrix.containsKey(matrixId)) {
            throw new PowsyblException("Matrix '" + matrixId + "' already exists.");
        }
        MatrixInfo info = new MatrixInfo(contingencyContextType, sensitivityFunctionType, sensitivityVariableType, branchesIds, variablesIds, contingencies);
        factorsMatrix.put(matrixId, info);
    }

    public void setVariableSets(List<SensitivityVariableSet> variableSets) {
        this.variableSets = Objects.requireNonNull(variableSets);
    }

    List<MatrixInfo> prepareMatrices() {
        List<MatrixInfo> matrices = new ArrayList<>();
        int offsetData = 0;
        int offsetColumns = 0;

        for (MatrixInfo matrix : factorsMatrix.values()) {
            matrix.setOffsetData(offsetData);
            matrix.setOffsetColumn(offsetColumns);
            matrices.add(matrix);
            offsetData += matrix.getColumnCount() * matrix.getRowCount();
            offsetColumns += matrix.getColumnCount();
        }

        return matrices;
    }

    int getTotalNumberOfMatrixFactors(List<MatrixInfo> matrices) {
        int count = 0;
        for (MatrixInfo matrix : matrices) {
            count += matrix.getColumnCount() * matrix.getRowCount();
        }
        return count;
    }

    int getTotalNumberOfMatrixFactorsColumns(List<MatrixInfo> matrices) {
        int count = 0;
        for (MatrixInfo matrix : matrices) {
            count += matrix.getColumnCount();
        }
        return count;
    }

    // Build the adjoint lever for one declared row, mirroring the per-variable logic the old cross-product
    // loop used inline: the type is the matrix's declared one, else inferred from the network, else a
    // variable set (INJECTION_ACTIVE_POWER). Set-ness is returned with the type rather than through an
    // out-parameter, since only the pair is a usable answer — the type alone cannot say whether the id
    // names a SensitivityVariableSet.
    private AcSensitivityAnalysis.AdjointVariable resolveAdjointVariable(Network network, String variableId, MatrixInfo matrix,
                                                                        Map<String, SensitivityVariableSet> variableSetsById) {
        SensitivityVariableType declaredType = matrix.getVariableType();
        if (declaredType != null) {
            return AcSensitivityAnalysis.AdjointVariable.of(declaredType, variableId);
        }
        SensitivityVariableType inferredType = getVariableType(network, variableId);
        if (inferredType != null) {
            return AcSensitivityAnalysis.AdjointVariable.of(inferredType, variableId);
        }
        if (variableSetsById.containsKey(variableId)) {
            return AcSensitivityAnalysis.AdjointVariable.ofVariableSet(SensitivityVariableType.INJECTION_ACTIVE_POWER, variableId);
        }
        throw new PowsyblException("Variable '" + variableId + "' not found");
    }

    private SensitivityVariableType getVariableType(Network network, String variableId) {
        Identifiable<?> identifiable = network.getIdentifiable(variableId);
        if (identifiable instanceof Injection<?>) {
            if (identifiable instanceof ShuntCompensator) {
                return SensitivityVariableType.SHUNT_COMPENSATOR_SUSCEPTANCE;
            }
            return SensitivityVariableType.INJECTION_ACTIVE_POWER;
        } else if (identifiable instanceof TwoWindingsTransformer) {
            return SensitivityVariableType.TRANSFORMER_PHASE;
        } else if (identifiable instanceof ThreeWindingsTransformer twt) {
            ThreeWindingsTransformer.Leg phaseTapChangerLeg = twt.getLegStream()
                    .filter(PhaseTapChangerHolder::hasPhaseTapChanger)
                    .findFirst()
                    .orElse(null);
            if (phaseTapChangerLeg != null) {
                return switch (phaseTapChangerLeg.getSide()) {
                    case ONE -> SensitivityVariableType.TRANSFORMER_PHASE_1;
                    case TWO -> SensitivityVariableType.TRANSFORMER_PHASE_2;
                    case THREE -> SensitivityVariableType.TRANSFORMER_PHASE_3;
                };
            }
            return null;
        } else if (identifiable instanceof HvdcLine) {
            return SensitivityVariableType.HVDC_LINE_ACTIVE_POWER;
        } else {
            return null;
        }
    }

    SensitivityAnalysisResultContext run(Network network, SensitivityAnalysisParameters sensitivityAnalysisParameters, String provider, ReportNode reportNode) {
        List<Contingency> contingencies = createContingencies(network);

        List<MatrixInfo> matrices = prepareMatrices();

        Map<String, SensitivityVariableSet> variableSetsById = variableSets.stream().collect(Collectors.toMap(SensitivityVariableSet::getId, e -> e));

        SensitivityFactorReader factorReader = handler -> {

            for (MatrixInfo matrix : matrices) {
                List<String> columns = matrix.getColumnIds();
                List<String> rows = matrix.getRowIds();
                List<ContingencyContext> contingencyContexts = new ArrayList<>();
                if (matrix.getContingencyContextType() == ContingencyContextType.ALL) {
                    contingencyContexts.add(ContingencyContext.all());
                } else if (matrix.getContingencyContextType() == ContingencyContextType.NONE) {
                    contingencyContexts.add(ContingencyContext.none());
                } else {
                    for (String c : matrix.getContingencyIds()) {
                        contingencyContexts.add(ContingencyContext.specificContingency(c));
                    }
                }

                for (String variableId : rows) {
                    for (String functionId : columns) {
                        SensitivityVariableType variableType = matrix.getVariableType();
                        boolean variableSet = false;
                        if (variableType == null) {
                            variableType = getVariableType(network, variableId);
                            if (variableType == null) {
                                if (variableSetsById.containsKey(variableId)) {
                                    variableSet = true;
                                    variableType = SensitivityVariableType.INJECTION_ACTIVE_POWER;
                                } else {
                                    throw new PowsyblException("Variable '" + variableId + "' not found");
                                }
                            }
                        }
                        for (ContingencyContext cCtx : contingencyContexts) {
                            SensitivityFunctionType functionType = matrix.getFunctionType();
                            String finalFunctionId = SensitivityFactor.resolveBusId(functionId, matrix.getFunctionType(), network);
                            handler.onFactor(functionType, finalFunctionId, variableType, variableId, variableSet, cCtx);
                        }
                    }
                }
            }
        };

        int baseCaseValueSize = getTotalNumberOfMatrixFactors(matrices);
        double[] baseCaseValues = new double[baseCaseValueSize];
        double[][] valuesByContingencyIndex = new double[contingencies.size()][baseCaseValueSize];

        int totalColumnsCount = getTotalNumberOfMatrixFactorsColumns(matrices);
        double[] baseCaseReferences = new double[totalColumnsCount];
        double[][] referencesByContingencyIndex = new double[contingencies.size()][totalColumnsCount];

        NavigableMap<Integer, MatrixInfo> factorIndexMatrixMap = new TreeMap<>();
        for (MatrixInfo m : matrices) {
            factorIndexMatrixMap.put(m.getOffsetData(), m);
        }

        SensitivityResultWriter valueWriter = new SensitivityResultWriter() {
            @Override
            public void writeSensitivityValue(int factorContext, int contingencyIndex, int strategyIndex,
                                              double value, double functionReference) {
                MatrixInfo m = factorIndexMatrixMap.floorEntry(factorContext).getValue();

                int columnIdx = m.getOffsetColumn() + (factorContext - m.getOffsetData()) % m.getColumnCount();
                if (contingencyIndex != -1) {
                    valuesByContingencyIndex[contingencyIndex][factorContext] = value;
                    referencesByContingencyIndex[contingencyIndex][columnIdx] = functionReference;
                } else {
                    baseCaseValues[factorContext] = value;
                    baseCaseReferences[columnIdx] = functionReference;
                }
            }

            @Override
            public void writeStateStatus(int i, int strategy, SensitivityAnalysisResult.Status status) {
                // nothing to do
            }
        };

        SensitivityAnalysis.find(provider)
                .run(network,
                        network.getVariantManager().getWorkingVariantId(),
                        factorReader,
                        valueWriter,
                        contingencies,
                        variableSets,
                        sensitivityAnalysisParameters,
                        CommonObjects.getComputationManager(),
                        (reportNode == null) ? ReportNode.NO_OP : reportNode);

        Map<String, double[]> valuesByContingencyId = new HashMap<>(contingencies.size());
        Map<String, double[]> referencesByContingencyId = new HashMap<>(contingencies.size());
        for (int contingencyIndex = 0; contingencyIndex < contingencies.size(); contingencyIndex++) {
            Contingency contingency = contingencies.get(contingencyIndex);
            valuesByContingencyId.put(contingency.getId(), valuesByContingencyIndex[contingencyIndex]);
            referencesByContingencyId.put(contingency.getId(), referencesByContingencyIndex[contingencyIndex]);
        }

        return new SensitivityAnalysisResultContext(factorsMatrix,
                                                    baseCaseValues,
                                                    valuesByContingencyId,
                                                    baseCaseReferences,
                                                    referencesByContingencyId);
    }

    /**
     * Reverse-mode (adjoint / VJP) run: given output cotangents {@code ȳ} over the declared functions
     * (the columns of the factor matrices, flat in offsetColumn order), returns {@code θ̄ = Sᵀ·ȳ} per declared
     * factor matrix — the reverse-mode dual of {@link #run}. Reuses the OpenLoadFlow-retained
     * {@code networkCacheEnabled} context, so a cached AC load flow must have run on the network first.
     *
     * @param functionCotangents dL/dfunction, one entry per declared function column (offsetColumn layout).
     */
    SensitivityAnalysisAdjointResultContext runAdjoint(Network network, double[] functionCotangents,
                                                       SensitivityAnalysisParameters sensitivityAnalysisParameters,
                                                       String provider) {
        rejectDeclaredContingencies(network);
        // Sets the offsetColumn layout the flat cotangent vector is aligned with; the loop below then walks
        // the same matrices in declaration order, with their ids (which MatrixInfo does not carry).
        prepareMatrices();
        Map<String, SensitivityVariableSet> variableSetsById = variableSets.stream()
                .collect(Collectors.toMap(SensitivityVariableSet::getId, e -> e));

        // Reverse mode needs only the monitored functions + the levers, NOT the functions×variables cross
        // product: the cotangent map declares the functions, and OpenLoadFlow builds the O(F+V) adjoint
        // factor set from it (AcSensitivityAnalysis.buildAdjointFactors, gated in its own test), turning a
        // ~F·V allocation (millions of factors on a real case) into ~F+V.
        //
        // LinkedHashMap / LinkedHashSet throughout: the declaration order of the matrices is the caller's,
        // and keeping it makes the emitted factor order — hence anything that reads back by index — stable
        // from one run to the next.
        Map<AcSensitivityAnalysis.FunctionRef, Double> cotangentByFunction = new LinkedHashMap<>();
        // Deduplicated across matrices: the same element is one lever however many matrices declare it, and
        // a repeated AdjointVariable would ask OpenLoadFlow for a factor group it already has.
        Map<AcSensitivityAnalysis.VariableRef, AcSensitivityAnalysis.AdjointVariable> leversByRef = new LinkedHashMap<>();
        // Which factor matrix last stated a non-zero cotangent for a function, so a contradiction can name
        // both sides of it. Only this side knows both the matrix ids and the RESOLVED function.
        Map<AcSensitivityAnalysis.FunctionRef, String> statedByMatrix = new HashMap<>();
        // Per matrix, the θ̄ key of each of its variable (row) ids, aligned with getRowIds(). OpenLoadFlow
        // returns the gradient keyed by (variableType, variableId), so reading a row back needs that row's
        // RESOLVED type — a resolution that needs the network, which the result context does not have.
        Map<String, List<AcSensitivityAnalysis.VariableRef>> gradientKeysByMatrixId = new LinkedHashMap<>();

        for (Map.Entry<String, MatrixInfo> matrixEntry : factorsMatrix.entrySet()) {
            MatrixInfo matrix = matrixEntry.getValue();
            List<String> columns = matrix.getColumnIds();
            List<String> rows = matrix.getRowIds();
            SensitivityFunctionType functionType = matrix.getFunctionType();

            // dL/dfunction per declared function (column), read from the flat column-aligned cotangent vector.
            // This build stays caller-side: it depends on the matrix column layout (offsetColumn), which OLF
            // ignores. The id is RESOLVED first, exactly as the forward run() resolves before handing it over
            // — a bus monitored by its bus-breaker id or a busbar section id then names the same function as
            // its bus-view id would, and two caller ids for one bus collapse to a single monitored function
            // (their cotangents must then agree, see putFunctionCotangent).
            for (int j = 0; j < columns.size(); j++) {
                var function = new AcSensitivityAnalysis.FunctionRef(functionType,
                        SensitivityFactor.resolveBusId(columns.get(j), functionType, network));
                putFunctionCotangent(cotangentByFunction, statedByMatrix, function,
                        functionCotangents[matrix.getOffsetColumn() + j], matrixEntry.getKey());
            }

            // the levers (rows) of this matrix, each with its resolved type + set-ness
            List<AcSensitivityAnalysis.VariableRef> gradientKeys = new ArrayList<>(rows.size());
            for (String variableId : rows) {
                AcSensitivityAnalysis.AdjointVariable lever = resolveAdjointVariable(network, variableId, matrix, variableSetsById);
                leversByRef.putIfAbsent(lever.ref(), lever);
                gradientKeys.add(lever.ref());
            }
            gradientKeysByMatrixId.put(matrixEntry.getKey(), gradientKeys);
        }

        SensitivityAnalysisProvider p = SensitivityAnalysisCUtils.getSensitivityAnalysisProvider(provider);
        if (!(p instanceof OpenSensitivityAnalysisProvider olfProvider)) {
            throw new PowsyblException("Adjoint (VJP) sensitivity requires the OpenLoadFlow provider, got '" + p.getName() + "'");
        }
        Map<AcSensitivityAnalysis.VariableRef, Double> gradientByVariableKey = olfProvider.runAdjoint(network,
                network.getVariantManager().getWorkingVariantId(), cotangentByFunction,
                List.copyOf(leversByRef.values()), variableSets, sensitivityAnalysisParameters);

        // Join the gradient onto each matrix's rows here, so the result context holds answers rather than the
        // keys needed to compute them.
        Map<String, double[]> gradientByMatrixId = new LinkedHashMap<>();
        gradientKeysByMatrixId.forEach((matrixId, rowKeys) -> {
            double[] values = new double[rowKeys.size()];
            for (int i = 0; i < rowKeys.size(); i++) {
                // Every declared lever must be answered. OpenLoadFlow returns one entry per lever it was
                // given — a lever outside the solved component answers 0, mirroring what the forward path
                // writes for it, and one it cannot differentiate at all makes runAdjoint throw rather than
                // hand back a NaN. So a missing key here is not a degenerate network, it is the two sides
                // disagreeing about the lever set, and substituting a number for it would put a fabricated
                // gradient in front of a caller who cannot tell. Name it instead.
                AcSensitivityAnalysis.VariableRef rowKey = rowKeys.get(i);
                Double value = gradientByVariableKey.get(rowKey);
                if (value == null) {
                    throw new PowsyblException("No gradient returned for the declared lever " + rowKey.type()
                            + " '" + rowKey.id() + "' of factor matrix '" + matrixId + "'. OpenLoadFlow answers "
                            + "every lever it is given, so this means the lever it was given differs from the "
                            + "one read back.");
                }
                values[i] = value;
            }
            gradientByMatrixId.put(matrixId, values);
        });
        return new SensitivityAnalysisAdjointResultContext(gradientByMatrixId);
    }

    /**
     * Refuse a reverse-mode request that declares contingencies, naming them.
     *
     * <p>Reverse mode is BASE CASE only: it contracts the cotangent against the Jacobian the network cache
     * retained for the solved base state, and OpenLoadFlow emits every adjoint factor with
     * {@code ContingencyContext.none()}. A declared contingency therefore changes nothing in the answer.
     * Returning the base-case gradient anyway is the worst option available — it is a plausible number for a
     * question nobody asked, and nothing downstream can tell it apart from the post-contingency gradient the
     * caller believed they requested.</p>
     *
     * <p>The test is what was DECLARED, not the contingency context type: the lever wrappers
     * ({@code add_shunt_susceptance_factor_matrix} and friends) all pass {@code ContingencyContextType.ALL},
     * which with no contingency declared means all of none and is exactly how the adjoint is meant to be
     * called. Rejecting on the context type would reject the recommended usage.</p>
     */
    private void rejectDeclaredContingencies(Network network) {
        List<String> contingencyIds = hasDeclaredContingencies()
                ? createContingencies(network).stream().map(Contingency::getId).sorted().toList()
                : List.of();
        List<String> matricesWithContingencies = factorsMatrix.entrySet().stream()
                .filter(e -> !e.getValue().getContingencyIds().isEmpty())
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        List<String> declared = new ArrayList<>();
        if (!contingencyIds.isEmpty()) {
            declared.add("contingencies " + contingencyIds);
        }
        if (!matricesWithContingencies.isEmpty()) {
            declared.add("factor matrices declaring contingencies " + matricesWithContingencies);
        }
        if (declared.isEmpty()) {
            return;
        }
        throw new PowsyblException("Adjoint (VJP) sensitivity is base case only, so it refuses a request that "
                + "declares " + String.join(" and ", declared) + ": they would otherwise be ignored in "
                + "silence and the gradient returned for the base case. Drop them from this analysis to run "
                + "the adjoint, or use the forward run for post-contingency sensitivities.");
    }

    /**
     * Record one declared column's cotangent under its {@code (functionType, functionId)} key, tolerating the
     * SAME function being declared by several factor matrices but never double-counting it.
     *
     * <p>{@code ȳ} is a property of a monitored FUNCTION, not of the matrix that happens to declare it. The
     * flat input vector carries one slot per (matrix, column) instead, so fusing several variable families
     * into one call — each declaring the same monitored functions against its own variables — presents the
     * same function several times. Summing those slots, which this did, silently multiplies {@code ȳ} by the
     * number of families and returns a plausible gradient that is simply k times too large. Callers were left
     * to defeat it by putting the real cotangent on the first family's matrices and zeros on all the others,
     * an invariant nothing checked and a reader of the caller could not guess.</p>
     *
     * <p>So: the repeated slots must AGREE, and the agreed value is used once. Zero reads as "not stated
     * here", which keeps the zero-fill convention working unchanged; two different non-zero values are a
     * genuine contradiction about one number and are rejected rather than blended.</p>
     *
     * <p>Agreement is relative, not bit-exact: a caller that recomputes its cotangent per factor matrix does
     * the same arithmetic twice and can land on values differing in the last bits, which is the same number
     * stated twice, not a contradiction. Only a disagreement above {@link #COTANGENT_RELATIVE_TOLERANCE}
     * is one, and the first slot's value is the one used.</p>
     */
    private static void putFunctionCotangent(Map<AcSensitivityAnalysis.FunctionRef, Double> cotangentByFunction,
                                             Map<AcSensitivityAnalysis.FunctionRef, String> statedByMatrix,
                                             AcSensitivityAnalysis.FunctionRef key, double value, String matrixId) {
        Double previous = cotangentByFunction.get(key);
        if (previous == null || previous == 0.0) {
            cotangentByFunction.put(key, value);
            if (value != 0.0) {
                statedByMatrix.put(key, matrixId);
            }
            return;
        }
        if (value != 0.0 && !agreeOnCotangent(previous, value)) {
            throw new PowsyblException("Conflicting cotangents for monitored function " + key.type() + " '"
                    + key.id() + "': factor matrix '" + statedByMatrix.get(key) + "' states " + previous
                    + ", factor matrix '" + matrixId + "' states " + value + ". A monitored function has ONE "
                    + "dL/dfunction, whichever factor matrices declare it; state the same value (or 0) in "
                    + "every matrix that does. The id above is the RESOLVED one, so two different ids for one "
                    + "bus — a bus-breaker id and its bus-view bus — are the same monitored function here.");
        }
    }

    /** Above this relative gap, two slots for one monitored function state different numbers. */
    private static final double COTANGENT_RELATIVE_TOLERANCE = 1e-9;

    private static boolean agreeOnCotangent(double previous, double value) {
        // Relative to the larger magnitude: the slots are the same computation repeated, so the gap that
        // matters is the one relative to their own scale, not an absolute epsilon that a cotangent of 1e6
        // (or 1e-6) would read completely differently.
        return Math.abs(value - previous) <= COTANGENT_RELATIVE_TOLERANCE * Math.max(Math.abs(previous), Math.abs(value));
    }

}
