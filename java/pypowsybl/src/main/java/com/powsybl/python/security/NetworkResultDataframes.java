/**
 * Copyright (c) 2026, Elia Group (http://www.eliagroup.eu)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.security;

import com.powsybl.dataframe.DataframeHandler;
import com.powsybl.dataframe.StringDictionary;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.results.BranchResult;
import com.powsybl.security.results.BusResult;
import com.powsybl.security.results.NetworkResult;
import com.powsybl.security.results.ThreeWindingsTransformerResult;

import java.util.List;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;

/**
 * Writes the network results of a security analysis (branch, bus and three windings transformer results) as
 * dataframes with one row per monitored element and state.
 *
 * <p>A table is indexed by the state and the element: the {@code contingency_id} level, the
 * {@code operator_strategy_id} level (branch and bus results only) and the element id level(s), followed by the
 * value columns of the result. The pre-contingency rows have an empty contingency id, and the rows of a state
 * without operator strategy have an empty operator strategy id. The rows of a state are contiguous: first the
 * pre-contingency state, then the post-contingency states and then the operator strategy states, each in the
 * order of the result.</p>
 *
 *
 * @author Nico Westerbeck {@literal <nico.westerbeck at 50hertz.com>}
 */
public final class NetworkResultDataframes {

    private static final String CONTINGENCY_ID = "contingency_id";
    private static final String OPERATOR_STRATEGY_ID = "operator_strategy_id";

    private NetworkResultDataframes() {
    }

    /**
     * The results of one state, which become contiguous rows of the table.
     *
     * @param contingencyId      the contingency id, or null for the pre-contingency state
     * @param operatorStrategyId the operator strategy id, or null when the state is not an operator strategy state
     * @param results            the results of the state, one per row
     */
    private record Block<R>(String contingencyId, String operatorStrategyId, List<R> results) {
    }

    /**
     * An index level identifying the element of a row.
     *
     * @param name the name of the level
     * @param id   the id of the element of a result
     */
    private record Level<R>(String name, Function<R, String> id) {
    }

    /**
     * A value column of the table.
     *
     * @param name  the name of the column
     * @param value the value of a result
     */
    private record Column<R>(String name, ToDoubleFunction<R> value) {
    }

    /**
     * Dictionary for element ids. Each state usually reports the same elements, in the same order and with the same
     * string instances (the ids of the network elements): the id found at the same position in the previous state
     * is checked by reference first, which avoids hashing (and reading) millions of strings. An id that is not the
     * same instance as the one at that position in the previous state is looked up by value, like any value of a
     * {@link StringDictionary}.
     */
    private static final class PositionalStringDictionary extends StringDictionary {
        private final String[] previousIds;
        private final int[] previousCodes;

        /**
         * @param maxStateSize the largest number of results of a state, the number of positions to remember
         */
        PositionalStringDictionary(int maxStateSize) {
            previousIds = new String[maxStateSize];
            previousCodes = new int[maxStateSize];
        }

        /**
         * The code of the id of the result at a position of the current state.
         */
        @SuppressWarnings("java:S4973") // identity check on purpose, see the class javadoc
        int code(int position, String value) {
            if (value != null && previousIds[position] == value) {
                return previousCodes[position];
            }
            int code = code(value);
            previousIds[position] = value;
            previousCodes[position] = code;
            return code;
        }
    }

    /**
     * Writes the branch results: index levels {@code contingency_id}, {@code operator_strategy_id} and
     * {@code branch_id}, columns {@code p1}, {@code q1}, {@code i1}, {@code p2}, {@code q2}, {@code i2} and
     * {@code flow_transfer}.
     */
    public static void writeBranchResults(SecurityAnalysisResult result, DataframeHandler handler) {
        write(handler, blocks(result, NetworkResult::getBranchResults, true), true,
                List.of(new Level<>("branch_id", BranchResult::getBranchId)),
                List.of(new Column<>("p1", BranchResult::getP1),
                        new Column<>("q1", BranchResult::getQ1),
                        new Column<>("i1", BranchResult::getI1),
                        new Column<>("p2", BranchResult::getP2),
                        new Column<>("q2", BranchResult::getQ2),
                        new Column<>("i2", BranchResult::getI2),
                        new Column<>("flow_transfer", BranchResult::getFlowTransfer)));
    }

