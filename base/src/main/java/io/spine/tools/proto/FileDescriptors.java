/*
 * Copyright 2018, TeamDev. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.spine.tools.proto;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Streams;
import com.google.common.io.CharStreams;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import io.spine.io.ResourceFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.newHashSet;
import static io.spine.io.ResourceFiles.loadAll;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.util.stream.Collectors.toSet;

/**
 * A utility class which allows to obtain Protobuf file descriptors.
 *
 * @author Alexander Litus
 * @author Alex Tymchenko
 */
public class FileDescriptors {


    public static final String KNOWN_TYPE_PROVIDERS = "known_type_providers";

    /**
     * Default file name for descriptor set generated from the proto files under
     * the {@code main/proto} project directory.
     */
    public static final String MAIN_FILE = "main.desc";

    /**
     * Default file name for the descriptor set generated from the proto files under
     * the {@code test/proto} project directory.
     */
    public static final String TEST_FILE = "test.desc";

    /** Prevents instantiation of this utility class. */
    private FileDescriptors() {
    }

    /**
     * Returns descriptors of all {@code .proto} files described in the descriptor set file.
     *
     * @param descriptorSetFile the path to the file generated by the {@code protobuf-gradle-plugin}
     *                          which contains descriptors of the project {@code .proto} files
     * @return a list of descriptors
     */
    public static List<FileDescriptorProto> parse(String descriptorSetFile) {
        return parseAndFilter(descriptorSetFile, descriptor -> true);
    }

    /**
     * Obtains the list of files from the passed descriptor set file, skipping files provided
     * by Google Protobuf.
     */
    public static List<FileDescriptorProto> parseSkipStandard(String descriptorSetFile) {
        return parseAndFilter(descriptorSetFile, isNotGoogleProto());
    }

    /**
     * Returns descriptors of `.proto` files described in the descriptor set file
     * which match the filter predicate.
     *
     * @param descriptorSetFile the path to the file generated by {@code protobuf-gradle-plugin}
     *                          which contains descriptors of the project {@code .proto} files
     * @param filter            a filter predicate to apply to the files
     * @return a list of descriptors
     */
    private static List<FileDescriptorProto> parseAndFilter(String descriptorSetFile,
                                                            Predicate<FileDescriptorProto> filter) {
        final File descriptorsFile = new File(descriptorSetFile);
        checkArgument(descriptorsFile.exists(), "File %s does not exist", descriptorSetFile);

        final Logger log = log();
        if (log.isDebugEnabled()) {
            log.debug("Looking up for the proto files matching predicate {} under {}",
                      filter,
                      descriptorSetFile);
        }

        final ImmutableList.Builder<FileDescriptorProto> files = ImmutableList.builder();
        try (final FileInputStream fis = new FileInputStream(descriptorsFile)) {
            final FileDescriptorSet fileSet = FileDescriptorSet.parseFrom(fis);
            for (FileDescriptorProto file : fileSet.getFileList()) {
                if (filter.test(file)) {
                    files.add(file);
                }
            }
        } catch (IOException e) {
            throw newIllegalStateException(
                    e, "Cannot get proto file descriptors. Path: %s", descriptorSetFile
            );
        }

        final ImmutableList<FileDescriptorProto> result = files.build();
        log.debug("Found {} files: {}", result.size(), files);
        return result;
    }

    /**
     * Loads main file descriptor set from resources.
     */
    public static Iterator<FileDescriptorSet> load() {
        final Iterator<FileDescriptorSet> sets = readAllTypeProviders()
                .stream()
                .map(FileDescriptors::loadFrom)
                .reduce(Iterators::concat)
                .orElseThrow(() -> newIllegalStateException("Known types cannot be loaded."));
        return sets;
    }

    /**
     * Extracts all instances of {@link FileDescriptorProto} from the given descriptor sets.
     */
    static Collection<FileDescriptorProto>
    extractFiles(Iterator<FileDescriptorSet> fileDescriptorSets) {
        final Set<FileDescriptorProto> files = newHashSet();
        while (fileDescriptorSets.hasNext()) {
            final FileDescriptorSet descriptorSet = fileDescriptorSets.next();
            files.addAll(descriptorSet.getFileList());
        }
        return files;
    }

    private static Collection<String> readAllTypeProviders() {
        return Streams.stream(ResourceFiles.loadAll(KNOWN_TYPE_PROVIDERS))
                      .flatMap(file -> readTypeProviders(file).stream())
                      .collect(toSet());
    }

    private static Collection<String> readTypeProviders(URL url) {
        try (Reader reader = new InputStreamReader(url.openStream())) {
            return CharStreams.readLines(reader);
        } catch (IOException e) {
            throw illegalStateWithCauseOf(e);
        }
    }

    private static Iterator<FileDescriptorSet> loadFrom(String resourceName) {
        final Iterator<URL> descriptorFiles = loadAll(resourceName);
        final Iterator<FileDescriptorSet> result = Streams.stream(descriptorFiles)
                                                          .map(FileDescriptorLoader.FUNCTION)
                                                          .iterator();
        return result;
    }

    /**
     * Obtains the predicate that filters out file descriptors for types with {@code "google"}
     * in the package name.
     */
    public static Predicate<FileDescriptorProto> isNotGoogleProto() {
        return IsNotGoogleProto.PREDICATE;
    }

    /**
     * Verifies if a package of a file does not contain {@code "google"} in its path.
     */
    private enum IsNotGoogleProto implements Predicate<FileDescriptorProto> {
        PREDICATE;

        private static final String GOOGLE_PACKAGE = "google";

        @Override
        public boolean test(FileDescriptorProto file) {
            checkNotNull(file);
            final boolean result = !file.getPackage()
                                        .contains(GOOGLE_PACKAGE);
            return result;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    /**
     * Loads the {@link FileDescriptorSet} from a given resource file.
     */
    private enum FileDescriptorLoader implements Function<URL, FileDescriptorSet> {
        FUNCTION;

        @Override
        public FileDescriptorSet apply(URL file) {
            checkNotNull(file);
            try {
                final InputStream stream = file.openStream();
                final FileDescriptorSet parsed = FileDescriptorSet.parseFrom(stream);
                return parsed;
            } catch (IOException e) {
                throw newIllegalStateException(
                        e,
                        "Unable to load descriptor file set from %s.",
                        file
                );
            }
        }
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(FileDescriptors.class);
    }
}
