package com.anair.demo.component.movieratingsplitter.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Streams;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Useful utility methods for working with files for unit testing.
 */
public final class TestFileUtils {

    private static final Path TEST_RESOURCES = Paths.get("src/test/resources/");

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private TestFileUtils() {
    }

    /**
     * Get a File as a String using the default charset.
     *
     * @param file the file
     * @return the String
     */
    public static String fileAsString(final File file) {
        return fileAsString(file.toPath());
    }

    /**
     * Get a Path as a String using the default charset
     *
     * @param path the Path
     * @return the file as a String
     */
    public static String fileAsString(final Path path) {
        try {
            return IOUtils.toString(path.toUri(), Charset.defaultCharset());
        } catch (IOException e) {
            throw new RuntimeException("IOException thrown reading file to String", e);
        }
    }

    /**
     * Get a test resources file as a String in the default charset.
     *
     * @param fileName the file to get as a String
     * @return the file content as a String
     */
    public static String getTestResourceAsString(final String fileName) {
        final Path resolved = TEST_RESOURCES.resolve(fileName);
        return Streams.stream(Optional.of(resolved.toFile()))
                .filter(File::isFile)
                .filter(File::canRead)
                .map(TestFileUtils::fileAsString)
                .collect(Collectors.toList())
                .get(0);
    }

}