    /**
     * Writes the bus results: index levels {@code contingency_id}, {@code operator_strategy_id},
     * {@code voltage_level_id} and {@code bus_id}, columns {@code v_mag} and {@code v_angle}.
     */
    public static void writeBusResults(SecurityAnalysisResult result, DataframeHandler handler) {
        write(handler, blocks(result, NetworkResult::getBusResults, true), true,
                List.of(new Level<>("voltage_level_id", BusResult::getVoltageLevelId),
                        new Level<>("bus_id", BusResult::getBusId)),
                List.of(new Column<>("v_mag", BusResult::getV),
                        new Column<>("v_angle", BusResult::getAngle)));
    }

    /**
     * Writes the three windings transformer results: index levels {@code contingency_id} and {@code transformer_id},
     * columns {@code p1}, {@code q1}, {@code i1} and the same for sides 2 and 3. Operator strategy results do not
     * report three windings transformers, so the table has no operator strategy rows and no
     * {@code operator_strategy_id} level.
     */
    public static void writeThreeWindingsTransformerResults(SecurityAnalysisResult result, DataframeHandler handler) {
        write(handler, blocks(result, NetworkResult::getThreeWindingsTransformerResults, false), false,
                List.of(new Level<>("transformer_id", ThreeWindingsTransformerResult::getThreeWindingsTransformerId)),
                List.of(new Column<>("p1", ThreeWindingsTransformerResult::getP1),
                        new Column<>("q1", ThreeWindingsTransformerResult::getQ1),
                        new Column<>("i1", ThreeWindingsTransformerResult::getI1),
                        new Column<>("p2", ThreeWindingsTransformerResult::getP2),
                        new Column<>("q2", ThreeWindingsTransformerResult::getQ2),
                        new Column<>("i2", ThreeWindingsTransformerResult::getI2),
                        new Column<>("p3", ThreeWindingsTransformerResult::getP3),
                        new Column<>("q3", ThreeWindingsTransformerResult::getQ3),
                        new Column<>("i3", ThreeWindingsTransformerResult::getI3)));
    }

    /**
     * The states of a security analysis result, in row order: pre-contingency, post-contingency and, if requested,
     * operator strategy states.
     *
     * @param results the results of one kind of element of a network result
     */
    private static <R> List<Block<R>> blocks(SecurityAnalysisResult result, Function<NetworkResult, List<R>> results,
                                             boolean withOperatorStrategies) {
        Stream<Block<R>> pre = Stream.of(new Block<>(null, null, results.apply(result.getPreContingencyResult().getNetworkResult())));
        Stream<Block<R>> post = result.getPostContingencyResults().stream()
                .map(r -> new Block<>(r.getContingency().getId(), null, results.apply(r.getNetworkResult())));
        // OperatorStrategyResult.getNetworkResult() throws when there is no conditional actions result:
        // such an operator strategy result has no network result, it contributes no row
        Stream<Block<R>> strategies = withOperatorStrategies
                ? result.getOperatorStrategyResults().stream()
                    .filter(r -> !r.getConditionalActionsResults().isEmpty())
                    .map(r -> new Block<>(r.getOperatorStrategy().getContingencyContext().getContingencyId(),
                            r.getOperatorStrategy().getId(), results.apply(r.getNetworkResult())))
                : Stream.empty();
        return Stream.of(pre, post, strategies).flatMap(Function.identity()).toList();
    }

