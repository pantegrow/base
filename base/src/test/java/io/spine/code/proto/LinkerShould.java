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

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

import static com.google.common.collect.ImmutableList.of;
import static org.junit.Assert.assertTrue;

/**
 * Tests resolving of {@link com.google.protobuf.Descriptors.FileDescriptor FileDescriptor}s
 * using test resource file.
 *
 * <p>See {@code test/resources/main.desc}.
 *
 * @author Alexander Yevsyukov
 */
public class LinkerShould {

    private Linker linker;

    @Before
    public void setUp() throws DescriptorValidationException {
        Collection<FileDescriptorProto> fileSets = FileDescriptors.load();
        linker = new Linker(fileSets);
        linker.resolve();
    }

    @Test
    public void resolve_files() {
        FileSet resolved = linker.getResolved();
        assertTrue(resolved.size() > 0);
        assertTrue(resolved.containsAll(of(
                FileName.of("google/protobuf/any.proto"),
                FileName.of("google/protobuf/descriptor.proto")
        )));
    }

    @Test
    public void obtain_partial() {
        // No such in the given test data.
        assertTrue(linker.getPartiallyResolved()
                         .isEmpty());
    }

    @Test
    public void obtain_unresolved() {
        // No such in the given test data.
        assertTrue(linker.getUnresolved()
                         .isEmpty());
    }

    @Test
    public void do_not_leave_remaining() {
        assertTrue(linker.getRemaining()
                         .isEmpty());
    }
}
