/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.commons;

import com.oracle.svm.core.c.ProjectHeaderFile;
import org.graalvm.nativeimage.c.CContext;

import java.util.Collections;
import java.util.List;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public class Directives implements CContext.Directives {

    @Override
    public List<String> getHeaderFiles() {
        return Collections.singletonList(ProjectHeaderFile.resolve("com.powsybl.python", "powsybl-api.h"));
    }
}
