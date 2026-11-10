/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.fast;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.binary.BinReader;
import com.powsybl.commons.io.TreeDataHeader;
import com.powsybl.commons.io.TreeDataReader;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.NetworkFactory;
import com.powsybl.iidm.serde.BinaryImporter;
import com.powsybl.iidm.serde.ImportOptions;
import com.powsybl.iidm.serde.NetworkSerDe;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Reads a binary IIDM file without building the parts of the network the caller does not need: the import
 * parameter {@link #SKIP_PARAMETER} lists what to leave out. The skipped nodes are still decoded (the binary format
 * has no node length to jump over) but nothing is created or validated for them, which is where the time goes.
 *
 * <p>The other import parameters of the BIIDM importer keep their meaning.
 */
public final class SkippingBinaryImporter extends BinaryImporter {

    /** Import parameter: comma separated list of {@link Skip} names, e.g. {@code temporary-limits,bus-state}. */
    public static final String SKIP_PARAMETER = "pypowsybl.import.skip";

    public enum Skip {
        /** temporary limits of all the loading limits; permanent limits are kept */
        TEMPORARY_LIMITS("temporary-limits"),
        /** current, active power and apparent power limits (the operational limits groups are kept, empty) */
        LIMITS("limits"),
        /** voltage and angle stored for the buses of node/breaker voltage levels (they come back with a load flow) */
        BUS_STATE("bus-state"),
        /** properties of all the elements */
        PROPERTIES("properties"),
        /** all the extensions */
        EXTENSIONS("extensions");

        private final String key;

        Skip(String key) {
            this.key = key;
        }

        static Skip of(String key) {
            for (Skip skip : values()) {
                if (skip.key.equals(key)) {
                    return skip;
                }
            }
            throw new PowsyblException("Unknown value '" + key + "' for import parameter " + SKIP_PARAMETER
                    + ", expected a list of: temporary-limits, limits, bus-state, properties, extensions");
        }
    }

    private static final byte[] BIIDM_MAGIC_NUMBER = "Binary IIDM".getBytes(StandardCharsets.US_ASCII);

    public static EnumSet<Skip> parseSkip(String value) {
        EnumSet<Skip> skips = EnumSet.noneOf(Skip.class);
        for (String key : value.split(",")) {
            if (!key.isBlank()) {
                skips.add(Skip.of(key.trim()));
            }
        }
        return skips;
    }

