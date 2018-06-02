package com.anair.demo.component.movieratingsplitter.component.util;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class FileUtil {

    private static final Path TEST_RESOURCES = Paths.get("src/test/resources/test-json-files/");

    private FileUtil() {
        //Left blank intentionally
    }

    /**
     * Get a test file in the resources directory as a String.
     *
     * @param fileName the file name
     * @return the file as a String
     */
    public static String getResourceAsString(final String fileName) {
        final Path resolved = TEST_RESOURCES.resolve(fileName);
        final File file = resolved.toFile();
        if (!file.exists()) {
            throw new RuntimeException(String.format("The file [%s] does not exist", file.getAbsolutePath()));
        } else if (!file.canRead()) {
            throw new RuntimeException(String.format("The file [%s] exists but cannot be read", file.getAbsolutePath()));
        } else {
            try {
                return IOUtils.toString(resolved.toUri());
            } catch (IOException e) {
                throw new RuntimeException("IOException thrown reading file to String", e);
            }
        }
    }

}