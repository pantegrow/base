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
package io.spine.tools.reflections;

import org.gradle.api.Project;
import org.junit.Before;
import org.junit.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class ExtensionShould {

    private Project project;

    @Before
    public void setUp() {
        project = Given.newProject();
        project.getPluginManager()
               .apply(Given.REFLECTIONS_PLUGIN_ID);
    }

    @Test
    public void return_default_targetDir_if_not_set() {
        String dir = Extension.getTargetDir(project);

        assertFalse(dir.trim()
                       .isEmpty());
        assertTrue(dir.startsWith(project.getProjectDir()
                                         .getAbsolutePath()));
    }

    @Test
    public void return_targetDir_if_set() {
        Extension extension = Extension.reflectionsPlugin(project);
        extension.targetDir = "some target dir value";

        String dir = Extension.getTargetDir(project);

        assertEquals(extension.targetDir, dir);
    }
}
