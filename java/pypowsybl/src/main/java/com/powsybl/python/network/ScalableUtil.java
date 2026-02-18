/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.network;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.commons.PyPowsyblApiHeader;

/**
 * Utility methods to manage Scalable in python
 *
 * @author Hugo Kulesza {@literal <hugo.kulesza@rte-france.com>}
 */
public class ScalableUtil {

    protected static Scalable convertScalablePointerToScalable(PyPowsyblApiHeader.ScalablePointer scalablePtr) {
        Scalable scalable = null;
        PyPowsyblApiHeader.ScalableType scalableType = scalablePtr.getScalableType();
        switch (scalableType) {
            case ELEMENT:
                if (scalablePtr.getInjectionId() == null) {
                    throw new PowsyblException("Injection id not found for ELEMENT type scalable.");
                }
                double minValue = scalablePtr.getMinValue();
                double maxValue = scalablePtr.getMaxValue();
                String injectionId = CTypeUtil.toString(scalablePtr.getInjectionId());
                scalable = Scalable.scalable(injectionId, minValue, maxValue);
                break;
            case STACK:
                scalable = convertStackScalablePointerToScalable(scalablePtr);
                break;
            default:
                throw new PowsyblException("Scalable type not supported: " + scalableType);
        }
        return scalable;
    }

    private static Scalable convertStackScalablePointerToScalable(PyPowsyblApiHeader.ScalablePointer scalablePtr) {
        if (scalablePtr.getChildren() == null) {
            throw new PowsyblException("Scalable children not found for STACK type scalable.");
        }
        double minValue = scalablePtr.getMinValue();
        double maxValue = scalablePtr.getMaxValue();
        PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.ScalablePointer> children = scalablePtr.getChildren();
        Scalable[] childrenArray = new Scalable[children.getLength()];
        for (int i = 0; i < children.getLength(); i++) {
            PyPowsyblApiHeader.ScalablePointer childPtr = (PyPowsyblApiHeader.ScalablePointer) children.addressOf(i).getPtr();
            if (childPtr == null) {
                throw new PowsyblException("Child pointer cannot be null in STACK type scalable.");
            }
            Scalable childScalable = convertScalablePointerToScalable(childPtr);
            childrenArray[i] = (childScalable);
        }
        return Scalable.stack(minValue, maxValue, childrenArray);
    }
}
