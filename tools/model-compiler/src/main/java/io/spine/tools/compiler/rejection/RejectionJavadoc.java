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

package io.spine.tools.compiler.rejection;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.SourceCodeInfo.Location;
import io.spine.code.javadoc.JavadocEscaper;
import io.spine.code.proto.FieldName;
import io.spine.code.proto.LocationPath;
import io.spine.code.proto.RejectionDeclaration;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Generates Javadoc for a rejection.
 *
 * <p>Requires the following Protobuf plugin configuration:
 * <pre> {@code
 * generateProtoTasks {
 *     all().each { final task ->
 *         // If true, the descriptor set will contain line number information
 *         // and comments. Default is false.
 *         task.descriptorSetOptions.includeSourceInfo = true
 *         // ...
 *     }
 * }
 * }</pre>
 *
 * @author Dmytro Grankin
 * @author Alexander Yevsyukov
 * @see <a href="https://github.com/google/protobuf-gradle-plugin/blob/master/README.md#generate-descriptor-set-files">
 * Protobuf plugin configuration</a>
 */
public class RejectionJavadoc {

    private static final String OPENING_PRE = "<pre>";
    private static final String CLOSING_PRE = "</pre>";
    /*
      TODO:2017-03-24:dmytro.grankin: Replace hardcoded line separator by system-independent
      after https://github.com/square/javapoet/issues/552 is fixed.
    */
    @SuppressWarnings("HardcodedLineSeparator")
    private static final String LINE_SEPARATOR = "\n";

    private final RejectionDeclaration declaration;

    RejectionJavadoc(RejectionDeclaration declaration) {
        this.declaration = declaration;
    }

    /**
     * Generates a Javadoc content for the rejection.
     *
     * @return the class-level Javadoc content
     */
    public String forClass() {
        Optional<String> leadingComments = getRejectionLeadingComments();
        StringBuilder builder = new StringBuilder(256);

        leadingComments.ifPresent(s -> builder.append(OPENING_PRE)
                                              .append(LINE_SEPARATOR)
                                              .append(JavadocEscaper.escape(s))
                                              .append(CLOSING_PRE)
                                              .append(LINE_SEPARATOR)
                                              .append(LINE_SEPARATOR));

        builder.append("Rejection based on proto type {@code ")
               .append(declaration.getJavaPackage())
               .append('.')
               .append(declaration.getSimpleJavaClassName())
               .append('}')
               .append(LINE_SEPARATOR);
        return builder.toString();
    }

    /**
     * Generates a Javadoc content for the rejection constructor.
     *
     * @return the constructor Javadoc content
     */
    public String forConstructor() {
        StringBuilder builder = new StringBuilder("Creates a new instance.");
        Map<FieldDescriptorProto, String> commentedFields = getCommentedFields();

        if (!commentedFields.isEmpty()) {
            int maxFieldLength = getMaxFieldNameLength(commentedFields.keySet());

            builder.append(LINE_SEPARATOR)
                   .append(LINE_SEPARATOR);
            for (Entry<FieldDescriptorProto, String> commentedField : commentedFields.entrySet()) {
                String fieldName = FieldName.of(commentedField.getKey()
                                                              .getName())
                                            .javaCase();
                int commentOffset = maxFieldLength - fieldName.length() + 1;
                builder.append("@param ")
                       .append(fieldName)
                       .append(Strings.repeat(" ", commentOffset))
                       .append(JavadocEscaper.escape(commentedField.getValue()));
            }
        }

        return builder.toString();
    }

    /**
     * Returns the rejection field leading comments.
     *
     * @param field the descriptor of the field
     * @return the field leading comments or empty {@code Optional} if there are no such comments
     */
    private Optional<String> getFieldLeadingComments(FieldDescriptorProto field) {
        LocationPath fieldPath = getFieldLocationPath(field);
        return getLeadingComments(fieldPath);
    }

    /**
     * Returns the rejection leading comments.
     *
     * @return the comments text or empty {@code Optional} if there are no such comments
     */
    private Optional<String> getRejectionLeadingComments() {
        LocationPath messagePath = getMessageLocationPath();
        return getLeadingComments(messagePath);
    }

