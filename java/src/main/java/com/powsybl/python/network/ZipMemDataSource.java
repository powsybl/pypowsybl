/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.network;

import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.datasource.DataSourceUtil;
import com.powsybl.commons.io.ForwardingOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public class ZipMemDataSource implements DataSource {

    private final String baseName;

    private final ZipOutputStream zos;

    public ZipMemDataSource(String baseName, ZipOutputStream zos) {
        this.baseName = Objects.requireNonNull(baseName);
        this.zos = Objects.requireNonNull(zos);
    }

    @Override
    public OutputStream newOutputStream(String fileName, boolean append) throws IOException {
        Objects.requireNonNull(fileName);
        if (append) {
            throw new UnsupportedOperationException("append not supported in zip file data source");
        }
        zos.putNextEntry(new ZipEntry(fileName));
        return new ForwardingOutputStream<OutputStream>(zos) {
            @Override
            public void close() throws IOException {
                zos.closeEntry();
            }
        };
    }

    @Override
    public OutputStream newOutputStream(String suffix, String ext, boolean append) throws IOException {
        return newOutputStream(DataSourceUtil.getFileName(baseName, suffix, ext), append);
    }

    @Override
    public String getBaseName() {
        return baseName;
    }

    @Override
    public boolean exists(String suffix, String ext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean exists(String fileName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream newInputStream(String suffix, String ext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream newInputStream(String fileName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> listNames(String regex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDataExtension(String ext) {
        return true;
    }

}
