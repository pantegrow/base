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

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;
import com.google.protobuf.Descriptors.FileDescriptor;
import io.spine.code.java.DefaultJavaProject;
import io.spine.code.java.SourceFile;
import io.spine.code.proto.FileName;
import io.spine.code.proto.FileSet;
import io.spine.tools.compiler.annotation.check.FieldAnnotationCheck;
import io.spine.tools.compiler.annotation.check.MainDefinitionAnnotationCheck;
import io.spine.tools.compiler.annotation.check.NestedTypeFieldsAnnotationCheck;
import io.spine.tools.compiler.annotation.check.NestedTypesAnnotationCheck;
import io.spine.tools.compiler.annotation.check.SourceCheck;
import io.spine.tools.gradle.GradleProject;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.impl.AbstractJavaSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;
import static io.spine.tools.compiler.annotation.given.GivenProtoFile.NO_SPI_OPTIONS;
import static io.spine.tools.compiler.annotation.given.GivenProtoFile.NO_SPI_OPTIONS_MULTIPLE;
import static io.spine.tools.compiler.annotation.given.GivenProtoFile.POTENTIAL_ANNOTATION_DUP;
import static io.spine.tools.compiler.annotation.given.GivenProtoFile.SPI_ALL;
import static io.spine.tools.compiler.annotation.given.GivenProtoFile.SPI_ALL_MULTIPLE;
import static io.spine.tools.compiler.annotation.given.GivenProtoFile.SPI_ALL_SERVICE;
import static io.spine.tools.compiler.annotation.given.GivenProtoFile.SPI_FIELD;
import static io.spine.tools.compiler.annotation.given.GivenProtoFile.SPI_FIELD_MULTIPLE;
import static io.spine.tools.compiler.annotation.given.GivenProtoFile.SPI_MESSAGE;
import static io.spine.tools.compiler.annotation.given.GivenProtoFile.SPI_MESSAGE_MULTIPLE;
import static io.spine.tools.compiler.annotation.given.GivenProtoFile.SPI_SERVICE;
import static io.spine.tools.gradle.TaskName.ANNOTATE_PROTO;
import static io.spine.tools.gradle.TaskName.COMPILE_JAVA;

public class ProtoAnnotatorPluginShould {

    private static final String PROJECT_NAME = "annotator-plugin-test";

    @Rule
    public TemporaryFolder testProjectDir = new TemporaryFolder();

    @Test
    public void annotate_if_file_option_is_true() throws FileNotFoundException {
        assertNestedTypesAnnotations(SPI_ALL, true);
    }

    @Test
    public void annotate_service_if_file_option_is_true() throws FileNotFoundException {
        assertServiceAnnotations(SPI_ALL_SERVICE, true);
    }

    @Test
    public void not_annotate_if_file_option_if_false() throws FileNotFoundException {
        assertNestedTypesAnnotations(NO_SPI_OPTIONS, false);
    }

    @Test
    public void not_annotate_service_if_file_option_if_false() throws FileNotFoundException {
        assertNestedTypesAnnotations(NO_SPI_OPTIONS, false);
    }

    @Test
    public void annotate_multiple_files_if_file_option_is_true() throws FileNotFoundException {
        assertMainDefinitionAnnotations(SPI_ALL_MULTIPLE, true);
    }

    @Test
    public void not_annotate_multiple_files_if_file_option_is_false() throws FileNotFoundException {
        assertMainDefinitionAnnotations(NO_SPI_OPTIONS_MULTIPLE, false);
    }

    @Test
    public void annotate_if_message_option_is_true() throws FileNotFoundException {
        assertNestedTypesAnnotations(SPI_MESSAGE, true);
    }

    @Test
    public void not_annotate_if_message_option_is_false() throws FileNotFoundException {
        assertNestedTypesAnnotations(NO_SPI_OPTIONS, false);
    }

    @Test
    public void annotate_multiple_files_if_message_option_is_true() throws FileNotFoundException {
        assertMainDefinitionAnnotations(SPI_MESSAGE_MULTIPLE, true);
    }

    @Test
    public void not_annotate_multiple_files_if_message_option_is_false()
            throws FileNotFoundException {
        assertMainDefinitionAnnotations(NO_SPI_OPTIONS_MULTIPLE, false);
    }

    @Test
    public void annotate_accessors_if_field_option_is_true() throws FileNotFoundException {
        assertFieldAnnotations(SPI_FIELD, true);
    }

    @Test
    public void not_annotate_accessors_if_field_option_is_false() throws FileNotFoundException {
        assertFieldAnnotations(NO_SPI_OPTIONS, false);
    }

    @Test
    public void annotate_accessors_in_multiple_files_if_field_option_is_true()
            throws FileNotFoundException {
        assertFieldAnnotationsMultiple(SPI_FIELD_MULTIPLE, true);
    }

    @Test
    public void not_annotate_accessors_in_multiple_files_if_field_option_is_false()
            throws FileNotFoundException {
        assertFieldAnnotationsMultiple(NO_SPI_OPTIONS_MULTIPLE, false);
    }

    @Test
    public void annotate_grpc_services_if_service_option_is_true() throws FileNotFoundException {
        assertServiceAnnotations(SPI_SERVICE, true);
    }

    @Test
    public void not_annotate_grpc_services_if_service_option_is_false()
            throws FileNotFoundException {
        assertServiceAnnotations(NO_SPI_OPTIONS, false);
    }

