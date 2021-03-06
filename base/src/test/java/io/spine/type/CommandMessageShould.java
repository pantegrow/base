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

package io.spine.type;

import com.google.protobuf.Descriptors;
import com.google.protobuf.StringValue;
import io.spine.base.CommandMessage;
import io.spine.base.given.CommandFromCommands;
import org.junit.Test;

import static io.spine.testing.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CommandMessageShould {

    @Test
    public void have_utility_ctor_for_File_class() {
        assertHasPrivateParameterlessCtor(CommandMessage.File.class);
    }

    @Test
    public void tell_commands_file_by_its_descriptor() {
        Descriptors.FileDescriptor file = CommandFromCommands.getDescriptor()
                                                             .getFile();
        assertTrue(CommandMessage.File.predicate()
                                      .test(file));

        file = StringValue.getDescriptor()
                          .getFile();

        assertFalse(CommandMessage.File.predicate()
                                       .test(file));
    }
}
