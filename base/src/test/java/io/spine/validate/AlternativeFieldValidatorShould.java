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

import com.google.protobuf.Descriptors.Descriptor;
import io.spine.test.validate.altfields.MessageWithMissingField;
import io.spine.test.validate.altfields.PersonName;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AlternativeFieldValidatorShould {

    private static final FieldContext EMPTY_CONTEXT = FieldContext.empty();

    private AlternativeFieldValidator validator;

    @Before
    public void setUp() {
        Descriptor descriptor = PersonName.getDescriptor();
        validator = new AlternativeFieldValidator(descriptor, EMPTY_CONTEXT);
    }

    @Test
    public void pass_if_one_field_populated() {
        PersonName fieldPopulated = PersonName.newBuilder()
                                              .setFirstName("Alexander")
                                              .build();
        List<? extends ConstraintViolation> violations = validator.validate(fieldPopulated);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void pass_if_combination_defined() {
        PersonName combinationDefined = PersonName.newBuilder()
                                                  .setHonorificPrefix("Mr.")
                                                  .setLastName("Yevsyukov")
                                                  .build();
        List<? extends ConstraintViolation> violations = validator.validate(combinationDefined);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void fail_if_nothing_defined() {
        PersonName empty = PersonName.getDefaultInstance();
        List<? extends ConstraintViolation> violations = validator.validate(empty);
        assertFalse(violations.isEmpty());
    }

    @Test
    public void fail_if_defined_not_required() {
        PersonName notRequiredPopulated = PersonName.newBuilder()
                                                    .setHonorificSuffix("I")
                                                    .build();
        List<? extends ConstraintViolation> violations = validator.validate(notRequiredPopulated);
        assertFalse(violations.isEmpty());
    }

    @Test
    public void report_missing_field() {
        AlternativeFieldValidator testee =
                new AlternativeFieldValidator(MessageWithMissingField.getDescriptor(),
                                              EMPTY_CONTEXT);
        MessageWithMissingField msg = MessageWithMissingField.newBuilder()
                                                             .setPresent(true)
                                                             .build();
        List<? extends ConstraintViolation> violations = testee.validate(msg);
        assertFalse(violations.isEmpty());
    }
}
