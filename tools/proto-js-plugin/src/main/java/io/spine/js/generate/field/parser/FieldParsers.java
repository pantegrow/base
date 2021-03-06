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

package io.spine.js.generate.field.parser;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import io.spine.js.generate.JsOutput;
import io.spine.js.generate.type.ProtoParsersGenerator;
import io.spine.type.TypeUrl;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.code.js.Fields.isEnum;
import static io.spine.code.js.Fields.isMessage;

/**
 * A helper class which creates a {@link FieldParser} instance for the passed field.
 */
public final class FieldParsers {

    /** Prevents instantiation of this utility class. */
    private FieldParsers() {
    }

    /**
     * Creates a {@code FieldParser} for the given field.
     *
     * @param field
     *         the descriptor of the field to create the parser for
     * @param jsOutput
     *         the {@code JsOutput} to accumulate the generated code
     * @return the {@code FieldParser} of the appropriate type
     */
    public static FieldParser parserFor(FieldDescriptor field, JsOutput jsOutput) {
        checkNotNull(field);
        checkNotNull(jsOutput);
        if (isMessage(field)) {
            Descriptor message = field.getMessageType();
            TypeUrl typeUrl = TypeUrl.from(message);
            boolean isWellKnownType = ProtoParsersGenerator.hasParser(typeUrl);
            return isWellKnownType
                    ? WellKnownFieldParser.createFor(field, jsOutput)
                    : MessageFieldParser.createFor(field, jsOutput);
        }
        if (isEnum(field)) {
            return EnumFieldParser.createFor(field, jsOutput);
        }
        return PrimitiveFieldParser.createFor(field, jsOutput);
    }
}
