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

package io.spine.tools.javadoc;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.RootDoc;
import io.spine.testing.Tests;
import io.spine.util.Exceptions;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static io.spine.tools.javadoc.RootDocProxyReceiver.rootDocFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ExcludeInternalDocletShould {

    private static final String TEST_SOURCES_PACKAGE = "testsources";
    private static final String INTERNAL_PACKAGE = TEST_SOURCES_PACKAGE + ".internal";
    private static final String INTERNAL_METHOD_CLASS_FILENAME = "InternalMethodClass.java";
    private static final String INTERNAL_CLASS_FILENAME = "InternalClass.java";
    private static final String DERIVED_FROM_INTERNAL_CLASS_FILENAME = "DerivedFromInternalClass.java";
    private static final String NOT_INTERNAL_CLASS_FILENAME = "/notinternal/NotInternalClass.java";

    @After
    public void tearDown() {
        cleanUpGeneratedJavadocs();
    }

    @Test
    public void run_standard_doclet() {
        String[] args = new JavadocArgsBuilder()
                .addSource(NOT_INTERNAL_CLASS_FILENAME)
                .build();

        ExcludeInternalDoclet.main(args);

        assertTrue(Files.exists(Paths.get(JavadocArgsBuilder.getJavadocDir())));
    }

    @Test
    public void exclude_internal_annotated_annotations() {
        String[] args = new JavadocArgsBuilder()
                .addSource("InternalAnnotatedAnnotation.java")
                .build();

        RootDoc rootDoc = rootDocFor(args);

        assertEquals(0, rootDoc.specifiedClasses().length);
    }

    @Test
    public void exclude_internal_ctors() {
        String[] args = new JavadocArgsBuilder().addSource("InternalCtorClass.java")
                                                .build();

        RootDoc rootDoc = rootDocFor(args);

        ClassDoc classDoc = rootDoc.specifiedClasses()[0];
        assertEquals(0, classDoc.constructors().length);
    }

    @Test
    public void exclude_internal_fields() {
        String[] args = new JavadocArgsBuilder()
                .addSource("InternalFieldClass.java")
                .build();

        RootDoc rootDoc = rootDocFor(args);

        ClassDoc classDoc = rootDoc.specifiedClasses()[0];
        assertEquals(0, classDoc.fields().length);
    }

    @Test
    public void exclude_internal_methods() {
        String[] args = new JavadocArgsBuilder()
                .addSource(INTERNAL_METHOD_CLASS_FILENAME)
                .build();

        RootDoc rootDoc = rootDocFor(args);

        ClassDoc classDoc = rootDoc.specifiedClasses()[0];
        assertEquals(0, classDoc.methods().length);
    }

    @Test
    public void exclude_internal_package_content() {
        String[] args = new JavadocArgsBuilder()
                .addSource("/internal/InternalPackageClass.java")
                .build();

        RootDoc rootDoc = rootDocFor(args);

        assertEquals(0, rootDoc.specifiedClasses().length);
    }

    @Test
    public void exclude_only_from_internal_subpackages() {
        String[] args = new JavadocArgsBuilder()
                .addSource("/internal/subinternal/SubInternalPackageClass.java")
                .addSource(NOT_INTERNAL_CLASS_FILENAME)
                .addPackage(INTERNAL_PACKAGE)
                .addPackage(TEST_SOURCES_PACKAGE + ".notinternal")
                .build();

        RootDoc rootDoc = rootDocFor(args);

        assertEquals(1, rootDoc.specifiedClasses().length);
    }

    @Test
    public void exclude_internal_classes() {
        String[] args = new JavadocArgsBuilder()
                .addSource(INTERNAL_CLASS_FILENAME)
                .build();

        RootDoc rootDoc = rootDocFor(args);

        assertEquals(0, rootDoc.specifiedClasses().length);
    }

    @Test
    public void exclude_internal_interfaces() {
        String[] args = new JavadocArgsBuilder()
                .addSource("InternalAnnotatedInterface.java")
                .build();

        RootDoc rootDoc = rootDocFor(args);

        assertEquals(0, rootDoc.specifiedClasses().length);
    }

    @Test
    public void exclude_internal_enums() {
        String[] args = new JavadocArgsBuilder()
                .addSource("InternalEnum.java")
                .build();

        RootDoc rootDoc = rootDocFor(args);

        assertEquals(0, rootDoc.specifiedClasses().length);
    }

    @Test
    public void not_exclude_elements_annotated_with_Internal_located_in_another_package() {
        String[] args = new JavadocArgsBuilder()
                .addSource("GrpcInternalAnnotatedClass.java")
                .build();

        RootDoc rootDoc = rootDocFor(args);

        assertEquals(1, rootDoc.specifiedClasses().length);
    }

    @Test
    public void correctly_work_when_compareTo_called() {
        String[] args = new JavadocArgsBuilder()
                .addSource(INTERNAL_CLASS_FILENAME)
                .addSource(DERIVED_FROM_INTERNAL_CLASS_FILENAME)
                .build();

        RootDoc rootDoc = rootDocFor(args);

        // invoke compareTo to be sure, that proxy unwrapping
        // doest not expose object passed to compareTo method
        ClassDoc classDoc = rootDoc.specifiedClasses()[0];
        ClassDoc anotherClassDoc = classDoc.superclass();
        classDoc.compareTo(anotherClassDoc);

        assertEquals(1, rootDoc.specifiedClasses().length);
    }

    @Test
    public void correctly_work_when_overrides_called() {
        String[] args = new JavadocArgsBuilder()
                .addSource(INTERNAL_METHOD_CLASS_FILENAME)
                .addSource("OverridesInternalMethod.java")
                .build();

        RootDoc rootDoc = rootDocFor(args);

        // invoke overrides to be sure, that proxy unwrapping
        // doest not expose overridden method
        ClassDoc overridesInternalMethod =
                rootDoc.classNamed(TEST_SOURCES_PACKAGE + ".OverridesInternalMethod");
        MethodDoc methodDoc = overridesInternalMethod.methods()[0];
        MethodDoc overriddenMethod = methodDoc.overriddenMethod();
        methodDoc.overrides(overriddenMethod);

        assertEquals(0, rootDoc.classNamed(TEST_SOURCES_PACKAGE + ".InternalMethodClass")
                               .methods().length);
    }

    @Test
    public void correctly_work_when_subclassOf_invoked() {
        String[] args = new JavadocArgsBuilder()
                .addSource(INTERNAL_CLASS_FILENAME)
                .addSource(DERIVED_FROM_INTERNAL_CLASS_FILENAME)
                .build();

        RootDoc rootDoc = rootDocFor(args);

        // invoke subclassOf to be sure, that proxy unwrapping
        // doest not expose parent internal class
        ClassDoc classDoc = rootDoc.specifiedClasses()[0];
        ClassDoc superclass = classDoc.superclass();
        classDoc.subclassOf(superclass);

        assertEquals(1, rootDoc.specifiedClasses().length);
    }

    @SuppressWarnings({
            "ProhibitedExceptionCaught" /* Need to catch NPE to fail test. */,
            "CheckReturnValue"
    })
    @Test
    public void not_throw_NPE_processing_null_values() {
        ExcludeInternalDoclet doclet = new NullProcessingTestDoclet();

        try {
            doclet.process(Tests.nullRef(), void.class);
        } catch (NullPointerException e) {
            fail("Should process null values without throwing NPE.");
        }
    }

    @SuppressWarnings("ConstantConditions") // Ok to not initialize ExcludePrinciple here.
    private static class NullProcessingTestDoclet extends ExcludeInternalDoclet {

        private NullProcessingTestDoclet() {
            super(null);
        }
    }

    private static void cleanUpGeneratedJavadocs() {
        Path javadocRoot = Paths.get(JavadocArgsBuilder.getJavadocDir());

        if (Files.exists(javadocRoot)) {
            try {
                Files.walkFileTree(javadocRoot, new FileRemover());
            } catch (IOException e) {
                throw Exceptions.illegalStateWithCauseOf(e);
            }
        }
    }

    /**
     * Deletes files and directories.
     */
    private static class FileRemover extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    }
}
