/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.datasource;

import com.powsybl.commons.datasource.DataSourceUtil;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.commons.io.ForwardingInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Bertrand Rix {@literal <bertrand.rix at artelys.com>}
 */
public class InMemoryZipFileDataSource implements ReadOnlyDataSource {

    private final byte[] zipFileBytes;

    private String basename;

    public InMemoryZipFileDataSource(byte[] zipFileBytes, String basename) {
        this.zipFileBytes = zipFileBytes;
        this.basename = basename;
    }

    public InMemoryZipFileDataSource(byte[] zipFileBytes) {
        this.zipFileBytes = zipFileBytes;
    }

    @Override
    public String getBaseName() {
        return basename;
    }

    public void setBaseName(String basename) {
        this.basename = basename;
    }

    @Override
    public boolean exists(String suffix, String ext) throws IOException {
        return exists(DataSourceUtil.getFileName(basename, suffix, ext));
    }

    @Override
    public boolean exists(String fileName) throws IOException {
        Objects.requireNonNull(fileName);
        return entryExists(zipFileBytes, fileName);
    }

    private static boolean entryExists(byte[] zipFileBytes, String fileName) {
        try (ZipFile zipFile = ZipFile.builder().setSeekableByteChannel(new SeekableInMemoryByteChannel(zipFileBytes)).get()) {
            return zipFile.getEntry(fileName) != null;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public InputStream newInputStream(String suffix, String ext) throws IOException {
        return newInputStream(DataSourceUtil.getFileName(basename, suffix, ext));
    }

    private static final class ZipEntryInputStream extends ForwardingInputStream<InputStream> {

        private final ZipFile zipFile;

        public ZipEntryInputStream(ZipFile zipFile, String fileName) throws IOException {
            super(zipFile.getInputStream(zipFile.getEntry(fileName)));
            this.zipFile = zipFile;
        }

        @Override
        public void close() throws IOException {
            super.close();

            zipFile.close();
        }
    }

    @Override
    public InputStream newInputStream(String fileName) throws IOException {
        Objects.requireNonNull(fileName);
        if (entryExists(zipFileBytes, fileName)) {
            return new ZipEntryInputStream(ZipFile.builder().setSeekableByteChannel(new SeekableInMemoryByteChannel(zipFileBytes)).get(), fileName);
        }
        return null;
    }

    @Override
    public Set<String> listNames(String regex) throws IOException {
        // Consider only files in the given folder, do not go into folders
        Pattern p = Pattern.compile(regex);
        Set<String> names = new HashSet<>();
        try (ZipFile zipFile = ZipFile.builder().setSeekableByteChannel(new SeekableInMemoryByteChannel(zipFileBytes)).get()) {
            Enumeration<ZipArchiveEntry> e = zipFile.getEntries();
            while (e.hasMoreElements()) {
                ZipArchiveEntry zipEntry = e.nextElement();
                if (!zipEntry.isDirectory() && p.matcher(zipEntry.getName()).matches()) {
                    names.add(zipEntry.getName());
                }
            }
        }
        return names;
    }
    @Override
    public boolean isDataExtension(String ext) {
        return true;
    }
}