    /**
     * Reads {@code file}; {@code parameters} are the import parameters, {@link #SKIP_PARAMETER} included.
     */
    public Network read(Path file, Properties parameters, ReportNode reportNode) {
        String fileName = file.getFileName().toString();
        if (!fileName.endsWith(".biidm") && !fileName.endsWith(".bin")) {
            throw new PowsyblException("Import parameter " + SKIP_PARAMETER + " is only supported for uncompressed binary IIDM files (.biidm)");
        }
        Set<Skip> skips = parseSkip(parameters.getProperty(SKIP_PARAMETER, ""));
        Properties importerParameters = new Properties();
        importerParameters.putAll(parameters);
        importerParameters.remove(SKIP_PARAMETER);
        ImportOptions options = createImportOptions(importerParameters);
        try (InputStream is = new BufferedInputStream(Files.newInputStream(file), 1 << 16);
             TreeDataReader reader = new SkippingReader(new BinReader(is, BIIDM_MAGIC_NUMBER), skips)) {
            return NetworkSerDe.read(reader, options, null, NetworkFactory.findDefault(), reportNode);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Delegates everything, except that the child nodes selected by the skip set are consumed without being read. */
    static final class SkippingReader implements TreeDataReader {

        private final TreeDataReader delegate;
        private final Set<Skip> skips;
        /** names of the nodes being read, innermost last */
        private final Deque<String> path = new ArrayDeque<>();

        SkippingReader(TreeDataReader delegate, Set<Skip> skips) {
            this.delegate = delegate;
            this.skips = skips;
        }

        private boolean isSkipped(String name) {
            switch (name) {
                case "temporaryLimit":
                    return skips.contains(Skip.TEMPORARY_LIMITS);
                case "property":
                    return skips.contains(Skip.PROPERTIES);
                case "extension":
                    return skips.contains(Skip.EXTENSIONS);
                case "bus":
                    // only the calculated buses of node/breaker voltage levels: the buses of a bus/breaker topology
                    // are the topology itself
                    return skips.contains(Skip.BUS_STATE) && "nodeBreakerTopology".equals(path.peekLast());
                default:
                    return skips.contains(Skip.LIMITS)
                            && (name.startsWith("currentLimits") || name.startsWith("activePowerLimits") || name.startsWith("apparentPowerLimits"))
                            // currentLimitsPerSeason and the like are extensions, not loading limits
                            && !"extension".equals(path.peekLast());
            }
        }

        @Override
        public void readChildNodes(ChildNodeReader childNodeReader) {
            delegate.readChildNodes(name -> {
                if (isSkipped(name)) {
                    delegate.skipNode();
                } else {
                    path.addLast(name);
                    childNodeReader.onStartNode(name);
                    path.removeLast();
                }
            });
        }

        @Override
        public TreeDataHeader readHeader() {
            return delegate.readHeader();
        }

        @Override
        public double readDoubleAttribute(String name) {
            return delegate.readDoubleAttribute(name);
        }

        @Override
        public double readDoubleAttribute(String name, double defaultValue) {
            return delegate.readDoubleAttribute(name, defaultValue);
        }

        @Override
        public OptionalDouble readOptionalDoubleAttribute(String name) {
            return delegate.readOptionalDoubleAttribute(name);
        }

        @Override
        public float readFloatAttribute(String name) {
            return delegate.readFloatAttribute(name);
        }

        @Override
        public float readFloatAttribute(String name, float defaultValue) {
            return delegate.readFloatAttribute(name, defaultValue);
        }

        @Override
        public String readStringAttribute(String name) {
            return delegate.readStringAttribute(name);
        }

        @Override
        public String readStringAttribute(String name, String defaultValue) {
            return delegate.readStringAttribute(name, defaultValue);
        }

        @Override
        public int readIntAttribute(String name) {
            return delegate.readIntAttribute(name);
        }

        @Override
        public OptionalInt readOptionalIntAttribute(String name) {
            return delegate.readOptionalIntAttribute(name);
        }

        @Override
        public int readIntAttribute(String name, int defaultValue) {
            return delegate.readIntAttribute(name, defaultValue);
        }

        @Override
        public boolean readBooleanAttribute(String name) {
            return delegate.readBooleanAttribute(name);
        }

        @Override
        public boolean readBooleanAttribute(String name, boolean defaultValue) {
            return delegate.readBooleanAttribute(name, defaultValue);
        }

        @Override
        public Optional<Boolean> readOptionalBooleanAttribute(String name) {
            return delegate.readOptionalBooleanAttribute(name);
        }

        @Override
        public <T extends Enum<T>> T readEnumAttribute(String name, Class<T> clazz) {
            return delegate.readEnumAttribute(name, clazz);
        }

        @Override
        public <T extends Enum<T>> T readEnumAttribute(String name, Class<T> clazz, T defaultValue) {
            return delegate.readEnumAttribute(name, clazz, defaultValue);
        }

        @Override
        public String readContent() {
            return delegate.readContent();
        }

        @Override
        public List<Integer> readIntArrayAttribute(String name) {
            return delegate.readIntArrayAttribute(name);
        }

        @Override
        public List<String> readStringArrayAttribute(String name) {
            return delegate.readStringArrayAttribute(name);
        }

        @Override
        public void skipNode() {
            delegate.skipNode();
        }

        @Override
        public void readEndNode() {
            delegate.readEndNode();
        }

        @Override
        public void close() {
            delegate.close();
        }
    }
}
