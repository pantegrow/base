/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.tools.compiler.validation;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import io.spine.tools.Indent;
import io.spine.tools.compiler.MessageTypeCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Gradle {@code Action} for validating builder generation.
 *
 * <p>An instance-per-scope is usually created. E.g. test sources and main source are
 * generated with different instances of this class.
 *
 * @author Illia Shepilov
 */
public class VBuilderGenerator {

    /** Code will be generated into this directory. */
    private final String targetDirPath;

    /** Source directory with proto files. */
    private final String protoSrcDirPath;

    /** Controls the scope of validating builder generation. */
    private final boolean classpathGenEnabled;

    /** Indentation for the generated code. */
    private final Indent indent;

    /**
     * Creates new instance of the generator.
     *  @param targetDirPath
     *        an absolute path to the folder, serving as a target for the generation for
     *        the given scope
     * @param protoSrcDirPath
     *        an absolute path to the folder, containing the {@code .proto} files for
     *        the given scope
     * @param classpathGenEnabled
     *        If {@code true}, validating builders will be generated for all types from the
     *        classpath. If {@code false}, validating builders will be generated only to the
     * @param indent
     *        indentation for the generated code
     */
    public VBuilderGenerator(String targetDirPath,
                             String protoSrcDirPath,
                             boolean classpathGenEnabled,
                             Indent indent) {
        this.targetDirPath = targetDirPath;
        this.protoSrcDirPath = protoSrcDirPath;
        this.classpathGenEnabled = classpathGenEnabled;
        this.indent = indent;
    }

    public void processDescriptorSetFile(File setFile) {
        final Logger log = log();
        log.debug("Generating the validating builders from {}.", setFile);

        final MetadataAssembler assembler = new MetadataAssembler(setFile.getPath());
        final Set<VBMetadata> allFound = assembler.assemble();
        final MessageTypeCache messageTypeCache = assembler.getAssembledMessageTypeCache();

        final Set<VBMetadata> filtered = filter(classpathGenEnabled, allFound);
        if (filtered.isEmpty()) {
            log.warn("No validating builders will be generated.");
        } else {
            writeVBuilders(filtered, messageTypeCache);
        }
    }

    private void writeVBuilders(Set<VBMetadata> builders, MessageTypeCache cache) {
        final Logger log = log();
        final ValidatingBuilderWriter writer =
                new ValidatingBuilderWriter(targetDirPath, indent, cache);

        for (VBMetadata vb : builders) {
            try {
                writer.write(vb);
            } catch (RuntimeException e) {
                final String message =
                        format("Cannot generate the validating builder for %s. %n" +
                               "Error: %s", vb, e.toString());
                log.debug(message, e);
                log.warn(message);
            }
        }
        log.debug("The validating builder generation is finished.");
    }

    private Set<VBMetadata> filter(boolean classpathGenEnabled,
                                   Set<VBMetadata> metadataItems) {
        final Predicate<VBMetadata> shouldWrite = getPredicate(classpathGenEnabled);
        final Iterable<VBMetadata> filtered = Iterables.filter(metadataItems, shouldWrite);
        final Set<VBMetadata> result = ImmutableSet.copyOf(filtered);
        return result;
    }

    private Predicate<VBMetadata> getPredicate(final boolean classpathGenEnabled) {
        final Predicate<VBMetadata> result;
        if (classpathGenEnabled) {
            result = Predicates.alwaysTrue();
        } else {
            final String rootPath = protoSrcDirPath.endsWith(File.separator)
                                    ? protoSrcDirPath
                                    : protoSrcDirPath + File.separator;
            result = new SourceProtoBelongsToModule(rootPath);
        }
        return result;
    }

    /**
     * A predicate determining if the given {@linkplain VBMetadata validating builder metadata}
     * has been collected from the source file in the specified module.
     *
     * <p>Each predicate instance requires to specify the root folder of Protobuf definitions
     * for the module. This value is used to match the given {@code VBMetadata}.
     */
    private static class SourceProtoBelongsToModule implements Predicate<VBMetadata> {

        /**
         *  An absolute path to the root folder for the {@code .proto} files in the module.
         */
        private final String rootPath;

        private SourceProtoBelongsToModule(String rootPath) {
            this.rootPath = rootPath;
        }

        @Override
        public boolean apply(@Nullable VBMetadata input) {
            checkNotNull(input);

            final String path = input.getSourceProtoFilePath();
            final File protoFile = new File(rootPath + path);
            final boolean belongsToModule = protoFile.exists();
            return belongsToModule;
        }
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(VBuilderGenerator.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