    /**
     * Obtains a leading comments by the {@link LocationPath}.
     *
     * @param locationPath the location path to get leading comments
     * @return the leading comments or empty {@code Optional} if there are no such comments
     */
    private Optional<String> getLeadingComments(LocationPath locationPath) {
        if (!declaration.getFile()
                        .hasSourceCodeInfo()) {
            String errMsg =
                    "To enable rejection generation, please configure the Gradle " +
                    "Protobuf plugin as follows: " +
                    "`task.descriptorSetOptions.includeSourceInfo = true`.";
            throw new IllegalStateException(errMsg);
        }

        Location location = getLocation(locationPath);
        return location.hasLeadingComments()
               ? Optional.of(location.getLeadingComments())
               : Optional.<String>empty();
    }

    /**
     * Returns the message location path for a top-level message definition.
     *
     * @return the message location path
     */
    private LocationPath getMessageLocationPath() {
        return new LocationPath(
                Arrays.asList(
                        FileDescriptorProto.MESSAGE_TYPE_FIELD_NUMBER,
                        getTopLevelMessageIndex())
        );
    }

    /**
     * Returns the field {@link LocationPath} for a top-level message definition.
     *
     * <p>Protobuf extensions are not supported.
     *
     * @param field the field to get location path
     * @return the field location path
     */
    private LocationPath getFieldLocationPath(FieldDescriptorProto field) {
        LocationPath locationPath = new LocationPath();

        locationPath.addAll(getMessageLocationPath());
        locationPath.add(DescriptorProto.FIELD_FIELD_NUMBER);
        locationPath.add(getFieldIndex(field));
        return locationPath;
    }

    private int getTopLevelMessageIndex() {
        List<DescriptorProto> messages = declaration.getFile()
                                                    .getMessageTypeList();
        for (DescriptorProto currentMessage : messages) {
            if (currentMessage.equals(declaration.getMessage())) {
                return messages.indexOf(declaration.getMessage());
            }
        }

        String msg = format("The rejection file \"%s\" should contain \"%s\" rejection.",
                            declaration.getFile()
                                       .getName(),
                            declaration.getMessage()
                                       .getName());
        throw new IllegalStateException(msg);
    }

    private int getFieldIndex(FieldDescriptorProto field) {
        return declaration.getMessage()
                          .getFieldList()
                          .indexOf(field);
    }

    /**
     * Returns the {@link Location} for the {@link LocationPath}.
     *
     * @param locationPath the location path
     * @return the location for the path
     */
    private Location getLocation(LocationPath locationPath) {
        for (Location location : declaration.getFile()
                                            .getSourceCodeInfo()
                                            .getLocationList()) {
            if (location.getPathList()
                        .equals(locationPath.getPath())) {
                return location;
            }
        }

        String msg = format("The location with %s path should be present in \"%s\".",
                            locationPath,
                            declaration.getFile()
                                       .getName());
        throw new IllegalStateException(msg);
    }

    /**
     * Returns field-to-comment map in order of {@linkplain FieldDescriptorProto fields}
     * declaration in the rejection.
     *
     * @return the commented fields
     */
    private Map<FieldDescriptorProto, String> getCommentedFields() {
        Map<FieldDescriptorProto, String> commentedFields = Maps.newLinkedHashMap();

        for (FieldDescriptorProto field : declaration.getMessage()
                                                     .getFieldList()) {
            Optional<String> leadingComments = getFieldLeadingComments(field);
            leadingComments.ifPresent(s -> commentedFields.put(field, s));
        }

        return commentedFields;
    }

    /**
     * Returns a max field name length among the non-empty
     * {@linkplain FieldDescriptorProto field} collection.
     *
     * @param fields the non-empty fields collection
     * @return the max name length
     */
    private static int getMaxFieldNameLength(Iterable<FieldDescriptorProto> fields) {
        Ordering<FieldDescriptorProto> ordering = new Ordering<FieldDescriptorProto>() {
            @Override
            public int compare(@Nullable FieldDescriptorProto left,
                               @Nullable FieldDescriptorProto right) {
                checkNotNull(left);
                checkNotNull(right);

                int result = Ints.compare(left.getName()
                                              .length(),
                                          right.getName()
                                               .length());
                return result;
            }
        };

        FieldDescriptorProto longestNameField = ordering.max(fields);
        return longestNameField.getName()
                               .length();
    }
}