    /**
     * Writes one table: the index levels of the state, the element levels and the value columns, with the rows of
     * each block in turn.
     */
    private static <R> void write(DataframeHandler handler, List<Block<R>> blocks, boolean withOperatorStrategyLevel,
                                  List<Level<R>> rowLevels, List<Column<R>> columns) {
        int rowCount = blocks.stream().mapToInt(b -> b.results().size()).sum();
        int maxBlockSize = blocks.stream().mapToInt(b -> b.results().size()).max().orElse(0);
        handler.allocate(rowLevels.size() + (withOperatorStrategyLevel ? 2 : 1) + columns.size());
        TableWriter<R> writer = new TableWriter<>(handler, rowCount, maxBlockSize, withOperatorStrategyLevel, rowLevels, columns);
        for (Block<R> block : blocks) {
            writer.write(block);
        }
        writer.writeDictionaries();
    }

    /**
     * A dictionary-encoded index level of the state: the writer of its codes and the dictionary giving them.
     */
    private record IndexLevel(DataframeHandler.DictionaryStringSeriesWriter writer, StringDictionary dictionary) {
        IndexLevel(DataframeHandler.DictionaryStringSeriesWriter writer) {
            this(writer, new StringDictionary());
        }

        void writeDictionary() {
            writer.setDictionary(dictionary.getValues());
        }
    }

    /**
     * The series writers of one table: the contingency level, the optional operator strategy level, the element
     * levels (each with its positional dictionary) and the value columns. The rows are appended block by block
     * with {@link #write(Block)}; the dictionaries are written once all rows are, with {@link #writeDictionaries()}.
     */
    private static final class TableWriter<R> {
        private final IndexLevel contingencies;
        private final IndexLevel strategies;
        private final List<Level<R>> rowLevels;
        private final List<DataframeHandler.DictionaryStringSeriesWriter> rowCodes;
        private final List<PositionalStringDictionary> rowDictionaries;
        private final List<Column<R>> columns;
        private final List<DataframeHandler.DoubleSeriesWriter> values;
        private int row;

        TableWriter(DataframeHandler handler, int rowCount, int maxBlockSize, boolean withOperatorStrategyLevel,
                    List<Level<R>> rowLevels, List<Column<R>> columns) {
            contingencies = new IndexLevel(handler.newDictionaryStringIndex(CONTINGENCY_ID, rowCount));
            strategies = withOperatorStrategyLevel ? new IndexLevel(handler.newDictionaryStringIndex(OPERATOR_STRATEGY_ID, rowCount)) : null;
            this.rowLevels = rowLevels;
            rowCodes = rowLevels.stream().map(l -> handler.newDictionaryStringIndex(l.name(), rowCount)).toList();
            rowDictionaries = Stream.generate(() -> new PositionalStringDictionary(maxBlockSize)).limit(rowLevels.size()).toList();
            this.columns = columns;
            values = columns.stream().map(c -> handler.newDoubleSeries(c.name(), rowCount)).toList();
        }

        /**
         * Appends the rows of a block, one per result; a block without results adds no row and registers no id.
         */
        void write(Block<R> block) {
            List<R> results = block.results();
            if (results.isEmpty()) {
                return;
            }
            int contingencyCode = contingencies.dictionary().code(block.contingencyId());
            int strategyCode = strategies == null ? -1 : strategies.dictionary().code(block.operatorStrategyId());
            for (int position = 0; position < results.size(); position++) {
                R r = results.get(position);
                contingencies.writer().set(row, contingencyCode);
                if (strategies != null) {
                    strategies.writer().set(row, strategyCode);
                }
                for (int l = 0; l < rowLevels.size(); l++) {
                    rowCodes.get(l).set(row, rowDictionaries.get(l).code(position, rowLevels.get(l).id().apply(r)));
                }
                for (int c = 0; c < columns.size(); c++) {
                    values.get(c).set(row, columns.get(c).value().applyAsDouble(r));
                }
                row++;
            }
        }

        /**
         * Writes the dictionaries of the index levels, which are complete once every block is written.
         */
        void writeDictionaries() {
            contingencies.writeDictionary();
            if (strategies != null) {
                strategies.writeDictionary();
            }
            for (int l = 0; l < rowLevels.size(); l++) {
                rowCodes.get(l).setDictionary(rowDictionaries.get(l).getValues());
            }
        }
    }
}
