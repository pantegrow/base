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

package io.spine.js.generate.type;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import io.spine.code.js.FileName;
import io.spine.code.js.TypeName;
import io.spine.code.proto.FileSet;
import io.spine.js.generate.JsCodeGenerator;
import io.spine.js.generate.JsOutput;
import io.spine.js.generate.importado.JsImportGenerator;
import io.spine.type.TypeUrl;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * The generator of the global known types {@code Map}.
 *
 * <p>This class generates the map with all the known types written in the form of
 * "{@linkplain TypeUrl type-url}-to-JS-type", as well as the imports necessary to use the types.
 */
public final class KnownTypesGenerator extends JsCodeGenerator {

    /**
     * The exported map name.
     */
    private static final String MAP_NAME = "types";

    private final FileSet fileSet;

    /**
     * Creates a new {@code KnownTypesGenerator}.
     *
     * <p>All the known types will be acquired from the {@code fileSet} and the {@code jsOutput}
     * accumulates the JS code lines.
     *
     * @param fileSet
     *         the {@code FileSet} containing all the known types
     * @param jsOutput
     *         the {@code JsOutput} to accumulate the generated code
     */
    public KnownTypesGenerator(FileSet fileSet, JsOutput jsOutput) {
        super(jsOutput);
        this.fileSet = fileSet;
    }

    /**
     * Generates the known types code.
     *
     * <p>The code includes:
     * <ol>
     *     <li>Imports of all JS files declaring generated messages
     *     <li>The global {@code Map} of known types
     * </ol>
     */
    @Override
    public void generate() {
        generateImports();
        generateKnownTypesMap();
    }

    /**
     * Generates import statements for all files declaring generated messages.
     */
    @VisibleForTesting
    void generateImports() {
        Collection<FileDescriptor> files = fileSet.files();
        Set<FileName> imports = files.stream()
                                     .filter(file -> !file.getMessageTypes()
                                                          .isEmpty())
                                     .map(FileName::from)
                                     .collect(toSet());
        JsImportGenerator generator = JsImportGenerator
                .newBuilder()
                .setImports(imports)
                .setJsOutput(jsOutput())
                .build();
        generator.generate();
    }

    /**
     * Generates the {@code Map} of known types.
     *
     * <p>Map entries are known types stored in the "{@linkplain TypeUrl type-url}-to-JS-type"
     * format.
     *
     * <p>The map is exported under the {@link #MAP_NAME}.
     */
    @VisibleForTesting
    void generateKnownTypesMap() {
        jsOutput().addEmptyLine();
        jsOutput().exportMap(MAP_NAME);
        storeKnownTypes();
        jsOutput().quitMapDeclaration();
    }

    /**
     * Stores known types to the declared {@code Map}.
     */
    private void storeKnownTypes() {
        Collection<FileDescriptor> files = fileSet.files();
        for (Iterator<FileDescriptor> it = files.iterator(); it.hasNext(); ) {
            FileDescriptor file = it.next();
            boolean isLastFile = !it.hasNext();
            storeTypesFromFile(file, isLastFile);
        }
    }

    /**
     * Stores all message types declared in a file as known types {@code Map} entries.
     */
    private void storeTypesFromFile(FileDescriptor file, boolean isLastFile) {
        List<Descriptor> messages = file.getMessageTypes();
        for (Iterator<Descriptor> it = messages.iterator(); it.hasNext(); ) {
            Descriptor message = it.next();
            boolean isLastMessage = !it.hasNext() && isLastFile;
            addMapEntry(message, isLastMessage);
        }
    }

    /**
     * Converts the {@code message} to the {@code Map} entry and adds it to the {@link #jsOutput}.
     */
    private void addMapEntry(Descriptor message, boolean isLastMessage) {
        String mapEntry = jsMapEntry(message);
        jsOutput().addMapEntry(mapEntry, isLastMessage);
    }

    /**
     * Obtains type URL and JS type name of the {@code message} and creates a {@code Map} entry of
     * the "{@linkplain TypeUrl type-url}-to-JS-type" format.
     */
    private static String jsMapEntry(Descriptor message) {
        TypeUrl typeUrl = TypeUrl.from(message);
        TypeName typeName = TypeName.from(message);
        String mapEntry = "['" + typeUrl + "', " + typeName + ']';
        return mapEntry;
    }
}
