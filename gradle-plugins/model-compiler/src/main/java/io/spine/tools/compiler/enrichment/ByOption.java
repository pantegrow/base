/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.tools.compiler.enrichment;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkState;
import static io.spine.option.OptionsProto.BY_FIELD_NUMBER;
import static io.spine.option.RawListParser.getValueSeparator;
import static io.spine.option.UnknownOptions.getUnknownOptionValue;
import static io.spine.option.UnknownOptions.hasUnknownOption;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.util.regex.Pattern.compile;

/**
 * Obtains event names from the {@code "by"} field option of a message.
 *
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
class ByOption {

    /**
     * Wildcard option used in {@code "by"} field option.
     *
     * <p>{@code string enrichment_value [(by) = "*.my_event_id"];} tells that this enrichment
     * may have any target event types. That's why an FQN of the target type is replaced by
     * this wildcard option.
     */
    private static final String ANY_BY_OPTION_TARGET = "*";

    private static final String PIPE_SEPARATOR = "|";
    private static final Pattern PATTERN_PIPE_SEPARATOR = compile("\\|");

    private final String packagePrefix;
    private final DescriptorProto message;
    private final FieldDescriptorProto field;

    ByOption(String packagePrefix, DescriptorProto message, FieldDescriptorProto field) {
        this.packagePrefix = packagePrefix;
        this.message = message;
        this.field = field;
    }

    static boolean isSetFor(FieldDescriptorProto field) {
        return hasUnknownOption(field, BY_FIELD_NUMBER);
    }

    Map.Entry<String, String> collect() {
        final Collection<String> eventNamesFromBy = parse();
        final Map.Entry<String, String> result =
                group(eventNamesFromBy);
        return result;
    }

    /**
     * Obtains the list with fully-qualified names of target event types for
     * the given field.
     */
    private List<String> parse() {
        final String[] fieldRefs = fieldReferences(field);

        final ImmutableList.Builder<String> result = ImmutableList.builder();

        for (String fieldRef : fieldRefs) {
            if (fieldRef == null) {
                throw invalidByOptionValue(field.getName());
            }
            if (fieldRef.startsWith(ANY_BY_OPTION_TARGET) && fieldRefs.length > 1) {
                // Multiple argument `by` annotation can not contain wildcard reference onto
                // the event type if the type was not specified with a `enrichment_for` annotation
                throw invalidByOptionUsage(field.getName());
            }
            final int index = fieldRef.lastIndexOf(EnrichmentFinder.PROTO_TYPE_SEPARATOR);
            if (index < 0) {
                // The short form type names are handled as inner types
                continue;
            }
            final String typeFqn = fieldRef.substring(0, index)
                                           .trim();
            checkState(!typeFqn.isEmpty(),
                       "Error parsing `by` annotation for field %s", field.getName());
            result.add(typeFqn);
        }

        return result.build();
    }

    @SuppressWarnings("IndexOfReplaceableByContains") // On performance purposes
    private static String[] fieldReferences(FieldDescriptorProto field) {
        final String byArgument = getUnknownOptionValue(field, BY_FIELD_NUMBER);
        final String[] fieldFqnsArray;

        if (byArgument.indexOf(PIPE_SEPARATOR) < 0) {
            fieldFqnsArray = new String[]{byArgument};
        } else {
            fieldFqnsArray = PATTERN_PIPE_SEPARATOR.split(byArgument);
        }
        return fieldFqnsArray;
    }

    private Map.Entry<String, String> group(Collection<String> events) {
        final String enrichment = message.getName();
        final String fieldName = field.getName();
        final Logger log = log();
        final Collection<String> eventGroup = new HashSet<>(events.size());
        for (String eventName : events) {
            if (eventName == null || eventName.trim()
                                              .isEmpty()) {
                throw invalidByOptionValue(enrichment);
            }
            log.debug("'by' option found on field {} targeting {}", fieldName, eventName);

            if (ANY_BY_OPTION_TARGET.equals(eventName)) {
                log.warn("Skipping a wildcard event");
                /* Ignore the wildcard `by` options, as we don't know
                   the target event type in this case. */
                continue;
            }
            eventGroup.add(eventName);
        }
        final String enrichmentName = packagePrefix + enrichment;
        final String eventGroupString = Joiner.on(getValueSeparator())
                                              .join(eventGroup);
        final Map.Entry<String, String> result =
                new AbstractMap.SimpleEntry<>(enrichmentName, eventGroupString);

        return result;
    }

    private static IllegalStateException invalidByOptionValue(String msgName) {
        throw newIllegalStateException(
                "The message field `%s` has invalid 'by' option value, " +
                        "which must be a fully-qualified field reference.",
                msgName
        );
    }

    private static IllegalStateException invalidByOptionUsage(String msgName) {
        throw newIllegalStateException(
                "Field of message `%s` has invalid 'by' option value. " +
                        "Wildcard type is not allowed with multiple arguments. " +
                        "Please, specify the type either with `by` or " +
                        "with `enrichment_for` annotation.",
                msgName
        );
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(ByOption.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
