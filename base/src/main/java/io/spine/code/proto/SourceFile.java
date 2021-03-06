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

package io.spine.code.proto;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import io.spine.code.AbstractSourceFile;
import io.spine.code.java.SimpleClassName;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.code.proto.MessageDeclaration.create;

/**
 * A Protobuf file which also gives access to its {@link FileDescriptorProto descriptor}.
 *
 * @author Alexander Yevsyukov
 */
public class SourceFile extends AbstractSourceFile {

    private final FileDescriptorProto descriptor;

    SourceFile(FileDescriptorProto descriptor) {
        super(toPath(descriptor));
        this.descriptor = descriptor;
    }

    /**
     * Creates a new instance by the passed file descriptor.
     */
    public static SourceFile from(FileDescriptorProto file) {
        SourceFile result = new SourceFile(file);
        return result;
    }

    private static Path toPath(FileDescriptorProto file) {
        checkNotNull(file);
        Path result = Paths.get(file.getName());
        return result;
    }

    /**
     * Returns {@code true} if the source file matches conventions for rejection files.
     *
     * <p>A valid rejections file must have:
     * <ul>
     *     <li>The file name which ends on
     *         {@link io.spine.base.RejectionMessage.File#suffix() “rejections.proto”}.
     *     <li>The option {@code java_multiple_files} set to {@code false}.
     *     <li>Do not have the option {@code java_outer_classname} or have the value, which
     *         ends with {@linkplain RejectionDeclaration#isValidOuterClassName(SimpleClassName)}
     *         “Rejections”}.
     * </ul>
     */
    public boolean isRejections() {
        // By convention rejections are generated into one file.
        if (descriptor.getOptions()
                      .getJavaMultipleFiles()) {
            return false;
        }
        Optional<SimpleClassName> outerClass = SimpleClassName.declaredOuterClassName(descriptor);

        if (!outerClass.isPresent()) {
            // There's no outer class name given in options.
            // Assuming the file name ends with `rejections.proto`, it's a good rejections file.
            return true;
        }

        boolean result = RejectionDeclaration.isValidOuterClassName(outerClass.get());
        return result;
    }

    /**
     * Obtains descriptor of the file.
     */
    public FileDescriptorProto getDescriptor() {
        return descriptor;
    }

    /**
     * Obtains all message declarations that match the passed predicate.
     */
    public List<MessageDeclaration> allThat(Predicate<DescriptorProto> predicate) {
        ImmutableList.Builder<MessageDeclaration> result = ImmutableList.builder();
        for (DescriptorProto messageType : descriptor.getMessageTypeList()) {
            MessageDeclaration declaration = create(messageType, descriptor);
            if (predicate.test(messageType)) {
                result.add(declaration);
            }
            Collection<MessageDeclaration> allNested =
                    declaration.getAllNested(predicate);
            result.addAll(allNested);
        }
        return result.build();
    }

    /**
     * Obtains message declarations, that match the specified {@link Predicate} in all
     * passed files.
     *
     * @param files     the file descriptors to scan
     * @param predicate the predicate to test a message
     * @return the message declarations
     */
    public static List<MessageDeclaration> allThat(Iterable<FileDescriptorProto> files,
                                                   Predicate<DescriptorProto> predicate) {
        ImmutableList.Builder<MessageDeclaration> result = ImmutableList.builder();
        for (FileDescriptorProto file : files) {
            SourceFile sourceFile = from(file);
            Collection<MessageDeclaration> declarations = sourceFile.allThat(predicate);
            result.addAll(declarations);
        }
        return result.build();
    }
}
