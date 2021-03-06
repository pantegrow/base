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

import io.spine.tools.gradle.TaskName;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskContainer;
import org.junit.Before;
import org.junit.Test;

import static io.spine.tools.gradle.TaskDependencies.dependsOn;
import static io.spine.tools.gradle.TaskName.ANNOTATE_PROTO;
import static io.spine.tools.gradle.TaskName.ANNOTATE_TEST_PROTO;
import static io.spine.tools.gradle.TaskName.CLEAN;
import static io.spine.tools.gradle.TaskName.COMPILE_JAVA;
import static io.spine.tools.gradle.TaskName.COMPILE_TEST_JAVA;
import static io.spine.tools.gradle.TaskName.FIND_ENRICHMENTS;
import static io.spine.tools.gradle.TaskName.FIND_TEST_ENRICHMENTS;
import static io.spine.tools.gradle.TaskName.FIND_TEST_VALIDATION_RULES;
import static io.spine.tools.gradle.TaskName.FIND_VALIDATION_RULES;
import static io.spine.tools.gradle.TaskName.GENERATE_PROTO;
import static io.spine.tools.gradle.TaskName.GENERATE_REJECTIONS;
import static io.spine.tools.gradle.TaskName.GENERATE_TEST_PROTO;
import static io.spine.tools.gradle.TaskName.GENERATE_TEST_REJECTIONS;
import static io.spine.tools.gradle.TaskName.GENERATE_TEST_VALIDATING_BUILDERS;
import static io.spine.tools.gradle.TaskName.GENERATE_VALIDATING_BUILDERS;
import static io.spine.tools.gradle.TaskName.PRE_CLEAN;
import static io.spine.tools.gradle.TaskName.PROCESS_RESOURCES;
import static io.spine.tools.gradle.TaskName.PROCESS_TEST_RESOURCES;
import static io.spine.tools.gradle.compiler.given.ModelCompilerTestEnv.SPINE_PROTOBUF_PLUGIN_ID;
import static io.spine.tools.gradle.compiler.given.ModelCompilerTestEnv.newProject;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ModelCompilerPluginShould {

    private TaskContainer tasks;

    @Before
    public void setUp() {
        Project project = newProject();
        project.getPluginManager()
               .apply(SPINE_PROTOBUF_PLUGIN_ID);
        tasks = project.getTasks();
    }

    @Test
    public void apply_to_project() {
        Project project = newProject();
        project.getPluginManager()
               .apply(SPINE_PROTOBUF_PLUGIN_ID);
    }

    @Test
    public void add_task_preClean() {
        assertNotNull(task(PRE_CLEAN));
        assertTrue(dependsOn(task(CLEAN), task(PRE_CLEAN)));
    }

    @Test
    public void add_task_generateRejections() {
        Task genRejections = task(GENERATE_REJECTIONS);
        assertNotNull(genRejections);
        assertTrue(dependsOn(genRejections, GENERATE_PROTO));
        assertTrue(dependsOn(task(COMPILE_JAVA), genRejections));
    }

    @Test
    public void add_task_generateTestRejections() {
        Task genTestRejections = task(GENERATE_TEST_REJECTIONS);
        assertNotNull(genTestRejections);
        assertTrue(dependsOn(genTestRejections, GENERATE_TEST_PROTO));
        assertTrue(dependsOn(task(COMPILE_TEST_JAVA), genTestRejections));
    }

    @Test
    public void add_task_findEnrichments() {
        Task find = task(FIND_ENRICHMENTS);
        assertNotNull(find);
        assertTrue(dependsOn(find, COMPILE_JAVA));
        assertTrue(dependsOn(task(PROCESS_RESOURCES), find));
    }

    @Test
    public void add_task_findTestEnrichments() {
        Task find = task(FIND_TEST_ENRICHMENTS);
        assertNotNull(find);
        assertTrue(dependsOn(find, COMPILE_TEST_JAVA));
        assertTrue(dependsOn(task(PROCESS_TEST_RESOURCES), find));
    }

    @Test
    public void add_task_findValidationRules() {
        Task find = task(FIND_VALIDATION_RULES);
        assertNotNull(find);
        assertTrue(dependsOn(find, GENERATE_PROTO));
        assertTrue(dependsOn(task(PROCESS_RESOURCES), find));
    }

    @Test
    public void add_task_findTestValidationRules() {
        Task find = task(FIND_TEST_VALIDATION_RULES);
        assertNotNull(find);
        assertTrue(dependsOn(find, GENERATE_TEST_PROTO));
        assertTrue(dependsOn(task(PROCESS_TEST_RESOURCES), find));
    }

    @Test
    public void add_task_generation_validating_builders() {
        Task genValidatingBuilders = task(GENERATE_VALIDATING_BUILDERS);
        assertNotNull(genValidatingBuilders);
        assertTrue(dependsOn(genValidatingBuilders, GENERATE_PROTO));
        assertTrue(dependsOn(task(COMPILE_JAVA), genValidatingBuilders));
    }

    @Test
    public void add_task_generation_test_validating_builders() {
        Task genTestValidatingBuidlers = task(GENERATE_TEST_VALIDATING_BUILDERS);
        assertNotNull(genTestValidatingBuidlers);
        assertTrue(dependsOn(genTestValidatingBuidlers, GENERATE_TEST_PROTO));
        assertTrue(dependsOn(task(COMPILE_TEST_JAVA), genTestValidatingBuidlers));
    }

    @Test
    public void add_task_annotateProto() {
        Task annotateProto = task(ANNOTATE_PROTO);
        assertNotNull(annotateProto);
        assertTrue(dependsOn(annotateProto, GENERATE_PROTO));
        assertTrue(dependsOn(task(COMPILE_JAVA), annotateProto));
    }

    @Test
    public void add_task_annotateTestProto() {
        Task annotateTestProto = task(ANNOTATE_TEST_PROTO);
        assertNotNull(annotateTestProto);
        assertTrue(dependsOn(annotateTestProto, GENERATE_TEST_PROTO));
        assertTrue(dependsOn(task(COMPILE_TEST_JAVA), annotateTestProto));
    }

    private Task task(TaskName taskName) {
        return tasks.getByName(taskName.getValue());
    }
}
