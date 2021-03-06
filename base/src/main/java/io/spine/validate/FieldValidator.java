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

package io.spine.validate;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.DescriptorProtos.FieldOptions;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.GeneratedMessage.GeneratedExtension;
import com.google.protobuf.Message;
import io.spine.base.CommandMessage;
import io.spine.base.FieldPath;
import io.spine.logging.Logging;
import io.spine.option.IfInvalidOption;
import io.spine.option.IfMissingOption;
import io.spine.option.OptionsProto;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Lists.newLinkedList;
import static io.spine.validate.rules.ValidationRuleOptions.getOptionValue;

/**
 * Validates messages according to Spine custom protobuf options and
 * provides constraint violations found.
 *
 * @param <V> a type of field values
 * @author Alexander Litus
 */
abstract class FieldValidator<V> implements Logging {

    private static final String ENTITY_ID_REPEATED_FIELD_MSG =
            "Entity ID must not be a repeated field.";

    private final FieldDescriptor fieldDescriptor;
    private final ImmutableList<V> values;
    private final FieldContext fieldContext;

    private final List<ConstraintViolation> violations = newLinkedList();

    private final boolean isCommandsFile;
    private final boolean isFirstField;
    private final boolean required;
    private final IfMissingOption ifMissingOption;
    private final boolean validate;
    private final IfInvalidOption ifInvalid;

    /**
     * If set the validator would assume that the field is required even
     * if the {@code required} option is not set.
     */
    private final boolean strict;

    /**
     * Creates a new validator instance.
     *
     * @param fieldContext the context of the field to validate
     * @param values       values to validate
     * @param strict       if {@code true} the validator would assume that the field
     *                     is required, even if corresponding field option is not present
     */
    protected FieldValidator(FieldContext fieldContext,
                             ImmutableList<V> values,
                             boolean strict) {
        this.fieldContext = checkNotNull(fieldContext);
        this.values = checkNotNull(values);
        this.fieldDescriptor = fieldContext.getTarget();
        this.strict = strict;
        FileDescriptor file = fieldDescriptor.getFile();
        this.isCommandsFile = CommandMessage.File.predicate()
                                                 .test(file);
        this.isFirstField = fieldDescriptor.getIndex() == 0;
        this.required = getFieldOption(OptionsProto.required);
        this.ifMissingOption = getFieldOption(OptionsProto.ifMissing);
        this.validate = getFieldOption(OptionsProto.valid);
        this.ifInvalid = getFieldOption(OptionsProto.ifInvalid);
    }

    @SuppressWarnings({
            "unchecked"               /* specific validator must call with its type */,
            "ChainOfInstanceofChecks" /* because fields do not have common parent class */
    })
    static <T> ImmutableList<T> toValueList(Object fieldValue) {
        if (fieldValue instanceof List) {
            List<T> value = (List<T>) fieldValue;
            return copyOf(value);
        } else if (fieldValue instanceof Map) {
            Map<?, T> map = (Map<?, T>) fieldValue;
            return copyOf(map.values());
        } else {
            T value = (T) fieldValue;
            return of(value);
        }
    }

    /**
     * Checks if the value of the validated field is not set.
     *
     * <p>Works for both repeated/map fields and ordinary single-value fields.
     *
     * @return {@code true} if the field value is not set and {@code false} otherwise
     */
    boolean fieldValueNotSet() {
        boolean valueNotSet =
                values.isEmpty()
                        || (isNotRepeatedOrMap() && isNotSet(values.get(0)));
        return valueNotSet;
    }

    /**
     * Checks if the specified field value is not set.
     *
     * <p>If the field type is {@link Message}, it must be set to a non-default instance;
     * if it is {@link String} or {@link com.google.protobuf.ByteString ByteString}, it must be
     * set to a non-empty string or array.
     *
     * @param value a field value to check
     * @return {@code true} if the field is not set, {@code false} otherwise
     */
    protected abstract boolean isNotSet(V value);

    /**
     * Validates messages according to Spine custom protobuf options and returns validation
     * constraint violations found.
     *
     * <p>This method defines the general flow of the field validation. Override
     * {@link #validateOwnRules()} to customize the validation behavior.
     *
     * <p>The flow of the validation is as follows:
     * <ol>
     *     <li>check the field to be set if it is {@code required};
     *     <li>validate the field as an Entity ID if required;
     *     <li>performs the {@linkplain #validateOwnRules() custom type-dependant validation}.
     * </ol>
     *
     * @return a list of found {@linkplain ConstraintViolation constraint violations} is any
     */
    protected final List<ConstraintViolation> validate() {
        checkIfRequiredAndNotSet();
        if (isRequiredEntityIdField()) {
            validateEntityId();
        }
        if (shouldValidate()) {
            validateOwnRules();
        }
        List<ConstraintViolation> result = assembleViolations();
        return result;
    }

