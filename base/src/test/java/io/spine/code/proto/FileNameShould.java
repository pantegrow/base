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
import com.google.common.testing.NullPointerTester;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static io.spine.code.proto.FileName.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FileNameShould {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester().testStaticMethods(FileName.class,
                                                  NullPointerTester.Visibility.PACKAGE);
    }

    @Test
    public void require_standard_extension() {
        thrown.expect(IllegalArgumentException.class);
        of("some_thing");
    }

    @Test
    public void return_words() {
        List<String> words = of("some_file_name.proto").words();

        assertEquals(ImmutableList.of("some", "file", "name"), words);
    }

    @SuppressWarnings("DuplicateStringLiteralInspection")
    @Test
    public void calculate_outer_class_name() {
        assertEquals("Rejections", of("rejections.proto").nameOnlyCamelCase());
        assertEquals("ManyRejections", of("many_rejections.proto").nameOnlyCamelCase());
        assertEquals("ManyMoreRejections", of("many_more_rejections.proto").nameOnlyCamelCase());
    }

    @Test
    public void return_file_name_without_extension() {
        assertEquals("package/commands", of("package/commands.proto").nameWithoutExtension());
    }

    @Test
    public void tell_commands_file_kind() {
        FileName commandsFile = of("my_commands.proto");

        assertTrue(commandsFile.isCommands());
        assertFalse(commandsFile.isEvents());
        assertFalse(commandsFile.isRejections());
    }

    @Test
    public void tell_events_file_kind() {
        FileName eventsFile = of("project_events.proto");

        assertTrue(eventsFile.isEvents());
        assertFalse(eventsFile.isCommands());
        assertFalse(eventsFile.isRejections());
    }

    @Test
    public void tell_rejections_file_kind() {
        FileName rejectsionFile = of("rejections.proto");

        assertTrue(rejectsionFile.isRejections());
        assertFalse(rejectsionFile.isCommands());
        assertFalse(rejectsionFile.isEvents());
    }
}
