/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.datasource;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.awaitility.Awaitility;

/**
 * @author Olivier Perrin {@literal <olivier.perrin at rte-france.com>}
 */
class InMemoryZipFileDataSourceTest {

    @Test
    void polynomialRegexTest() throws IOException {
        String filename = "a".repeat(100) + "!";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry(filename));
            zos.write(new byte[1]);
            zos.closeEntry();
        }
        byte[] zipBytes = baos.toByteArray();
        InMemoryZipFileDataSource source = new InMemoryZipFileDataSource(zipBytes);

        AtomicBoolean finished = new AtomicBoolean(false);
        Runnable runnable = () -> {
            try {
                source.listNames("(.*a){1000}");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            finished.set(true);
        };
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(runnable);

        Awaitility.await("Quick processing")
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .untilTrue(finished);
    }

}
