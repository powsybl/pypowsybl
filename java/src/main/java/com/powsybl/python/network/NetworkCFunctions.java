/**
 * Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.network;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.commons.reporter.Reporter;
import com.powsybl.dataframe.DataframeElementType;
import com.powsybl.dataframe.DataframeFilter;
import com.powsybl.dataframe.DataframeFilter.AttributeFilterType;
import com.powsybl.dataframe.SeriesDataType;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.NetworkDataframeMapper;
import com.powsybl.dataframe.network.NetworkDataframes;
import com.powsybl.dataframe.network.adders.AliasDataframeAdder;
import com.powsybl.dataframe.network.adders.FeederBaysLineSeries;
import com.powsybl.dataframe.network.adders.FeederBaysTwtSeries;
import com.powsybl.dataframe.network.adders.NetworkElementAdders;
import com.powsybl.dataframe.network.extensions.NetworkExtensions;
import com.powsybl.dataframe.update.DefaultUpdatingDataframe;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.reducer.*;
import com.powsybl.python.commons.*;
import com.powsybl.python.dataframe.CDoubleSeries;
import com.powsybl.python.dataframe.CIntSeries;
import com.powsybl.python.dataframe.CStringSeries;
import com.powsybl.python.report.ReportCUtils;
import com.powsybl.sld.layout.LayoutParameters;
import org.apache.commons.io.IOUtils;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CDoublePointer;
import org.graalvm.nativeimage.c.type.CIntPointer;
import org.graalvm.word.WordFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;

import static com.powsybl.python.commons.CTypeUtil.toStringList;
import static com.powsybl.python.commons.PyPowsyblApiHeader.*;
import static com.powsybl.python.commons.Util.*;
import static com.powsybl.python.dataframe.CDataframeHandler.*;

/**
 * Defines the basic C functions for a network.
 *
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 */
@CContext(Directives.class)
public final class NetworkCFunctions {

    private static final Supplier<ExportersLoader> EXPORTERS_LOADER_SUPPLIER = Suppliers.memoize(ExportersServiceLoader::new);
    private static final Supplier<ImportersLoader> IMPORTERS_LOADER_SUPPLIER = Suppliers.memoize(ImportersServiceLoader::new);

    private NetworkCFunctions() {
    }