    @Test
    public void compile_generated_sources_with_potential_annotation_duplication() {
        newProjectWithFile(POTENTIAL_ANNOTATION_DUP).executeTask(COMPILE_JAVA);
    }

    private void assertServiceAnnotations(FileName testFile, boolean shouldBeAnnotated)
            throws FileNotFoundException {
        FileDescriptorProto fileDescriptor = compileAndAnnotate(testFile);
        List<ServiceDescriptorProto> services = fileDescriptor.getServiceList();
        for (ServiceDescriptorProto serviceDescriptor : services) {
            SourceFile serviceFile = SourceFile.forService(serviceDescriptor, fileDescriptor);
            checkGrpcService(serviceFile, new MainDefinitionAnnotationCheck(shouldBeAnnotated));
        }
    }

    private void assertFieldAnnotations(FileName testFile, boolean shouldBeAnnotated)
            throws FileNotFoundException {
        FileDescriptorProto fileDescriptor = compileAndAnnotate(testFile);
        DescriptorProto messageDescriptor = fileDescriptor.getMessageType(0);
        Path sourcePath = SourceFile.forMessage(messageDescriptor, fileDescriptor)
                                    .getPath();
        NestedTypeFieldsAnnotationCheck check =
                new NestedTypeFieldsAnnotationCheck(messageDescriptor, shouldBeAnnotated);
        check(sourcePath, check);
    }

    private void assertFieldAnnotationsMultiple(FileName testFile, boolean shouldBeAnnotated)
            throws FileNotFoundException {
        FileDescriptorProto fileDescriptor = compileAndAnnotate(testFile);
        DescriptorProto messageDescriptor = fileDescriptor.getMessageType(0);
        FieldDescriptorProto experimentalField = messageDescriptor.getField(0);
        Path sourcePath = SourceFile.forMessage(messageDescriptor, fileDescriptor)
                                    .getPath();
        check(sourcePath, new FieldAnnotationCheck(experimentalField, shouldBeAnnotated));
    }

    private void assertMainDefinitionAnnotations(FileName testFile, boolean shouldBeAnnotated)
            throws FileNotFoundException {
        FileDescriptorProto fileDescriptor = compileAndAnnotate(testFile);
        for (DescriptorProto messageDescriptor : fileDescriptor.getMessageTypeList()) {
            Path messagePath =
                    SourceFile.forMessage(messageDescriptor, fileDescriptor)
                              .getPath();
            Path messageOrBuilderPath =
                    SourceFile.forMessageOrBuilder(messageDescriptor, fileDescriptor)
                              .getPath();
            SourceCheck annotationCheck =
                    new MainDefinitionAnnotationCheck(shouldBeAnnotated);
            check(messagePath, annotationCheck);
            check(messageOrBuilderPath, annotationCheck);
        }
    }

    private void assertNestedTypesAnnotations(FileName testFile, boolean shouldBeAnnotated)
            throws FileNotFoundException {
        FileDescriptorProto fileDescriptor = compileAndAnnotate(testFile);
        Path sourcePath = SourceFile.forOuterClassOf(fileDescriptor)
                                    .getPath();
        check(sourcePath, new NestedTypesAnnotationCheck(shouldBeAnnotated));
    }

    private void check(Path sourcePath, SourceCheck check) throws FileNotFoundException {
        Path filePath = DefaultJavaProject.at(testProjectDir.getRoot())
                                          .generated()
                                          .mainJava()
                                          .resolve(sourcePath);
        @SuppressWarnings("unchecked")
        AbstractJavaSource<JavaClassSource> javaSource =
                Roaster.parse(AbstractJavaSource.class, filePath.toFile());
        check.apply(javaSource);
    }

    private void checkGrpcService(SourceFile serviceFile, SourceCheck check)
            throws FileNotFoundException {
        Path fullPath = DefaultJavaProject.at(testProjectDir.getRoot())
                                          .generated()
                                          .mainGrpc()
                                          .resolve(serviceFile);
        @SuppressWarnings("unchecked")
        AbstractJavaSource<JavaClassSource> javaSource =
                Roaster.parse(AbstractJavaSource.class, fullPath.toFile());
        check.apply(javaSource);
    }

    /*
     * Test environment setup
     ************************************/

    private GradleProject newProjectWithFile(FileName protoFileName) {
        return GradleProject.newBuilder()
                            .setProjectName(PROJECT_NAME)
                            .setProjectFolder(testProjectDir.getRoot())
                            .addProtoFile(protoFileName.value())
                            .build();
    }

    private FileDescriptorProto compileAndAnnotate(FileName testFile) {
        GradleProject gradleProject = newProjectWithFile(testFile);
        gradleProject.executeTask(ANNOTATE_PROTO);
        FileDescriptorProto result = getDescriptor(testFile);
        return result;
    }

    private FileDescriptorProto getDescriptor(FileName fileName) {
        File descriptorSet = DefaultJavaProject.at(testProjectDir.getRoot())
                                               .mainDescriptors();
        FileSet fileSet = FileSet.parse(descriptorSet);
        Optional<FileDescriptor> file = fileSet.tryFind(fileName);
        checkState(file.isPresent(), "Unable to get file descriptor for %s", fileName);
        return file.get()
                   .toProto();
    }
}
