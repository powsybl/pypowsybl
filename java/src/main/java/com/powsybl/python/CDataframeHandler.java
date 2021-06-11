package com.powsybl.python;

import com.powsybl.python.PyPowsyblApiHeader.ArrayPointer;
import com.powsybl.python.PyPowsyblApiHeader.SeriesPointer;
import com.powsybl.dataframe.DataframeHandler;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CDoublePointer;
import org.graalvm.nativeimage.c.type.CIntPointer;
import org.graalvm.word.PointerBase;
import org.graalvm.word.WordFactory;

/**
 * Writes dataframe to C structures.
 *
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public class CDataframeHandler implements DataframeHandler {

    public static final int STRING_SERIES_TYPE = 0;
    public static final int DOUBLE_SERIES_TYPE = 1;
    public static final int INT_SERIES_TYPE = 2;
    public static final int BOOLEAN_SERIES_TYPE = 3;

    private ArrayPointer<SeriesPointer> dataframePtr;
    private int currentIndex;

    public CDataframeHandler() {
        this.dataframePtr = WordFactory.nullPointer();
        this.currentIndex = 0;
    }

    public ArrayPointer<SeriesPointer> getDataframePtr() {
        return dataframePtr;
    }

    @Override
    public void allocate(int seriesCount) {
        SeriesPointer seriesPtr = UnmanagedMemory.calloc(seriesCount * SizeOf.get(SeriesPointer.class));
        dataframePtr = PyPowsyblApiHeader.allocArrayPointer(seriesPtr, seriesCount);
    }

    @Override
    public StringSeriesWriter newStringIndex(String name, int size) {
        CCharPointerPointer dataPtr = UnmanagedMemory.calloc(size * SizeOf.get(CCharPointerPointer.class));
        addIndex(name, size, dataPtr, STRING_SERIES_TYPE);
        return (i, v) -> dataPtr.addressOf(i).write(CTypeUtil.toCharPtr(v));
    }

    @Override
    public IntSeriesWriter newIntIndex(String name, int size) {
        CIntPointer dataPtr = UnmanagedMemory.calloc(size * SizeOf.get(CIntPointer.class));
        addIndex(name, size, dataPtr, INT_SERIES_TYPE);
        return (i, v) -> dataPtr.addressOf(i).write(v);
    }

    @Override
    public StringSeriesWriter newStringSeries(String name, int size) {
        CCharPointerPointer dataPtr = UnmanagedMemory.calloc(size * SizeOf.get(CCharPointerPointer.class));
        addSeries(name, size, dataPtr, STRING_SERIES_TYPE);
        return (i, v) -> dataPtr.addressOf(i).write(CTypeUtil.toCharPtr(v));
    }

    @Override
    public IntSeriesWriter newIntSeries(String name, int size) {
        CIntPointer dataPtr = UnmanagedMemory.calloc(size * SizeOf.get(CIntPointer.class));
        addSeries(name, size, dataPtr, INT_SERIES_TYPE);
        return (i, v) -> dataPtr.addressOf(i).write(v);
    }

    @Override
    public BooleanSeriesWriter newBooleanSeries(String name, int size) {
        CCharPointer dataPtr = UnmanagedMemory.calloc(size * SizeOf.get(CCharPointer.class));
        addSeries(name, size, dataPtr, BOOLEAN_SERIES_TYPE);
        return (i, v) -> dataPtr.addressOf(i).write(v ? (byte) 1 : 0);
    }

    @Override
    public DoubleSeriesWriter newDoubleSeries(String name, int size) {
        CDoublePointer dataPtr = UnmanagedMemory.calloc(size * SizeOf.get(CDoublePointer.class));
        addSeries(name, size, dataPtr, DOUBLE_SERIES_TYPE);
        return (i, v) -> dataPtr.addressOf(i).write(v);
    }

    private void addSeries(String name, int count, PointerBase dataPtr, int type) {
        addSeries(false, name, count, dataPtr, type);
    }

    private void addIndex(String name, int count, PointerBase dataPtr, int type) {
        addSeries(true, name, count, dataPtr, type);
    }

    private void addSeries(boolean index, String name, int count, PointerBase dataPtr, int type) {
        SeriesPointer seriesPtrI = dataframePtr.getPtr().addressOf(currentIndex);
        seriesPtrI.setName(CTypeUtil.toCharPtr(name));
        seriesPtrI.setIndex(index);
        seriesPtrI.setType(type);
        seriesPtrI.data().setLength(count);
        seriesPtrI.data().setPtr(dataPtr);
        currentIndex++;
    }
}