    /**
     * Performs type-specific field validation.
     *
     * <p>Use {@link #addViolation(ConstraintViolation)} method in custom implementations.
     *
     * <p>Do not call this method directly. Use {@link #validate() validate()} instead.
     */
    protected abstract void validateOwnRules();

    private List<ConstraintViolation> assembleViolations() {
        return copyOf(violations);
    }

    /**
     * Validates the current field as it is a required entity ID.
     *
     * <p>The field must not be repeated or not set.
     *
     * @see #isRequiredEntityIdField()
     */
    protected void validateEntityId() {
        if (fieldDescriptor.isRepeated()) {
            ConstraintViolation violation = ConstraintViolation
                    .newBuilder()
                    .setMsgFormat(ENTITY_ID_REPEATED_FIELD_MSG)
                    .setFieldPath(getFieldPath())
                    .build();
            addViolation(violation);
            return;
        }
        if (fieldValueNotSet()) {
            addViolation(newViolation(ifMissingOption));
        }
    }

    /**
     * Returns {@code true} if the field has required attribute or validation is strict.
     */
    protected boolean isRequiredField() {
        boolean result = required || strict;
        return result;
    }

    /**
     * Returns {@code true} in case `if_missing` option is set with a non-default error message.
     */
    private boolean hasCustomMissingMessage() {
        boolean result = !ifMissingOption.equals(IfMissingOption.getDefaultInstance());
        return result;
    }

    /**
     * Checks if the field is required and not set and adds violations found.
     *
     * <p>If the field is repeated, it must have at least one value set, and all its values
     * must be valid.
     *
     * <p>It is required to override {@link #isNotSet(Object)} method to use this one.
     */
    protected void checkIfRequiredAndNotSet() {
        if (!isRequiredField()) {
            if (hasCustomMissingMessage()) {
                log().warn("'if_missing' option is set without '(required) = true'");
            }
            return;
        }
        if (fieldValueNotSet()) {
            addViolation(newViolation(ifMissingOption));
        }
    }

    /** Returns an immutable list of the field values. */
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // is immutable list
    protected ImmutableList<V> getValues() {
        return values;
    }

    /**
     * Adds a validation constraint validation to the collection of violations.
     *
     * @param violation a violation to add
     */
    protected void addViolation(ConstraintViolation violation) {
        violations.add(violation);
    }

    private ConstraintViolation newViolation(IfMissingOption option) {
        String msg = getErrorMsgFormat(option, option.getMsgFormat());
        ConstraintViolation violation = ConstraintViolation
                .newBuilder()
                .setMsgFormat(msg)
                .setFieldPath(getFieldPath())
                .build();
        return violation;
    }

    /**
     * Returns a validation error message (a custom one (if present) or the default one).
     *
     * @param option    a validation option used to get the default message
     * @param customMsg a user-defined error message
     */
    protected String getErrorMsgFormat(Message option, String customMsg) {
        String defaultMsg = option.getDescriptorForType()
                                  .getOptions()
                                  .getExtension(OptionsProto.defaultMessage);
        String msg = customMsg.isEmpty() ? defaultMsg : customMsg;
        return msg;
    }

    /**
     * Returns a field validation option.
     *
     * @param extension an extension key used to obtain a validation option
     * @param <T>       the type of the option
     */
    protected final <T> T getFieldOption(GeneratedExtension<FieldOptions, T> extension) {
        Optional<T> externalOption = getOptionValue(fieldContext, extension);
        if (externalOption.isPresent()) {
            return externalOption.get();
        }

        T ownOption = fieldDescriptor.getOptions()
                                     .getExtension(extension);
        return ownOption;
    }

    private boolean shouldValidate() {
        return isNotRepeatedOrMap() || validate;
    }

    final IfInvalidOption ifInvalid() {
        return ifInvalid;
    }

    final boolean getValidateOption() {
        return validate;
    }

    /**
     * Returns {@code true} if the field must be an entity ID
     * (if the field is the first in a command message), {@code false} otherwise.
     */
    private boolean isRequiredEntityIdField() {
        boolean result = isCommandsFile && isFirstField;
        return result;
    }

    private boolean isNotRepeatedOrMap() {
        return !fieldDescriptor.isRepeated()
                && !fieldDescriptor.isMapField();
    }

    /**
     * This test-only method is used from the module {@code smoke-tests}.
     */
    @SuppressWarnings("unused")
    @VisibleForTesting
    boolean isRepeatedOrMap() {
        return !isNotRepeatedOrMap();
    }

    /**
     * Obtains field context for the validator.
     *
     * @return the field context
     */
    protected FieldContext getFieldContext() {
        return fieldContext;
    }

    /** Returns a path to the current field. */
    protected FieldPath getFieldPath() {
        return fieldContext.getFieldPath();
    }
}
