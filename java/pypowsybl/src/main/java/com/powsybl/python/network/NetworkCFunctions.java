/**
 * Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.network;

import com.google.common.collect.Iterables;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.datasource.*;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.dataframe.DataframeElementType;
import com.powsybl.dataframe.DataframeFilter;
import com.powsybl.dataframe.DataframeFilter.AttributeFilterType;
import com.powsybl.dataframe.SeriesDataType;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.NetworkDataframeContext;
import com.powsybl.dataframe.network.NetworkDataframeMapper;
import com.powsybl.dataframe.network.NetworkDataframes;
import com.powsybl.dataframe.network.adders.AliasDataframeAdder;
import com.powsybl.dataframe.network.adders.InternalConnectionDataframeAdder;
import com.powsybl.dataframe.network.adders.NetworkElementAdders;
import com.powsybl.dataframe.network.adders.NetworkUtils;
import com.powsybl.dataframe.network.extensions.NetworkExtensions;
import com.powsybl.dataframe.network.modifications.DataframeNetworkModificationType;
import com.powsybl.dataframe.network.modifications.NetworkModifications;
import com.powsybl.dataframe.update.DefaultUpdatingDataframe;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.reducer.*;
import com.powsybl.nad.NadParameters;
import com.powsybl.nad.layout.*;
import com.powsybl.nad.model.*;
import com.powsybl.nad.svg.CustomLabelProvider;
import com.powsybl.nad.svg.CustomStyleProvider;
import com.powsybl.nad.svg.EdgeInfo;
import com.powsybl.nad.svg.SvgParameters;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.commons.Util;
import com.powsybl.python.dataframe.CDoubleSeries;
import com.powsybl.python.dataframe.CIntSeries;
import com.powsybl.python.dataframe.CStringSeries;
import com.powsybl.python.datasource.InMemoryZipFileDataSource;
import com.powsybl.python.report.ReportCUtils;
import com.powsybl.sld.SldParameters;
import com.powsybl.sld.library.SldComponentLibrary;
import com.powsybl.sld.library.ConvergenceComponentLibrary;
import com.powsybl.sld.svg.styles.DefaultStyleProviderFactory;
import com.powsybl.sld.svg.styles.NominalVoltageStyleProviderFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.*;
import org.graalvm.word.WordFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.IntStream;
import java.util.zip.ZipOutputStream;

import static com.powsybl.nad.svg.SvgParameters.EdgeInfoEnum.*;
import static com.powsybl.python.commons.CTypeUtil.toStringList;
import static com.powsybl.python.commons.PyPowsyblApiHeader.*;
import static com.powsybl.python.commons.Util.*;
import static com.powsybl.python.dataframe.CDataframeHandler.*;

/**
 * Defines the basic C functions for a network.
 *
 * @author Etienne Lesot {@literal <etienne.lesot at rte-france.com>}
 */
@CContext(Directives.class)
public final class NetworkCFunctions {

    private static final ExportersLoader EXPORTERS_LOADER_SUPPLIER = new ExportersServiceLoader();
    private static final ImportersLoader IMPORTERS_LOADER_SUPPLIER = new ImportersServiceLoader();

    private NetworkCFunctions() {
    }