    @CEntryPoint(name = "getNetworkImportFormats")
    public static ArrayPointer<CCharPointerPointer> getNetworkImportFormats(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> createCharPtrArray(new ArrayList<>(Importer.getFormats())));
    }

    @CEntryPoint(name = "getNetworkExportFormats")
    public static ArrayPointer<CCharPointerPointer> getNetworkExportFormats(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> createCharPtrArray(new ArrayList<>(Exporter.getFormats())));
    }

    @CEntryPoint(name = "createNetwork")
    public static ObjectHandle createNetwork(IsolateThread thread, CCharPointer name, CCharPointer id, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            String networkName = CTypeUtil.toString(name);
            String networkId = CTypeUtil.toString(id);
            Network network = Networks.create(networkName, networkId);
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
        ptr.setCaseDate(network.getCaseDate().getMillis() / 1000.0d);
        return ptr;
    }

    private static void freeNetworkMetadata(NetworkMetadataPointer networkMetadataPointer) {
        UnmanagedMemory.free(networkMetadataPointer.getId());
        UnmanagedMemory.free(networkMetadataPointer.getName());
        UnmanagedMemory.free(networkMetadataPointer.getSourceFormat());
        UnmanagedMemory.free(networkMetadataPointer);
    }

    @CEntryPoint(name = "loadNetwork")
    public static ObjectHandle loadNetwork(IsolateThread thread, CCharPointer file, CCharPointerPointer parameterNamesPtrPtr, int parameterNamesCount,
                                           CCharPointerPointer parameterValuesPtrPtr, int parameterValuesCount, ObjectHandle reporterHandle,
                                           ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            String fileStr = CTypeUtil.toString(file);
            Properties parameters = createParameters(parameterNamesPtrPtr, parameterNamesCount, parameterValuesPtrPtr, parameterValuesCount);
            Reporter reporter = ReportCUtils.getReporter(reporterHandle);
            Network network = Network.read(Paths.get(fileStr), CommonObjects.getComputationManager(), ImportConfig.load(), parameters, IMPORTERS_LOADER_SUPPLIER.get(), reporter);
            return ObjectHandles.getGlobal().create(network);
        });
    }

    @CEntryPoint(name = "loadNetworkFromString")
    public static ObjectHandle loadNetworkFromString(IsolateThread thread, CCharPointer fileName, CCharPointer fileContent,
                                                     CCharPointerPointer parameterNamesPtrPtr, int parameterNamesCount,
                                                     CCharPointerPointer parameterValuesPtrPtr, int parameterValuesCount,
                                                     ObjectHandle reporterHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            String fileNameStr = CTypeUtil.toString(fileName);
            String fileContentStr = CTypeUtil.toString(fileContent);
            Properties parameters = createParameters(parameterNamesPtrPtr, parameterNamesCount, parameterValuesPtrPtr, parameterValuesCount);
            Reporter reporter = ReportCUtils.getReporter(reporterHandle);
            try (InputStream is = new ByteArrayInputStream(fileContentStr.getBytes(StandardCharsets.UTF_8))) {
                Network network = Network.read(fileNameStr, is, CommonObjects.getComputationManager(), ImportConfig.load(), parameters, IMPORTERS_LOADER_SUPPLIER.get(), reporter);
                return ObjectHandles.getGlobal().create(network);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @CEntryPoint(name = "dumpNetwork")
    public static void dumpNetwork(IsolateThread thread, ObjectHandle networkHandle, CCharPointer file, CCharPointer format,
                                   CCharPointerPointer parameterNamesPtrPtr, int parameterNamesCount,
                                   CCharPointerPointer parameterValuesPtrPtr, int parameterValuesCount,
                                   ObjectHandle reporterHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String fileStr = CTypeUtil.toString(file);
            String formatStr = CTypeUtil.toString(format);
            Properties parameters = createParameters(parameterNamesPtrPtr, parameterNamesCount, parameterValuesPtrPtr, parameterValuesCount);
            Reporter reporter = ReportCUtils.getReporter(reporterHandle);
            network.write(EXPORTERS_LOADER_SUPPLIER.get(), formatStr, parameters, Paths.get(fileStr), reporter);
        });
    }

    @CEntryPoint(name = "dumpNetworkToString")
    public static CCharPointer dumpNetworkToString(IsolateThread thread, ObjectHandle networkHandle, CCharPointer format,
                                                   CCharPointerPointer parameterNamesPtrPtr, int parameterNamesCount,
                                                   CCharPointerPointer parameterValuesPtrPtr, int parameterValuesCount,
                                                   ObjectHandle reporterHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String formatStr = CTypeUtil.toString(format);
            Properties parameters = createParameters(parameterNamesPtrPtr, parameterNamesCount, parameterValuesPtrPtr, parameterValuesCount);
            MemDataSource dataSource = new MemDataSource();
            var exporter = Exporter.find(formatStr);
            if (exporter == null) {
                throw new PowsyblException("No exporter found for '" + formatStr + "' to export as a string");
            }
            Reporter reporter = ReportCUtils.getReporter(reporterHandle);
            exporter.export(network, parameters, dataSource, reporter);
            try {
                var names = dataSource.listNames(".*?");
                if (names.size() != 1) {
                    throw new PowsyblException("Currently we only support string export for single file format(ex, 'XIIDM').");
                }
                try (InputStream is = new ByteArrayInputStream(dataSource.getData(Iterables.getOnlyElement(names)));
                     ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                    IOUtils.copy(is, os);
                    return CTypeUtil.toCharPtr(os.toString());
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
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
        AttributeFilterType filterType = AttributeFilterType.DEFAULT_ATTRIBUTES;
        switch (filterAttributesType) {
            case ALL_ATTRIBUTES:
                filterType = AttributeFilterType.ALL_ATTRIBUTES;
                break;
            case SELECTION_ATTRIBUTES:
                filterType = AttributeFilterType.INPUT_ATTRIBUTES;
                break;
            case DEFAULT_ATTRIBUTES:
                filterType = AttributeFilterType.DEFAULT_ATTRIBUTES;
                break;
        }

        DataframeFilter dataframeFilter = selectedElementsDataframe.isNonNull()
                ? new DataframeFilter(filterType, attributes, createDataframe(selectedElementsDataframe))
                : new DataframeFilter(filterType, attributes);
        return dataframeFilter;
    }

    @CEntryPoint(name = "createNetworkElementsSeriesArray")
    public static ArrayPointer<SeriesPointer> createNetworkElementsSeriesArray(IsolateThread thread, ObjectHandle networkHandle,
                                                                               ElementType elementType,
                                                                               FilterAttributesType filterAttributesType,
                                                                               CCharPointerPointer attributesPtrPtr, int attributesCount,
                                                                               DataframePointer selectedElementsDataframe,
                                                                               ExceptionHandlerPointer exceptionHandlerPtr) {
        return Util.doCatch(exceptionHandlerPtr, () -> {
            NetworkDataframeMapper mapper = NetworkDataframes.getDataframeMapper(convert(elementType));
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            DataframeFilter dataframeFilter = createDataframeFilter(filterAttributesType, attributesPtrPtr, attributesCount, selectedElementsDataframe);
            return Dataframes.createCDataframe(mapper, network, dataframeFilter);
        });
    }

    @CEntryPoint(name = "createNetworkElementsExtensionSeriesArray")
    public static ArrayPointer<PyPowsyblApiHeader.SeriesPointer> createNetworkElementsExtensionSeriesArray(IsolateThread thread, ObjectHandle networkHandle,
                                                                                                           CCharPointer extensionName,
                                                                                                           PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        String name = CTypeUtil.toString(extensionName);
        return doCatch(exceptionHandlerPtr, () -> {
            NetworkDataframeMapper mapper = NetworkDataframes.getExtensionDataframeMapper(name);
            if (mapper != null) {
                Network network = ObjectHandles.getGlobal().get(networkHandle);
                return Dataframes.createCDataframe(mapper, network);
            } else {
                throw new PowsyblException("extension " + name + " not found");
            }
        });
    }

    @CEntryPoint(name = "getExtensionsNames")
    public static ArrayPointer<CCharPointerPointer> getExtensionsNames(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            return createCharPtrArray(List.copyOf(NetworkExtensions.getExtensionsNames()));
        });
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
                                                       DataframePointer dataframe,
                                                       PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            UpdatingDataframe updatingDataframe = createDataframe(dataframe);
            NetworkDataframes.getDataframeMapper(convert(elementType)).updateSeries(network, updatingDataframe);
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

    @CEntryPoint(name = "removeNetworkElements")
    public static void removeNetworkElements(IsolateThread thread, ObjectHandle networkHandle, CCharPointerPointer cElementIds,
                                             int elementCount, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            List<String> elementIds = CTypeUtil.toStringList(cElementIds, elementCount);
            elementIds.forEach(elementId -> {
                Identifiable identifiable = network.getIdentifiable(elementId);
                if (identifiable == null) {
                    throw new PowsyblException(String.format("identifiable with id : %s was not found", elementId));
                }
                if (identifiable instanceof Connectable) {
                    ((Connectable) identifiable).remove();
                } else if (identifiable instanceof HvdcLine) {
                    ((HvdcLine) identifiable).remove();
                } else if (identifiable instanceof VoltageLevel) {
                    ((VoltageLevel) identifiable).remove();
                } else if (identifiable instanceof Substation) {
                    ((Substation) identifiable).remove();
                } else if (identifiable instanceof Switch) {
                    VoltageLevel voltageLevel = ((Switch) identifiable).getVoltageLevel();
                    switch (voltageLevel.getTopologyKind()) {
                        case NODE_BREAKER:
                            voltageLevel.getNodeBreakerView().removeSwitch(identifiable.getId());
                            break;
                        case BUS_BREAKER:
                            voltageLevel.getBusBreakerView().removeSwitch(identifiable.getId());
                            break;
                        default:
                            throw new PowsyblException("this voltage level does not have a proper topology kind");
                    }
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
                case STRING_SERIES_TYPE:
                    updatingDataframe.addSeries(name, seriesPointer.isIndex(), new CStringSeries((CCharPointerPointer) seriesPointer.data().getPtr()));
                    break;
                case DOUBLE_SERIES_TYPE:
                    updatingDataframe.addSeries(name, seriesPointer.isIndex(), new CDoubleSeries((CDoublePointer) seriesPointer.data().getPtr()));
                    break;
                case INT_SERIES_TYPE:
                case BOOLEAN_SERIES_TYPE:
                    updatingDataframe.addSeries(name, seriesPointer.isIndex(), new CIntSeries((CIntPointer) seriesPointer.data().getPtr()));
                    break;
                default:
                    throw new IllegalStateException("Unexpected series type: " + seriesPointer.getType());
            }
        }
        return updatingDataframe;
    }

    @CEntryPoint(name = "getNodeBreakerViewSwitches")
    public static ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getNodeBreakerViewSwitches(IsolateThread thread, ObjectHandle networkHandle, CCharPointer voltageLevel, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            VoltageLevel.NodeBreakerView nodeBreakerView = network.getVoltageLevel(CTypeUtil.toString(voltageLevel)).getNodeBreakerView();
            return Dataframes.createCDataframe(Dataframes.nodeBreakerViewSwitches(), nodeBreakerView);
        });
    }

    @CEntryPoint(name = "getNodeBreakerViewNodes")
    public static ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getNodeBreakerViewNodes(IsolateThread thread, ObjectHandle networkHandle, CCharPointer voltageLevel, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            VoltageLevel.NodeBreakerView nodeBreakerView = network.getVoltageLevel(CTypeUtil.toString(voltageLevel)).getNodeBreakerView();

            return Dataframes.createCDataframe(Dataframes.nodeBreakerViewNodes(), nodeBreakerView);

        });
    }

    @CEntryPoint(name = "getNodeBreakerViewInternalConnections")
    public static ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getNodeBreakerViewInternalConnections(IsolateThread thread, ObjectHandle networkHandle, CCharPointer voltageLevel, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            VoltageLevel.NodeBreakerView nodeBreakerView = network.getVoltageLevel(CTypeUtil.toString(voltageLevel)).getNodeBreakerView();
            return Dataframes.createCDataframe(Dataframes.nodeBreakerViewInternalConnection(), nodeBreakerView);
        });
    }

    @CEntryPoint(name = "getBusBreakerViewSwitches")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getBusBreakerViewSwitches(IsolateThread thread, ObjectHandle networkHandle, CCharPointer voltageLevel, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            VoltageLevel.BusBreakerView busBreakerView = network.getVoltageLevel(CTypeUtil.toString(voltageLevel)).getBusBreakerView();
            return Dataframes.createCDataframe(Dataframes.busBreakerViewSwitches(), busBreakerView);
        });
    }

    @CEntryPoint(name = "getBusBreakerViewBuses")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getBusBreakerViewBuses(IsolateThread thread, ObjectHandle networkHandle, CCharPointer voltageLevel, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            VoltageLevel voltageLevel1 = network.getVoltageLevel(CTypeUtil.toString(voltageLevel));
            return Dataframes.createCDataframe(Dataframes.busBreakerViewBuses(), voltageLevel1);
        });
    }

    @CEntryPoint(name = "getBusBreakerViewElements")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getBusBreakerViewElements(IsolateThread thread, ObjectHandle networkHandle, CCharPointer voltageLevel, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            VoltageLevel voltageLevel1 = network.getVoltageLevel(CTypeUtil.toString(voltageLevel));
            return Dataframes.createCDataframe(Dataframes.busBreakerViewElements(), voltageLevel1);
        });
    }

    @CEntryPoint(name = "merge")
    public static void merge(IsolateThread thread, ObjectHandle networkHandle, VoidPointerPointer othersHandle, int othersCount,
                             ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            Network[] otherNetworks = new Network[othersCount];
            for (int i = 0; i < othersCount; ++i) {
                ObjectHandle handleToMerge = othersHandle.read(i);
                Network otherNetwork = ObjectHandles.getGlobal().get(handleToMerge);
                otherNetworks[i] = otherNetwork;
            }
            network.merge(otherNetworks);
        });
    }

    @CEntryPoint(name = "getSeriesMetadata")
    public static DataframeMetadataPointer getSeriesMetadata(IsolateThread thread, ElementType elementType,
                                                             ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            DataframeElementType type = convert(elementType);
            List<SeriesMetadata> seriesMetadata = NetworkDataframes.getDataframeMapper(type).getSeriesMetadata();
            return createSeriesMetadata(seriesMetadata);
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
                createSeriesMetadata(dataframeMetadata, dataframeMetadataArray.addressOf(i));
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
                        Identifiable identifiable = network.getIdentifiable(id);
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
            ids.forEach(id -> properties.forEach(property -> network.getIdentifiable(id).removeProperty(property)));
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

    private static DataframeMetadataPointer createSeriesMetadata(List<SeriesMetadata> metadata) {
        DataframeMetadataPointer res = UnmanagedMemory.calloc(SizeOf.get(DataframeMetadataPointer.class));
        createSeriesMetadata(metadata, res);
        return res;
    }

    @CEntryPoint(name = "updateNetworkElementsExtensionsWithSeries")
    public static void updateNetworkElementsExtensionsWithSeries(IsolateThread thread, ObjectHandle networkHandle, CCharPointer namePtr,
                                                                 DataframePointer dataframe,
                                                                 PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            String name = CTypeUtil.toString(namePtr);
            NetworkDataframeMapper mapper = NetworkDataframes.getExtensionDataframeMapper(name);
            if (mapper != null) {
                Network network = ObjectHandles.getGlobal().get(networkHandle);
                UpdatingDataframe updatingDataframe = createDataframe(dataframe);
                mapper.updateSeries(network, updatingDataframe);
            } else {
                throw new PowsyblException("extension " + name + " not found");
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
    public static DataframeMetadataPointer getExtensionSeriesMetadata(IsolateThread thread, CCharPointer namePtr,
                                                                      ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            String name = CTypeUtil.toString(namePtr);
            NetworkDataframeMapper mapper = NetworkDataframes.getExtensionDataframeMapper(name);
            if (mapper != null) {
                List<SeriesMetadata> seriesMetadata = mapper.getSeriesMetadata();
                return createSeriesMetadata(seriesMetadata);
            } else {
                throw new PowsyblException("extension " + name + " not found");
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
                createSeriesMetadata(dataframeMetadata, dataframeMetadataArray.addressOf(i));
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

    public static class LayoutParametersExt {
        public final LayoutParameters layoutParameters;
        public final boolean topologicalColoring;

        public LayoutParametersExt() {
            this(new LayoutParameters(), true);
        }

        public LayoutParametersExt(LayoutParameters layoutParameters, boolean topologicalColoring) {
            Objects.requireNonNull(layoutParameters);
            this.layoutParameters = layoutParameters;
            this.layoutParameters.setSvgWidthAndHeightAdded(true);
            this.topologicalColoring = topologicalColoring;
        }
    }

    public static void copyToCLayoutParameters(LayoutParametersExt parameters, LayoutParametersPointer cParameters) {
        cParameters.setUseName(parameters.layoutParameters.isUseName());
        cParameters.setCenterName(parameters.layoutParameters.isLabelCentered());
        cParameters.setDiagonalLabel(parameters.layoutParameters.isLabelDiagonal());
        cParameters.setTopologicalColoring(parameters.topologicalColoring);
    }

    public static LayoutParametersPointer convertToLayoutParametersPointer(LayoutParametersExt parameters) {
        LayoutParametersPointer paramsPtr = UnmanagedMemory.calloc(SizeOf.get(LayoutParametersPointer.class));
        copyToCLayoutParameters(parameters, paramsPtr);
        return paramsPtr;
    }

    @CEntryPoint(name = "createLayoutParameters")
    public static LayoutParametersPointer createLayoutParameters(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> convertToLayoutParametersPointer(new LayoutParametersExt()));
    }

    public static void freeLayoutParametersPointer(LayoutParametersPointer layoutParametersPtr) {
        UnmanagedMemory.free(layoutParametersPtr);
    }

    @CEntryPoint(name = "freeLayoutParameters")
    public static void freeLayoutParameters(IsolateThread thread, LayoutParametersPointer layoutParametersPtr,
                                              PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            freeLayoutParametersPointer(layoutParametersPtr);
        });
    }

    public static LayoutParametersExt convertLayoutParameters(LayoutParametersPointer layoutParametersPtr) {
        return new LayoutParametersExt(new LayoutParameters()
                .setUseName(layoutParametersPtr.isUseName())
                .setLabelCentered(layoutParametersPtr.isCenterName())
                .setLabelDiagonal(layoutParametersPtr.isDiagonalLabel()),
                layoutParametersPtr.isTopologicalColoring());
    }

    @CEntryPoint(name = "writeSingleLineDiagramSvg")
    public static void writeSingleLineDiagramSvg(IsolateThread thread, ObjectHandle networkHandle, CCharPointer containerId,
                                                 CCharPointer svgFile, CCharPointer metadataFile, LayoutParametersPointer layoutParametersPtr,
                                                 ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String containerIdStr = CTypeUtil.toString(containerId);
            String svgFileStr = CTypeUtil.toString(svgFile);
            String metadataFileStr = metadataFile.isNonNull() ? CTypeUtil.toString(metadataFile) : null;
            LayoutParametersExt layoutParametersExt = convertLayoutParameters(layoutParametersPtr);
            SingleLineDiagramUtil.writeSvg(network, containerIdStr, svgFileStr, metadataFileStr, layoutParametersExt);
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
                                                                                        LayoutParametersPointer layoutParametersPtr, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String containerIdStr = CTypeUtil.toString(containerId);
            LayoutParametersExt layoutParametersExt = convertLayoutParameters(layoutParametersPtr);
            List<String> svgAndMeta = SingleLineDiagramUtil.getSvgAndMetadata(network, containerIdStr, layoutParametersExt);
            return createCharPtrArray(svgAndMeta);
        });
    }

    @CEntryPoint(name = "writeNetworkAreaDiagramSvg")
    public static void writeNetworkAreaDiagramSvg(IsolateThread thread, ObjectHandle networkHandle, CCharPointer svgFile,
                                                  CCharPointerPointer voltageLevelIdsPointer, int voltageLevelIdCount, int depth, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String svgFileStr = CTypeUtil.toString(svgFile);
            List<String> voltageLevelIds = toStringList(voltageLevelIdsPointer, voltageLevelIdCount);
            NetworkAreaDiagramUtil.writeSvg(network, voltageLevelIds, depth, svgFileStr);
        });
    }

    @CEntryPoint(name = "getNetworkAreaDiagramSvg")
    public static CCharPointer getNetworkAreaDiagramSvg(IsolateThread thread, ObjectHandle networkHandle, CCharPointerPointer voltageLevelIdsPointer,
                                                        int voltageLevelIdCount, int depth, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            List<String> voltageLevelIds = toStringList(voltageLevelIdsPointer, voltageLevelIdCount);
            String svg = NetworkAreaDiagramUtil.getSvg(network, voltageLevelIds, depth);
            return CTypeUtil.toCharPtr(svg);
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

    @CEntryPoint(name = "getTwtFeederBaysMetadata")
    public static DataframeMetadataPointer getTwtFeederBaysMetadata(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            List<SeriesMetadata> seriesMetadata = FeederBaysTwtSeries.getSeriesMetadata();
            return createSeriesMetadata(seriesMetadata);
        });
    }

    @CEntryPoint(name = "getLineFeederBaysMetadata")
    public static DataframeMetadataPointer getLineFeederBaysMetadata(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            List<SeriesMetadata> seriesMetadata = FeederBaysLineSeries.getSeriesMetadata();
            return createSeriesMetadata(seriesMetadata);
        });
    }

}
