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

package io.spine.js.generate.field.precondition;

import com.google.protobuf.Descriptors.FieldDescriptor;
import io.spine.js.generate.JsOutput;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.code.js.Fields.isMessage;

/**
 * The helper which creates {@link FieldPrecondition} instances based on the field type.
 */
public final class FieldPreconditions {

    /** Prevents instantiation of this utility class. */
    private FieldPreconditions() {
    }

    /**
     * Creates a {@code FieldPrecondition} for the given {@code field}.
     *
     * @param field
     *         the descriptor of the Protobuf field to create the precondition for
     * @param jsOutput
     *         the {@code JsOutput} which will accumulate all the generated code
     * @return a {@code FieldPrecondition} of the appropriate type
     */
    public static FieldPrecondition preconditionFor(FieldDescriptor field, JsOutput jsOutput) {
        checkNotNull(field);
        checkNotNull(jsOutput);
        if (isMessage(field)) {
            return new MessagePrecondition(field, jsOutput);
        }
        return new PrimitivePrecondition(jsOutput);
    }
}
