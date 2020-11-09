/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
#include "libgridpy-java.h"
#include <iostream>

namespace gridpy {

void printVersion() {
    graal_isolate_t* isolate = nullptr;
    graal_isolatethread_t* thread = nullptr;

    if (graal_create_isolate(nullptr, &isolate, &thread) != 0) {
        throw std::runtime_error("graal_create_isolate error");
    }

    printVersion(thread);

    if (graal_detach_thread(thread) != 0) {
        throw std::runtime_error("graal_detach_thread error");
    }
}

}
