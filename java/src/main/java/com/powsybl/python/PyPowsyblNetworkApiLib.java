package com.powsybl.python;

import com.google.common.collect.Iterables;
import com.powsybl.cgmes.conformity.test.CgmesConformity1Catalog;
import com.powsybl.cgmes.model.test.TestGridModelResources;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.dataframe.DataframeFilter;
import com.powsybl.dataframe.DataframeFilter.AttributeFilterType;
import com.powsybl.dataframe.IndexedSeries;
import com.powsybl.dataframe.SeriesDataType;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.NetworkDataframeMapper;
import com.powsybl.dataframe.network.NetworkDataframes;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.export.Exporters;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.impl.NetworkFactoryImpl;
import com.powsybl.iidm.network.test.*;
import com.powsybl.iidm.reducer.*;
import com.powsybl.python.update.CUpdatingDataframe;
import com.powsybl.python.update.DoubleSeries;
import com.powsybl.python.update.IntSeries;
import com.powsybl.python.update.StringSeries;
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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;

import static com.powsybl.python.CDataframeHandler.*;
import static com.powsybl.python.CTypeUtil.toStringList;
import static com.powsybl.python.PyPowsyblApiHeader.ExceptionHandlerPointer;
import static com.powsybl.python.PyPowsyblApiHeader.VoidPointerPointer;
import static com.powsybl.python.Util.*;

/**
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 */
@CContext(Directives.class)
public final class PyPowsyblNetworkApiLib {

    private PyPowsyblNetworkApiLib() {
    }

    private static Network importCgmes(TestGridModelResources modelResources) {
        return Importers.getImporter("CGMES")
                .importData(modelResources.dataSource(), new NetworkFactoryImpl(), null);
    }

    @CEntryPoint(name = "createNetwork")
    public static ObjectHandle createNetwork(IsolateThread thread, CCharPointer name, CCharPointer id, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        String networkName = CTypeUtil.toString(name);
        return doCatch(exceptionHandlerPtr, () -> {
            Network network;
            switch (networkName) {
                case "four_substations_node_breaker":
                    network = FourSubstationsNodeBreakerFactory.create();
                    break;
                case "eurostag_tutorial_example1":
                    network = NetworkUtil.createEurostagTutorialExample1WithFixedCurrentLimits();
                    break;
                case "batteries":
                    network = BatteryNetworkFactory.create();
                    break;
                case "dangling_lines":
                    network = DanglingLineNetworkFactory.create();
                    break;
                case "three_windings_transformer":
                    network = ThreeWindingsTransformerNetworkFactory.create();
                    break;
                case "shunt":
                    network = ShuntTestCaseFactory.create();
                    break;
                case "non_linear_shunt":
                    network = ShuntTestCaseFactory.createNonLinear();
                    break;
                case "ieee9":
                    network = IeeeCdfNetworkFactory.create9();
                    break;
                case "ieee14":
                    network = IeeeCdfNetworkFactory.create14();
                    break;
                case "ieee30":
                    network = IeeeCdfNetworkFactory.create30();
                    break;
                case "ieee57":
                    network = IeeeCdfNetworkFactory.create57();
                    break;
                case "ieee118":
                    network = IeeeCdfNetworkFactory.create118();
                    break;
                case "ieee300":
                    network = IeeeCdfNetworkFactory.create300();
                    break;
                case "empty":
                    String networkId = CTypeUtil.toString(id);
                    network = Network.create(networkId, "");
                    break;
                case "micro_grid_be":
                    network = importCgmes(CgmesConformity1Catalog.microGridBaseCaseBE());
                    break;
                case "micro_grid_nl":
                    network = importCgmes(CgmesConformity1Catalog.microGridBaseCaseNL());
                    break;
                default:
                    throw new PowsyblException("network " + networkName + " not found");
            }
            return ObjectHandles.getGlobal().create(network);
        });
    }