    @CEntryPoint(name = "getNetworkImportFormats")
    public static ArrayPointer<CCharPointerPointer> getNetworkImportFormats(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> createCharPtrArray(Importer.getFormats().stream().sorted().toList()));
    }

    @CEntryPoint(name = "getNetworkImportSupportedExtensions")
    public static ArrayPointer<CCharPointerPointer> getNetworkImportSupportedExtensions(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> createCharPtrArray(Importer.list().stream().flatMap(l -> l.getSupportedExtensions().stream()).distinct().sorted().toList()));
    }

    @CEntryPoint(name = "getNetworkExportFormats")
    public static ArrayPointer<CCharPointerPointer> getNetworkExportFormats(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> createCharPtrArray(Exporter.getFormats().stream().sorted().toList()));
    }

    @CEntryPoint(name = "getNetworkImportPostProcessors")
    public static ArrayPointer<CCharPointerPointer> getNetworkImportPostProcessors(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> createCharPtrArray(Importer.getPostProcessorNames().stream().sorted().toList()));
    }

    @CEntryPoint(name = "createNetwork")
    public static ObjectHandle createNetwork(IsolateThread thread, CCharPointer name, CCharPointer id, boolean allowVariantMultiThreadAccess,
                                             ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            String networkName = CTypeUtil.toString(name);
            String networkId = CTypeUtil.toString(id);
            Network network = Networks.create(networkName, networkId);
            network.getVariantManager().allowVariantMultiThreadAccess(allowVariantMultiThreadAccess);
            return ObjectHandles.getGlobal().create(network);
        });
    }

    @CEntryPoint(name = "getNetworkMetadata")
    public static NetworkMetadataPointer getNetworkMetadata(IsolateThread thread, ObjectHandle networkHandle,
                                                            ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            return createNetworkMetadata(network);
        });
    }

    @CEntryPoint(name = "freeNetworkMetadata")
    public static void freeNetworkMetadata(IsolateThread thread, NetworkMetadataPointer ptr,
                                           ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> freeNetworkMetadata(ptr));
    }

    private static NetworkMetadataPointer createNetworkMetadata(Network network) {
        NetworkMetadataPointer ptr = UnmanagedMemory.calloc(SizeOf.get(NetworkMetadataPointer.class));
        ptr.setId(CTypeUtil.toCharPtr(network.getId()));
        ptr.setName(CTypeUtil.toCharPtr(network.getNameOrId()));
        ptr.setSourceFormat(CTypeUtil.toCharPtr(network.getSourceFormat()));
        ptr.setForecastDistance(network.getForecastDistance());
        ptr.setCaseDate(network.getCaseDate().toInstant().toEpochMilli() / 1000.0d);
        return ptr;
    }

    private static void freeNetworkMetadata(NetworkMetadataPointer networkMetadataPointer) {
        UnmanagedMemory.free(networkMetadataPointer.getId());
        UnmanagedMemory.free(networkMetadataPointer.getName());
        UnmanagedMemory.free(networkMetadataPointer.getSourceFormat());
        UnmanagedMemory.free(networkMetadataPointer);
    }

    private static ImportConfig createImportConfig(CCharPointerPointer postProcessorsPtrPtr, int postProcessorsCount) {
        var importConfig = ImportConfig.load();
        importConfig.addPostProcessors(toStringList(postProcessorsPtrPtr, postProcessorsCount));
        return importConfig;
    }

    @CEntryPoint(name = "isNetworkLoadable")
    public static boolean isNetworkLoadable(IsolateThread thread, CCharPointer file, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            String fileStr = CTypeUtil.toString(file);
            ReadOnlyDataSource dataSource = DataSource.fromPath(Path.of(fileStr));
            for (Importer importer : Importer.list()) {
                if (importer.exists(dataSource)) {
                    return true;
                }
            }
            return false;
        });
    }

    @CEntryPoint(name = "loadNetwork")
    public static ObjectHandle loadNetwork(IsolateThread thread, CCharPointer file, CCharPointerPointer parameterNamesPtrPtr, int parameterNamesCount,
                                           CCharPointerPointer parameterValuesPtrPtr, int parameterValuesCount,
                                           CCharPointerPointer postProcessorsPtrPtr, int postProcessorsCount,
                                           ObjectHandle reportNodeHandle,
                                           boolean allowVariantMultiThreadAccess,
                                           ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            String fileStr = CTypeUtil.toString(file);
            Properties parameters = createParameters(parameterNamesPtrPtr, parameterNamesCount, parameterValuesPtrPtr, parameterValuesCount);
            ReportNode reportNode = ObjectHandles.getGlobal().get(reportNodeHandle);
            if (reportNode == null) {
                reportNode = ReportNode.NO_OP;
            }
            var importConfig = createImportConfig(postProcessorsPtrPtr, postProcessorsCount);
            Network network = Network.read(Paths.get(fileStr), LocalComputationManager.getDefault(), importConfig, parameters, IMPORTERS_LOADER_SUPPLIER, reportNode);
            network.getVariantManager().allowVariantMultiThreadAccess(allowVariantMultiThreadAccess);
            return ObjectHandles.getGlobal().create(network);
        });
    }

    @CEntryPoint(name = "loadNetworkFromString")
    public static ObjectHandle loadNetworkFromString(IsolateThread thread, CCharPointer fileName, CCharPointer fileContent,
                                                     CCharPointerPointer parameterNamesPtrPtr, int parameterNamesCount,
                                                     CCharPointerPointer parameterValuesPtrPtr, int parameterValuesCount,
                                                     CCharPointerPointer postProcessorsPtrPtr, int postProcessorsCount,
                                                     ObjectHandle reportNodeHandle,
                                                     boolean allowVariantMultiThreadAccess,
                                                     ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            String fileNameStr = CTypeUtil.toString(fileName);
            String fileContentStr = CTypeUtil.toString(fileContent);
            Properties parameters = createParameters(parameterNamesPtrPtr, parameterNamesCount, parameterValuesPtrPtr, parameterValuesCount);
            ReportNode reportNode = ReportCUtils.getReportNode(reportNodeHandle);
            try (InputStream is = new ByteArrayInputStream(fileContentStr.getBytes(StandardCharsets.UTF_8))) {
                if (reportNode == null) {
                    reportNode = ReportNode.NO_OP;
                }
                var importConfig = createImportConfig(postProcessorsPtrPtr, postProcessorsCount);
                Network network = Network.read(fileNameStr, is, LocalComputationManager.getDefault(), importConfig, parameters, IMPORTERS_LOADER_SUPPLIER, reportNode);
                network.getVariantManager().allowVariantMultiThreadAccess(allowVariantMultiThreadAccess);
                return ObjectHandles.getGlobal().create(network);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @CEntryPoint(name = "loadNetworkFromBinaryBuffers")
    public static ObjectHandle loadNetworkFromBinaryBuffers(IsolateThread thread, CCharPointerPointer data, CIntPointer dataSizes, int bufferCount,
                                                            CCharPointerPointer parameterNamesPtrPtr, int parameterNamesCount,
                                                            CCharPointerPointer parameterValuesPtrPtr, int parameterValuesCount,
                                                            CCharPointerPointer postProcessorsPtrPtr, int postProcessorsCount,
                                                            ObjectHandle reportNodeHandle,
                                                            boolean allowVariantMultiThreadAccess,
                                                            ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Properties parameters = createParameters(parameterNamesPtrPtr, parameterNamesCount, parameterValuesPtrPtr, parameterValuesCount);
            ReportNode reportNode = ObjectHandles.getGlobal().get(reportNodeHandle);
            List<Integer> bufferSizes = CTypeUtil.toIntegerList(dataSizes, bufferCount);
            List<ReadOnlyDataSource> dataSourceList = new ArrayList<>();
            for (int i = 0; i < bufferCount; ++i) {
                ByteBuffer buffer = CTypeConversion.asByteBuffer(data.read(i), bufferSizes.get(i));
                Optional<CompressionFormat> format = detectCompressionFormat(buffer);
                if (format.isPresent() && CompressionFormat.ZIP.equals(format.get())) {
                    InMemoryZipFileDataSource ds = new InMemoryZipFileDataSource(binaryBufferToBytes(buffer));
                    String commonBasename = null;
                    try {
                        for (String filename : ds.listNames(".*")) {
                            String basename = DataSourceUtil.getBaseName(filename);
                            commonBasename = commonBasename == null ? basename : StringUtils.getCommonPrefix(commonBasename, basename);
                        }
                    } catch (IOException e) {
                        throw new PowsyblException("Unsupported network data format in zip buffer.");
                    }
                    if (commonBasename != null) {
                        ds.setBaseName(commonBasename);
                    }
                    dataSourceList.add(ds);
                } else {
                    throw new PowsyblException("Network loading from memory buffer only supported with zipped networks.");
                }
            }
            if (reportNode == null) {
                reportNode = ReportNode.NO_OP;
            }
            MultipleReadOnlyDataSource dataSource = new MultipleReadOnlyDataSource(dataSourceList);
            var importConfig = createImportConfig(postProcessorsPtrPtr, postProcessorsCount);
            // FIXME there is no way to pass the import config with powsybl 2024.2.0. To FIX when upgrading to next release.
            Network network = Network.read(dataSource, parameters, reportNode);
            network.getVariantManager().allowVariantMultiThreadAccess(allowVariantMultiThreadAccess);
            return ObjectHandles.getGlobal().create(network);
        });
    }

    @CEntryPoint(name = "saveNetwork")
    public static void saveNetwork(IsolateThread thread, ObjectHandle networkHandle, CCharPointer file, CCharPointer format,
                                   CCharPointerPointer parameterNamesPtrPtr, int parameterNamesCount,
                                   CCharPointerPointer parameterValuesPtrPtr, int parameterValuesCount,
                                   ObjectHandle reportNodeHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String fileStr = CTypeUtil.toString(file);
            String formatStr = CTypeUtil.toString(format);
            Properties parameters = createParameters(parameterNamesPtrPtr, parameterNamesCount, parameterValuesPtrPtr, parameterValuesCount);
            ReportNode reportNode = ObjectHandles.getGlobal().get(reportNodeHandle);
            if (reportNode == null) {
                reportNode = ReportNode.NO_OP;
            }
            network.write(EXPORTERS_LOADER_SUPPLIER, formatStr, parameters, Paths.get(fileStr), reportNode);
        });
    }

    @CEntryPoint(name = "saveNetworkToString")
    public static CCharPointer saveNetworkToString(IsolateThread thread, ObjectHandle networkHandle, CCharPointer format,
                                                   CCharPointerPointer parameterNamesPtrPtr, int parameterNamesCount,
                                                   CCharPointerPointer parameterValuesPtrPtr, int parameterValuesCount,
                                                   ObjectHandle reportNodeHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String formatStr = CTypeUtil.toString(format);
            Properties parameters = createParameters(parameterNamesPtrPtr, parameterNamesCount, parameterValuesPtrPtr, parameterValuesCount);
            MemDataSource dataSource = new MemDataSource();
            var exporter = Exporter.find(formatStr);
            if (exporter == null) {
                throw new PowsyblException("No exporter found for '" + formatStr + "' to export as a string");
            }
            ReportNode reportNode = ReportCUtils.getReportNode(reportNodeHandle);
            exporter.export(network, parameters, dataSource, reportNode);
            try {
                var names = dataSource.listNames(".*?");
                if (names.size() != 1) {
                    throw new PowsyblException("Currently we only support string export for single file format(ex, 'XIIDM').");
                }
                try (InputStream is = new ByteArrayInputStream(dataSource.getData(Iterables.getOnlyElement(names)));
                     ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                    IOUtils.copy(is, os);
                    return CTypeUtil.toCharPtr(os.toString(StandardCharsets.UTF_8));
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @CEntryPoint(name = "saveNetworkToBinaryBuffer")
    public static ArrayPointer<CCharPointer> saveNetworkToBinaryBuffer(IsolateThread thread, ObjectHandle networkHandle, CCharPointer format,
                                                                       CCharPointerPointer parameterNamesPtrPtr, int parameterNamesCount,
                                                                       CCharPointerPointer parameterValuesPtrPtr, int parameterValuesCount,
                                                                       ObjectHandle reportNodeHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String formatStr = CTypeUtil.toString(format);
            Properties parameters = createParameters(parameterNamesPtrPtr, parameterNamesCount, parameterValuesPtrPtr, parameterValuesCount);
            var exporter = Exporter.find(formatStr);
            if (exporter == null) {
                throw new PowsyblException("No exporter found for '" + formatStr + "' to export as a string");
            }
            ReportNode reportNode = ReportCUtils.getReportNode(reportNodeHandle);
            // to support all kind of export: simple file or multiple to an archive,
            // best is to write to a zip file
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(bos)) {
                DataSource dataSource = new ZipMemDataSource("file", zos);
                exporter.export(network, parameters, dataSource, reportNode);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            byte[] bytes = bos.toByteArray();
            return Util.createByteArray(bytes);
        });
    }

    @CEntryPoint(name = "freeBinaryBuffer")
    public static void freeBinaryBuffer(IsolateThread thread, PyPowsyblApiHeader.ArrayPointer<CCharPointer> byteArrayPtr,
                                               PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> freeArrayPointer(byteArrayPtr));
    }

    @CEntryPoint(name = "reduceNetwork")
    public static void reduceNetwork(IsolateThread thread, ObjectHandle networkHandle,
                                     double vMin, double vMax,
                                     CCharPointerPointer idsPtrPtr, int idsCount,
                                     CCharPointerPointer vlsPtrPtr, int vlsCount,
                                     CIntPointer depthsPtr, int depthsCount,
                                     boolean withDanglingLines,
                                     ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            ReductionOptions options = new ReductionOptions();
            options.withDanglingLlines(withDanglingLines);
            List<NetworkPredicate> predicates = new ArrayList<>();
            if (vMax != Double.MAX_VALUE || vMin != 0) {
                predicates.add(new NominalVoltageNetworkPredicate(vMin, vMax));
            }
            if (idsCount != 0) {
                List<String> ids = toStringList(idsPtrPtr, idsCount);
                predicates.add(new IdentifierNetworkPredicate(ids));
            }
            if (depthsCount != 0) {
                final List<Integer> depths = CTypeUtil.toIntegerList(depthsPtr, depthsCount);
                final List<String> voltageLeveles = toStringList(vlsPtrPtr, vlsCount);
                for (int i = 0; i < depths.size(); i++) {
                    predicates.add(new SubNetworkPredicate(network.getVoltageLevel(voltageLeveles.get(i)), depths.get(i)));
                }
            }
            final OrNetworkPredicate orNetworkPredicate = new OrNetworkPredicate(predicates);
            NetworkReducer.builder()
                    .withNetworkPredicate(orNetworkPredicate)
                    .withReductionOptions(options)
                    .build()
                    .reduce(network);
        });
    }

    private static Properties createParameters(CCharPointerPointer parameterNamesPtrPtr, int parameterNamesCount,
                                               CCharPointerPointer parameterValuesPtrPtr, int parameterValuesCount) {
        List<String> parameterNames = toStringList(parameterNamesPtrPtr, parameterNamesCount);
        List<String> parameterValues = toStringList(parameterValuesPtrPtr, parameterValuesCount);
        Properties parameters = new Properties();
        for (int i = 0; i < parameterNames.size(); i++) {
            parameters.setProperty(parameterNames.get(i), parameterValues.get(i));
        }
        return parameters;
    }

    @CEntryPoint(name = "getNetworkElementsIds")
    public static ArrayPointer<CCharPointerPointer> getNetworkElementsIds(IsolateThread thread, ObjectHandle networkHandle, ElementType elementType,
                                                                          CDoublePointer nominalVoltagePtr, int nominalVoltageCount,
                                                                          CCharPointerPointer countryPtr, int countryCount, boolean mainCc, boolean mainSc,
                                                                          boolean notConnectedToSameBusAtBothSides, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            Set<Double> nominalVoltages = new HashSet<>(CTypeUtil.toDoubleList(nominalVoltagePtr, nominalVoltageCount));
            Set<String> countries = new HashSet<>(toStringList(countryPtr, countryCount));
            List<String> elementsIds = NetworkUtil.getElementsIds(network, elementType, nominalVoltages, countries, mainCc, mainSc, notConnectedToSameBusAtBothSides);
            return createCharPtrArray(elementsIds);
        });
    }

    @CEntryPoint(name = "cloneVariant")
    public static void cloneVariant(IsolateThread thread, ObjectHandle networkHandle, CCharPointer src, CCharPointer variant, boolean mayOverwrite, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            network.getVariantManager().cloneVariant(CTypeUtil.toString(src), CTypeUtil.toString(variant), mayOverwrite);
        });
    }

    @CEntryPoint(name = "setWorkingVariant")
    public static void setWorkingVariant(IsolateThread thread, ObjectHandle networkHandle, CCharPointer variant, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            network.getVariantManager().setWorkingVariant(CTypeUtil.toString(variant));
        });
    }

    @CEntryPoint(name = "removeVariant")
    public static void removeVariant(IsolateThread thread, ObjectHandle networkHandle, CCharPointer variant, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            network.getVariantManager().removeVariant(CTypeUtil.toString(variant));
        });
    }

    @CEntryPoint(name = "getVariantsIds")
    public static ArrayPointer<CCharPointerPointer> getVariantsIds(IsolateThread thread, ObjectHandle networkHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            return createCharPtrArray(List.copyOf(network.getVariantManager().getVariantIds()));
        });
    }

    private static DataframeFilter createDataframeFilter(FilterAttributesType filterAttributesType, CCharPointerPointer attributesPtrPtr, int attributesCount, DataframePointer selectedElementsDataframe) {
        List<String> attributes = toStringList(attributesPtrPtr, attributesCount);
        AttributeFilterType filterType = switch (filterAttributesType) {
            case ALL_ATTRIBUTES -> AttributeFilterType.ALL_ATTRIBUTES;
            case SELECTION_ATTRIBUTES -> AttributeFilterType.INPUT_ATTRIBUTES;
            case DEFAULT_ATTRIBUTES -> AttributeFilterType.DEFAULT_ATTRIBUTES;
        };

        return selectedElementsDataframe.isNonNull()
                ? new DataframeFilter(filterType, attributes, createDataframe(selectedElementsDataframe))
                : new DataframeFilter(filterType, attributes);
    }

    @CEntryPoint(name = "createNetworkElementsSeriesArray")
    public static ArrayPointer<SeriesPointer> createNetworkElementsSeriesArray(IsolateThread thread, ObjectHandle networkHandle,
                                                                               ElementType elementType,
                                                                               FilterAttributesType filterAttributesType,
                                                                               CCharPointerPointer attributesPtrPtr, int attributesCount,
                                                                               DataframePointer selectedElementsDataframe,
                                                                               boolean perUnit,
                                                                               double nominalApparentPower,
                                                                               ExceptionHandlerPointer exceptionHandlerPtr) {
        return Util.doCatch(exceptionHandlerPtr, () -> {
            NetworkDataframeMapper mapper = NetworkDataframes.getDataframeMapper(convert(elementType));
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            DataframeFilter dataframeFilter = createDataframeFilter(filterAttributesType, attributesPtrPtr, attributesCount, selectedElementsDataframe);
            return Dataframes.createCDataframe(mapper, network, dataframeFilter, new NetworkDataframeContext(perUnit, nominalApparentPower));
        });
    }

    @CEntryPoint(name = "createNetworkElementsExtensionSeriesArray")
    public static ArrayPointer<PyPowsyblApiHeader.SeriesPointer> createNetworkElementsExtensionSeriesArray(IsolateThread thread, ObjectHandle networkHandle,
                                                                                                           CCharPointer extensionName,
                                                                                                           CCharPointer cTableName,
                                                                                                           PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        String name = CTypeUtil.toString(extensionName);
        String tempName = CTypeUtil.toString(cTableName);
        String tableName = tempName.isEmpty() ? null : tempName;
        return doCatch(exceptionHandlerPtr, () -> {
            NetworkDataframeMapper mapper = NetworkDataframes.getExtensionDataframeMapper(name, tableName);
            if (mapper != null) {
                Network network = ObjectHandles.getGlobal().get(networkHandle);
                return Dataframes.createCDataframe(mapper, network, new DataframeFilter(), NetworkDataframeContext.DEFAULT);
            } else {
                throw new PowsyblException(errorMessageForWrongExtensionName(name, tableName));
            }
        });
    }

    private static String errorMessageForWrongExtensionName(String name, String tableName) {
        String message = "No extension named " + name + " available";
        if (tableName != null) {
            message = "No table " + tableName + " for extension " + name + " available";
        }
        return message;
    }

    @CEntryPoint(name = "getExtensionsNames")
    public static ArrayPointer<CCharPointerPointer> getExtensionsNames(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> createCharPtrArray(List.copyOf(NetworkExtensions.getExtensionsNames())));
    }

    @CEntryPoint(name = "getExtensionsInformation")
    public static ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getExtensionsInformation(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> NetworkExtensions.getExtensionInformation(NetworkDataframeContext.DEFAULT));
    }

    @CEntryPoint(name = "createElement")
    public static void createElement(IsolateThread thread, ObjectHandle networkHandle,
                                     ElementType elementType,
                                     DataframeArrayPointer cDataframes,
                                     ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            DataframeElementType type = convert(elementType);
            List<UpdatingDataframe> dataframes = new ArrayList<>();
            for (int i = 0; i < cDataframes.getDataframesCount(); i++) {
                dataframes.add(createDataframe(cDataframes.getDataframes().addressOf(i)));
            }
            NetworkElementAdders.addElements(type, network, dataframes);
        });
    }

    @CEntryPoint(name = "updateNetworkElementsWithSeries")
    public static void updateNetworkElementsWithSeries(IsolateThread thread, ObjectHandle networkHandle, ElementType elementType,
                                                       DataframePointer dataframe, boolean perUnit,
                                                       double nominalApparentPower,
                                                       PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            UpdatingDataframe updatingDataframe = createDataframe(dataframe);
            NetworkDataframes.getDataframeMapper(convert(elementType))
                .updateSeries(network, updatingDataframe, new NetworkDataframeContext(perUnit, nominalApparentPower));
        });
    }

    @CEntryPoint(name = "removeAliases")
    public static void removeAliases(IsolateThread thread, ObjectHandle networkHandle,
                                     DataframePointer cDataframe,
                                     ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            UpdatingDataframe dataframe = createDataframe(cDataframe);
            AliasDataframeAdder.deleteElements(network, dataframe);
        });
    }

    @CEntryPoint(name = "removeInternalConnections")
    public static void removeInternalConnections(IsolateThread thread, ObjectHandle networkHandle,
                                     DataframePointer cDataframe,
                                     ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            UpdatingDataframe dataframe = createDataframe(cDataframe);
            InternalConnectionDataframeAdder.deleteElements(network, dataframe);
        });
    }

    @CEntryPoint(name = "removeNetworkElements")
    public static void removeNetworkElements(IsolateThread thread, ObjectHandle networkHandle, CCharPointerPointer cElementIds,
                                             int elementCount, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            List<String> elementIds = CTypeUtil.toStringList(cElementIds, elementCount);
            elementIds.forEach(elementId -> {
                Identifiable<?> identifiable = network.getIdentifiable(elementId);
                if (identifiable == null) {
                    throw new PowsyblException(String.format("identifiable with id : %s was not found", elementId));
                }
                if (identifiable instanceof Connectable) {
                    ((Connectable<?>) identifiable).remove();
                } else if (identifiable instanceof HvdcLine hvdcLine) {
                    hvdcLine.remove();
                } else if (identifiable instanceof VoltageLevel voltageLevel) {
                    voltageLevel.remove();
                } else if (identifiable instanceof Substation substation) {
                    substation.remove();
                } else if (identifiable instanceof Switch sw) {
                    VoltageLevel voltageLevel = sw.getVoltageLevel();
                    switch (voltageLevel.getTopologyKind()) {
                        case NODE_BREAKER -> voltageLevel.getNodeBreakerView().removeSwitch(identifiable.getId());
                        case BUS_BREAKER -> voltageLevel.getBusBreakerView().removeSwitch(identifiable.getId());
                        default ->
                                throw new PowsyblException("this voltage level does not have a proper topology kind");
                    }
                } else if (identifiable instanceof TieLine tieLine) {
                    tieLine.remove();
                } else if (identifiable instanceof Area area) {
                    area.remove();
                } else {
                    throw new PowsyblException(String.format("identifiable with id : %s can't be removed", identifiable.getId()));
                }
            });
        });
    }

    public static UpdatingDataframe createDataframe(DataframePointer dataframe) {
        if (dataframe.isNull()) {
            return null;
        }
        int elementCount = dataframe.getSeries().addressOf(0).data().getLength();
        int columnsNumber = dataframe.getSeriesCount();
        DefaultUpdatingDataframe updatingDataframe = new DefaultUpdatingDataframe(elementCount);
        for (int i = 0; i < columnsNumber; i++) {
            PyPowsyblApiHeader.SeriesPointer seriesPointer = dataframe.getSeries().addressOf(i);
            String name = CTypeUtil.toString(seriesPointer.getName());
            switch (seriesPointer.getType()) {
                case STRING_SERIES_TYPE ->
                        updatingDataframe.addSeries(name, seriesPointer.isIndex(), new CStringSeries((CCharPointerPointer) seriesPointer.data().getPtr()));
                case DOUBLE_SERIES_TYPE ->
                        updatingDataframe.addSeries(name, seriesPointer.isIndex(), new CDoubleSeries((CDoublePointer) seriesPointer.data().getPtr()));
                case INT_SERIES_TYPE, BOOLEAN_SERIES_TYPE ->
                        updatingDataframe.addSeries(name, seriesPointer.isIndex(), new CIntSeries((CIntPointer) seriesPointer.data().getPtr()));
                default -> throw new IllegalStateException("Unexpected series type: " + seriesPointer.getType());
            }
        }
        return updatingDataframe;
    }

    @CEntryPoint(name = "getNodeBreakerViewSwitches")
    public static ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getNodeBreakerViewSwitches(IsolateThread thread, ObjectHandle networkHandle, CCharPointer voltageLevelPtr, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            VoltageLevel.NodeBreakerView nodeBreakerView = NetworkUtils.getVoltageLevelOrThrow(network, CTypeUtil.toString(voltageLevelPtr)).getNodeBreakerView();
            return Dataframes.createCDataframe(Dataframes.nodeBreakerViewSwitches(), nodeBreakerView);
        });
    }

    @CEntryPoint(name = "getNodeBreakerViewNodes")
    public static ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getNodeBreakerViewNodes(IsolateThread thread, ObjectHandle networkHandle, CCharPointer voltageLevelPtr, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            VoltageLevel.NodeBreakerView nodeBreakerView = NetworkUtils.getVoltageLevelOrThrow(network, CTypeUtil.toString(voltageLevelPtr)).getNodeBreakerView();

            return Dataframes.createCDataframe(Dataframes.nodeBreakerViewNodes(), nodeBreakerView);

        });
    }

    @CEntryPoint(name = "getNodeBreakerViewInternalConnections")
    public static ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getNodeBreakerViewInternalConnections(IsolateThread thread, ObjectHandle networkHandle, CCharPointer voltageLevelPtr, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            VoltageLevel.NodeBreakerView nodeBreakerView = NetworkUtils.getVoltageLevelOrThrow(network, CTypeUtil.toString(voltageLevelPtr)).getNodeBreakerView();
            return Dataframes.createCDataframe(Dataframes.nodeBreakerViewInternalConnection(), nodeBreakerView);
        });
    }

    @CEntryPoint(name = "getBusBreakerViewSwitches")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getBusBreakerViewSwitches(IsolateThread thread, ObjectHandle networkHandle, CCharPointer voltageLevelPtr, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            VoltageLevel.BusBreakerView busBreakerView = NetworkUtils.getVoltageLevelOrThrow(network, CTypeUtil.toString(voltageLevelPtr)).getBusBreakerView();
            return Dataframes.createCDataframe(Dataframes.busBreakerViewSwitches(), busBreakerView);
        });
    }

    @CEntryPoint(name = "getBusBreakerViewBuses")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getBusBreakerViewBuses(IsolateThread thread, ObjectHandle networkHandle, CCharPointer voltageLevelPtr, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            VoltageLevel voltageLevel = NetworkUtils.getVoltageLevelOrThrow(network, CTypeUtil.toString(voltageLevelPtr));
            return Dataframes.createCDataframe(Dataframes.busBreakerViewBuses(), voltageLevel);
        });
    }

    @CEntryPoint(name = "getBusBreakerViewElements")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getBusBreakerViewElements(IsolateThread thread, ObjectHandle networkHandle, CCharPointer voltageLevelPtr, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            VoltageLevel voltageLevel = NetworkUtils.getVoltageLevelOrThrow(network, CTypeUtil.toString(voltageLevelPtr));
            return Dataframes.createCDataframe(Dataframes.busBreakerViewElements(), voltageLevel);
        });
    }

    @CEntryPoint(name = "merge")
    public static ObjectHandle merge(IsolateThread thread, VoidPointerPointer networkHandles, int networkCount,
                                     ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network[] networks = new Network[networkCount];
            for (int i = 0; i < networkCount; ++i) {
                ObjectHandle networkHandle = networkHandles.read(i);
                Network network = ObjectHandles.getGlobal().get(networkHandle);
                networks[i] = network;
            }
            return ObjectHandles.getGlobal().create(Network.merge(networks));
        });
    }

    @CEntryPoint(name = "getSeriesMetadata")
    public static DataframeMetadataPointer getSeriesMetadata(IsolateThread thread, ElementType elementType,
                                                             ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            DataframeElementType type = convert(elementType);
            List<SeriesMetadata> seriesMetadata = NetworkDataframes.getDataframeMapper(type).getSeriesMetadata();
            return CTypeUtil.createSeriesMetadata(seriesMetadata);
        });
    }

    @CEntryPoint(name = "freeDataframeMetadata")
    public static void freeDataframeMetadata(IsolateThread thread, DataframeMetadataPointer metadata, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            freeDataframeMetadataContent(metadata);
            UnmanagedMemory.free(metadata);
        });
    }

    @CEntryPoint(name = "getCreationMetadata")
    public static DataframesMetadataPointer getCreationMetadata(IsolateThread thread,
                                                                ElementType elementType,
                                                                ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            DataframeElementType type = convert(elementType);
            List<List<SeriesMetadata>> metadata = NetworkElementAdders.getAdder(type).getMetadata();
            DataframeMetadataPointer dataframeMetadataArray = UnmanagedMemory.calloc(metadata.size() * SizeOf.get(DataframeMetadataPointer.class));
            int i = 0;
            for (List<SeriesMetadata> dataframeMetadata : metadata) {
                CTypeUtil.createSeriesMetadata(dataframeMetadata, dataframeMetadataArray.addressOf(i));
                i++;
            }

            DataframesMetadataPointer res = UnmanagedMemory.calloc(SizeOf.get(DataframesMetadataPointer.class));
            res.setDataframesMetadata(dataframeMetadataArray);
            res.setDataframesCount(metadata.size());
            return res;
        });
    }

    @CEntryPoint(name = "freeDataframesMetadata")
    public static void freeDataframesMetadata(IsolateThread thread, DataframesMetadataPointer cMetadata, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            for (int i = 0; i < cMetadata.getDataframesCount(); i++) {
                DataframeMetadataPointer cDataframeMetadata = cMetadata.getDataframesMetadata().addressOf(i);
                freeDataframeMetadataContent(cDataframeMetadata);
            }
            UnmanagedMemory.free(cMetadata.getDataframesMetadata());
            UnmanagedMemory.free(cMetadata);
        });
    }

    @CEntryPoint(name = "addNetworkElementProperties")
    public static void addNetworkElementProperties(IsolateThread thread, ObjectHandle networkHandle, DataframePointer properties, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            UpdatingDataframe propertiesDataframe = createDataframe(properties);
            Objects.requireNonNull(propertiesDataframe);
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            StringSeries idSerie = propertiesDataframe.getStrings("id");
            if (idSerie == null) {
                throw new PowsyblException("id is missing");
            }
            for (SeriesMetadata column : propertiesDataframe.getSeriesMetadata()) {
                if (!column.isIndex() && column.getType() == SeriesDataType.STRING) {
                    String seriesName = column.getName();
                    StringSeries columnSerie = propertiesDataframe.getStrings(seriesName);
                    for (int i = 0; i < propertiesDataframe.getRowCount(); i++) {
                        String id = idSerie.get(i);
                        Identifiable<?> identifiable = network.getIdentifiable(id);
                        if (identifiable != null) {
                            identifiable.setProperty(seriesName, columnSerie.get(i));
                        } else {
                            throw new PowsyblException(String.format("identifiable with id : %s does not exist", id));
                        }
                    }
                }
            }
        });
    }

    @CEntryPoint(name = "removeNetworkElementProperties")
    public static void removeNetworkElementProperties(IsolateThread thread, ObjectHandle networkHandle, CCharPointerPointer idsPointer, int idsCount, CCharPointerPointer propertiesPointer, int propertiesCount, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            List<String> ids = CTypeUtil.toStringList(idsPointer, idsCount);
            List<String> properties = CTypeUtil.toStringList(propertiesPointer, propertiesCount);
            ids.forEach(id -> properties.forEach(property -> {
                Identifiable<?> identifiable = NetworkUtils.getIdentifiableOrThrow(network, id);
                identifiable.removeProperty(property);
            }));
        });
    }

    private static void freeDataframeMetadataContent(DataframeMetadataPointer metadata) {
        for (int i = 0; i < metadata.getAttributesCount(); i++) {
            SeriesMetadataPointer attrMetadata = metadata.getAttributesMetadata().addressOf(i);
            UnmanagedMemory.free(attrMetadata.getName());
        }
        UnmanagedMemory.free(metadata.getAttributesMetadata());
    }

    private static void createSeriesMetadata(List<SeriesMetadata> metadata, DataframeMetadataPointer cMetadata) {
        SeriesMetadataPointer seriesMetadataPtr = UnmanagedMemory.calloc(metadata.size() * SizeOf.get(SeriesMetadataPointer.class));
        for (int i = 0; i < metadata.size(); i++) {
            SeriesMetadata colMetadata = metadata.get(i);
            SeriesMetadataPointer metadataPtr = seriesMetadataPtr.addressOf(i);
            metadataPtr.setName(CTypeUtil.toCharPtr(colMetadata.getName()));
            metadataPtr.setType(convert(colMetadata.getType()));
            metadataPtr.setIndex(colMetadata.isIndex());
            metadataPtr.setModifiable(colMetadata.isModifiable());
            metadataPtr.setDefault(colMetadata.isDefaultAttribute());
        }
        cMetadata.setAttributesCount(metadata.size());
        cMetadata.setAttributesMetadata(seriesMetadataPtr);
    }

    @CEntryPoint(name = "updateNetworkElementsExtensionsWithSeries")
    public static void updateNetworkElementsExtensionsWithSeries(IsolateThread thread, ObjectHandle networkHandle, CCharPointer namePtr,
                                                                 CCharPointer tableNamePtr, DataframePointer dataframe,
                                                                 PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            String name = CTypeUtil.toString(namePtr);
            String tmpName = CTypeUtil.toString(tableNamePtr);
            String tableName = tmpName.isEmpty() ? null : tmpName;
            NetworkDataframeMapper mapper = NetworkDataframes.getExtensionDataframeMapper(name, tableName);
            if (mapper != null) {
                Network network = ObjectHandles.getGlobal().get(networkHandle);
                UpdatingDataframe updatingDataframe = createDataframe(dataframe);
                mapper.updateSeries(network, updatingDataframe, NetworkDataframeContext.DEFAULT);
            } else {
                throw new PowsyblException(errorMessageForWrongExtensionName(name, tableName));
            }
        });
    }

    @CEntryPoint(name = "removeExtensions")
    public static void removeExtensions(IsolateThread thread, ObjectHandle networkHandle,
                                        CCharPointer namePtr,
                                        CCharPointerPointer idsPtr, int idsCount,
                                        PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String name = CTypeUtil.toString(namePtr);
            List<String> ids = CTypeUtil.toStringList(idsPtr, idsCount);
            NetworkExtensions.removeExtensions(network, name, ids);
        });
    }

    @CEntryPoint(name = "getExtensionSeriesMetadata")
    public static DataframeMetadataPointer getExtensionSeriesMetadata(IsolateThread thread, CCharPointer namePtr, CCharPointer tableNamePtr,
                                                                      ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            String name = CTypeUtil.toString(namePtr);
            String tmpName = CTypeUtil.toString(tableNamePtr);
            String tableName = tmpName.equals("") ? null : tmpName;
            NetworkDataframeMapper mapper = NetworkDataframes.getExtensionDataframeMapper(name, tableName);
            if (mapper != null) {
                List<SeriesMetadata> seriesMetadata = mapper.getSeriesMetadata();
                return CTypeUtil.createSeriesMetadata(seriesMetadata);
            } else {
                throw new PowsyblException(errorMessageForWrongExtensionName(name, tableName));
            }
        });
    }

    @CEntryPoint(name = "createExtensions")
    public static void createExtensions(IsolateThread thread, ObjectHandle networkHandle,
                                        CCharPointer namePtr,
                                        DataframeArrayPointer cDataframes,
                                        ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String name = CTypeUtil.toString(namePtr);
            List<UpdatingDataframe> dataframes = new ArrayList<>();
            for (int i = 0; i < cDataframes.getDataframesCount(); i++) {
                dataframes.add(createDataframe(cDataframes.getDataframes().addressOf(i)));
            }
            NetworkElementAdders.addExtensions(name, network, dataframes);
        });
    }

    @CEntryPoint(name = "getExtensionsCreationMetadata")
    public static DataframesMetadataPointer getExtensionsCreationMetadata(IsolateThread thread,
                                                                          CCharPointer namePtr,
                                                                          ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            String name = CTypeUtil.toString(namePtr);
            List<List<SeriesMetadata>> metadata = NetworkElementAdders.getExtensionAdder(name).getMetadata();
            DataframeMetadataPointer dataframeMetadataArray = UnmanagedMemory.calloc(metadata.size() * SizeOf.get(DataframeMetadataPointer.class));
            int i = 0;
            for (List<SeriesMetadata> dataframeMetadata : metadata) {
                CTypeUtil.createSeriesMetadata(dataframeMetadata, dataframeMetadataArray.addressOf(i));
                i++;
            }

            DataframesMetadataPointer res = UnmanagedMemory.calloc(SizeOf.get(DataframesMetadataPointer.class));
            res.setDataframesMetadata(dataframeMetadataArray);
            res.setDataframesCount(metadata.size());
            return res;
        });
    }

    @CEntryPoint(name = "createImporterParametersSeriesArray")
    static ArrayPointer<SeriesPointer> createImporterParametersSeriesArray(IsolateThread thread, CCharPointer formatPtr,
                                                                           ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            String format = CTypeUtil.toString(formatPtr);
            Importer importer = Importer.find(format);
            if (importer == null) {
                throw new PowsyblException("Format '" + format + "' not supported");
            }
            return Dataframes.createCDataframe(Dataframes.importerParametersMapper(), importer);
        });
    }

    @CEntryPoint(name = "createExporterParametersSeriesArray")
    static ArrayPointer<SeriesPointer> createExporterParametersSeriesArray(IsolateThread thread, CCharPointer formatPtr,
                                                                           ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            String format = CTypeUtil.toString(formatPtr);
            var exporter = Exporter.find(format);
            if (exporter == null) {
                throw new PowsyblException("Format '" + format + "' not supported");
            }
            return Dataframes.createCDataframe(Dataframes.exporterParametersMapper(), exporter);
        });
    }

    @CEntryPoint(name = "updateSwitchPosition")
    public static boolean updateSwitchPosition(IsolateThread thread, ObjectHandle networkHandle, CCharPointer id, boolean open,
                                               ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String idStr = CTypeUtil.toString(id);
            return NetworkUtil.updateSwitchPosition(network, idStr, open);
        });
    }

    @CEntryPoint(name = "updateConnectableStatus")
    public static boolean updateConnectableStatus(IsolateThread thread, ObjectHandle networkHandle, CCharPointer id, boolean connected,
                                                  ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String idStr = CTypeUtil.toString(id);
            return NetworkUtil.updateConnectableStatus(network, idStr, connected);
        });
    }

    public static void copyToCSldParameters(SldParameters parameters, SldParametersPointer cParameters) {
        cParameters.setUseName(parameters.getSvgParameters().isUseName());
        cParameters.setCenterName(parameters.getSvgParameters().isLabelCentered());
        cParameters.setDiagonalLabel(parameters.getSvgParameters().isLabelDiagonal());
        cParameters.setTopologicalColoring(parameters.getStyleProviderFactory() instanceof DefaultStyleProviderFactory);
        cParameters.setBusesLegendAdded(parameters.getSvgParameters().isBusesLegendAdded());
        cParameters.setTooltipEnabled(parameters.getSvgParameters().isTooltipEnabled());
        cParameters.setComponentLibrary(CTypeUtil.toCharPtr(parameters.getComponentLibrary().getName()));
        cParameters.setDisplayCurrentFeederInfo(parameters.getSvgParameters().isDisplayCurrentFeederInfo());
        cParameters.setActivePowerUnit(CTypeUtil.toCharPtr(parameters.getSvgParameters().getActivePowerUnit()));
        cParameters.setReactivePowerUnit(CTypeUtil.toCharPtr(parameters.getSvgParameters().getReactivePowerUnit()));
        cParameters.setCurrentUnit(CTypeUtil.toCharPtr(parameters.getSvgParameters().getCurrentUnit()));
    }

    public static SldParametersPointer convertToSldParametersPointer(SldParameters parameters) {
        SldParametersPointer paramsPtr = UnmanagedMemory.calloc(SizeOf.get(SldParametersPointer.class));
        copyToCSldParameters(parameters, paramsPtr);
        return paramsPtr;
    }

    @CEntryPoint(name = "createSldParameters")
    public static SldParametersPointer createSldParameters(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> convertToSldParametersPointer(SingleLineDiagramUtil.createSldParameters()));
    }

    public static NadParametersPointer convertToNadParametersPointer(NadParameters parameters) {
        NadParametersPointer paramsPtr = UnmanagedMemory.calloc(SizeOf.get(NadParametersPointer.class));
        copyToCNadParameters(parameters, paramsPtr);
        return paramsPtr;
    }

    public static void copyToCNadParameters(NadParameters parameters, NadParametersPointer cParameters) {
        int edgeInfo = switch (parameters.getSvgParameters().getEdgeInfoDisplayed()) {
            case ACTIVE_POWER -> 0;
            case REACTIVE_POWER -> 1;
            case CURRENT -> 2;
            default -> throw new PowsyblException("Type of information not taken into account");
        };
        cParameters.setEdgeNameDisplayed(parameters.getSvgParameters().isEdgeNameDisplayed());
        cParameters.setEdgeInfoAlongEdge(parameters.getSvgParameters().isEdgeInfoAlongEdge());
        cParameters.setIdDisplayed(parameters.getSvgParameters().isIdDisplayed());
        cParameters.setPowerValuePrecision(parameters.getSvgParameters().getPowerValuePrecision());
        cParameters.setCurrentValuePrecision(parameters.getSvgParameters().getCurrentValuePrecision());
        cParameters.setAngleValuePrecision(parameters.getSvgParameters().getAngleValuePrecision());
        cParameters.setVoltageValuePrecision(parameters.getSvgParameters().getVoltageValuePrecision());
        cParameters.setBusLegend(parameters.getSvgParameters().isBusLegend());
        cParameters.setSubstationDescriptionDisplayed(parameters.getSvgParameters().isSubstationDescriptionDisplayed());
        cParameters.setEdgeInfoDisplayed(edgeInfo);
        cParameters.setVoltageLevelDetails(parameters.getSvgParameters().isVoltageLevelDetails());
    }

    @CEntryPoint(name = "createNadParameters")
    public static NadParametersPointer createNadParameters(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> convertToNadParametersPointer(NetworkAreaDiagramUtil.createNadParameters()));
    }

    public static void freeSldParametersPointer(SldParametersPointer sldParametersPtr) {
        UnmanagedMemory.free(sldParametersPtr);
    }

    @CEntryPoint(name = "freeSldParameters")
    public static void freeSldParameters(IsolateThread thread, SldParametersPointer sldParametersPtr,
                                         PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            freeSldParametersPointer(sldParametersPtr);
        });
    }

    @CEntryPoint(name = "freeNadParameters")
    public static void freeNadParameters(IsolateThread thread, NadParametersPointer nadParametersPointer,
                                         PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            UnmanagedMemory.free(nadParametersPointer);
        });
    }

    public static SldParameters convertSldParameters(SldParametersPointer sldParametersPtr) {
        String componentLibraryName = CTypeUtil.toString(sldParametersPtr.getComponentLibrary());
        SldParameters sldParameters = SingleLineDiagramUtil.createSldParameters()
                .setStyleProviderFactory(sldParametersPtr.isTopologicalColoring() ? new DefaultStyleProviderFactory()
                        : new NominalVoltageStyleProviderFactory())
                .setComponentLibrary(SldComponentLibrary.find(componentLibraryName).orElseGet(ConvergenceComponentLibrary::new));
        sldParameters.getSvgParameters()
                .setUseName(sldParametersPtr.isUseName())
                .setLabelCentered(sldParametersPtr.isCenterName())
                .setLabelDiagonal(sldParametersPtr.isDiagonalLabel())
                .setBusesLegendAdded(sldParametersPtr.isBusesLegendAdded())
                .setTooltipEnabled(sldParametersPtr.getTooltipEnabled())
                .setDisplayCurrentFeederInfo(sldParametersPtr.isDisplayCurrentFeederInfo())
                .setTooltipEnabled(sldParametersPtr.getTooltipEnabled())
                .setActivePowerUnit(CTypeUtil.toString(sldParametersPtr.getActivePowerUnit()))
                .setReactivePowerUnit(CTypeUtil.toString(sldParametersPtr.getReactivePowerUnit()))
                .setCurrentUnit(CTypeUtil.toString(sldParametersPtr.getCurrentUnit()));
        return sldParameters;
    }

    public static NadParameters convertNadParameters(NadParametersPointer nadParametersPointer, Network network) {
        NadParameters nadParameters = NetworkAreaDiagramUtil.createNadParameters();
        LayoutFactory layoutFactory = switch (nadParametersPointer.getLayoutType()) {
            case 1: yield new GeographicalLayoutFactory(network, nadParametersPointer.getScalingFactor(), nadParametersPointer.getRadiusFactor(), new BasicForceLayoutFactory());
            default: yield new BasicForceLayoutFactory();
        };
        SvgParameters.EdgeInfoEnum edgeInfo = switch (nadParametersPointer.getEdgeInfoDisplayed()) {
            case 0 -> ACTIVE_POWER;
            case 1 -> REACTIVE_POWER;
            case 2 -> CURRENT;
            default -> throw new PowsyblException("Type of information not taken into account");
        };
        nadParameters.setLayoutFactory(layoutFactory);
        nadParameters.getSvgParameters()
                .setEdgeNameDisplayed(nadParametersPointer.isEdgeNameDisplayed())
                .setEdgeInfoAlongEdge(nadParametersPointer.isEdgeInfoAlongEdge())
                .setPowerValuePrecision(nadParametersPointer.getPowerValuePrecision())
                .setCurrentValuePrecision(nadParametersPointer.getCurrentValuePrecision())
                .setAngleValuePrecision(nadParametersPointer.getAngleValuePrecision())
                .setVoltageValuePrecision(nadParametersPointer.getVoltageValuePrecision())
                .setIdDisplayed(nadParametersPointer.isIdDisplayed())
                .setBusLegend(nadParametersPointer.isBusLegend())
                .setSubstationDescriptionDisplayed(nadParametersPointer.isSubstationDescriptionDisplayed())
                .setEdgeInfoDisplayed(edgeInfo);
        return nadParameters;
    }

    @CEntryPoint(name = "writeSingleLineDiagramSvg")
    public static void writeSingleLineDiagramSvg(IsolateThread thread, ObjectHandle networkHandle, CCharPointer containerId,
                                                 CCharPointer svgFile, CCharPointer metadataFile, SldParametersPointer sldParametersPtr,
                                                 ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String containerIdStr = CTypeUtil.toString(containerId);
            String svgFileStr = CTypeUtil.toString(svgFile);
            String metadataFileStr = metadataFile.isNonNull() ? CTypeUtil.toString(metadataFile) : null;
            SldParameters sldParameters = convertSldParameters(sldParametersPtr);
            SingleLineDiagramUtil.writeSvg(network, containerIdStr, svgFileStr, metadataFileStr, sldParameters);
        });
    }

    @CEntryPoint(name = "writeMatrixMultiSubstationSingleLineDiagramSvg")
    public static void writeMatrixMultiSubstationSingleLineDiagramSvg(IsolateThread thread, ObjectHandle networkHandle, CCharPointerPointer substationIdsPointer,
                                                                int substationIdCount, int substationIdRowCount,
                                                 CCharPointer svgFile, CCharPointer metadataFile, SldParametersPointer sldParametersPtr,
                                                 ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String[][] matrixIds = CTypeUtil.toString2DArray(substationIdsPointer, substationIdCount, substationIdRowCount);
            String svgFileStr = CTypeUtil.toString(svgFile);
            String metadataFileStr = metadataFile.isNonNull() ? CTypeUtil.toString(metadataFile) : null;
            SldParameters sldParameters = convertSldParameters(sldParametersPtr);

            SingleLineDiagramUtil.writeMatrixMultiSubstationSvg(network, matrixIds, svgFileStr, metadataFileStr, sldParameters);
        });
    }

    @CEntryPoint(name = "getSingleLineDiagramSvg")
    public static CCharPointer getSingleLineDiagramSvg(IsolateThread thread, ObjectHandle networkHandle, CCharPointer containerId,
                                                       ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String containerIdStr = CTypeUtil.toString(containerId);
            String svg = SingleLineDiagramUtil.getSvg(network, containerIdStr);
            return CTypeUtil.toCharPtr(svg);
        });
    }

    @CEntryPoint(name = "getSingleLineDiagramSvgAndMetadata")
    public static ArrayPointer<CCharPointerPointer> getSingleLineDiagramSvgAndMetadata(IsolateThread thread, ObjectHandle networkHandle, CCharPointer containerId,
                                                                                       SldParametersPointer sldParametersPtr, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String containerIdStr = CTypeUtil.toString(containerId);
            SldParameters sldParameters = convertSldParameters(sldParametersPtr);
            List<String> svgAndMeta = SingleLineDiagramUtil.getSvgAndMetadata(network, containerIdStr, sldParameters);
            return createCharPtrArray(svgAndMeta);
        });
    }

    @CEntryPoint(name = "getMatrixMultiSubstationSvgAndMetadata")
    public static ArrayPointer<CCharPointerPointer> getMatrixMultiSubstationSvgAndMetadata(IsolateThread thread, ObjectHandle networkHandle, CCharPointerPointer substationIdsPointer,
                                                                                           int substationIdCount, int substationIdRowCount,
                                                                                           SldParametersPointer sldParametersPtr, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String[][] matrixIds = CTypeUtil.toString2DArray(substationIdsPointer, substationIdCount, substationIdRowCount);
            SldParameters sldParameters = convertSldParameters(sldParametersPtr);
            List<String> svgAndMeta = SingleLineDiagramUtil.getMatrixMultiSubstationSvgAndMetadata(network, matrixIds, sldParameters);
            return createCharPtrArray(svgAndMeta);
        });
    }

    @CEntryPoint(name = "getSingleLineDiagramComponentLibraryNames")
    public static PyPowsyblApiHeader.ArrayPointer<CCharPointerPointer> getSingleLineDiagramComponentLibraryNames(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> createCharPtrArray(SingleLineDiagramUtil.getComponentLibraryNames()));
    }

    @CEntryPoint(name = "writeNetworkAreaDiagramSvg")
    public static void writeNetworkAreaDiagramSvg(IsolateThread thread, ObjectHandle networkHandle, CCharPointer svgFile, CCharPointer metadataFile,
                                                  CCharPointerPointer voltageLevelIdsPointer, int voltageLevelIdCount, int depth,
                                                  double highNominalVoltageBound, double lowNominalVoltageBound, NadParametersPointer nadParametersPointer,
                                                  DataframePointer fixedPositions, DataframePointer branchLabels, DataframePointer threeWtLabels,
                                                  DataframePointer busDescriptions, DataframePointer vlDescriptions, DataframePointer busNodeStyles,
                                                  DataframePointer edgeStyles, DataframePointer threeWtStyles, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String svgFileStr = CTypeUtil.toString(svgFile);
            String metadataFileStr = metadataFile.isNonNull() ? CTypeUtil.toString(metadataFile) : null;
            List<String> voltageLevelIds = toStringList(voltageLevelIdsPointer, voltageLevelIdCount);
            NadParameters nadParameters = convertNadParameters(nadParametersPointer, network);
            applyFixedPositions(fixedPositions, nadParameters);
            applyCustomLabels(branchLabels, threeWtLabels, busDescriptions, vlDescriptions, nadParameters);
            applyCustomStyles(busNodeStyles, edgeStyles, threeWtStyles, nadParameters);
            NetworkAreaDiagramUtil.writeSvg(network, voltageLevelIds, depth, svgFileStr, metadataFileStr, highNominalVoltageBound, lowNominalVoltageBound, nadParameters);
        });
    }

    @CEntryPoint(name = "getNetworkAreaDiagramSvg")
    public static CCharPointer getNetworkAreaDiagramSvg(IsolateThread thread, ObjectHandle networkHandle, CCharPointerPointer voltageLevelIdsPointer,
                                                        int voltageLevelIdCount, int depth, double highNominalVoltageBound,
                                                        double lowNominalVoltageBound, NadParametersPointer nadParametersPointer, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            List<String> voltageLevelIds = toStringList(voltageLevelIdsPointer, voltageLevelIdCount);
            NadParameters nadParameters = convertNadParameters(nadParametersPointer, network);
            String svg = NetworkAreaDiagramUtil.getSvg(network, voltageLevelIds, depth, highNominalVoltageBound, lowNominalVoltageBound, nadParameters);
            return CTypeUtil.toCharPtr(svg);
        });
    }

    private static Map<String, Point> getNadFixedPositionsMap(int rowCount, StringSeries idSeries, DoubleSeries xSeries, DoubleSeries ySeries) {
        Map<String, Point> fixedPositions = new HashMap<>();
        if (xSeries != null && ySeries != null) {
            for (int i = 0; i < rowCount; i++) {
                String id = idSeries.get(i);
                double x = xSeries.get(i);
                double y = ySeries.get(i);
                if (!Double.isNaN(x) && !Double.isNaN(y)) {
                    fixedPositions.put(id, new Point(x, y));
                }
            }
        }
        return fixedPositions;
    }

    private static Map<String, TextPosition> getNadFixedTextPositionsMap(int rowCount, StringSeries idSeries, DoubleSeries shiftXSeries, DoubleSeries shiftYSeries,
                                                                         DoubleSeries connectionShiftXSeries, DoubleSeries connectionShiftYSeries) {
        Map<String, TextPosition> fixedTextPositions = new HashMap<>();
        if (shiftXSeries != null && shiftYSeries != null && connectionShiftXSeries != null && connectionShiftYSeries != null) {
            for (int i = 0; i < rowCount; i++) {
                String id = idSeries.get(i);
                double shiftX = shiftXSeries.get(i);
                double shiftY = shiftYSeries.get(i);
                double connectionShiftX = connectionShiftXSeries.get(i);
                double connectionShiftY = connectionShiftYSeries.get(i);
                if (!Double.isNaN(shiftX) && !Double.isNaN(shiftY) && !Double.isNaN(connectionShiftX) && !Double.isNaN(connectionShiftY)) {
                    fixedTextPositions.put(id, new TextPosition(new Point(shiftX, shiftY), new Point(connectionShiftX, connectionShiftY)));
                }
            }
        }
        return fixedTextPositions;
    }

    private static void applyFixedPositions(DataframePointer fixedPositions, NadParameters nadParameters) {
        UpdatingDataframe fixedPositionsDataframe = createDataframe(fixedPositions);
        if (fixedPositionsDataframe != null) {
            StringSeries idSeries = fixedPositionsDataframe.getStrings("id");
            if (idSeries == null) {
                throw new PowsyblException("id is missing");
            }
            int rowCount = fixedPositionsDataframe.getRowCount();
            Map<String, Point> fixedPositionsMap = getNadFixedPositionsMap(rowCount, idSeries,
                    fixedPositionsDataframe.getDoubles("x"), fixedPositionsDataframe.getDoubles("y"));
            Map<String, TextPosition> fixedTextPositionsMap = getNadFixedTextPositionsMap(rowCount, idSeries,
                    fixedPositionsDataframe.getDoubles("legend_shift_x"), fixedPositionsDataframe.getDoubles("legend_shift_y"),
                    fixedPositionsDataframe.getDoubles("legend_connection_shift_x"), fixedPositionsDataframe.getDoubles("legend_connection_shift_y"));
            nadParameters.setLayoutFactory(new FixedLayoutFactory(fixedPositionsMap, fixedTextPositionsMap, nadParameters.getLayoutFactory()));
        }
    }

    private static String getValueFromSeriesOrNull(StringSeries series, int row) {
        return (series != null) ? series.get(row) : null;
    }

    private static String getNonEmptyValueFromSeries(StringSeries series, int row) {
        if (series == null) {
            return null;
        }
        String str = series.get(row);
        return str != null && !str.isBlank() ? str : null;
    }

    private static EdgeInfo.Direction getDirectionFromSeriesOrNull(StringSeries series, int row) {
        if (series == null) {
            return null;
        }
        String dir = series.get(row);
        return (dir != null && !dir.isEmpty()) ? EdgeInfo.Direction.valueOf(dir) : null;
    }

    private static Map<String, CustomLabelProvider.BranchLabels> getNadCustomBranchLabels(int rowCount, StringSeries idSeries,
                                                                                                StringSeries side1Label, StringSeries middleLabel,
                                                                                                StringSeries side2Label, StringSeries arrow1,
                                                                                                StringSeries arrow2) {
        Map<String, CustomLabelProvider.BranchLabels> nadCustomBranchLabels = new HashMap<>();
        for (int i = 0; i < rowCount; i++) {
            String id = idSeries.get(i);
            CustomLabelProvider.BranchLabels labels = new CustomLabelProvider.BranchLabels(
                    getValueFromSeriesOrNull(side1Label, i),
                    getValueFromSeriesOrNull(middleLabel, i),
                    getValueFromSeriesOrNull(side2Label, i),
                    getDirectionFromSeriesOrNull(arrow1, i),
                    getDirectionFromSeriesOrNull(arrow2, i)
            );
            nadCustomBranchLabels.put(id, labels);
        }
        return nadCustomBranchLabels;
    }

    private static Map<String, CustomLabelProvider.ThreeWtLabels> getNadCustomThreeWtLabels(UpdatingDataframe threeWtLabelsDataframe) {
        int rowCount = threeWtLabelsDataframe.getRowCount();
        StringSeries idS = threeWtLabelsDataframe.getStrings("id");
        StringSeries side1S = threeWtLabelsDataframe.getStrings("side1");
        StringSeries side2S = threeWtLabelsDataframe.getStrings("side2");
        StringSeries side3S = threeWtLabelsDataframe.getStrings("side3");
        StringSeries arrow1S = threeWtLabelsDataframe.getStrings("arrow1");
        StringSeries arrow2S = threeWtLabelsDataframe.getStrings("arrow2");
        StringSeries arrow3S = threeWtLabelsDataframe.getStrings("arrow3");

        Map<String, CustomLabelProvider.ThreeWtLabels> nadCustomThreeWtLabels = new HashMap<>();
        for (int i = 0; i < rowCount; i++) {
            String id = idS.get(i);
            CustomLabelProvider.ThreeWtLabels labels = new CustomLabelProvider.ThreeWtLabels(
                    getValueFromSeriesOrNull(side1S, i),
                    getValueFromSeriesOrNull(side2S, i),
                    getValueFromSeriesOrNull(side3S, i),
                    getDirectionFromSeriesOrNull(arrow1S, i),
                    getDirectionFromSeriesOrNull(arrow2S, i),
                    getDirectionFromSeriesOrNull(arrow3S, i)
            );
            nadCustomThreeWtLabels.put(id, labels);
        }
        return nadCustomThreeWtLabels;
    }

    private static Map<String, String> getNadCustomBusDescriptions(int rowCount, StringSeries idS, StringSeries descriptionS) {
        Map<String, String> nadCustomDescriptions = new HashMap<>();
        for (int i = 0; i < rowCount; i++) {
            String id = idS.get(i);
            String description = descriptionS.get(i);
            nadCustomDescriptions.put(id, description);
        }
        return nadCustomDescriptions;
    }

    public record VlInfo(Map<String, List<String>> headers, Map<String, List<String>> footers) {

    }

    public static VlInfo getNadCustomVlInfos(int rowCount, StringSeries ids,
                                             StringSeries types, StringSeries descriptions) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        Map<String, List<String>> footers = new LinkedHashMap<>();

        IntStream.range(0, rowCount)
                .forEach(i -> {
                    String id = ids.get(i);
                    String description = descriptions.get(i);
                    String type = types.get(i);

                    Map<String, List<String>> targetMap = type.equals("HEADER") ? headers : footers;
                    targetMap.computeIfAbsent(id, k -> new ArrayList<>()).add(description);
                });

        return new VlInfo(headers, footers);
    }

    private static void applyCustomLabels(DataframePointer customLabels, DataframePointer threeWtLabels, DataframePointer busDescriptions, DataframePointer vlDescriptions, NadParameters nadParameters) {
        UpdatingDataframe customLabelsDataframe = createDataframe(customLabels);
        UpdatingDataframe threeWtLabelsDataframe = createDataframe(threeWtLabels);
        UpdatingDataframe busDescriptionsDataframe = createDataframe(busDescriptions);
        UpdatingDataframe customVlDescriptionsDataframe = createDataframe(vlDescriptions);
        if (customLabelsDataframe != null || threeWtLabelsDataframe != null || busDescriptionsDataframe != null || customVlDescriptionsDataframe != null) {
            final Map<String, CustomLabelProvider.BranchLabels> branchLabels;
            if (customLabelsDataframe != null) {
                //when the custom dataframe is defined, the displaying of the edge name is forced
                nadParameters.getSvgParameters().setEdgeNameDisplayed(true);
                branchLabels = getNadCustomBranchLabels(customLabelsDataframe.getRowCount(), customLabelsDataframe.getStrings("id"),
                        customLabelsDataframe.getStrings("side1"),
                        customLabelsDataframe.getStrings("middle"), customLabelsDataframe.getStrings("side2"),
                        customLabelsDataframe.getStrings("arrow1"), customLabelsDataframe.getStrings("arrow2"));
            } else {
                branchLabels = Collections.emptyMap();
            }

            Map<String, CustomLabelProvider.ThreeWtLabels> customThreeWtLabels = (threeWtLabelsDataframe != null) ? getNadCustomThreeWtLabels(threeWtLabelsDataframe) : Collections.emptyMap();

            final Map<String, String> customBusDescriptions;
            if (busDescriptionsDataframe != null) {
                //when the custom dataframe is defined, the displaying of the bus legend section is forced
                nadParameters.getSvgParameters().setBusLegend(true);
                customBusDescriptions = getNadCustomBusDescriptions(busDescriptionsDataframe.getRowCount(),
                        busDescriptionsDataframe.getStrings("id"),
                        busDescriptionsDataframe.getStrings("description"));
            } else {
                customBusDescriptions = Collections.emptyMap();
            }

            final Map<String, CustomLabelProvider.InjectionLabels> injectionlabels = Collections.emptyMap();

            final Map<String, List<String>> customVlDescriptions;
            final Map<String, List<String>> customVlDetails;
            if (customVlDescriptionsDataframe != null) {
                //when the custom dataframe is defined, the displaying of the vl details section is forced
                nadParameters.getSvgParameters().setVoltageLevelDetails(true);
                VlInfo vlInfo = getNadCustomVlInfos(customVlDescriptionsDataframe.getRowCount(),
                        customVlDescriptionsDataframe.getStrings("id"),
                        customVlDescriptionsDataframe.getStrings("type"),
                        customVlDescriptionsDataframe.getStrings("description"));
                customVlDescriptions = vlInfo.headers();
                customVlDetails = vlInfo.footers();
            } else {
                customVlDescriptions = Collections.emptyMap();
                customVlDetails = Collections.emptyMap();
            }
            nadParameters.setLabelProviderFactory((network, svgParameters) ->
                    new CustomLabelProvider(branchLabels, customThreeWtLabels, injectionlabels, customBusDescriptions, customVlDescriptions, customVlDetails));
        }
    }

    private static Map<String, CustomStyleProvider.BusNodeStyles> getNadCustomBusStyles(int rowCount, StringSeries idS, StringSeries fillS, StringSeries edgeS, StringSeries edgeWidthS) {
        Map<String, CustomStyleProvider.BusNodeStyles> customBusNodeStyles = new HashMap<>();
        for (int i = 0; i < rowCount; i++) {
            String id = idS.get(i);
            CustomStyleProvider.BusNodeStyles busStyles = new CustomStyleProvider.BusNodeStyles(
                    getNonEmptyValueFromSeries(fillS, i),
                    getNonEmptyValueFromSeries(edgeS, i),
                    getNonEmptyValueFromSeries(edgeWidthS, i)
            );
            customBusNodeStyles.put(id, busStyles);
        }
        return customBusNodeStyles;
    }

    private static Map<String, CustomStyleProvider.EdgeStyles> getNadCustomEdgeStyles(UpdatingDataframe customEdgeStylesDataframe) {
        int rowCount = customEdgeStylesDataframe.getRowCount();
        StringSeries idS = customEdgeStylesDataframe.getStrings("id");
        StringSeries edge1S = customEdgeStylesDataframe.getStrings("edge1");
        StringSeries width1S = customEdgeStylesDataframe.getStrings("width1");
        StringSeries dash1S = customEdgeStylesDataframe.getStrings("dash1");
        StringSeries edge2S = customEdgeStylesDataframe.getStrings("edge2");
        StringSeries width2S = customEdgeStylesDataframe.getStrings("width2");
        StringSeries dash2S = customEdgeStylesDataframe.getStrings("dash2");

        Map<String, CustomStyleProvider.EdgeStyles> customEdgeStyles = new HashMap<>();
        for (int i = 0; i < rowCount; i++) {
            String id = idS.get(i);
            CustomStyleProvider.EdgeStyles busStyles = new CustomStyleProvider.EdgeStyles(
                    getNonEmptyValueFromSeries(edge1S, i),
                    getNonEmptyValueFromSeries(width1S, i),
                    getNonEmptyValueFromSeries(dash1S, i),
                    getNonEmptyValueFromSeries(edge2S, i),
                    getNonEmptyValueFromSeries(width2S, i),
                    getNonEmptyValueFromSeries(dash2S, i)
            );
            customEdgeStyles.put(id, busStyles);
        }
        return customEdgeStyles;
    }

    private static Map<String, CustomStyleProvider.ThreeWtStyles> getNadCustomThreeWtStyles(UpdatingDataframe customThreeWtStylesDataframe) {
        StringSeries edge3S = customThreeWtStylesDataframe.getStrings("edge3");
        StringSeries width3S = customThreeWtStylesDataframe.getStrings("width3");
        StringSeries dash3S = customThreeWtStylesDataframe.getStrings("dash3");
        StringSeries edge2S = customThreeWtStylesDataframe.getStrings("edge2");
        StringSeries width2S = customThreeWtStylesDataframe.getStrings("width2");
        StringSeries dash2S = customThreeWtStylesDataframe.getStrings("dash2");
        StringSeries edge1S = customThreeWtStylesDataframe.getStrings("edge1");
        StringSeries width1S = customThreeWtStylesDataframe.getStrings("width1");
        StringSeries dash1S = customThreeWtStylesDataframe.getStrings("dash1");
        StringSeries idS = customThreeWtStylesDataframe.getStrings("id");
        int rowCount = customThreeWtStylesDataframe.getRowCount();

        Map<String, CustomStyleProvider.ThreeWtStyles> customThreeWtStyles = new HashMap<>();
        for (int i = 0; i < rowCount; i++) {
            String id = idS.get(i);
            CustomStyleProvider.ThreeWtStyles threeWtStyles = new CustomStyleProvider.ThreeWtStyles(
                    getNonEmptyValueFromSeries(edge1S, i),
                    getNonEmptyValueFromSeries(width1S, i),
                    getNonEmptyValueFromSeries(dash1S, i),
                    getNonEmptyValueFromSeries(edge2S, i),
                    getNonEmptyValueFromSeries(width2S, i),
                    getNonEmptyValueFromSeries(dash2S, i),
                    getNonEmptyValueFromSeries(edge3S, i),
                    getNonEmptyValueFromSeries(width3S, i),
                    getNonEmptyValueFromSeries(dash3S, i)
            );
            customThreeWtStyles.put(id, threeWtStyles);
        }
        return customThreeWtStyles;
    }

    private static void applyCustomStyles(DataframePointer busNodeStyles, DataframePointer edgeStyles, DataframePointer threeWtStyles, NadParameters nadParameters) {
        UpdatingDataframe customBusNodeStylesDataframe = createDataframe(busNodeStyles);
        UpdatingDataframe customEdgeStylesDataframe = createDataframe(edgeStyles);
        UpdatingDataframe customThreeWtStylesDataframe = createDataframe(threeWtStyles);
        if (customBusNodeStylesDataframe != null || customEdgeStylesDataframe != null || customThreeWtStylesDataframe != null) {
            final Map<String, CustomStyleProvider.BusNodeStyles> busStyles;
            if (customBusNodeStylesDataframe != null) {
                busStyles = getNadCustomBusStyles(customBusNodeStylesDataframe.getRowCount(), customBusNodeStylesDataframe.getStrings("id"),
                        customBusNodeStylesDataframe.getStrings("fill"),
                        customBusNodeStylesDataframe.getStrings("edge"),
                        customBusNodeStylesDataframe.getStrings("edge-width"));
            } else {
                busStyles = Collections.emptyMap();
            }

            final Map<String, CustomStyleProvider.EdgeStyles> edgesStyles;
            if (customEdgeStylesDataframe != null) {
                edgesStyles = getNadCustomEdgeStyles(customEdgeStylesDataframe);
            } else {
                edgesStyles = Collections.emptyMap();
            }

            Map<String, CustomStyleProvider.ThreeWtStyles> threeWtsStyles;
            if (customThreeWtStylesDataframe != null) {
                threeWtsStyles = getNadCustomThreeWtStyles(customThreeWtStylesDataframe);
            } else {
                threeWtsStyles = Collections.emptyMap();
            }

            nadParameters.setStyleProviderFactory(network -> new CustomStyleProvider(busStyles, edgesStyles, threeWtsStyles));
        }
    }

    @CEntryPoint(name = "getNetworkAreaDiagramSvgAndMetadata")
    public static ArrayPointer<CCharPointerPointer> getNetworkAreaDiagramSvgAndMetadata(IsolateThread thread, ObjectHandle networkHandle, CCharPointerPointer voltageLevelIdsPointer,
                                                                                        int voltageLevelIdCount, int depth, double highNominalVoltageBound,
                                                                                        double lowNominalVoltageBound, NadParametersPointer nadParametersPointer,
                                                                                        DataframePointer fixedPositions, DataframePointer branchLabels, DataframePointer threeWtLabels, DataframePointer busDescriptions,
                                                                                        DataframePointer vlDescriptions, DataframePointer busNodeStyles, DataframePointer edgeStyles,
                                                                                        DataframePointer threeWtStyles, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            List<String> voltageLevelIds = toStringList(voltageLevelIdsPointer, voltageLevelIdCount);
            NadParameters nadParameters = convertNadParameters(nadParametersPointer, network);
            applyFixedPositions(fixedPositions, nadParameters);
            applyCustomLabels(branchLabels, threeWtLabels, busDescriptions, vlDescriptions, nadParameters);
            applyCustomStyles(busNodeStyles, edgeStyles, threeWtStyles, nadParameters);
            List<String> svgAndMeta = NetworkAreaDiagramUtil.getSvgAndMetadata(network, voltageLevelIds, depth, highNominalVoltageBound, lowNominalVoltageBound, nadParameters);
            return createCharPtrArray(svgAndMeta);
        });
    }

    @CEntryPoint(name = "getNetworkAreaDiagramDisplayedVoltageLevels")
    public static PyPowsyblApiHeader.ArrayPointer<CCharPointerPointer> getNetworkAreaDiagramDisplayedVoltageLevels(IsolateThread thread, ObjectHandle networkHandle, CCharPointerPointer voltageLevelIdsPointer,
                                                                                                                   int voltageLevelIdCount, int depth, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            List<String> voltageLevelIds = toStringList(voltageLevelIdsPointer, voltageLevelIdCount);
            return createCharPtrArray(NetworkAreaDiagramUtil.getDisplayedVoltageLevels(network, voltageLevelIds, depth));
        });
    }

    private static SvgParameters getNadSvgParsForDefaultLabels() {
        return new SvgParameters()
                .setSvgWidthAndHeightAdded(true)
                .setEdgeNameDisplayed(true)
                .setVoltageLevelDetails(true)
                .setBusLegend(true);
    }

    @CEntryPoint(name = "getNetworkAreaDiagramDefaultBranchLabels")
    public static ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getNetworkAreaDiagramDefaultBranchLabels(IsolateThread thread, ObjectHandle networkHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            SvgParameters pars = getNadSvgParsForDefaultLabels();
            Map<String, CustomLabelProvider.BranchLabels> labelMap = NetworkAreaDiagramUtil.getBranchLabelsMap(network, pars);
            return Dataframes.createCDataframe(NetworkAreaDiagramUtil.BRANCH_LABELS_MAPPER, labelMap);
        });
    }

    @CEntryPoint(name = "getNetworkAreaDiagramDefaultThreeWtLabels")
    public static ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getNetworkAreaDiagramDefaultThreeWtLabels(IsolateThread thread, ObjectHandle networkHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            SvgParameters pars = getNadSvgParsForDefaultLabels();
            Map<String, CustomLabelProvider.ThreeWtLabels> labelMap = NetworkAreaDiagramUtil.getThreeWtBranchLabelsMap(network, pars);
            return Dataframes.createCDataframe(NetworkAreaDiagramUtil.TWT_LABELS_MAPPER, labelMap);
        });
    }

    @CEntryPoint(name = "getNetworkAreaDiagramDefaultBusDescriptions")
    public static ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getNetworkAreaDiagramDefaultBusDescriptions(IsolateThread thread, ObjectHandle networkHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            SvgParameters pars = getNadSvgParsForDefaultLabels();
            Map<String, String> labelMap = NetworkAreaDiagramUtil.getBusDescriptionsMap(network, pars);
            return Dataframes.createCDataframe(NetworkAreaDiagramUtil.BUS_DESCRIPTIONS_MAPPER, labelMap);
        });
    }

    @CEntryPoint(name = "getNetworkAreaDiagramDefaultVlDescriptions")
    public static ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getNetworkAreaDiagramDefaultVlDescriptions(IsolateThread thread, ObjectHandle networkHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            SvgParameters pars = getNadSvgParsForDefaultLabels();
            List<NetworkAreaDiagramUtil.VlInfos> vlInfos = NetworkAreaDiagramUtil.getVlDescriptionsWithType(network, pars);
            return Dataframes.createCDataframe(NetworkAreaDiagramUtil.VL_DESCRIPTIONS_MAPPER, vlInfos);
        });
    }

    @CEntryPoint(name = "getValidationLevel")
    public static ValidationLevelType getValidationLevel(IsolateThread thread, ObjectHandle networkHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        exceptionHandlerPtr.setMessage(WordFactory.nullPointer());
        try {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            return Util.convert(network.getValidationLevel());
        } catch (Throwable t) {
            setException(exceptionHandlerPtr, t);
            return Util.convert(ValidationLevel.MINIMUM_VALUE);
        }
    }

    @CEntryPoint(name = "validate")
    public static ValidationLevelType validate(IsolateThread thread, ObjectHandle networkHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        exceptionHandlerPtr.setMessage(WordFactory.nullPointer());
        try {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            return Util.convert(network.runValidationChecks());
        } catch (Throwable t) {
            setException(exceptionHandlerPtr, t);
            return Util.convert(ValidationLevel.MINIMUM_VALUE);
        }
    }

    @CEntryPoint(name = "setMinValidationLevel")
    public static void setMinValidationLevel(IsolateThread thread, ObjectHandle networkHandle,
                                             ValidationLevelType levelType,
                                             ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            network.setMinimumAcceptableValidationLevel(Util.convert(levelType));
        });
    }

    @CEntryPoint(name = "getModificationMetadataWithElementType")
    public static DataframesMetadataPointer getModificationMetadataWithElementType(IsolateThread thread,
                                                                                   NetworkModificationType networkModificationType,
                                                                                   ElementType elementType,
                                                                                   ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            DataframeNetworkModificationType modificationType = convert(networkModificationType);
            DataframeElementType type = convert(elementType);
            List<List<SeriesMetadata>> metadata = NetworkModifications.getModification(modificationType).getMetadata(type);
            DataframeMetadataPointer dataframeMetadataArray = UnmanagedMemory.calloc(metadata.size() * SizeOf.get(DataframeMetadataPointer.class));
            int i = 0;
            for (List<SeriesMetadata> dataframeMetadata : metadata) {
                createSeriesMetadata(dataframeMetadata, dataframeMetadataArray.addressOf(i));
                i++;
            }
            DataframesMetadataPointer res = UnmanagedMemory.calloc(SizeOf.get(DataframesMetadataPointer.class));
            res.setDataframesMetadata(dataframeMetadataArray);
            res.setDataframesCount(metadata.size());
            return res;
        });
    }

    @CEntryPoint(name = "getSubNetwork")
    public static ObjectHandle getSubNetwork(IsolateThread thread, ObjectHandle networkHandle, CCharPointer subNetworkId, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String subNetworkIdStr = CTypeUtil.toString(subNetworkId);
            Network subnetwork = network.getSubnetwork(subNetworkIdStr);
            if (subnetwork == null) {
                throw new PowsyblException("Sub network '" + subNetworkIdStr + "' not found");
            }
            return ObjectHandles.getGlobal().create(subnetwork);
        });
    }

    @CEntryPoint(name = "detachSubNetwork")
    public static ObjectHandle detachSubNetwork(IsolateThread thread, ObjectHandle subNetworkHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network subNetwork = ObjectHandles.getGlobal().get(subNetworkHandle);
            Network detachNetwork = subNetwork.detach();
            return ObjectHandles.getGlobal().create(detachNetwork);
        });
    }
}
