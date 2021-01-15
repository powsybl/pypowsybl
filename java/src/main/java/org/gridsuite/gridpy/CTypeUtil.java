/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.gridpy;

import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
final class CTypeUtil {

    private CTypeUtil() {
    }

    static List<String> createStringList(CCharPointerPointer charPtrPtr, int length) {
        List<String> stringList = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            CCharPointer charPtr = charPtrPtr.read(i);
            String str = CTypeConversion.toJavaString(charPtr);
            stringList.add(str);
        }
        return stringList;
    }
}
