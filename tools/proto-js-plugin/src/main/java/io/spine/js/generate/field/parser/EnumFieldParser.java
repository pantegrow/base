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

import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import io.spine.code.js.TypeName;
import io.spine.js.generate.JsOutput;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The value parser for the proto fields of {@code enum} type.
 */
final class EnumFieldParser implements FieldParser {

    private final TypeName typeName;
    private final JsOutput jsOutput;

    private EnumFieldParser(TypeName typeName, JsOutput jsOutput) {
        this.typeName = typeName;
        this.jsOutput = jsOutput;
    }

    /**
     * Creates a new {@code EnumFieldParser} for the given field.
     *
     * @param field
     *         the processed field
     * @param jsOutput
     *         the {@code JsOutput} to store the generated code
     */
    static EnumFieldParser createFor(FieldDescriptor field, JsOutput jsOutput) {
        checkNotNull(field);
        checkNotNull(jsOutput);
        EnumDescriptor enumType = field.getEnumType();
        TypeName typeName = TypeName.from(enumType);
        return new EnumFieldParser(typeName, jsOutput);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The {@code enum} proto value in JSON is represented as a plain {@code string}. Thus, the
     * parser obtains the JS enum object property using the given {@code string} as an attribute
     * name.
     */
    @Override
    public void parseIntoVariable(String value, String variable) {
        checkNotNull(value);
        checkNotNull(variable);
        jsOutput.declareVariable(variable, typeName.value() + '[' + value + ']');
    }
}