    @CEntryPoint(name = "getNetworkMetadata")
    public static PyPowsyblApiHeader.NetworkMetadataPointer getNetworkMetadata(IsolateThread thread, ObjectHandle networkHandle,
                                                                               PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            return createNetworkMetadata(network);
        });
    }

    @CEntryPoint(name = "freeNetworkMetadata")
    public static void freeNetworkMetadata(IsolateThread thread, PyPowsyblApiHeader.NetworkMetadataPointer ptr,
                                           PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> freeNetworkMetadata(ptr));
    }

    private static PyPowsyblApiHeader.NetworkMetadataPointer createNetworkMetadata(Network network) {
        PyPowsyblApiHeader.NetworkMetadataPointer ptr = UnmanagedMemory.calloc(SizeOf.get(PyPowsyblApiHeader.NetworkMetadataPointer.class));
        ptr.setId(CTypeUtil.toCharPtr(network.getId()));
        ptr.setName(CTypeUtil.toCharPtr(network.getNameOrId()));
        ptr.setSourceFormat(CTypeUtil.toCharPtr(network.getSourceFormat()));
        ptr.setForecastDistance(network.getForecastDistance());
        ptr.setCaseDate(network.getCaseDate().getMillis() / 1000.0d);
        return ptr;
    }

    private static void freeNetworkMetadata(PyPowsyblApiHeader.NetworkMetadataPointer networkMetadataPointer) {
        UnmanagedMemory.free(networkMetadataPointer.getId());
        UnmanagedMemory.free(networkMetadataPointer.getName());
        UnmanagedMemory.free(networkMetadataPointer.getSourceFormat());
        UnmanagedMemory.free(networkMetadataPointer);
    }

    @CEntryPoint(name = "loadNetwork")
    public static ObjectHandle loadNetwork(IsolateThread thread, CCharPointer file, CCharPointerPointer parameterNamesPtrPtr, int parameterNamesCount,
                                           CCharPointerPointer parameterValuesPtrPtr, int parameterValuesCount, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            String fileStr = CTypeUtil.toString(file);
            Properties parameters = createParameters(parameterNamesPtrPtr, parameterNamesCount, parameterValuesPtrPtr, parameterValuesCount);
            Network network = Importers.loadNetwork(Paths.get(fileStr), LocalComputationManager.getDefault(), ImportConfig.load(), parameters);
            return ObjectHandles.getGlobal().create(network);
        });
    }

    @CEntryPoint(name = "loadNetworkFromString")
    public static ObjectHandle loadNetworkFromString(IsolateThread thread, CCharPointer fileName, CCharPointer fileContent,
                                                     CCharPointerPointer parameterNamesPtrPtr, int parameterNamesCount,
                                                     CCharPointerPointer parameterValuesPtrPtr, int parameterValuesCount,
                                                     PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            String fileNameStr = CTypeUtil.toString(fileName);
            String fileContentStr = CTypeUtil.toString(fileContent);
            Properties parameters = createParameters(parameterNamesPtrPtr, parameterNamesCount, parameterValuesPtrPtr, parameterValuesCount);
            try (InputStream is = new ByteArrayInputStream(fileContentStr.getBytes(StandardCharsets.UTF_8))) {
                Network network = Importers.loadNetwork(fileNameStr, is, LocalComputationManager.getDefault(), ImportConfig.load(), parameters);
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
                                   PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String fileStr = CTypeUtil.toString(file);
            String formatStr = CTypeUtil.toString(format);
            Properties parameters = createParameters(parameterNamesPtrPtr, parameterNamesCount, parameterValuesPtrPtr, parameterValuesCount);
            Exporters.export(formatStr, network, parameters, Paths.get(fileStr));
        });
    }

    @CEntryPoint(name = "dumpNetworkToString")
    public static CCharPointer dumpNetworkToString(IsolateThread thread, ObjectHandle networkHandle, CCharPointer format,
                                                   CCharPointerPointer parameterNamesPtrPtr, int parameterNamesCount,
                                                   CCharPointerPointer parameterValuesPtrPtr, int parameterValuesCount,
                                                   PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String formatStr = CTypeUtil.toString(format);
            Properties parameters = createParameters(parameterNamesPtrPtr, parameterNamesCount, parameterValuesPtrPtr, parameterValuesCount);
            MemDataSource dataSource = new MemDataSource();
            var exporter = Exporters.getExporter(formatStr);
            if (exporter == null) {
                throw new PowsyblException("No expoxter found for '" + formatStr + "' to export as a string");
            }
            exporter.export(network, parameters, dataSource);
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
                                     PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
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
    public static PyPowsyblApiHeader.ArrayPointer<CCharPointerPointer> getNetworkElementsIds(IsolateThread thread, ObjectHandle networkHandle, PyPowsyblApiHeader.ElementType elementType,
                                                                                             CDoublePointer nominalVoltagePtr, int nominalVoltageCount,
                                                                                             CCharPointerPointer countryPtr, int countryCount, boolean mainCc, boolean mainSc,
                                                                                             boolean notConnectedToSameBusAtBothSides, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            Set<Double> nominalVoltages = new HashSet<>(CTypeUtil.toDoubleList(nominalVoltagePtr, nominalVoltageCount));
            Set<String> countries = new HashSet<>(toStringList(countryPtr, countryCount));
            List<String> elementsIds = NetworkUtil.getElementsIds(network, elementType, nominalVoltages, countries, mainCc, mainSc, notConnectedToSameBusAtBothSides);
            return createCharPtrArray(elementsIds);
        });
    }

    @CEntryPoint(name = "cloneVariant")
    public static void cloneVariant(IsolateThread thread, ObjectHandle networkHandle, CCharPointer src, CCharPointer variant, boolean mayOverwrite, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            network.getVariantManager().cloneVariant(CTypeUtil.toString(src), CTypeUtil.toString(variant), mayOverwrite);
        });
    }

    @CEntryPoint(name = "setWorkingVariant")
    public static void setWorkingVariant(IsolateThread thread, ObjectHandle networkHandle, CCharPointer variant, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            network.getVariantManager().setWorkingVariant(CTypeUtil.toString(variant));
        });
    }

    @CEntryPoint(name = "removeVariant")
    public static void removeVariant(IsolateThread thread, ObjectHandle networkHandle, CCharPointer variant, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            network.getVariantManager().removeVariant(CTypeUtil.toString(variant));
        });
    }

    @CEntryPoint(name = "getVariantsIds")
    public static PyPowsyblApiHeader.ArrayPointer<CCharPointerPointer> getVariantsIds(IsolateThread thread, ObjectHandle networkHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            return createCharPtrArray(List.copyOf(network.getVariantManager().getVariantIds()));
        });
    }

    @CEntryPoint(name = "createNetworkElementsSeriesArray")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> createNetworkElementsSeriesArray(IsolateThread thread, ObjectHandle networkHandle,
                                                                                                                     PyPowsyblApiHeader.ElementType elementType,
                                                                                                                     PyPowsyblApiHeader.FilterAttributesType filterAttributesType,
                                                                                                                     CCharPointerPointer attributesPtrPtr, int attributesCount,
                                                                                                                     PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            NetworkDataframeMapper mapper = NetworkDataframes.getDataframeMapper(convert(elementType));
            Network network = ObjectHandles.getGlobal().get(networkHandle);
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
            return Dataframes.createCDataframe(mapper, network, new DataframeFilter(filterType, attributes));
        });
    }

    @CEntryPoint(name = "updateNetworkElementsWithSeries")
    public static void updateNetworkElementsWithSeries(IsolateThread thread, ObjectHandle networkHandle, PyPowsyblApiHeader.ElementType elementType,
                                                       PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> dataframe,
                                                       PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            UpdatingDataframe updatingDataframe = createDataframe(dataframe);
            NetworkDataframes.getDataframeMapper(convert(elementType)).updateSeries(network, updatingDataframe);
        });
    }

    public static UpdatingDataframe createDataframe(PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> dataframe) {
        int elementCount = dataframe.getPtr().addressOf(0).data().getLength();
        int columnsNumber = dataframe.getLength();
        CUpdatingDataframe updatingDataframe = new CUpdatingDataframe(elementCount);
        for (int i = 0; i < columnsNumber; i++) {
            PyPowsyblApiHeader.SeriesPointer seriesPointer = dataframe.getPtr().addressOf(i);
            String name = CTypeUtil.toString(seriesPointer.getName());
            switch (seriesPointer.getType()) {
                case STRING_SERIES_TYPE:
                    updatingDataframe.addSeries(new StringSeries(name, elementCount,
                                    (CCharPointerPointer) seriesPointer.data().getPtr()),
                            new SeriesMetadata(seriesPointer.isIndex(), name, false, SeriesDataType.STRING, true));
                    break;
                case DOUBLE_SERIES_TYPE:
                    updatingDataframe.addSeries(new DoubleSeries(name, elementCount,
                                    (CDoublePointer) seriesPointer.data().getPtr()),
                            new SeriesMetadata(seriesPointer.isIndex(), name, false, SeriesDataType.DOUBLE, true));
                    break;
                case INT_SERIES_TYPE:
                case BOOLEAN_SERIES_TYPE:
                    updatingDataframe.addSeries(new IntSeries(name, elementCount,
                                    (CIntPointer) seriesPointer.data().getPtr()),
                            new SeriesMetadata(seriesPointer.isIndex(), name, false, SeriesDataType.INT, true));
                    break;
            }

        }
        return updatingDataframe;
    }

    @CEntryPoint(name = "getNodeBreakerViewSwitches")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getNodeBreakerViewSwitches(IsolateThread thread, ObjectHandle networkHandle, CCharPointer voltageLevel, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            VoltageLevel.NodeBreakerView nodeBreakerView = network.getVoltageLevel(CTypeUtil.toString(voltageLevel)).getNodeBreakerView();
            return Dataframes.createCDataframe(Dataframes.nodeBreakerViewSwitches(), nodeBreakerView);
        });
    }

    @CEntryPoint(name = "getNodeBreakerViewNodes")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getNodeBreakerViewNodes(IsolateThread thread, ObjectHandle networkHandle, CCharPointer voltageLevel, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            VoltageLevel.NodeBreakerView nodeBreakerView = network.getVoltageLevel(CTypeUtil.toString(voltageLevel)).getNodeBreakerView();

            return Dataframes.createCDataframe(Dataframes.nodeBreakerViewNodes(), nodeBreakerView);

        });
    }

    @CEntryPoint(name = "getNodeBreakerViewInternalConnections")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getNodeBreakerViewInternalConnections(IsolateThread thread, ObjectHandle networkHandle, CCharPointer voltageLevel, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            VoltageLevel.NodeBreakerView nodeBreakerView = network.getVoltageLevel(CTypeUtil.toString(voltageLevel)).getNodeBreakerView();
            return Dataframes.createCDataframe(Dataframes.nodeBreakerViewInternalConnection(), nodeBreakerView);
        });
    }

    private static IndexedSeries<String> createStringSeries(CCharPointerPointer elementIdPtrPtr, CCharPointerPointer valuePtr, int elementCount) {
        return new IndexedSeries<>() {
            @Override
            public int getSize() {
                return elementCount;
            }

            @Override
            public String getId(int index) {
                return CTypeUtil.toString(elementIdPtrPtr.read(index));
            }

            @Override
            public String getValue(int index) {
                return CTypeUtil.toString(valuePtr.read(index));
            }
        };
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
}
