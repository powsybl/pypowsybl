package com.powsybl.python;

import com.google.common.collect.Iterables;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.dataframe.DoubleIndexedSeries;
import com.powsybl.dataframe.IndexedSeries;
import com.powsybl.dataframe.IntIndexedSeries;
import com.powsybl.dataframe.network.NetworkDataframeMapper;
import com.powsybl.dataframe.network.NetworkDataframes;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.export.Exporters;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.BatteryNetworkFactory;
import com.powsybl.iidm.network.test.DanglingLineNetworkFactory;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.iidm.network.test.FourSubstationsNodeBreakerFactory;
import com.powsybl.iidm.reducer.*;
import org.apache.commons.io.IOUtils;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CDoublePointer;
import org.graalvm.nativeimage.c.type.CIntPointer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;

import static com.powsybl.python.CTypeUtil.toStringList;
import static com.powsybl.python.Util.*;

/**
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 */
@CContext(Directives.class)
public final class PyPowsyblNetworkApiLib {

    private PyPowsyblNetworkApiLib() {
    }

    @CEntryPoint(name = "createEmptyNetwork")
    public static ObjectHandle createEmptyNetwork(IsolateThread thread, CCharPointer id, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            String idStr = CTypeUtil.toString(id);
            Network network = Network.create(idStr, "");
            return ObjectHandles.getGlobal().create(network);
        });
    }

    @CEntryPoint(name = "createIeeeNetwork")
    public static ObjectHandle createIeeeNetwork(IsolateThread thread, int busCount, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network;
            switch (busCount) {
                case 9:
                    network = IeeeCdfNetworkFactory.create9();
                    break;
                case 14:
                    network = IeeeCdfNetworkFactory.create14();
                    break;
                case 30:
                    network = IeeeCdfNetworkFactory.create30();
                    break;
                case 57:
                    network = IeeeCdfNetworkFactory.create57();
                    break;
                case 118:
                    network = IeeeCdfNetworkFactory.create118();
                    break;
                case 300:
                    network = IeeeCdfNetworkFactory.create300();
                    break;
                default:
                    throw new PowsyblException("IEEE " + busCount + " buses case not found");
            }
            return ObjectHandles.getGlobal().create(network);
        });
    }

    @CEntryPoint(name = "createEurostagTutorialExample1Network")
    public static ObjectHandle createEurostagTutorialExample1Network(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = EurostagTutorialExample1Factory.createWithFixedCurrentLimits();
            return ObjectHandles.getGlobal().create(network);
        });
    }

    @CEntryPoint(name = "createFourSubstationsNodeBreakerNetwork")
    public static ObjectHandle createFourSubstationsNodeBreakerNetwork(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = FourSubstationsNodeBreakerFactory.create();
            return ObjectHandles.getGlobal().create(network);
        });
    }

    @CEntryPoint(name = "createBatteryNetwork")
    public static ObjectHandle createBatteryNetwork(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = BatteryNetworkFactory.create();
            return ObjectHandles.getGlobal().create(network);
        });
    }

    @CEntryPoint(name = "createDanglingLineNetwork")
    public static ObjectHandle createDanglingLineNetwork(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = DanglingLineNetworkFactory.create();
            return ObjectHandles.getGlobal().create(network);
        });
    }

    @CEntryPoint(name = "getMainAttributes")
    public static PyPowsyblApiHeader.MainAttributesPointer getMainAttributes(IsolateThread thread, ObjectHandle networkHandle,
                                                 PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            return PyPowsyblApiHeader.allocMainAttributePointer(NetworkUtil.mainAttributesMap(network));
        });
    }

    @CEntryPoint(name = "freeMainAttributes")
    public static void freeMainAttributes(IsolateThread thread, PyPowsyblApiHeader.MainAttributesPointer ptr,
                                          PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> PyPowsyblApiHeader.freeMainAttributePointer(ptr));
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
                                                                                                                     PyPowsyblApiHeader.ElementType elementType, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            NetworkDataframeMapper mapper = NetworkDataframes.getDataframeMapper(convert(elementType));
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            return Dataframes.createCDataframe(mapper, network);
        });
    }

    @CEntryPoint(name = "updateNetworkElementsWithIntSeries")
    public static void updateNetworkElementsWithIntSeries(IsolateThread thread, ObjectHandle networkHandle,
                                                          PyPowsyblApiHeader.ElementType elementType, CCharPointer seriesNamePtr,
                                                          CCharPointerPointer elementIdPtrPtr, CIntPointer valuePtr,
                                                          int elementCount, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String seriesName = CTypeUtil.toString(seriesNamePtr);
            NetworkDataframes.getDataframeMapper(convert(elementType))
                .updateIntSeries(network, seriesName, createIntSeries(elementIdPtrPtr, valuePtr, elementCount));
        });
    }

    private static IntIndexedSeries createIntSeries(CCharPointerPointer elementIdPtrPtr, CIntPointer valuePtr, int elementCount) {
        return new IntIndexedSeries() {
            @Override
            public int getSize() {
                return elementCount;
            }

            @Override
            public String getId(int index) {
                return CTypeUtil.toString(elementIdPtrPtr.read(index));
            }

            @Override
            public int getValue(int index) {
                return valuePtr.read(index);
            }
        };
    }

    @CEntryPoint(name = "updateNetworkElementsWithDoubleSeries")
    public static void updateNetworkElementsWithDoubleSeries(IsolateThread thread, ObjectHandle networkHandle,
                                                             PyPowsyblApiHeader.ElementType elementType, CCharPointer seriesNamePtr,
                                                             CCharPointerPointer elementIdPtrPtr, CDoublePointer valuePtr,
                                                             int elementCount, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String seriesName = CTypeUtil.toString(seriesNamePtr);
            NetworkDataframes.getDataframeMapper(convert(elementType))
                .updateDoubleSeries(network, seriesName, createDoubleSeries(elementIdPtrPtr, valuePtr, elementCount));
        });
    }

    private static DoubleIndexedSeries createDoubleSeries(CCharPointerPointer elementIdPtrPtr, CDoublePointer valuePtr, int elementCount) {
        return new DoubleIndexedSeries() {
            @Override
            public int getSize() {
                return elementCount;
            }

            @Override
            public String getId(int index) {
                return CTypeUtil.toString(elementIdPtrPtr.read(index));
            }

            @Override
            public double getValue(int index) {
                return valuePtr.read(index);
            }
        };
    }

    @CEntryPoint(name = "updateNetworkElementsWithStringSeries")
    public static void updateNetworkElementsWithStringSeries(IsolateThread thread, ObjectHandle networkHandle,
                                                             PyPowsyblApiHeader.ElementType elementType, CCharPointer seriesNamePtr,
                                                             CCharPointerPointer elementIdPtrPtr, CCharPointerPointer valuePtr,
                                                             int elementCount, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String seriesName = CTypeUtil.toString(seriesNamePtr);
            NetworkDataframes.getDataframeMapper(convert(elementType))
                .updateStringSeries(network, seriesName, createStringSeries(elementIdPtrPtr, valuePtr, elementCount));
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
}
