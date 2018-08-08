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

package io.spine.tools.gradle.compiler;

import io.spine.tools.gradle.compiler.given.SpineCheckerPluginTestEnv.ResolvingSpineCheckerPlugin;
import org.gradle.api.Project;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static io.spine.tools.gradle.compiler.Severity.ERROR;
import static io.spine.tools.gradle.compiler.given.ModelCompilerTestEnv.newProject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Dmytro Kuzmin
 */
public class SpineCheckExtensionShould {

    private Project project;

    @Rule
    public final TemporaryFolder projectDir = new TemporaryFolder();

    @Before
    public void setUp() {
        project = newProject(projectDir.getRoot());
        project.getPluginManager()
               .apply(ResolvingSpineCheckerPlugin.class);
    }

    @Test
    public void return_use_validating_builder_severity() {
        final Severity expected = ERROR;
        extension().useValidatingBuilder = expected;
        final Severity actual = SpineCheckExtension.getUseValidatingBuilder(project);
        assertEquals(expected, actual);
    }

    @Test
    public void return_null_use_validating_builder_severity_if_not_set() {
        final Severity severity = SpineCheckExtension.getUseValidatingBuilder(project);
        assertNull(severity);
    }

    private SpineCheckExtension extension() {
        return (SpineCheckExtension) project.getExtensions()
                                            .getByName(SpineCheckerPlugin.extensionName());
    }
}
