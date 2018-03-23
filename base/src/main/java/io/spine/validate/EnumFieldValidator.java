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

package io.spine.validate;

import com.google.protobuf.Descriptors.EnumValueDescriptor;

/**
 * Validates fields of type {@link EnumValueDescriptor}.
 *
 * @author Dmitry Kashcheiev
 */
class EnumFieldValidator extends FieldValidator<EnumValueDescriptor> {

    /**
     * Creates a new validator instance.
     *
     * @param fieldContext the context of the field to validate
     * @param fieldValues  values to validate
     */
    EnumFieldValidator(FieldContext fieldContext, Object fieldValues) {
        super(fieldContext,
              FieldValidator.<EnumValueDescriptor>toValueList(fieldValues),
              false);
    }

    @Override
    protected boolean isValueNotSet(EnumValueDescriptor value) {
        final int intValue = value.getNumber();
        final boolean result = intValue <= 0;
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Performs no action since no special options are declared for the enum values validation.
     */
    @Override
    protected void validateOwnRules() {
        // NoOp
    }
}
